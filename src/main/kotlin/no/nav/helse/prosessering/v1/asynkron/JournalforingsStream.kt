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

        val ignoreList = listOf(
            "generated-b6a9e2ab-d5f5-4c00-97ca-3c4f00b2319e",
            "generated-c13e0c9d-c9c1-4b28-b990-1af8c18a17cf",
            "generated-6213154f-2752-4d4c-8b15-ae73bde0b80b",
            "generated-26fcbf6a-16a2-4030-b181-14164adc4eb2",
            "generated-0972f2d4-56d1-464a-83bf-ee5045b54301",
            "generated-cc50f9e1-1184-464c-89b7-836e77d60586"
        )

        private fun topology(joarkGateway: JoarkGateway): Topology {
            val builder = StreamsBuilder()
            val fraPreprossesert = Topics.PREPROSSESERT
            val tilCleanup = Topics.CLEANUP

            val mapValues = builder
                .stream(fraPreprossesert.name, fraPreprossesert.consumed)
                .filter { _, entry -> 1 == entry.metadata.version }
                .filter { _, entry -> !ignoreList.contains(entry.metadata.correlationId) }
                .mapValues { soknadId, entry ->
                    process(NAME, soknadId, entry) {
                        logger.info(formaterStatuslogging(soknadId, "journalføres"))

                        val preprosessertMelding = entry.deserialiserTilPreprosessertMelding()
                        val dokumenter = preprosessertMelding.dokumentUrls
                        logger.info("Journalfører dokumenter: {}", dokumenter)

                        val journalPostId = when (preprosessertMelding.type) {
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

                            Meldingstype.FORDELING, Meldingstype.OVERFORING -> joarkGateway.journalførDelingsMelding(
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
