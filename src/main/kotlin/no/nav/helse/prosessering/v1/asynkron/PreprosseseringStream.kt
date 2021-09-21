package no.nav.helse.prosessering.v1.asynkron

import no.nav.helse.felles.formaterStatuslogging
import no.nav.helse.kafka.KafkaConfig
import no.nav.helse.kafka.ManagedKafkaStreams
import no.nav.helse.kafka.ManagedStreamHealthy
import no.nav.helse.kafka.ManagedStreamReady
import no.nav.helse.prosessering.v1.PreprosseseringV1Service
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.slf4j.LoggerFactory
import java.time.ZonedDateTime

internal class PreprosseseringStream(
    preprosseseringV1Service: PreprosseseringV1Service,
    kafkaConfig: KafkaConfig
) {
    private val stream = ManagedKafkaStreams(
        name = NAME,
        properties = kafkaConfig.stream(NAME),
        topology = topology(preprosseseringV1Service),
        unreadyAfterStreamStoppedIn = kafkaConfig.unreadyAfterStreamStoppedIn
    )

    internal val ready = ManagedStreamReady(stream)
    internal val healthy = ManagedStreamHealthy(stream)

    private companion object {

        private const val NAME = "PreprosesseringV1"
        private val logger = LoggerFactory.getLogger("no.nav.$NAME.topology")

        private fun topology(preprosseseringV1Service: PreprosseseringV1Service): Topology {
            val builder = StreamsBuilder()
            val fromMottatt = Topics.MOTTATT
            val tilPreprossesert = Topics.PREPROSSESERT
            val PREPROSESSER_MOTTATT_ETTER = ZonedDateTime.parse("2021-09-21T13:53:00.000+01")

            builder
                .stream(fromMottatt.name, fromMottatt.consumed)
                .filter { _, entry -> 1 == entry.metadata.version }
                .filter { _, entry -> entry.deserialiserTilMelding().mottatt.isAfter(PREPROSESSER_MOTTATT_ETTER) }

                .mapValues { soknadId, entry ->
                    process(NAME, soknadId, entry) {
                        logger.info(formaterStatuslogging(soknadId, "preprosesseres"))

                        val preprossesertMelding = preprosseseringV1Service.preprosseser(
                            melding = entry.deserialiserTilMelding(),
                            metadata = entry.metadata
                        )
                        logger.trace("Preprossesering ferdig.")

                        preprossesertMelding.serialiserTilData()
                    }
                }
                .to(tilPreprossesert.name, tilPreprossesert.produced)
            return builder.build()
        }
    }

    internal fun stop() = stream.stop(becauseOfError = false)
}