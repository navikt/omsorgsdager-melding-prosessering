package no.nav.helse.prosessering.v1.melding

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDate
import java.time.ZonedDateTime

data class Melding(
    val søknadId: String,
    val id: String,
    val språk: String,
    val mottatt: ZonedDateTime,
    val søker: Søker,
    val mottakerFnr: String,
    val mottakerNavn: String,
    val harAleneomsorg: Boolean,
    val harUtvidetRett: Boolean,
    val erYrkesaktiv: Boolean,
    val arbeiderINorge: Boolean,
    val arbeidssituasjon: List<Arbeidssituasjon>,
    val antallDagerBruktIÅr: Int? = null,
    val barn: List<Barn>,
    val type: Meldingstype,
    val korona: KoronaOverføringMelding? = null,
    val overføring: OverføringsMelding? = null,
    val fordeling: FordelingsMelding? = null,
    val harBekreftetOpplysninger: Boolean,
    val harForståttRettigheterOgPlikter: Boolean
){
    override fun toString(): String {
        return "Melding(søknadId='$søknadId', id='$id')"
    }
}

data class KoronaOverføringMelding(
    val antallDagerSomSkalOverføres: Int,
    val stengingsperiode: KoronaStengingsperiode
)

data class KoronaStengingsperiode(
    @JsonFormat(pattern = "yyyy-MM-dd") val fraOgMed: LocalDate,
    @JsonFormat(pattern = "yyyy-MM-dd") val tilOgMed: LocalDate
)

data class FordelingsMelding(
    val mottakerType: MottakerType,
    val samværsavtaleVedleggId: List<String> = listOf()
)

data class OverføringsMelding(
    val mottakerType: MottakerType,
    val antallDagerSomSkalOverføres: Int
)

enum class Meldingstype(val type: String) {
    KORONA("COVID-19"),
    OVERFORING("Overføring"),
    FORDELING("Fordeling")
}

enum class MottakerType(val type: String) {
    EKTEFELLE("ektefelle"),
    SAMBOER("samboer"),
    SAMVÆRSFORELDER("samværsforelder")
}

data class Barn(
    val identitetsnummer: String,
    val navn: String,
    @JsonFormat(pattern = "yyyy-MM-dd") val fødselsdato: LocalDate,
    val aleneOmOmsorgen: Boolean,
    val utvidetRett: Boolean
) {
    override fun toString(): String {
        return "Barn(identitetsnummer='*****', navn='$navn')"
    }
}

data class Søker(
    val fødselsnummer: String,
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
    @JsonFormat(pattern = "yyyy-MM-dd") val fødselsdato: LocalDate?,
    val aktørId: String
) {
    override fun toString(): String {
        return "Soker(fornavn='$fornavn', mellomnavn=$mellomnavn, etternavn='$etternavn', fødselsdato=$fødselsdato, aktørId='*****')"
    }
}

enum class Arbeidssituasjon(val utskriftvennlig: String) {
    SELVSTENDIG_NÆRINGSDRIVENDE("Selvstendig næringsdrivende"),
    ARBEIDSTAKER("Arbeidstaker"),
    FRILANSER("Frilanser"),
    ANNEN("Annen")
}

internal fun List<Arbeidssituasjon>.somMapTilPdfArbeidssituasjon(): List<Map<String, Any?>> {
    return map {
        mapOf<String, Any?>(
            "utskriftvennlig" to it.utskriftvennlig
        )
    }
}

internal fun List<Int>.somMapTilPdfFødselsår(): List<Map<String, Any?>> {
    return map {
        mapOf<String, Any?>(
            "alder" to it
        )
    }
}
