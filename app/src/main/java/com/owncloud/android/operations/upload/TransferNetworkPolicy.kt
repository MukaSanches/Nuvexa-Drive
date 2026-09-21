/*
 * Nuvexa Drive
 *
 * SPDX-FileCopyrightText: 2026 Nuvexa Drive contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.operations.upload

/**
 * Pure transfer-network policy so start-time and in-flight checks cannot drift.
 */
object TransferNetworkPolicy {
    fun canTransfer(wifiOnly: Boolean, isWifi: Boolean, isMetered: Boolean): Boolean =
        !wifiOnly || (isWifi && !isMetered)
}
