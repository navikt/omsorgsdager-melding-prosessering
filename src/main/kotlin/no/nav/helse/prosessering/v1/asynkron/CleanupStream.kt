package no.nav.helse.prosessering.v1.asynkron

import no.nav.helse.dokument.DokumentGateway
import no.nav.helse.dokument.DokumentService
import no.nav.helse.felles.CorrelationId
import no.nav.helse.felles.formaterStatuslogging
import no.nav.helse.kafka.KafkaConfig
import no.nav.helse.kafka.ManagedKafkaStreams
import no.nav.helse.kafka.ManagedStreamHealthy
import no.nav.helse.kafka.ManagedStreamReady
import no.nav.k9.rapid.behov.Behovssekvens
import no.nav.k9.rapid.behov.MidlertidigAleneBehov
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.slf4j.LoggerFactory

internal class CleanupStream(
    kafkaConfig: KafkaConfig,
    dokumentService: DokumentService
) {
    private val stream = ManagedKafkaStreams(
        name = NAME,
        properties = kafkaConfig.stream(NAME),
        topology = topology(dokumentService),
        unreadyAfterStreamStoppedIn = kafkaConfig.unreadyAfterStreamStoppedIn
    )

    internal val ready = ManagedStreamReady(stream)
    internal val healthy = ManagedStreamHealthy(stream)

    private companion object {
        private const val NAME = "CleanupV1"
        private val logger = LoggerFactory.getLogger("no.nav.$NAME.topology")

        private fun topology(dokumentService: DokumentService): Topology {
            val builder = StreamsBuilder()
            val fraCleanup = Topics.CLEANUP
            val tilK9Rapid = Topics.K9_RAPID_V2

            builder
                .stream(fraCleanup.name, fraCleanup.consumed)
                .filter { _, entry -> 1 == entry.metadata.version }
                .selectKey{ _, value ->
                    value.deserialiserTilCleanup().melding.id
                }
                .mapValues { soknadId, entry ->
                    process(NAME, soknadId, entry) {
                        val cleanupMelding = entry.deserialiserTilCleanup()

                        logger.info(formaterStatuslogging(cleanupMelding.melding.søknadId, "kjører cleanup"))
                        logger.trace("Sletter dokumenter.")

                        dokumentService.slettDokumeter(
                            urlBolks = cleanupMelding.melding.dokumentUrls,
                            dokumentEier = DokumentGateway.DokumentEier(cleanupMelding.melding.søker.fødselsnummer),
                            correlationId = CorrelationId(entry.metadata.correlationId)
                        )

                        logger.trace("Dokumenter slettet.")
                        logger.trace("Mapper om til Behovssekvens")
                        val behovssekvens = cleanupMelding.tilK9Behovssekvens()
                        val (id, løsning) = behovssekvens.keyValue

                        logger.info(formaterStatuslogging(cleanupMelding.melding.søknadId, "har behovssekvensID $id og sendes videre til topic ${tilK9Rapid.name}"))

                        Data(løsning)
                    }
                }
                .to(tilK9Rapid.name, tilK9Rapid.produced)
            return builder.build()
        }
    }

    internal fun stop() = stream.stop(becauseOfError = false)
}

internal fun Cleanup.tilK9Behovssekvens() : Behovssekvens{
    val melding = this.melding

    val correlationId = this.metadata.correlationId
    val journalPostIdListe = listOf(this.journalførtMelding.journalpostId)
    val søker = MidlertidigAleneBehov.Person(identitetsnummer = melding.søker.fødselsnummer)

    val behov = MidlertidigAleneBehov(
        søker = søker,
        annenForelder = MidlertidigAleneBehov.Person(søker.identitetsnummer),
        mottatt = melding.mottatt,
        journalpostIder = journalPostIdListe
    )

    return Behovssekvens(
        id = melding.id,
        correlationId = correlationId,
        behov = *arrayOf(
            behov
        )
    )
}
