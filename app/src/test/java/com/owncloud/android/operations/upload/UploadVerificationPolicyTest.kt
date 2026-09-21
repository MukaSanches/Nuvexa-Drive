/*
 * Nuvexa Drive
 *
 * SPDX-FileCopyrightText: 2026 Nuvexa Drive contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.operations.upload

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UploadVerificationPolicyTest {

    @Test
    fun exactSizeIsVerified() {
        assertTrue(UploadVerificationPolicy.matchesExpectedSize(4_096L, 4_096L))
    }

    @Test
    fun zeroByteRemoteDoesNotVerifyNonEmptySource() {
        assertFalse(UploadVerificationPolicy.matchesExpectedSize(4_096L, 0L))
    }

    @Test
    fun truncatedRemoteDoesNotVerify() {
        assertFalse(UploadVerificationPolicy.matchesExpectedSize(4_096L, 2_048L))
    }

    @Test
    fun emptySourceCanVerifyAsEmptyRemote() {
        assertTrue(UploadVerificationPolicy.matchesExpectedSize(0L, 0L))
    }

    @Test
    fun unknownNegativeSizesDoNotVerify() {
        assertFalse(UploadVerificationPolicy.matchesExpectedSize(-1L, -1L))
    }
}
