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

/**
 * Evaluates constraints that can change while an upload is already transferring bytes.
 *
 * WorkManager constraints are necessary but not sufficient for an active HTTP transfer because Android
 * can change the default transport while the worker remains alive. This guard is deliberately pure so
 * policy behavior is deterministic and unit-testable.
 */
internal object TransferPolicyGuard {

    fun blockingReason(
        wifiRequired: Boolean,
        chargingRequired: Boolean,
        ignorePowerSave: Boolean,
        connectivity: Connectivity,
        battery: BatteryStatus,
        powerSavingEnabled: Boolean
    ): ResultCode? {
        if (wifiRequired && (!connectivity.isWifi || connectivity.isMetered)) {
            return ResultCode.DELAYED_FOR_WIFI
        }

        if (chargingRequired && !battery.isCharging) {
            return ResultCode.DELAYED_FOR_CHARGING
        }

        if (!ignorePowerSave && powerSavingEnabled) {
            return ResultCode.DELAYED_IN_POWER_SAVE_MODE
        }

        return null
    }
}
