/*
 * VoIP.ms SMS
 * Copyright (C) 2017-2020 Michael Kourlas
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.kourlas.voipms_sms

import android.app.Application
import android.net.ConnectivityManager
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.work.ExistingWorkPolicy
import net.kourlas.voipms_sms.network.NetworkManager.Companion.getInstance
import net.kourlas.voipms_sms.preferences.fragments.AppearancePreferencesFragment
import net.kourlas.voipms_sms.preferences.getAppTheme
import net.kourlas.voipms_sms.sms.ConversationId
import net.kourlas.voipms_sms.sms.Database.Companion.getInstance
import net.kourlas.voipms_sms.sms.workers.SyncWorker.Companion.startPeriodicWorker
import net.kourlas.voipms_sms.utils.logException
import net.kourlas.voipms_sms.utils.subscribeToDidTopics

/**
 * Custom application implementation that keeps track of visible activities.
 */
class CustomApplication : Application() {
    private var conversationsActivitiesVisible = 0
    private val conversationActivitiesVisible = mutableMapOf<ConversationId, Int>()

    fun conversationsActivityVisible(): Boolean {
        return conversationsActivitiesVisible > 0
    }

    fun conversationsActivityIncrementCount() {
        conversationsActivitiesVisible++
    }

    fun conversationsActivityDecrementCount() {
        conversationsActivitiesVisible--
    }

    fun conversationActivityVisible(conversationId: ConversationId): Boolean {
        val count = conversationActivitiesVisible[conversationId]
        return count != null && count > 0
    }

    fun conversationActivityIncrementCount(
            conversationId: ConversationId) {
        val count = conversationActivitiesVisible[conversationId] ?: 0
        conversationActivitiesVisible[conversationId] = count + 1
    }

    fun conversationActivityDecrementCount(
            conversationId: ConversationId) {
        val count = conversationActivitiesVisible[conversationId] ?: 0
        conversationActivitiesVisible[conversationId] = count - 1
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Update theme
        when (getAppTheme(applicationContext)) {
            AppearancePreferencesFragment.SYSTEM_DEFAULT -> AppCompatDelegate.setDefaultNightMode(
                    AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            AppearancePreferencesFragment.LIGHT -> AppCompatDelegate.setDefaultNightMode(
                    AppCompatDelegate.MODE_NIGHT_NO)
            AppearancePreferencesFragment.DARK -> AppCompatDelegate.setDefaultNightMode(
                    AppCompatDelegate.MODE_NIGHT_YES)
        }

        // Register for network callbacks
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val connectivityManager = getSystemService(
                    CONNECTIVITY_SERVICE) as ConnectivityManager?
            if (connectivityManager == null) {
                val e = RuntimeException(
                        "Connectivity manager does not exist")
                logException(e)
                throw e
            }
            connectivityManager.registerDefaultNetworkCallback(
                    getInstance())
        }

        // Open database
        getInstance(applicationContext)

        // Subscribe to topics for current DIDs
        subscribeToDidTopics(applicationContext)

        // We want to start the periodic synchronization if it hasn't
        // already
        startPeriodicWorker(
                this,  /*existingWorkPolicy=*/ExistingWorkPolicy.KEEP)
    }

    companion object {
        lateinit var instance: CustomApplication
            private set
    }
}
