/*
 * Nuvexa Drive
 *
 * SPDX-FileCopyrightText: 2026 Nuvexa contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nuvexa.localserver

import java.io.File
import java.text.Normalizer

object LocalDrivePathPolicy {
    private const val MAX_NAME_LENGTH = 180

    fun safeLeafName(raw: String): String? {
        val normalized = Normalizer.normalize(raw.trim(), Normalizer.Form.NFC)
        if (normalized.isBlank() || normalized == "." || normalized == "..") return null
        if (normalized.length > MAX_NAME_LENGTH) return null
        if (normalized.any { it == '/' || it == '\\' || it == '\u0000' || it.code < 32 }) return null
        return normalized
    }

    fun resolveDirectory(root: File, relativePath: String?): File? {
        val rootCanonical = root.canonicalFile
        if (relativePath.isNullOrBlank()) return rootCanonical

        var current = rootCanonical
        relativePath.split('/').filter { it.isNotBlank() }.forEach { part ->
            val safe = safeLeafName(part) ?: return null
            current = File(current, safe)
        }

        val canonical = current.canonicalFile
        return canonical.takeIf { it == rootCanonical || it.path.startsWith(rootCanonical.path + File.separator) }
    }

    fun resolveChild(root: File, relativeDirectory: String?, leafName: String): File? {
        val safe = safeLeafName(leafName) ?: return null
        val parent = resolveDirectory(root, relativeDirectory) ?: return null
        val candidate = File(parent, safe).canonicalFile
        val rootCanonical = root.canonicalFile
        return candidate.takeIf { it.path.startsWith(rootCanonical.path + File.separator) }
    }
}
