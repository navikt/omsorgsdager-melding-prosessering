package no.nav.helse

import com.github.tomakehurst.wiremock.WireMockServer
import com.typesafe.config.ConfigFactory
import io.ktor.config.*
import io.ktor.http.*
import io.ktor.server.engine.*
import io.ktor.server.testing.*
import io.prometheus.client.CollectorRegistry
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.time.delay
import no.nav.common.KafkaEnvironment
import no.nav.helse.dusseldorf.testsupport.wiremock.WireMockBuilder
import no.nav.helse.k9.assertK9RapidFormat
import org.json.JSONObject
import org.junit.jupiter.api.AfterAll
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class OmsorgsdagerMeldingProsesseringTest {

    private companion object {

        private val logger: Logger = LoggerFactory.getLogger(OmsorgsdagerMeldingProsesseringTest::class.java)

        private val wireMockServer: WireMockServer = WireMockBuilder()
            .withAzureSupport()
            .navnOppslagConfig()
            .build()
            .stubK9MellomlagringHealth()
            .stubK9JoarkHealth()
            .stubJournalfor(path = "v1/omsorgsdageroverforing/journalforing")
            .stubJournalfor(path = "v1/omsorgsdagerdeling/journalforing")
            .stubLagreDokument()
            .stubSlettDokument()

        private val kafkaEnvironment = KafkaWrapper.bootstrap()
        private val kafkaTestProducer = kafkaEnvironment.meldingsProducer()

        private val k9RapidKonsumer = kafkaEnvironment.k9RapidKonsumer()

        private val dNummerA = "55125314561"
        private val gyldigFodselsnummerA = "02119970078"

        private var engine = newEngine(kafkaEnvironment).apply {
            start(wait = true)
        }

        private fun getConfig(kafkaEnvironment: KafkaEnvironment?): ApplicationConfig {
            val fileConfig = ConfigFactory.load()
            val testConfig = ConfigFactory.parseMap(
                TestConfiguration.asMap(
                    wireMockServer = wireMockServer,
                    kafkaEnvironment = kafkaEnvironment
                )
            )
            val mergedConfig = testConfig.withFallback(fileConfig)
            return HoconApplicationConfig(mergedConfig)
        }

        private fun newEngine(kafkaEnvironment: KafkaEnvironment?) = TestApplicationEngine(createTestEnvironment {
            config = getConfig(kafkaEnvironment)
        })

        private fun stopEngine() = engine.stop(5, 60, TimeUnit.SECONDS)

        internal fun restartEngine() {
            stopEngine()
            CollectorRegistry.defaultRegistry.clear()
            engine = newEngine(kafkaEnvironment)
            engine.start(wait = true)
        }

        @AfterAll
        @JvmStatic
        fun tearDown() {
            logger.info("Tearing down")
            wireMockServer.stop()
            k9RapidKonsumer.close()
            kafkaTestProducer.close()
            stopEngine()
            kafkaEnvironment.tearDown()
            logger.info("Tear down complete")
        }
    }

    @Test
    fun `test isready, isalive, health og metrics`() {
        with(engine) {
            handleRequest(HttpMethod.Get, "/isready") {}.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                handleRequest(HttpMethod.Get, "/isalive") {}.apply {
                    assertEquals(HttpStatusCode.OK, response.status())
                    handleRequest(HttpMethod.Get, "/metrics") {}.apply {
                        assertEquals(HttpStatusCode.OK, response.status())
                        handleRequest(HttpMethod.Get, "/health") {}.apply {
                            assertEquals(HttpStatusCode.OK, response.status())
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `Gylding søknad blir prosessert av journalføringskonsumer`() {
        val søknad = SøknadUtils.gyldigSøknad(
            id = "01ERQ023JVT5BEVSH8R0G04DXN"
        )

        kafkaTestProducer.leggTilMottak(søknad)
        k9RapidKonsumer
            .hentK9RapidMelding(søknad.id)
            .validerK9RapidFormat(søknad.id)
    }

    @Test
    fun `En feilprosessert søknad vil bli prosessert etter at tjenesten restartes`() {
        val søknad = SøknadUtils.gyldigSøknad(
            id = "01ERQ049W5H5A27ZA367N5BQ3P"
        )

        wireMockServer.stubJournalfor(500, "v") // Simulerer feil ved journalføring

        kafkaTestProducer.leggTilMottak(søknad)
        ventPaaAtRetryMekanismeIStreamProsessering()
        readyGir200HealthGir503()

        wireMockServer.stubJournalfor(
            201,
            "v1/omsorgsdagerdeling/journalforing"
        ) // Simulerer journalføring fungerer igjen
        restartEngine()
        k9RapidKonsumer
            .hentK9RapidMelding(søknad.id)
            .validerK9RapidFormat(søknad.id)

    }

    @Test
    fun `Sende søknad hvor søker har D-nummer`() {
        val søknad = SøknadUtils.gyldigSøknad(
            id = "01ERQ05R3MJ7XAFH3RA0YQEQP4",
            søkerFødselsnummer = dNummerA
        )

        kafkaTestProducer.leggTilMottak(søknad)
        k9RapidKonsumer
            .hentK9RapidMelding(søknad.id)
            .validerK9RapidFormat(søknad.id)
    }

    private fun ventPaaAtRetryMekanismeIStreamProsessering() = runBlocking { delay(Duration.ofSeconds(30)) }

    private fun readyGir200HealthGir503() {
        with(engine) {
            handleRequest(HttpMethod.Get, "/isready") {}.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                handleRequest(HttpMethod.Get, "/health") {}.apply {
                    assertEquals(HttpStatusCode.ServiceUnavailable, response.status())
                }
            }
        }
    }

    @Test
    fun `Gyldig melding om deling av omsorgsdager blir prosessert av journalføringkonsumer`() {
        val søknad = SøknadUtils.gyldigSøknad(
            id = "01ERQ05R3MJ7XAFH3RA0YQEQP5",
            søkerFødselsnummer = gyldigFodselsnummerA
        )

        kafkaTestProducer.leggTilMottak(søknad)
        k9RapidKonsumer
            .hentK9RapidMelding(søknad.id)
            .assertK9RapidFormat(søknad.id)
    }

    private infix fun String.validerK9RapidFormat(id: String) {
        val rawJson = JSONObject(this)
        println(rawJson)

        assertEquals(rawJson.getJSONArray("@behovsrekkefølge").getString(0), "OverføreOmsorgsdager")
        assertEquals(rawJson.getString("@type"), "Behovssekvens")
        assertEquals(rawJson.getString("@id"), id)

        assertNotNull(rawJson.getString("@correlationId"))
        assertNotNull(rawJson.getJSONObject("@behov"))
    }

}
