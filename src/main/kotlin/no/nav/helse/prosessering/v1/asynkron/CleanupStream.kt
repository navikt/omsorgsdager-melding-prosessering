package no.nav.helse.prosessering.v1.asynkron

import no.nav.helse.dokument.DokumentGateway
import no.nav.helse.dokument.DokumentService
import no.nav.helse.felles.CorrelationId
import no.nav.helse.felles.formaterStatuslogging
import no.nav.helse.kafka.KafkaConfig
import no.nav.helse.kafka.ManagedKafkaStreams
import no.nav.helse.kafka.ManagedStreamHealthy
import no.nav.helse.kafka.ManagedStreamReady
import no.nav.helse.prosessering.v1.melding.Barn
import no.nav.helse.prosessering.v1.melding.Meldingstype
import no.nav.helse.prosessering.v1.melding.MottakerType
import no.nav.k9.rapid.behov.*
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.slf4j.LoggerFactory
import java.lang.IllegalStateException

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
                .selectKey { _, value ->
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
            return builder.build()
        }
    }

    internal fun stop() = stream.stop(becauseOfError = false)
}

internal fun Cleanup.tilK9Behovssekvens(): Behovssekvens = let {
    val correlationId = metadata.correlationId
    val journalPostIdListe = listOf(journalførtMelding.journalpostId)

    val behov: Behov = when (melding.type) {
        Meldingstype.OVERFORING -> {
            val overføring = melding.overføring!!
            val mottakerType = overføring.mottakerType
            OverføreOmsorgsdagerBehov(
                kilde = OverføreOmsorgsdagerBehov.Kilde.Digital,
                mottatt = melding.mottatt,
                omsorgsdagerTattUtIÅr = melding.antallDagerBruktIÅr ?: 0,
                omsorgsdagerÅOverføre = overføring.antallDagerSomSkalOverføres,
                barn = melding.barn.map { it.tilOverføreOmsorgsdagerBehovBarn() },
                journalpostIder = journalPostIdListe,
                fra = OverføreOmsorgsdagerBehov.OverførerFra(
                    identitetsnummer = melding.søker.fødselsnummer,
                    jobberINorge = melding.arbeiderINorge
                ),
                til = OverføreOmsorgsdagerBehov.OverførerTil(
                    identitetsnummer = melding.mottakerFnr,
                    relasjon = mottakerType.tilOverføreOmsorgsdagerBehovRelasjon(),
                    harBoddSammenMinstEttÅr = mottakerType.let {
                        if (it == MottakerType.SAMBOER) true else null
                    }
                )
            )
        }

        Meldingstype.FORDELING -> {
            FordeleOmsorgsdagerBehov(
                mottatt = melding.mottatt,
                fra = FordeleOmsorgsdagerBehov.Fra(
                    identitetsnummer = melding.søker.fødselsnummer
                ),
                til = FordeleOmsorgsdagerBehov.Til(
                    identitetsnummer = melding.mottakerFnr
                ),
                barn = melding.barn.map { it.somFordeleOmsorgsdagerBehovBarn() },
                journalpostIder = journalPostIdListe
            )
        }

        Meldingstype.KORONA -> {
            val korona = melding.korona!!
            OverføreKoronaOmsorgsdagerBehov(
                fra = OverføreKoronaOmsorgsdagerBehov.OverførerFra(
                    identitetsnummer = melding.søker.fødselsnummer,
                    jobberINorge = melding.arbeiderINorge
                ),
                til = OverføreKoronaOmsorgsdagerBehov.OverførerTil(
                    identitetsnummer = melding.mottakerFnr
                ),
                omsorgsdagerTattUtIÅr = melding.antallDagerBruktIÅr ?: 0,
                omsorgsdagerÅOverføre = korona.antallDagerSomSkalOverføres,
                barn = melding.barn.map { it.somOverføreKoronaOmsorgsdagerBehovBarn() },
                periode = OverføreKoronaOmsorgsdagerBehov.Periode(
                    fraOgMed = korona.stengingsperiode.fraOgMed,
                    tilOgMed = korona.stengingsperiode.tilOgMed
                ),
                journalpostIder = journalPostIdListe,
                mottatt = melding.mottatt
            )
        }
    }

    Behovssekvens(
        id = melding.id,
        correlationId = correlationId,
        behov = *arrayOf(
            behov
        )
    )
}

private fun Barn.somOverføreKoronaOmsorgsdagerBehovBarn(): OverføreKoronaOmsorgsdagerBehov.Barn =
    OverføreKoronaOmsorgsdagerBehov.Barn(
        identitetsnummer = identitetsnummer,
        fødselsdato = fødselsdato,
        utvidetRett = utvidetRett,
        aleneOmOmsorgen = aleneOmOmsorgen
    )

private fun Barn.somFordeleOmsorgsdagerBehovBarn(): FordeleOmsorgsdagerBehov.Barn = FordeleOmsorgsdagerBehov.Barn(
    identitetsnummer = identitetsnummer,
    fødselsdato = fødselsdato
)

private fun MottakerType.tilOverføreOmsorgsdagerBehovRelasjon(): OverføreOmsorgsdagerBehov.Relasjon = when (this) {
    MottakerType.EKTEFELLE -> OverføreOmsorgsdagerBehov.Relasjon.NåværendeEktefelle
    MottakerType.SAMBOER -> OverføreOmsorgsdagerBehov.Relasjon.NåværendeSamboer
    else -> throw IllegalStateException("$this er ikke et gyldig relasjon for OverføreOmsorgsdagerBehov.")
}

internal fun Barn.tilOverføreOmsorgsdagerBehovBarn(): OverføreOmsorgsdagerBehov.Barn {
    return OverføreOmsorgsdagerBehov.Barn(
        identitetsnummer = this.identitetsnummer,
        fødselsdato = this.fødselsdato,
        aleneOmOmsorgen = this.aleneOmOmsorgen,
        utvidetRett = this.utvidetRett
    )
}
