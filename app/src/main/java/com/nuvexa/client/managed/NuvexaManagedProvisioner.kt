/*
 * Nuvexa Drive
 *
 * SPDX-FileCopyrightText: 2026 Nuvexa Drive contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nuvexa.client.managed

import android.net.Uri
import com.google.gson.JsonParser
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

data class NuvexaManagedCredentials(
    val server: String,
    val loginName: String,
    val password: String
)

/**
 * Creates the account material needed by the existing Nextcloud-compatible account flow.
 *
 * The public Nextcloud demo mode is intentionally QA-only. It creates a short-lived
 * account on the official trial service and must never be used as production storage.
 */
class NuvexaManagedProvisioner(
    private val provisioningUrl: String,
    private val provisioningType: String
) {

    fun provision(): NuvexaManagedCredentials =
        when (provisioningType) {
            TYPE_NEXTCLOUD_DEMO -> provisionOfficialNextcloudDemo()
            else -> throw IOException("Unsupported managed provisioning type")
        }

    private fun provisionOfficialNextcloudDemo(): NuvexaManagedCredentials {
        val baseUrl = provisioningUrl.trim()
        if (baseUrl != OFFICIAL_DEMO_URL) {
            throw IOException("Unexpected demo provisioning endpoint")
        }

        val landing = request(baseUrl)
        val csrfNameKey = inputValue(landing.body, "csrf-name-key")
        val csrfName = inputValue(landing.body, "csrf-name")
        val csrfValueKey = inputValue(landing.body, "csrf-value-key")
        val csrfValue = inputValue(landing.body, "csrf-value")

        val postBody = formPair(csrfNameKey, csrfName) + "&" + formPair(csrfValueKey, csrfValue)
        val trial = request(
            url = OFFICIAL_DEMO_CREATE_URL,
            method = "POST",
            body = postBody,
            cookieHeader = landing.cookies
        )

        val loginUrl = try {
            JsonParser.parseString(trial.body).asJsonObject.get("url")?.asString
        } catch (exception: Exception) {
            null
        } ?: throw IOException("Invalid demo provisioning response")

        val uri = Uri.parse(loginUrl)
        val scheme = uri.scheme
        val host = uri.host
        val authority = uri.encodedAuthority
        val username = uri.getQueryParameter("user")

        if (scheme != "https" ||
            host == null ||
            authority == null ||
            username.isNullOrBlank() ||
            !OFFICIAL_DEMO_HOST.matches(host)
        ) {
            throw IOException("Untrusted demo provisioning response")
        }

        return NuvexaManagedCredentials(
            server = "https://$authority",
            loginName = username,
            password = OFFICIAL_DEMO_PASSWORD
        )
    }

    private fun request(
        url: String,
        method: String = "GET",
        body: String? = null,
        cookieHeader: String? = null
    ): HttpResponse {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.instanceFollowRedirects = true
            connection.setRequestProperty("Accept", "text/html,application/json")
            connection.setRequestProperty("User-Agent", USER_AGENT)

            if (!cookieHeader.isNullOrBlank()) {
                connection.setRequestProperty("Cookie", cookieHeader)
            }

            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                connection.outputStream.use { stream ->
                    stream.write(body.toByteArray(StandardCharsets.UTF_8))
                }
            }

            val status = connection.responseCode
            val responseBody = (if (status in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader(StandardCharsets.UTF_8)
                ?.use { it.readText() }
                .orEmpty()

            if (status !in 200..299) {
                throw IOException("Managed provisioning HTTP $status")
            }

            val cookies = connection.headerFields
                .filterKeys { key -> key?.equals("Set-Cookie", ignoreCase = true) == true }
                .values
                .flatten()
                .map { header -> header.substringBefore(';').trim() }
                .filter { it.isNotBlank() }
                .joinToString("; ")
                .ifBlank { cookieHeader.orEmpty() }

            return HttpResponse(responseBody, cookies)
        } finally {
            connection.disconnect()
        }
    }

    private fun inputValue(html: String, name: String): String {
        val inputTag = Regex(
            """<input\b[^>]*\bname\s*=\s*["']${Regex.escape(name)}["'][^>]*>""",
            RegexOption.IGNORE_CASE
        ).find(html)?.value ?: throw IOException("Missing managed provisioning field")

        return Regex(
            """\bvalue\s*=\s*["']([^"']*)["']""",
            RegexOption.IGNORE_CASE
        ).find(inputTag)?.groupValues?.get(1)
            ?.replace("&amp;", "&")
            ?.replace("&quot;", "\"")
            ?.replace("&#39;", "'")
            ?: throw IOException("Missing managed provisioning value")
    }

    private fun formPair(name: String, value: String): String =
        encode(name) + "=" + encode(value)

    private fun encode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.name())

    private data class HttpResponse(
        val body: String,
        val cookies: String
    )

    private companion object {
        const val TYPE_NEXTCLOUD_DEMO = "nextcloud_demo"
        const val OFFICIAL_DEMO_URL = "https://try.nextcloud.com/"
        const val OFFICIAL_DEMO_CREATE_URL = "https://try.nextcloud.com/index.php/short-term"
        const val OFFICIAL_DEMO_PASSWORD = "demo"
        const val USER_AGENT = "Nuvexa-Drive/1.0 managed-qa"
        const val CONNECT_TIMEOUT_MS = 15_000
        const val READ_TIMEOUT_MS = 30_000

        val OFFICIAL_DEMO_HOST = Regex("""^demo\d+\.nextcloud\.com$""")
    }
}
