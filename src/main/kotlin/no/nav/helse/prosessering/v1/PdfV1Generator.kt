package no.nav.helse.prosessering.v1

import com.github.jknack.handlebars.Context
import com.github.jknack.handlebars.Handlebars
import com.github.jknack.handlebars.Helper
import com.github.jknack.handlebars.context.MapValueResolver
import com.github.jknack.handlebars.io.ClassPathTemplateLoader
import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder
import com.openhtmltopdf.util.XRLog
import no.nav.helse.dusseldorf.ktor.core.fromResources
import no.nav.helse.prosessering.v1.melding.*
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.DayOfWeek
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.logging.Level

internal class PdfV1Generator {
    private companion object {
        private const val ROOT = "handlebars"
        private const val SOKNAD = "soknad"

        private val REGULAR_FONT = "$ROOT/fonts/SourceSansPro-Regular.ttf".fromResources().readBytes()
        private val BOLD_FONT = "$ROOT/fonts/SourceSansPro-Bold.ttf".fromResources().readBytes()
        private val ITALIC_FONT = "$ROOT/fonts/SourceSansPro-Italic.ttf".fromResources().readBytes()

        private val sRGBColorSpace = "$ROOT/sRGB.icc".fromResources().readBytes()

        private val handlebars = Handlebars(ClassPathTemplateLoader("/$ROOT")).apply {
            registerHelper("eq", Helper<String> { context, options ->
                if (context == options.param(0)) options.fn() else options.inverse()
            })
            registerHelper("eqTall", Helper<Int> { context, options ->
                if (context == options.param(0)) options.fn() else options.inverse()
            })
            registerHelper("fritekst", Helper<String> { context, _ ->
                if (context == null) "" else {
                    val text = Handlebars.Utils.escapeExpression(context)
                        .toString()
                        .replace(Regex("\\r\\n|[\\n\\r]"), "<br/>")
                    Handlebars.SafeString(text)
                }
            })
            registerHelper("jaNeiSvar", Helper<Boolean> { context, _ ->
                if (context == true) "Ja" else "Nei"
            })

            infiniteLoops(true)
        }

        private val soknadTemplate = handlebars.compile(SOKNAD)

        private val ZONE_ID = ZoneId.of("Europe/Oslo")
        private val DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZONE_ID)
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    }

    internal fun generateSoknadOppsummeringPdf(melding: Melding): ByteArray {
        XRLog.listRegisteredLoggers().forEach { logger -> XRLog.setLevel(logger, Level.WARNING) }
        soknadTemplate.apply(
            Context
                .newBuilder(
                    mapOf(
                        "felles" to mapOf(
                            "id" to melding.id,
                            "søknadId" to melding.søknadId,
                            "søknadstypeTittel" to melding.type.somTittel(),
                            "søknadMottattDag" to melding.mottatt.withZoneSameInstant(ZONE_ID).norskDag(),
                            "søknadMottatt" to DATE_TIME_FORMATTER.format(melding.mottatt),
                            "søker" to mapOf(
                                "navn" to melding.søker.formatertNavn().capitalizeName(),
                                "fødselsnummer" to melding.søker.fødselsnummer
                            ),
                            "situasjon" to mapOf(
                                "harAleneomsorg" to melding.harAleneomsorg,
                                "harUtvidetRett" to melding.harUtvidetRett,
                                "erYrkesaktiv" to melding.erYrkesaktiv,
                                "arbeiderINorge" to melding.arbeiderINorge,
                                "arbeidssituasjon" to melding.arbeidssituasjon.somMapTilPdfArbeidssituasjon(),
                                "antallDagerBruktIÅr" to melding.antallDagerBruktIÅr
                            ),
                            "barn" to melding.barn.somMap(),
                            "mottaker" to mapOf(
                                "fnr" to melding.mottakerFnr,
                                "navn" to melding.mottakerNavn.capitalizeName()
                            ),
                            "samtykke" to mapOf(
                                "harForståttRettigheterOgPlikter" to melding.harForståttRettigheterOgPlikter,
                                "harBekreftetOpplysninger" to melding.harBekreftetOpplysninger
                            )
                        ),
                        "korona" to melding.korona?.let {
                            mapOf(
                                "antallDagerSomSkalOverføres" to it.antallDagerSomSkalOverføres,
                                "stengingsperiode" to mapOf(
                                    "fraOgMed" to DATE_FORMATTER.format(it.stengingsperiode.fraOgMed),
                                    "tilOgMed" to DATE_FORMATTER.format(it.stengingsperiode.tilOgMed)
                                )
                            )
                        },
                        "overføring" to melding.overføring?.let {
                            mapOf(
                                "mottakerType" to it.mottakerType.type,
                                "antallDagerSomSkalOverføres" to it.antallDagerSomSkalOverføres
                            )
                        },
                        "fordeling" to melding.fordeling?.let {
                            mapOf(
                                "mottakerType" to it.mottakerType.type,
                                "ettersenderSamværsavtale" to melding.fordeling.samværsavtaleVedleggId.isEmpty()
                            )
                        },
                        "hjelp" to mapOf(
                            "språk" to melding.språk.språkTilTekst()
                        )
                    )
                )
                .resolver(MapValueResolver.INSTANCE)
                .build()
        ).let { html ->
            val outputStream = ByteArrayOutputStream()
            PdfRendererBuilder()
                .useFastMode()
                .usePdfUaAccessbility(true)
                .usePdfAConformance(PdfRendererBuilder.PdfAConformance.PDFA_1_B)
                .useColorProfile(sRGBColorSpace)
                .withHtmlContent(html, "")
                .medFonter()
                .toStream(outputStream)
                .buildPdfRenderer()
                .createPDF()
            return outputStream.use {
                it.toByteArray()
            }
        }
    }

    private fun PdfRendererBuilder.medFonter() =
        useFont(
            { ByteArrayInputStream(REGULAR_FONT) },
            "Source Sans Pro",
            400,
            BaseRendererBuilder.FontStyle.NORMAL,
            false
        )
            .useFont(
                { ByteArrayInputStream(BOLD_FONT) },
                "Source Sans Pro",
                700,
                BaseRendererBuilder.FontStyle.NORMAL,
                false
            )
            .useFont(
                { ByteArrayInputStream(ITALIC_FONT) },
                "Source Sans Pro",
                400,
                BaseRendererBuilder.FontStyle.ITALIC,
                false
            )
}

private fun Meldingstype.somTittel(): String = when(this) {
    Meldingstype.OVERFORING -> "Melding om overføring av omsorgsdager"
    Meldingstype.FORDELING -> "Melding om fordeling av omsorgsdager"
    Meldingstype.KORONA -> "Melding om overføring av omsorgsdager grunnet korona"
}

private fun List<Barn>.somMap(): List<Map<String, Any?>> = map {
    mapOf(
        "fnr" to it.identitetsnummer,
        "navn" to it.navn.capitalizeName(),
        "fødselsdato" to it.fødselsdato,
        "aleneOmOmsorgen" to it.aleneOmOmsorgen,
        "utvidetRett" to it.utvidetRett
    )
}

private fun Søker.formatertNavn() = if (mellomnavn != null) "$fornavn $mellomnavn $etternavn" else "$fornavn $etternavn"

fun String.capitalizeName(): String = split(" ").joinToString(" ") { it.lowercase().capitalize() }

private fun String.språkTilTekst() = when (this.lowercase()) {
    "nb" -> "bokmål"
    "nn" -> "nynorsk"
    else -> this
}

private fun ZonedDateTime.norskDag() = when (dayOfWeek) {
    DayOfWeek.MONDAY -> "Mandag"
    DayOfWeek.TUESDAY -> "Tirsdag"
    DayOfWeek.WEDNESDAY -> "Onsdag"
    DayOfWeek.THURSDAY -> "Torsdag"
    DayOfWeek.FRIDAY -> "Fredag"
    DayOfWeek.SATURDAY -> "Lørdag"
    else -> "Søndag"
}