package no.nav.helse

import com.github.tomakehurst.wiremock.WireMockServer
import io.ktor.server.testing.*
import no.nav.helse.dusseldorf.testsupport.asArguments
import no.nav.helse.dusseldorf.testsupport.wiremock.WireMockBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory


class OmsorgsdagerMeldingProsesseringWithMocks {
    companion object {
        private val logger: Logger = LoggerFactory.getLogger(OmsorgsdagerMeldingProsesseringWithMocks::class.java)

        @JvmStatic
        fun main(args: Array<String>) {

            val wireMockServer: WireMockServer = WireMockBuilder()
                .withPort(8091)
                .withAzureSupport()
                .navnOppslagConfig()
                .build()
                .stubK9MellomlagringHealth()
                .stubK9JoarkHealth()
                .stubJournalfor(path = "v1/omsorgsdageroverforing/journalforing")
                .stubJournalfor(path = "v1/omsorgsdagerdeling/journalforing")
                .stubJournalfor(path = "v1/omsorgsdagerfordeling/journalforing")
                .stubLagreDokument()
                .stubSlettDokument()

            val kafkaEnvironment = KafkaWrapper.bootstrap()

            val testArgs = TestConfiguration.asMap(
                wireMockServer = wireMockServer,
                kafkaEnvironment = kafkaEnvironment,
                port = 8092
            ).asArguments()

            Runtime.getRuntime().addShutdownHook(object : Thread() {
                override fun run() {
                    logger.info("Tearing down")
                    wireMockServer.stop()
                    kafkaEnvironment.tearDown()
                    logger.info("Tear down complete")
                }
            })

            testApplication { no.nav.helse.main(testArgs) }
        }
    }
}
