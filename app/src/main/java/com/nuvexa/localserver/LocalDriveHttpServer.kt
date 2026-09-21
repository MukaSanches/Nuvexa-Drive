/*
 * Nuvexa Drive
 *
 * SPDX-FileCopyrightText: 2026 Nuvexa contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nuvexa.localserver

import android.content.Context
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.URLConnection
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class LocalDriveHttpServer(
    private val context: Context
) {
    private val running = AtomicBoolean(false)
    private val executor = Executors.newCachedThreadPool()
    private var serverSocket: ServerSocket? = null
    private val root: File = LocalDriveConfig.storageRoot(context)
    private val token: String = LocalDriveConfig.pairingToken(context)

    fun start(): Int {
        if (running.get()) return LocalDriveServerRuntime.port ?: LocalDriveConfig.DEFAULT_PORT

        var boundSocket: ServerSocket? = null
        var boundPort: Int? = null
        for (candidate in LocalDriveConfig.DEFAULT_PORT..LocalDriveConfig.MAX_PORT) {
            try {
                val socket = ServerSocket()
                socket.reuseAddress = true
                socket.bind(InetSocketAddress(candidate), 32)
                boundSocket = socket
                boundPort = candidate
                break
            } catch (_: IOException) {
                // Try the next local-only port.
            }
        }

        val socket = boundSocket ?: throw IOException("Nenhuma porta local livre entre 8787 e 8797.")
        serverSocket = socket
        running.set(true)
        LocalDriveServerRuntime.port = boundPort
        LocalDriveServerRuntime.running = true
        LocalDriveServerRuntime.lastError = null

        executor.execute {
            while (running.get()) {
                try {
                    val client = socket.accept()
                    client.soTimeout = SOCKET_TIMEOUT_MS
                    executor.execute { handleClient(client) }
                } catch (_: IOException) {
                    if (running.get()) {
                        LocalDriveServerRuntime.lastError = "Falha ao aceitar conexão local."
                    }
                }
            }
        }

        return boundPort ?: LocalDriveConfig.DEFAULT_PORT
    }

    fun stop() {
        running.set(false)
        LocalDriveServerRuntime.running = false
        LocalDriveServerRuntime.port = null
        runCatching { serverSocket?.close() }
        serverSocket = null
        executor.shutdownNow()
    }

    private fun handleClient(socket: Socket) {
        socket.use { client ->
            val input = BufferedInputStream(client.getInputStream(), BUFFER_SIZE)
            val output = BufferedOutputStream(client.getOutputStream(), BUFFER_SIZE)
            try {
                val requestLine = readAsciiLine(input, MAX_HEADER_LINE)
                    ?: return
                val requestParts = requestLine.split(' ')
                if (requestParts.size < 2) {
                    sendText(output, 400, "text/plain; charset=utf-8", "Requisição inválida.")
                    return
                }

                val method = requestParts[0].uppercase(Locale.US)
                val target = requestParts[1]
                val headers = linkedMapOf<String, String>()
                var headerBytes = requestLine.length
                while (true) {
                    val line = readAsciiLine(input, MAX_HEADER_LINE) ?: break
                    headerBytes += line.length
                    if (headerBytes > MAX_HEADER_BYTES) {
                        sendText(output, 431, "text/plain; charset=utf-8", "Cabeçalhos muito grandes.")
                        return
                    }
                    if (line.isEmpty()) break
                    val separator = line.indexOf(':')
                    if (separator > 0) {
                        headers[line.substring(0, separator).trim().lowercase(Locale.US)] =
                            line.substring(separator + 1).trim()
                    }
                }

                route(method, target, headers, input, output)
            } catch (_: IOException) {
                // Client disconnected. Nothing is persisted unless an atomic upload completed.
            } catch (_: Exception) {
                runCatching {
                    sendText(output, 500, "application/json; charset=utf-8", "{\"error\":\"internal\"}")
                }
            } finally {
                runCatching { output.flush() }
            }
        }
    }

    private fun route(
        method: String,
        target: String,
        headers: Map<String, String>,
        input: BufferedInputStream,
        output: BufferedOutputStream
    ) {
        val path = target.substringBefore('?')
        val query = parseQuery(target.substringAfter('?', ""))

        if (method == "GET" && path == "/") {
            val portuguese = headers["accept-language"]?.lowercase(Locale.US)?.startsWith("pt") == true
            sendText(output, 200, "text/html; charset=utf-8", portalHtml(portuguese))
            return
        }

        if (method == "GET" && path == "/favicon.ico") {
            sendBytes(output, 204, "image/x-icon", ByteArray(0))
            return
        }

        if (!authorized(headers, query)) {
            sendText(output, 401, "application/json; charset=utf-8", "{\"error\":\"unauthorized\"}")
            return
        }

        when {
            method == "GET" && path == "/api/status" -> {
                val ip = LocalDriveConfig.bestLanIpv4() ?: "127.0.0.1"
                val port = LocalDriveServerRuntime.port ?: LocalDriveConfig.DEFAULT_PORT
                val body = "{\"ok\":true,\"host\":\"" + jsonEscape(ip) + "\",\"port\":" + port + "}"
                sendText(output, 200, "application/json; charset=utf-8", body)
            }

            method == "GET" && path == "/api/files" -> {
                listFiles(output, query["path"])
            }

            method == "POST" && path == "/api/upload" -> {
                uploadFile(output, input, headers, query)
            }

            method == "POST" && path == "/api/mkdir" -> {
                createDirectory(output, query)
            }

            method == "DELETE" && path == "/api/file" -> {
                deleteEntry(output, query)
            }

            (method == "GET" || method == "HEAD") && path == "/file" -> {
                downloadFile(output, query, headOnly = method == "HEAD")
            }

            else -> sendText(output, 404, "application/json; charset=utf-8", "{\"error\":\"not_found\"}")
        }
    }

    private fun listFiles(output: BufferedOutputStream, relativePath: String?) {
        val directory = LocalDrivePathPolicy.resolveDirectory(root, relativePath)
        if (directory == null || !directory.exists() || !directory.isDirectory) {
            sendText(output, 404, "application/json; charset=utf-8", "{\"error\":\"directory_not_found\"}")
            return
        }

        val entries = directory.listFiles()
            ?.sortedWith(compareByDescending<File> { it.isDirectory }.thenBy { it.name.lowercase(Locale.getDefault()) })
            .orEmpty()

        val json = buildString {
            append("{\"path\":\"")
            append(jsonEscape(relativePath.orEmpty()))
            append("\",\"items\":[")
            entries.forEachIndexed { index, file ->
                if (index > 0) append(',')
                append("{\"name\":\"")
                append(jsonEscape(file.name))
                append("\",\"directory\":")
                append(file.isDirectory)
                append(",\"size\":")
                append(if (file.isFile) file.length() else 0)
                append(",\"modified\":")
                append(file.lastModified())
                append('}')
            }
            append("]}")
        }
        sendText(output, 200, "application/json; charset=utf-8", json)
    }

    private fun uploadFile(
        output: BufferedOutputStream,
        input: BufferedInputStream,
        headers: Map<String, String>,
        query: Map<String, String>
    ) {
        val name = query["name"] ?: ""
        val destination = LocalDrivePathPolicy.resolveChild(root, query["path"], name)
        val length = headers["content-length"]?.toLongOrNull()

        if (destination == null || length == null || length < 0 || length > MAX_UPLOAD_BYTES) {
            sendText(output, 400, "application/json; charset=utf-8", "{\"error\":\"invalid_upload\"}")
            return
        }

        destination.parentFile?.mkdirs()
        if (destination.parentFile?.usableSpace?.let { it < length + MIN_FREE_SPACE_BYTES } == true) {
            sendText(output, 507, "application/json; charset=utf-8", "{\"error\":\"insufficient_storage\"}")
            return
        }

        val temp = File(destination.parentFile, "." + destination.name + ".nuvexa-upload")
        runCatching { temp.delete() }

        try {
            FileOutputStream(temp).use { fileOutput ->
                var remaining = length
                val buffer = ByteArray(BUFFER_SIZE)
                while (remaining > 0) {
                    val read = input.read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt())
                    if (read < 0) throw IOException("Upload interrompido.")
                    fileOutput.write(buffer, 0, read)
                    remaining -= read
                }
                fileOutput.fd.sync()
            }

            if (temp.length() != length) throw IOException("Tamanho do upload divergente.")
            if (destination.exists() && !destination.delete()) throw IOException("Falha ao substituir arquivo existente.")
            if (!temp.renameTo(destination)) {
                temp.copyTo(destination, overwrite = true)
                if (!temp.delete()) temp.deleteOnExit()
            }

            sendText(
                output,
                201,
                "application/json; charset=utf-8",
                "{\"ok\":true,\"name\":\"" + jsonEscape(destination.name) + "\",\"size\":" + destination.length() + "}"
            )
        } catch (_: Exception) {
            runCatching { temp.delete() }
            sendText(output, 500, "application/json; charset=utf-8", "{\"error\":\"upload_failed\"}")
        }
    }

    private fun createDirectory(output: BufferedOutputStream, query: Map<String, String>) {
        val name = query["name"] ?: ""
        val directory = LocalDrivePathPolicy.resolveChild(root, query["path"], name)
        if (directory == null) {
            sendText(output, 400, "application/json; charset=utf-8", "{\"error\":\"invalid_name\"}")
            return
        }

        if (directory.exists()) {
            sendText(output, 409, "application/json; charset=utf-8", "{\"error\":\"already_exists\"}")
            return
        }

        if (directory.mkdirs()) {
            sendText(output, 201, "application/json; charset=utf-8", "{\"ok\":true}")
        } else {
            sendText(output, 500, "application/json; charset=utf-8", "{\"error\":\"mkdir_failed\"}")
        }
    }

    private fun deleteEntry(output: BufferedOutputStream, query: Map<String, String>) {
        val name = query["name"] ?: ""
        val entry = LocalDrivePathPolicy.resolveChild(root, query["path"], name)
        if (entry == null || !entry.exists()) {
            sendText(output, 404, "application/json; charset=utf-8", "{\"error\":\"not_found\"}")
            return
        }

        val deleted = if (entry.isDirectory) entry.deleteRecursively() else entry.delete()
        if (deleted) {
            sendText(output, 200, "application/json; charset=utf-8", "{\"ok\":true}")
        } else {
            sendText(output, 500, "application/json; charset=utf-8", "{\"error\":\"delete_failed\"}")
        }
    }

    private fun downloadFile(
        output: BufferedOutputStream,
        query: Map<String, String>,
        headOnly: Boolean
    ) {
        val name = query["name"] ?: ""
        val file = LocalDrivePathPolicy.resolveChild(root, query["path"], name)
        if (file == null || !file.exists() || !file.isFile) {
            sendText(output, 404, "text/plain; charset=utf-8", "Arquivo não encontrado.")
            return
        }

        val contentType = URLConnection.guessContentTypeFromName(file.name) ?: "application/octet-stream"
        val dispositionName = file.name.replace("\"", "_")
        writeHeaders(
            output,
            200,
            contentType,
            file.length(),
            mapOf("Content-Disposition" to "attachment; filename=\"" + dispositionName + "\"")
        )
        if (!headOnly) {
            file.inputStream().buffered().use { source -> source.copyTo(output, BUFFER_SIZE) }
        }
    }

    private fun authorized(headers: Map<String, String>, query: Map<String, String>): Boolean {
        val headerToken = headers["x-nuvexa-token"]
        val bearer = headers["authorization"]
            ?.takeIf { it.startsWith("Bearer ", ignoreCase = true) }
            ?.substring(7)
        val supplied = headerToken ?: bearer ?: query["token"]
        return supplied != null && constantTimeEquals(supplied, token)
    }

    private fun constantTimeEquals(left: String, right: String): Boolean {
        val a = left.toByteArray(StandardCharsets.UTF_8)
        val b = right.toByteArray(StandardCharsets.UTF_8)
        var diff = a.size xor b.size
        val max = maxOf(a.size, b.size)
        for (index in 0 until max) {
            val av = if (index < a.size) a[index].toInt() else 0
            val bv = if (index < b.size) b[index].toInt() else 0
            diff = diff or (av xor bv)
        }
        return diff == 0
    }

    private fun parseQuery(raw: String): Map<String, String> {
        if (raw.isBlank()) return emptyMap()
        return raw.split('&').mapNotNull { pair ->
            if (pair.isBlank()) return@mapNotNull null
            val key = decodeUrl(pair.substringBefore('='))
            val value = decodeUrl(pair.substringAfter('=', ""))
            key to value
        }.toMap()
    }

    private fun decodeUrl(value: String): String =
        runCatching { URLDecoder.decode(value, StandardCharsets.UTF_8.name()) }.getOrDefault("")

    private fun readAsciiLine(input: BufferedInputStream, maxLength: Int): String? {
        val bytes = ArrayList<Byte>()
        while (bytes.size <= maxLength) {
            val value = input.read()
            if (value == -1) return if (bytes.isEmpty()) null else bytes.toByteArray().toString(StandardCharsets.ISO_8859_1)
            if (value == '\n'.code) break
            if (value != '\r'.code) bytes.add(value.toByte())
        }
        if (bytes.size > maxLength) throw IOException("Linha HTTP grande demais.")
        return bytes.toByteArray().toString(StandardCharsets.ISO_8859_1)
    }

    private fun sendText(
        output: BufferedOutputStream,
        status: Int,
        contentType: String,
        text: String
    ) {
        sendBytes(output, status, contentType, text.toByteArray(StandardCharsets.UTF_8))
    }

    private fun sendBytes(
        output: BufferedOutputStream,
        status: Int,
        contentType: String,
        bytes: ByteArray
    ) {
        writeHeaders(output, status, contentType, bytes.size.toLong())
        output.write(bytes)
    }

    private fun writeHeaders(
        output: BufferedOutputStream,
        status: Int,
        contentType: String,
        contentLength: Long,
        extra: Map<String, String> = emptyMap()
    ) {
        val statusText = when (status) {
            200 -> "OK"
            201 -> "Created"
            204 -> "No Content"
            400 -> "Bad Request"
            401 -> "Unauthorized"
            404 -> "Not Found"
            409 -> "Conflict"
            431 -> "Request Header Fields Too Large"
            500 -> "Internal Server Error"
            507 -> "Insufficient Storage"
            else -> "Status"
        }
        val header = buildString {
            append("HTTP/1.1 ")
            append(status)
            append(' ')
            append(statusText)
            append("\r\n")
            append("Content-Type: ")
            append(contentType)
            append("\r\n")
            append("Content-Length: ")
            append(contentLength)
            append("\r\n")
            append("Cache-Control: no-store\r\n")
            append("X-Content-Type-Options: nosniff\r\n")
            append("Referrer-Policy: no-referrer\r\n")
            append("Content-Security-Policy: default-src 'self' 'unsafe-inline'; connect-src 'self'\r\n")
            extra.forEach { (key, value) ->
                append(key)
                append(": ")
                append(value)
                append("\r\n")
            }
            append("Connection: close\r\n\r\n")
        }
        output.write(header.toByteArray(StandardCharsets.ISO_8859_1))
        output.flush()
    }

    private fun jsonEscape(value: String): String =
        buildString {
            value.forEach { char ->
                when (char) {
                    '\\\\' -> append("\\\\\\\\")
                    '"' -> append("\\\\\"")
                    '\n' -> append("\\\\n")
                    '\r' -> append("\\\\r")
                    '\t' -> append("\\\\t")
                    else -> if (char.code < 32) append('?') else append(char)
                }
            }
        }

    private fun portalHtml(portuguese: Boolean): String {
        val title = if (portuguese) "Nuvexa Drive local" else "Nuvexa Drive local"
        val subtitle = if (portuguese) {
            "Arquivos servidos diretamente por este celular. Nenhuma nuvem externa é necessária."
        } else {
            "Files are served directly by this phone. No external cloud is required."
        }
        val upload = if (portuguese) "Enviar arquivos" else "Upload files"
        val folder = if (portuguese) "Nova pasta" else "New folder"
        val refresh = if (portuguese) "Atualizar" else "Refresh"
        val up = if (portuguese) "Voltar uma pasta" else "Up one folder"
        val empty = if (portuguese) "Esta pasta está vazia." else "This folder is empty."
        val deleting = if (portuguese) "Excluir este item?" else "Delete this item?"

        return """
<!doctype html>
<html lang="pt-BR">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>$title</title>
<style>
:root{color-scheme:dark;background:#07111d;color:#ecf5ff;font-family:system-ui,-apple-system,sans-serif}
body{max-width:980px;margin:auto;padding:28px 18px 60px}
header{padding:22px;border:1px solid #1f3448;border-radius:20px;background:#0c1928}
h1{margin:0;color:#58d2dd;font-size:28px}p{color:#a9bbcc;line-height:1.5}
.toolbar{display:flex;gap:10px;flex-wrap:wrap;margin:20px 0}
button,label.btn{border:1px solid #2c5268;background:#102536;color:#eefaff;padding:11px 15px;border-radius:12px;cursor:pointer}
button.primary,label.primary{background:#12646b;border-color:#43c4cf}
input[type=file]{display:none}
#path{font-family:ui-monospace,monospace;color:#68d9df;margin:12px 0;word-break:break-all}
.item{display:grid;grid-template-columns:1fr auto auto;gap:10px;align-items:center;padding:13px 14px;border-bottom:1px solid #172b3b}
.item:hover{background:#0b1b2a}.name{overflow:hidden;text-overflow:ellipsis}.meta{color:#8296a8;font-size:13px}
.card{border:1px solid #1f3448;border-radius:16px;overflow:hidden;background:#091623}
#status{min-height:22px;color:#69d9a6}.danger{border-color:#62333a;color:#ffb4b9}
a{color:#66d9e2}
</style>
</head>
<body>
<header><h1>Nuvexa Drive</h1><p>$subtitle</p><div id="status"></div></header>
<div class="toolbar">
<label class="btn primary">$upload<input id="picker" type="file" multiple></label>
<button id="mkdir">$folder</button>
<button id="up">$up</button>
<button id="refresh">$refresh</button>
</div>
<div id="path">/</div>
<div class="card" id="list"></div>
<script>
const q=new URLSearchParams(location.search);
const token=q.get('token')||localStorage.getItem('nuvexaToken')||'';
if(token){localStorage.setItem('nuvexaToken',token)}
let path='';
const list=document.getElementById('list');
const statusEl=document.getElementById('status');
function qp(v){return encodeURIComponent(v||'')}
async function api(url,opt={}){
  opt.headers=Object.assign({},opt.headers||{}, {'X-Nuvexa-Token':token});
  const r=await fetch(url,opt);
  if(!r.ok){throw new Error('HTTP '+r.status)}
  const type=r.headers.get('content-type')||'';
  return type.includes('json')?r.json():r.text();
}
function bytes(n){if(n<1024)return n+' B';if(n<1048576)return (n/1024).toFixed(1)+' KB';if(n<1073741824)return (n/1048576).toFixed(1)+' MB';return (n/1073741824).toFixed(1)+' GB'}
async function refresh(){
  statusEl.textContent='';
  document.getElementById('path').textContent='/'+path;
  try{
    const data=await api('/api/files?path='+qp(path));
    list.innerHTML='';
    if(!data.items.length){list.textContent='$empty';return}
    data.items.forEach(item=>{
      const row=document.createElement('div');row.className='item';
      const name=document.createElement('div');name.className='name';name.textContent=(item.directory?'📁 ':'📄 ')+item.name;
      const meta=document.createElement('div');meta.className='meta';meta.textContent=item.directory?'':bytes(item.size);
      const actions=document.createElement('div');
      if(item.directory){
        const open=document.createElement('button');open.textContent='Abrir';open.onclick=()=>{path=path?path+'/'+item.name:item.name;refresh()};actions.appendChild(open)
      }else{
        const down=document.createElement('button');down.textContent='Baixar';down.onclick=()=>{location.href='/file?path='+qp(path)+'&name='+qp(item.name)+'&token='+qp(token)};actions.appendChild(down)
      }
      const del=document.createElement('button');del.textContent='Excluir';del.className='danger';del.onclick=async()=>{if(confirm('$deleting')){await api('/api/file?path='+qp(path)+'&name='+qp(item.name),{method:'DELETE'});refresh()}};actions.appendChild(del);
      row.append(name,meta,actions);list.appendChild(row);
    })
  }catch(e){statusEl.textContent='Acesso negado ou servidor indisponível. Abra o link completo fornecido pelo Nuvexa.'}
}
document.getElementById('refresh').onclick=refresh;
document.getElementById('up').onclick=()=>{const p=path.split('/').filter(Boolean);p.pop();path=p.join('/');refresh()};
document.getElementById('mkdir').onclick=async()=>{const n=prompt('$folder');if(n){await api('/api/mkdir?path='+qp(path)+'&name='+qp(n),{method:'POST'});refresh()}};
document.getElementById('picker').onchange=async e=>{
  for(const f of e.target.files){
    statusEl.textContent='Enviando '+f.name+'...';
    await api('/api/upload?path='+qp(path)+'&name='+qp(f.name),{method:'POST',headers:{'Content-Type':'application/octet-stream'},body:f});
  }
  statusEl.textContent='Concluído.';e.target.value='';refresh();
};
refresh();
</script>
</body>
</html>
""".trimIndent()
    }

    companion object {
        private const val SOCKET_TIMEOUT_MS = 30_000
        private const val BUFFER_SIZE = 64 * 1024
        private const val MAX_HEADER_LINE = 16 * 1024
        private const val MAX_HEADER_BYTES = 64 * 1024
        private const val MAX_UPLOAD_BYTES = 8L * 1024L * 1024L * 1024L
        private const val MIN_FREE_SPACE_BYTES = 5L * 1024L * 1024L
    }
}
