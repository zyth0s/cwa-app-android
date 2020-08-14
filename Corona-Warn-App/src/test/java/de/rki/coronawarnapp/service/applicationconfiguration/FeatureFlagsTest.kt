package de.rki.coronawarnapp.service.applicationconfiguration

import de.rki.coronawarnapp.server.protocols.ApplicationConfigurationOuterClass
import de.rki.coronawarnapp.util.newWebRequestBuilder
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.Test

class FeatureFlagsTest {
    @Test
    fun plausibleDeniability_enabled() = runBlocking {
        val appConfigService = mockk<ApplicationConfigurationService>()
        coEvery { appConfigService.asyncRetrieveApplicationConfiguration() } returns ApplicationConfigurationOuterClass.ApplicationConfiguration.newBuilder()
            .setAppFeatures(
                ApplicationConfigurationOuterClass.AppFeatures.newBuilder().addAppFeatures(
                    ApplicationConfigurationOuterClass.AppFeature.newBuilder()
                        .setLabel(FEATURE_PLAUSIBLE_DENIABILITY)
                        .setValue(STATE_ENABLED)
                )
            )
            .build()

        val featureFlags = FeatureFlags(appConfigService)
        assertThat(featureFlags.isPlausibleDeniabilityEnabled(), equalTo(true))
    }

    @Test
    fun plausibleDeniability_disabled() = runBlocking {
        val appConfigService = mockk<ApplicationConfigurationService>()
        coEvery { appConfigService.asyncRetrieveApplicationConfiguration() } returns ApplicationConfigurationOuterClass.ApplicationConfiguration.newBuilder()
            .setAppFeatures(
                ApplicationConfigurationOuterClass.AppFeatures.newBuilder().addAppFeatures(
                    ApplicationConfigurationOuterClass.AppFeature.newBuilder()
                        .setLabel(FEATURE_PLAUSIBLE_DENIABILITY)
                        .setValue(STATE_DISABLED)
                )
            )
            .build()

        val featureFlags = FeatureFlags(appConfigService)
        assertThat(featureFlags.isPlausibleDeniabilityEnabled(), equalTo(false))
    }

    @Test
    fun plausibleDeniability_error() = runBlocking {
        val server = MockWebServer()
        server.start()

        server.enqueue(MockResponse().setResponseCode(500))

        val appConfigService = ApplicationConfigurationService(server.newWebRequestBuilder())

        val featureFlags = FeatureFlags(appConfigService)
        assertThat(featureFlags.isPlausibleDeniabilityEnabled(), equalTo(true))
    }

    @Test
    fun plausibleDeniability_missing() = runBlocking {
        val appConfigService = mockk<ApplicationConfigurationService>()
        coEvery { appConfigService.asyncRetrieveApplicationConfiguration() } returns ApplicationConfigurationOuterClass.ApplicationConfiguration.newBuilder()
            .build()

        val featureFlags = FeatureFlags(appConfigService)
        assertThat(featureFlags.isPlausibleDeniabilityEnabled(), equalTo(true))
    }
}