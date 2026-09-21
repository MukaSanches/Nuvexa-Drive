/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2023 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-FileCopyrightText: 2023-2024 TSI-mc <surinder.kumar@t-systems.com>
 * SPDX-FileCopyrightText: 2026 Nuvexa Drive contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nmc.android.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.View
import androidx.annotation.VisibleForTesting
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.nextcloud.client.preferences.AppPreferences
import com.nextcloud.utils.mdm.MDMConfig
import com.nuvexa.client.managed.NuvexaManagedCredentials
import com.nuvexa.client.managed.NuvexaManagedProvisioner
import com.owncloud.android.R
import com.owncloud.android.authentication.AuthenticatorActivity
import com.owncloud.android.databinding.ActivitySplashBinding
import com.owncloud.android.ui.activity.BaseActivity
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.ui.activity.SettingsActivity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class LauncherActivity : BaseActivity() {

    private lateinit var binding: ActivitySplashBinding
    private var managedProvisioningStarted = false

    @Inject
    lateinit var appPreferences: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        // Mandatory to call this before super method to show system launch screen for api level 31+
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
        if (!user.isPresent && resources.getBoolean(R.bool.nuvexa_managed_mode)) {
            startManagedProvisioning()
            return
        }

        Handler(Looper.getMainLooper()).postDelayed({
            openRegularDestination()
        }, SPLASH_DURATION)
    }

    private fun openRegularDestination() {
        if (user.isPresent) {
            if (MDMConfig.enforceProtection(this) && appPreferences.lockPreference == SettingsActivity.LOCK_NONE) {
                startActivity(Intent(this, SettingsActivity::class.java))
            } else {
                startActivity(Intent(this, FileDisplayActivity::class.java))
            }
        } else {
            startActivity(Intent(this, AuthenticatorActivity::class.java))
        }
        finish()
    }

    private fun startManagedProvisioning() {
        if (managedProvisioningStarted) {
            return
        }

        managedProvisioningStarted = true
        binding.splashScreenBold.visibility = View.GONE
        binding.splashScreenNormal.visibility = View.VISIBLE
        binding.splashScreenNormal.text = getString(R.string.nuvexa_managed_configuring)

        lifecycleScope.launch {
            try {
                val credentials = withContext(Dispatchers.IO) {
                    NuvexaManagedProvisioner(
                        provisioningUrl = getString(R.string.nuvexa_managed_provisioning_url),
                        provisioningType = getString(R.string.nuvexa_managed_provisioning_type)
                    ).provision()
                }
                openManagedAccount(credentials)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                managedProvisioningStarted = false
                showManagedProvisioningError()
            }
        }
    }

    private fun openManagedAccount(credentials: NuvexaManagedCredentials) {
        val scheme = getString(R.string.login_data_own_scheme)
        val directLoginUri = Uri.parse(
            buildString {
                append(scheme)
                append("://login/server:")
                append(Uri.encode(credentials.server))
                append("&user:")
                append(Uri.encode(credentials.loginName))
                append("&password:")
                append(Uri.encode(credentials.password))
            }
        )

        startActivity(
            Intent(this, AuthenticatorActivity::class.java).apply {
                data = directLoginUri
            }
        )
        finish()
    }

    private fun showManagedProvisioningError() {
        Snackbar.make(binding.root, R.string.nuvexa_managed_error, Snackbar.LENGTH_INDEFINITE)
            .setAction(R.string.nuvexa_managed_retry) {
                startManagedProvisioning()
            }
            .show()
    }

    companion object {
        const val SPLASH_DURATION = 1500L
    }
}
