/*
 * Nuvexa Drive
 *
 * SPDX-FileCopyrightText: 2026 Nuvexa contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nuvexa.localserver

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.owncloud.android.R
import java.io.File
import java.text.DateFormat
import java.util.Date
import java.util.Locale

class LocalDriveActivity : AppCompatActivity() {
    private lateinit var statusView: TextView
    private lateinit var addressView: TextView
    private lateinit var storageView: TextView
    private lateinit var fileList: LinearLayout

    private val picker = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) {
            var imported = 0
            uris.forEach { uri ->
                if (importDocument(uri)) imported++
            }
            statusView.text = resources.getQuantityString(
                R.plurals.nuvexa_local_imported_files,
                imported,
                imported
            )
            refresh()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.nuvexa_local_title)
        setContentView(buildContent())
        LocalDriveServerService.start(this)
        window.decorView.postDelayed({ refresh() }, 350)
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun buildContent(): View {
        val outer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(28))
            setBackgroundColor(Color.rgb(7, 17, 29))
        }

        outer.addView(text(getString(R.string.nuvexa_local_title), 28f, Color.rgb(88, 210, 221)))
        outer.addView(text(getString(R.string.nuvexa_local_subtitle), 15f, Color.rgb(176, 194, 208)))
        outer.addView(space(12))

        statusView = text("", 14f, Color.rgb(105, 217, 166))
        outer.addView(statusView)

        addressView = text("", 15f, Color.WHITE).apply {
            setTextIsSelectable(true)
            setPadding(0, dp(10), 0, dp(10))
        }
        outer.addView(addressView)

        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.START
        }
        actions.addView(button(getString(R.string.nuvexa_local_copy_address)) { copyAddress() })
        actions.addView(button(getString(R.string.nuvexa_local_import)) {
            picker.launch(arrayOf("*/*"))
        })
        outer.addView(actions)

        val secondActions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.START
        }
        secondActions.addView(button(getString(R.string.nuvexa_local_new_folder)) { askNewFolder() })
        secondActions.addView(button(getString(R.string.nuvexa_local_rotate_token)) { rotateToken() })
        secondActions.addView(button(getString(R.string.nuvexa_local_stop_server)) {
            LocalDriveServerService.stop(this@LocalDriveActivity)
            window.decorView.postDelayed({ refresh() }, 250)
        })
        outer.addView(secondActions)

        outer.addView(space(12))
        storageView = text("", 13f, Color.rgb(140, 160, 177))
        outer.addView(storageView)

        outer.addView(space(14))
        outer.addView(text(getString(R.string.nuvexa_local_files), 20f, Color.WHITE))

        val scroll = ScrollView(this).apply {
            isFillViewport = true
        }
        fileList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        scroll.addView(fileList)
        outer.addView(
            scroll,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        return outer
    }

    private fun refresh() {
        val running = LocalDriveServerRuntime.running
        val error = LocalDriveServerRuntime.lastError
        statusView.text = when {
            running -> getString(R.string.nuvexa_local_server_running)
            error != null -> getString(R.string.nuvexa_local_server_error, error)
            else -> getString(R.string.nuvexa_local_server_starting)
        }

        addressView.text = if (running) {
            LocalDriveConfig.accessUrl(this)
        } else {
            getString(R.string.nuvexa_local_server_no_address)
        }

        val root = LocalDriveConfig.storageRoot(this)
        val used = root.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        storageView.text = getString(
            R.string.nuvexa_local_storage_summary,
            humanSize(used),
            root.absolutePath
        )

        renderFiles(root)
    }

    private fun renderFiles(root: File) {
        fileList.removeAllViews()
        val entries = root.listFiles()
            ?.sortedWith(compareByDescending<File> { it.isDirectory }.thenBy { it.name.lowercase(Locale.getDefault()) })
            .orEmpty()

        if (entries.isEmpty()) {
            fileList.addView(text(getString(R.string.nuvexa_local_empty), 15f, Color.rgb(156, 176, 193)))
            return
        }

        entries.forEach { file ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(9), 0, dp(9))
            }

            val label = text(
                (if (file.isDirectory) "📁 " else "📄 ") + file.name +
                    if (file.isFile) "\n" + humanSize(file.length()) + " • " +
                        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                            .format(Date(file.lastModified()))
                    else "",
                14f,
                Color.WHITE
            )
            row.addView(
                label,
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            )

            row.addView(button(getString(R.string.nuvexa_local_delete)) {
                confirmDelete(file)
            })
            fileList.addView(row)
        }
    }

    private fun copyAddress() {
        if (!LocalDriveServerRuntime.running) return
        val clipboard = ContextCompat.getSystemService(this, ClipboardManager::class.java) ?: return
        clipboard.setPrimaryClip(
            ClipData.newPlainText(
                getString(R.string.nuvexa_local_copy_address),
                LocalDriveConfig.accessUrl(this)
            )
        )
        statusView.text = getString(R.string.nuvexa_local_address_copied)
    }

    private fun rotateToken() {
        LocalDriveConfig.rotatePairingToken(this)
        LocalDriveServerService.stop(this)
        window.decorView.postDelayed(
            {
                LocalDriveServerService.start(this)
                window.decorView.postDelayed({ refresh() }, 350)
            },
            250
        )
        statusView.text = getString(R.string.nuvexa_local_token_rotated)
    }

    private fun askNewFolder() {
        val input = EditText(this).apply {
            hint = getString(R.string.nuvexa_local_folder_name)
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.nuvexa_local_new_folder)
            .setView(input)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val name = LocalDrivePathPolicy.safeLeafName(input.text.toString())
                val root = LocalDriveConfig.storageRoot(this)
                if (name == null) {
                    statusView.text = getString(R.string.nuvexa_local_invalid_name)
                } else {
                    val created = File(root, name).mkdir()
                    statusView.text = if (created) {
                        getString(R.string.nuvexa_local_folder_created)
                    } else {
                        getString(R.string.nuvexa_local_folder_not_created)
                    }
                    refresh()
                }
            }
            .show()
    }

    private fun confirmDelete(file: File) {
        AlertDialog.Builder(this)
            .setTitle(R.string.nuvexa_local_delete)
            .setMessage(getString(R.string.nuvexa_local_delete_confirm, file.name))
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.nuvexa_local_delete) { _, _ ->
                val deleted = if (file.isDirectory) file.deleteRecursively() else file.delete()
                statusView.text = if (deleted) {
                    getString(R.string.nuvexa_local_deleted)
                } else {
                    getString(R.string.nuvexa_local_delete_failed)
                }
                refresh()
            }
            .show()
    }

    private fun importDocument(uri: Uri): Boolean {
        val displayName = queryDisplayName(uri) ?: "arquivo"
        val safeName = LocalDrivePathPolicy.safeLeafName(displayName) ?: return false
        val root = LocalDriveConfig.storageRoot(this)
        val destination = uniqueDestination(root, safeName)
        val temp = File(root, "." + destination.name + ".importing")

        return runCatching {
            contentResolver.openInputStream(uri)?.use { input ->
                temp.outputStream().buffered().use { output -> input.copyTo(output) }
            } ?: error("Não foi possível abrir o arquivo.")
            if (destination.exists() && !destination.delete()) error("Destino indisponível.")
            if (!temp.renameTo(destination)) {
                temp.copyTo(destination, overwrite = true)
                temp.delete()
            }
            true
        }.getOrElse {
            temp.delete()
            false
        }
    }

    private fun queryDisplayName(uri: Uri): String? {
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) return cursor.getString(index)
            }
        }
        return uri.lastPathSegment?.substringAfterLast('/')
    }

    private fun uniqueDestination(root: File, name: String): File {
        var candidate = File(root, name)
        if (!candidate.exists()) return candidate

        val dot = name.lastIndexOf('.')
        val base = if (dot > 0) name.substring(0, dot) else name
        val extension = if (dot > 0) name.substring(dot) else ""
        var index = 2
        while (candidate.exists()) {
            candidate = File(root, base + " (" + index + ")" + extension)
            index++
        }
        return candidate
    }

    private fun button(label: String, action: () -> Unit): Button =
        Button(this).apply {
            text = label
            isAllCaps = false
            setOnClickListener { action() }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = dp(8)
                bottomMargin = dp(8)
            }
        }

    private fun text(value: String, sizeSp: Float, color: Int): TextView =
        TextView(this).apply {
            text = value
            textSize = sizeSp
            setTextColor(color)
        }

    private fun space(height: Int): View =
        View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(height)
            )
        }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun humanSize(bytes: Long): String =
        when {
            bytes < 1024L -> bytes.toString() + " B"
            bytes < 1024L * 1024L -> String.format(Locale.getDefault(), "%.1f KB", bytes / 1024.0)
            bytes < 1024L * 1024L * 1024L -> String.format(Locale.getDefault(), "%.1f MB", bytes / (1024.0 * 1024.0))
            else -> String.format(Locale.getDefault(), "%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        }
}
