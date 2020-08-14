package de.rki.coronawarnapp.http.playbook

import de.rki.coronawarnapp.http.WebRequestBuilder
import de.rki.coronawarnapp.service.applicationconfiguration.ApplicationConfigurationService
import de.rki.coronawarnapp.service.applicationconfiguration.FeatureFlags
import de.rki.coronawarnapp.service.submission.SubmissionConstants
import de.rki.coronawarnapp.storage.LocalData
import de.rki.coronawarnapp.worker.BackgroundWorkScheduler
import kotlin.random.Random

class BackgroundNoise {
    companion object {
        @Volatile
        private var instance: BackgroundNoise? = null

        fun getInstance(): BackgroundNoise {
            return instance ?: synchronized(this) {
                instance ?: BackgroundNoise().also {
                    instance = it
                }
            }
        }
    }

    suspend fun scheduleDummyPattern() {
        val featureFlags = FeatureFlags(ApplicationConfigurationService.getInstance())
        if (!featureFlags.isPlausibleDeniabilityEnabled())
            return

        BackgroundWorkScheduler.scheduleBackgroundNoisePeriodicWork()
    }

    suspend fun foregroundScheduleCheck() {
        if (LocalData.isAllowedToSubmitDiagnosisKeys() == true) {
            val chance = Random.nextFloat() * 100
            if (chance < SubmissionConstants.probabilityToExecutePlaybookWhenOpenApp) {

                val featureFlags = FeatureFlags(ApplicationConfigurationService.getInstance())
                val plausibleDeniabilityEnabled = featureFlags.isPlausibleDeniabilityEnabled()
                if (!plausibleDeniabilityEnabled)
                    return

                PlaybookImpl(WebRequestBuilder.getInstance(), plausibleDeniabilityEnabled)
                    .dummy()
            }
        }
    }
}
