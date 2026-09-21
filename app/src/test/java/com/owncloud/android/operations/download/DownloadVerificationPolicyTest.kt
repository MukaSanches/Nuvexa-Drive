/*
 * Nuvexa Drive
 *
 * SPDX-FileCopyrightText: 2026 Nuvexa Drive contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.operations.download

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadVerificationPolicyTest {

    @Test
    fun exactKnownSizeCanBePromoted() {
        assertTrue(DownloadVerificationPolicy.canPromote(8_192L, 8_192L))
    }

    @Test
    fun partialKnownDownloadCannotBePromoted() {
        assertFalse(DownloadVerificationPolicy.canPromote(8_192L, 4_096L))
    }

    @Test
    fun zeroBytePartialCannotReplaceKnownNonEmptyFile() {
        assertFalse(DownloadVerificationPolicy.canPromote(8_192L, 0L))
    }

    @Test
    fun unknownServerSizeDoesNotBlockValidTemporaryFile() {
        assertTrue(DownloadVerificationPolicy.canPromote(0L, 4_096L))
    }

    @Test
    fun invalidNegativeDownloadedSizeCannotBePromoted() {
        assertFalse(DownloadVerificationPolicy.canPromote(8_192L, -1L))
    }
}
