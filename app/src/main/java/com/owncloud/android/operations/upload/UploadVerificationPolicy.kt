/*
 * Nuvexa Drive
 *
 * SPDX-FileCopyrightText: 2026 Nuvexa Drive contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.operations.upload

/**
 * Data-integrity gate used after the transport reports a successful upload.
 *
 * A successful HTTP/chunk operation is not enough for Nuvexa to perform
 * destructive local actions. The remote object must also be readable and have
 * the exact expected byte length.
 */
object UploadVerificationPolicy {
    fun matchesExpectedSize(expectedSize: Long, remoteSize: Long): Boolean =
        expectedSize >= 0L && remoteSize >= 0L && expectedSize == remoteSize
}
