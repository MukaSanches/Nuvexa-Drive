/*
 * Nuvexa Drive
 *
 * SPDX-FileCopyrightText: 2026 Nuvexa Drive contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.jobs.upload

import com.nextcloud.client.device.BatteryStatus
import com.nextcloud.client.network.Connectivity
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TransferPolicyGuardTest {

    @Test
    fun wifiOnlyAllowsUnmeteredWifi() {
        assertNull(
            TransferPolicyGuard.blockingReason(
                wifiRequired = true,
                chargingRequired = false,
                ignorePowerSave = true,
                connectivity = Connectivity.CONNECTED_WIFI,
                battery = BatteryStatus(isCharging = false, level = 80),
                powerSavingEnabled = false
            )
        )
    }

    @Test
    fun wifiOnlyPausesWhenTransportChangesToCellular() {
        assertEquals(
            ResultCode.DELAYED_FOR_WIFI,
            TransferPolicyGuard.blockingReason(
                wifiRequired = true,
                chargingRequired = false,
                ignorePowerSave = true,
                connectivity = Connectivity(isConnected = true, isMetered = true, isWifi = false),
                battery = BatteryStatus(isCharging = false, level = 80),
                powerSavingEnabled = false
            )
        )
    }

    @Test
    fun wifiOnlyPausesOnMeteredWifi() {
        assertEquals(
            ResultCode.DELAYED_FOR_WIFI,
            TransferPolicyGuard.blockingReason(
                wifiRequired = true,
                chargingRequired = false,
                ignorePowerSave = true,
                connectivity = Connectivity(isConnected = true, isMetered = true, isWifi = true),
                battery = BatteryStatus(isCharging = false, level = 80),
                powerSavingEnabled = false
            )
        )
    }

    @Test
    fun chargingOnlyPausesWhenPowerIsRemoved() {
        assertEquals(
            ResultCode.DELAYED_FOR_CHARGING,
            TransferPolicyGuard.blockingReason(
                wifiRequired = false,
                chargingRequired = true,
                ignorePowerSave = true,
                connectivity = Connectivity.CONNECTED_WIFI,
                battery = BatteryStatus(isCharging = false, level = 80),
                powerSavingEnabled = false
            )
        )
    }

    @Test
    fun automaticUploadPausesInPowerSaveMode() {
        assertEquals(
            ResultCode.DELAYED_IN_POWER_SAVE_MODE,
            TransferPolicyGuard.blockingReason(
                wifiRequired = false,
                chargingRequired = false,
                ignorePowerSave = false,
                connectivity = Connectivity.CONNECTED_WIFI,
                battery = BatteryStatus(isCharging = false, level = 20),
                powerSavingEnabled = true
            )
        )
    }

    @Test
    fun userInitiatedUploadCanIgnorePowerSaveMode() {
        assertNull(
            TransferPolicyGuard.blockingReason(
                wifiRequired = false,
                chargingRequired = false,
                ignorePowerSave = true,
                connectivity = Connectivity.CONNECTED_WIFI,
                battery = BatteryStatus(isCharging = false, level = 20),
                powerSavingEnabled = true
            )
        )
    }
}
