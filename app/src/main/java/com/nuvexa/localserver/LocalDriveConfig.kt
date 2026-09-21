/*
 * Nuvexa Drive
 *
 * SPDX-FileCopyrightText: 2026 Nuvexa contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nuvexa.localserver

import android.content.Context
import java.io.File
import java.net.NetworkInterface
import java.security.SecureRandom
import java.util.Collections

object LocalDriveConfig {
    const val DEFAULT_PORT = 8787
    const val MAX_PORT = 8797
    private const val PREFS = "nuvexa_local_server"
    private const val TOKEN_KEY = "pairing_token"

    fun storageRoot(context: Context): File =
        File(context.filesDir, "nuvexa-drive").apply { mkdirs() }

    fun pairingToken(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.getString(TOKEN_KEY, null)?.let { if (it.length >= 32) return it }

        val bytes = ByteArray(24)
        SecureRandom().nextBytes(bytes)
        val token = bytes.joinToString("") { "%02x".format(it) }
        prefs.edit().putString(TOKEN_KEY, token).apply()
        return token
    }

    fun rotatePairingToken(context: Context): String {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(TOKEN_KEY)
            .apply()
        return pairingToken(context)
    }

    fun bestLanIpv4(): String? =
        runCatching {
            Collections.list(NetworkInterface.getNetworkInterfaces())
                .asSequence()
                .filter { it.isUp && !it.isLoopback && !it.isVirtual }
                .flatMap { Collections.list(it.inetAddresses).asSequence() }
                .firstOrNull { address ->
                    !address.isLoopbackAddress &&
                        address.hostAddress?.contains(':') == false &&
                        address.isSiteLocalAddress
                }
                ?.hostAddress
        }.getOrNull()

    fun accessUrl(context: Context): String {
        val host = bestLanIpv4() ?: "127.0.0.1"
        val port = LocalDriveServerRuntime.port ?: DEFAULT_PORT
        return "http://" + host + ":" + port + "/?token=" + pairingToken(context)
    }
}

object LocalDriveServerRuntime {
    @Volatile
    var port: Int? = null

    @Volatile
    var running: Boolean = false

    @Volatile
    var lastError: String? = null
}
