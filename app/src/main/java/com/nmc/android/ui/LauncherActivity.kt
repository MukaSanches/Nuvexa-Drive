/*
 * Nuvexa Drive
 *
 * SPDX-FileCopyrightText: 2023 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2023 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-FileCopyrightText: 2023-2024 TSI-mc <surinder.kumar@t-systems.com>
 * SPDX-FileCopyrightText: 2026 Nuvexa contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nmc.android.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.View
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.nuvexa.localserver.LocalDriveActivity
import com.owncloud.android.R
import com.owncloud.android.databinding.ActivitySplashBinding

/**
 * Nuvexa launcher.
 *
 * Nuvexa 1.0 opens the self-hosted local-drive mode directly. No external
 * Nextcloud installation, server URL, account or login is required to reach
 * the primary product experience.
 */
class LauncherActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        updateTitleVisibility()
        scheduleSplashScreen()
    }

    @VisibleForTesting
    fun setSplashTitles(boldText: String, normalText: String) {
        binding.splashScreenBold.visibility = View.VISIBLE
        binding.splashScreenNormal.visibility = View.VISIBLE
        binding.splashScreenBold.text = boldText
        binding.splashScreenNormal.text = normalText
    }

    private fun updateTitleVisibility() {
        if (TextUtils.isEmpty(resources.getString(R.string.splashScreenBold))) {
            binding.splashScreenBold.visibility = View.GONE
        }
        if (TextUtils.isEmpty(resources.getString(R.string.splashScreenNormal))) {
            binding.splashScreenNormal.visibility = View.GONE
        }
    }

    private fun scheduleSplashScreen() {
        Handler(Looper.getMainLooper()).postDelayed(
            {
                startActivity(Intent(this, LocalDriveActivity::class.java))
                finish()
            },
            SPLASH_DURATION
        )
    }

    companion object {
        const val SPLASH_DURATION = 900L
    }
}
