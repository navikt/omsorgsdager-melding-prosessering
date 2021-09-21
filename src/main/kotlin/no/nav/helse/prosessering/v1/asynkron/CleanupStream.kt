package no.nav.helse.prosessering.v1.asynkron

import no.nav.helse.dokument.DokumentGateway
import no.nav.helse.dokument.DokumentService
import no.nav.helse.felles.CorrelationId
import no.nav.helse.felles.Ytelse
import no.nav.helse.felles.formaterStatuslogging
import no.nav.helse.felles.tilK9Beskjed
import no.nav.helse.kafka.KafkaConfig
import no.nav.helse.kafka.ManagedKafkaStreams
import no.nav.helse.kafka.ManagedStreamHealthy
import no.nav.helse.kafka.ManagedStreamReady
import no.nav.helse.prosessering.v1.tilK9Behovssekvens
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.slf4j.LoggerFactory

internal class CleanupStream(
    kafkaConfig: KafkaConfig,
    dokumentService: DokumentService
) {
    private val stream = ManagedKafkaStreams(
        name = cleanup,
        properties = kafkaConfig.stream(cleanup),
        topology = topology(dokumentService),
        unreadyAfterStreamStoppedIn = kafkaConfig.unreadyAfterStreamStoppedIn
    )

    internal val ready = ManagedStreamReady(stream)
    internal val healthy = ManagedStreamHealthy(stream)

    private companion object {
        private const val cleanup = "CleanupV2"
        private const val k9DittnavVarsel = "K9DittnavVarselV1"
        private val logger = LoggerFactory.getLogger("no.nav.$cleanup.topology")

        private fun topology(dokumentService: DokumentService): Topology {
            val fraCleanup = Topics.CLEANUP
            val tilK9Rapid = Topics.K9_RAPID_V2
            val tilK9DittnavVarsel = Topics.K9_DITTNAV_VARSEL
            val builder = StreamsBuilder()
            val inputStream = builder.stream(fraCleanup.name, fraCleanup.consumed)

            inputStream
                .filter { _, entry -> 1 == entry.metadata.version }
                .selectKey { _, value ->
                    value.deserialiserTilCleanup().melding.id
                }
                .mapValues { soknadId, entry ->
                    process(cleanup, soknadId, entry) {
                        val cleanupMelding = entry.deserialiserTilCleanup()
                        logger.info(formaterStatuslogging(cleanupMelding.melding.søknadId, "kjører cleanup"))

                        dokumentService.slettDokumeter(
                            urlBolks = cleanupMelding.melding.dokumentUrls,
                            dokumentEier = DokumentGateway.DokumentEier(cleanupMelding.melding.søker.fødselsnummer),
                            correlationId = CorrelationId(entry.metadata.correlationId)
                        )

                        val behovssekvens = cleanupMelding.tilK9Behovssekvens()
                        val (id, løsning) = behovssekvens.keyValue

                        logger.info(
                            formaterStatuslogging(
                                cleanupMelding.melding.søknadId,
                                "har behovssekvensID $id og sendes videre til topic ${tilK9Rapid.name}"
                            )
                        )

                        Data(løsning)
                    }
                }
                .to(tilK9Rapid.name, tilK9Rapid.produced)

            inputStream
                .filter { _, entry -> 1 == entry.metadata.version }
                .mapValues { soknadId, entry ->
                    process(k9DittnavVarsel, soknadId, entry) {
                        val cleanupMelding = entry.deserialiserTilCleanup()
                        logger.info(formaterStatuslogging(cleanupMelding.melding.søknadId, "mappes om til K9Beskjed"))

                        val ytelse = when {
                            cleanupMelding.melding.fordeling != null -> Ytelse.OMSORGSDAGER_MELDING_FORDELE
                            cleanupMelding.melding.overføring != null -> Ytelse.OMSORGSDAGER_MELDING_OVERFØRE
                            else -> Ytelse.OMSORGSDAGER_MELDING_KORONA
                        }

                        val k9beskjed = cleanupMelding.tilK9Beskjed(ytelse)
                        logger.info(
                            formaterStatuslogging(
                                cleanupMelding.melding.søknadId,
                                "sender K9Beskjed videre til k9-dittnav-varsel med eventId ${k9beskjed.eventId}"
                            )
                        )
                        k9beskjed.serialiserTilData()
                    }
                }
                .to(tilK9DittnavVarsel.name, tilK9DittnavVarsel.produced)

            return builder.build()
        }
    }

    internal fun stop() = stream.stop(becauseOfError = false)
}