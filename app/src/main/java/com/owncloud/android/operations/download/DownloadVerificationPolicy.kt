/*
 * Nuvexa Drive
 *
 * SPDX-FileCopyrightText: 2026 Nuvexa Drive contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.operations.download

/**
 * Integrity gate for promoting a temporary download into the user's final file.
 *
 * A positive expected size is authoritative. A zero/unknown metadata size is
 * treated conservatively because some servers may not expose a useful length.
 */
object DownloadVerificationPolicy {
    fun canPromote(expectedSize: Long, downloadedSize: Long): Boolean {
        if (downloadedSize < 0L) return false
        return expectedSize <= 0L || expectedSize == downloadedSize
    }
}
