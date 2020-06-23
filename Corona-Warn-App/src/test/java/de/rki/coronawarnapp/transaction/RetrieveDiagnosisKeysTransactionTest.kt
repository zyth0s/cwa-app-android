package de.rki.coronawarnapp.transaction

import com.google.android.gms.nearby.exposurenotification.ExposureConfiguration
import de.rki.coronawarnapp.nearby.InternalExposureNotificationClient
import de.rki.coronawarnapp.service.applicationconfiguration.ApplicationConfigurationService
import de.rki.coronawarnapp.storage.LocalData
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Paths
import java.util.Date
import java.util.UUID

/**
 * RetrieveDiagnosisKeysTransaction test.
 */
class RetrieveDiagnosisKeysTransactionTest {

    @Before
    fun setUp() {
        mockkObject(InternalExposureNotificationClient)
        mockkObject(ApplicationConfigurationService)
        mockkObject(RetrieveDiagnosisKeysTransaction)
        mockkObject(LocalData)

        coEvery { InternalExposureNotificationClient.asyncIsEnabled() } returns true
        coEvery {
            InternalExposureNotificationClient.asyncProvideDiagnosisKeys(
                any(),
                any(),
                any()
            )
        } returns mockk()
        coEvery { ApplicationConfigurationService.asyncRetrieveExposureConfiguration() } returns mockk()
        every { LocalData.googleApiToken(any()) } just Runs
        every { LocalData.lastTimeDiagnosisKeysFromServerFetch() } returns Date()
        every { LocalData.lastTimeDiagnosisKeysFromServerFetch(any()) } just Runs
        every { LocalData.googleApiToken() } returns UUID.randomUUID().toString()
    }

    @Test
    fun testTransactionNoFiles() {
        coEvery {
            RetrieveDiagnosisKeysTransaction["executeFetchKeyFilesFromServer"](any<Date>())
        } returns listOf<File>()

        runBlocking {
            RetrieveDiagnosisKeysTransaction.start()

            coVerifyOrder {
                RetrieveDiagnosisKeysTransaction["executeSetup"]()
                RetrieveDiagnosisKeysTransaction["executeRetrieveRiskScoreParams"]()
                RetrieveDiagnosisKeysTransaction["executeFetchKeyFilesFromServer"](any<Date>())
                RetrieveDiagnosisKeysTransaction["executeFetchDateUpdate"](any<Date>())
            }
        }
    }

    @Test
    fun testTransactionHasFiles() {
        val file = Paths.get("src", "test", "resources", "keys.bin").toFile()

        coEvery { RetrieveDiagnosisKeysTransaction["executeFetchKeyFilesFromServer"](any<Date>()) } returns listOf(
            file
        )

        runBlocking {
            RetrieveDiagnosisKeysTransaction.start()

            coVerifyOrder {
                RetrieveDiagnosisKeysTransaction["executeSetup"]()
                RetrieveDiagnosisKeysTransaction["executeRetrieveRiskScoreParams"]()
                RetrieveDiagnosisKeysTransaction["executeFetchKeyFilesFromServer"](any<Date>())
                RetrieveDiagnosisKeysTransaction["executeAPISubmission"](
                    any<String>(),
                    listOf(file),
                    any<ExposureConfiguration>()
                )
                RetrieveDiagnosisKeysTransaction["executeFetchDateUpdate"](any<Date>())
            }
        }
    }

    /**
     * Test rollback after
     * [RetrieveDiagnosisKeysTransaction.RetrieveDiagnosisKeysTransactionState.SETUP]
     * transaction state.
     */
    @Test
    fun testSetupRollback() {
        coEvery {
            RetrieveDiagnosisKeysTransaction["executeFetchKeyFilesFromServer"](any<Date>())
        } returns listOf<File>()
        coEvery { RetrieveDiagnosisKeysTransaction["executeToken"]() } throws Exception()

        val date = Date()
        val rollbackDate = slot<Date>()
        every { LocalData.lastTimeDiagnosisKeysFromServerFetch() } returns date
        every { LocalData.lastTimeDiagnosisKeysFromServerFetch(capture(rollbackDate)) } just Runs

        runBlocking {

            try {
                RetrieveDiagnosisKeysTransaction.start()
            } catch (ex: Exception) {
            }

            coVerifyOrder {
                RetrieveDiagnosisKeysTransaction.start()
                RetrieveDiagnosisKeysTransaction["executeSetup"]()
                LocalData.lastTimeDiagnosisKeysFromServerFetch(any())
            }

            assertThat(rollbackDate.isCaptured, `is`(true))
            assertThat(date, `is`(rollbackDate.captured))
        }
    }

    /**
     * Test rollback after
     * [RetrieveDiagnosisKeysTransaction.RetrieveDiagnosisKeysTransactionState.TOKEN]
     * transaction state.
     */
    @Test
    fun testTokenRollback() {
        coEvery {
            RetrieveDiagnosisKeysTransaction["executeFetchKeyFilesFromServer"](any<Date>())
        } returns listOf<File>()
        coEvery { RetrieveDiagnosisKeysTransaction["executeRetrieveRiskScoreParams"]() } throws Exception()

        val token = UUID.randomUUID().toString()
        val rollbackToken = slot<String>()

        every { LocalData.googleApiToken() } returns token
        every { LocalData.googleApiToken(capture(rollbackToken)) } just Runs

        runBlocking {

            try {
                RetrieveDiagnosisKeysTransaction.start()
            } catch (ex: Exception) {
            }

            coVerifyOrder {
                RetrieveDiagnosisKeysTransaction.start()
                RetrieveDiagnosisKeysTransaction["executeToken"]()
                LocalData.googleApiToken(any())
            }

            assertThat(rollbackToken.isCaptured, `is`(true))
            assertThat(token, `is`(rollbackToken.captured))
        }
    }

    @After
    fun cleanUp() {
        unmockkAll()
    }
}
