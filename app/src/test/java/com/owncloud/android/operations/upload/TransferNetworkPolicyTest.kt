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

class TransferNetworkPolicyTest {

    @Test
    fun unrestrictedTransferCanUseMobileNetwork() {
        assertTrue(TransferNetworkPolicy.canTransfer(false, false, true))
    }

    @Test
    fun wifiOnlyAllowsUnmeteredWifi() {
        assertTrue(TransferNetworkPolicy.canTransfer(true, true, false))
    }

    @Test
    fun wifiOnlyRejectsMobileNetwork() {
        assertFalse(TransferNetworkPolicy.canTransfer(true, false, true))
    }

    @Test
    fun wifiOnlyRejectsMeteredWifi() {
        assertFalse(TransferNetworkPolicy.canTransfer(true, true, true))
    }
}
