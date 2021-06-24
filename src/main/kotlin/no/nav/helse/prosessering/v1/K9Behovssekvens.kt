package no.nav.helse.prosessering.v1

import no.nav.helse.prosessering.v1.asynkron.Cleanup
import no.nav.helse.prosessering.v1.melding.Barn
import no.nav.helse.prosessering.v1.melding.Meldingstype
import no.nav.helse.prosessering.v1.melding.MottakerType
import no.nav.k9.rapid.behov.*

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
        behov = arrayOf(behov)
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