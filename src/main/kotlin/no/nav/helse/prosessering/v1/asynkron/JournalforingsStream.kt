package no.nav.helse.prosessering.v1.asynkron

import no.nav.helse.felles.CorrelationId
import no.nav.helse.felles.formaterStatuslogging
import no.nav.helse.joark.JoarkGateway
import no.nav.helse.joark.Navn
import no.nav.helse.kafka.KafkaConfig
import no.nav.helse.kafka.ManagedKafkaStreams
import no.nav.helse.kafka.ManagedStreamHealthy
import no.nav.helse.kafka.ManagedStreamReady
import no.nav.helse.prosessering.v1.melding.Meldingstype
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.slf4j.LoggerFactory

internal class JournalforingsStream(
    joarkGateway: JoarkGateway,
    kafkaConfig: KafkaConfig
) {

    private val stream = ManagedKafkaStreams(
        name = NAME,
        properties = kafkaConfig.stream(NAME),
        topology = topology(joarkGateway),
        unreadyAfterStreamStoppedIn = kafkaConfig.unreadyAfterStreamStoppedIn
    )

    internal val ready = ManagedStreamReady(stream)
    internal val healthy = ManagedStreamHealthy(stream)

    private companion object {
        private const val NAME = "JournalforingV1"
        private val logger = LoggerFactory.getLogger("no.nav.$NAME.topology")

        private fun topology(joarkGateway: JoarkGateway): Topology {
            val builder = StreamsBuilder()
            val fraPreprossesert = Topics.PREPROSSESERT
            val tilCleanup = Topics.CLEANUP

            val mapValues = builder
                .stream(fraPreprossesert.name, fraPreprossesert.consumed)
                .filter { _, entry -> 1 == entry.metadata.version }
                .mapValues { soknadId, entry ->
                    process(NAME, soknadId, entry) {
                        logger.info(formaterStatuslogging(soknadId, "journalføres"))

                        val preprosessertMelding = entry.deserialiserTilPreprosessertMelding()
                        val dokumenter = preprosessertMelding.dokumentUrls
                        logger.trace("Journalfører dokumenter: {}", dokumenter)

                        val journalPostId = when(preprosessertMelding.type) {
                            Meldingstype.KORONA -> joarkGateway.journalførKoronaOverføringsMelding(
                                mottatt = preprosessertMelding.mottatt,
                                norskIdent = preprosessertMelding.søker.fødselsnummer,
                                correlationId = CorrelationId(entry.metadata.correlationId),
                                dokumenter = dokumenter,
                                navn = Navn(
                                    fornavn = preprosessertMelding.søker.fornavn,
                                    mellomnavn = preprosessertMelding.søker.mellomnavn,
                                    etternavn = preprosessertMelding.søker.etternavn
                                )
                            )

                            Meldingstype.FORDELING -> joarkGateway.journalførFordelingsmelding(
                                mottatt = preprosessertMelding.mottatt,
                                norskIdent = preprosessertMelding.søker.fødselsnummer,
                                correlationId = CorrelationId(entry.metadata.correlationId),
                                dokumenter = dokumenter,
                                navn = Navn(
                                    fornavn = preprosessertMelding.søker.fornavn,
                                    mellomnavn = preprosessertMelding.søker.mellomnavn,
                                    etternavn = preprosessertMelding.søker.etternavn
                                )
                            )

                            Meldingstype.OVERFORING -> joarkGateway.journalførOverføringsmelding(
                                mottatt = preprosessertMelding.mottatt,
                                norskIdent = preprosessertMelding.søker.fødselsnummer,
                                correlationId = CorrelationId(entry.metadata.correlationId),
                                dokumenter = dokumenter,
                                navn = Navn(
                                    fornavn = preprosessertMelding.søker.fornavn,
                                    mellomnavn = preprosessertMelding.søker.mellomnavn,
                                    etternavn = preprosessertMelding.søker.etternavn
                                )
                            )
                        }

                        logger.trace("Dokumenter journalført med ID = ${journalPostId.journalpostId}.")
                        val journalfort = Journalfort(journalpostId = journalPostId.journalpostId)

                        Cleanup(
                            metadata = entry.metadata,
                            melding = preprosessertMelding,
                            journalførtMelding = journalfort
                        ).serialiserTilData()
                    }
                }
            mapValues
                .to(tilCleanup.name, tilCleanup.produced)
            return builder.build()
        }
    }

    internal fun stop() = stream.stop(becauseOfError = false)
}
