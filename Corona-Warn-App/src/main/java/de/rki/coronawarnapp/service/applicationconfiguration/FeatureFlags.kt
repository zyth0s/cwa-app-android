package de.rki.coronawarnapp.service.applicationconfiguration

const val FEATURE_PLAUSIBLE_DENIABILITY = "isPlausibleDeniabilityActive"

const val STATE_ENABLED = 1
const val STATE_DISABLED = 0

/**
 * Enable or disable app features based on the application config
 */
class FeatureFlags(private val appConfigService: ApplicationConfigurationService) {

    /**
     * Returns if the plausible deniability feature should be active or not.
     * If something goes wrong, assume active.
     */
    suspend fun isPlausibleDeniabilityEnabled(): Boolean {
        try {
            val config = appConfigService.asyncRetrieveApplicationConfiguration()

            val feature = config.appFeatures?.appFeaturesList?.find {
                it.label == FEATURE_PLAUSIBLE_DENIABILITY
            }

            return when (feature?.value) {
                STATE_ENABLED -> true
                STATE_DISABLED -> false
                // assume true otherwise
                else -> true
            }
        } catch (e: Exception) {
            // if something goes wrong, assume true
            return true
        }
    }
}
