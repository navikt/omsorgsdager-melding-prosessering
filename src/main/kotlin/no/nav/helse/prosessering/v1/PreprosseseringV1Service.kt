package no.nav.helse.prosessering.v1

import no.nav.helse.dokument.DokumentGateway
import no.nav.helse.dokument.DokumentService
import no.nav.helse.felles.CorrelationId
import no.nav.helse.felles.Metadata
import no.nav.helse.felles.SøknadId
import no.nav.helse.prosessering.v1.melding.Melding
import no.nav.helse.prosessering.v1.melding.Meldingstype
import no.nav.helse.prosessering.v1.melding.PreprossesertMelding
import org.slf4j.LoggerFactory

internal class PreprosseseringV1Service(
    private val pdfV1Generator: PdfV1Generator,
    private val dokumentService: DokumentService
) {

    private companion object {
        private val logger = LoggerFactory.getLogger(PreprosseseringV1Service::class.java)
    }

    internal suspend fun preprosseser(
        melding: Melding,
        metadata: Metadata
    ): PreprossesertMelding {
        val søknadId = SøknadId(melding.søknadId)
        logger.trace("Preprosseserer $søknadId")

        val correlationId = CorrelationId(metadata.correlationId)
        val dokumentEier = DokumentGateway.DokumentEier(melding.søker.fødselsnummer)

        logger.trace("Genererer Oppsummerings-PDF av søknaden.")
        val søknadOppsummeringPdf = pdfV1Generator.generateSoknadOppsummeringPdf(melding)
        logger.trace("Generering av Oppsummerings-PDF OK.")

        logger.trace("Mellomlagrer Oppsummerings-PDF.")

        val soknadOppsummeringPdfUrl = dokumentService.lagreSoknadsOppsummeringPdf(
            pdf = søknadOppsummeringPdf,
            correlationId = correlationId,
            dokumentEier = dokumentEier,
            dokumentbeskrivelse = when(melding.type) {
                Meldingstype.OVERFORING -> "Melding om deling av omsorgsdager"
                Meldingstype.FORDELING -> "Melding om fordeling av omsorgsdager"
                Meldingstype.KORONA -> "Melding om overføring av omsorgsdager"
            }
        )

        logger.trace("Mellomlagring av Oppsummerings-PDF OK")

        logger.trace("Mellomlagrer Oppsummerings-JSON")

        val søknadJsonUrl = dokumentService.lagreSoknadsMelding(
            melding = melding,
            dokumentEier = dokumentEier,
            correlationId = correlationId
        )
        logger.trace("Mellomlagrer Oppsummerings-JSON OK.")

        val komplettDokumentUrls = mutableListOf(
            listOf(
                soknadOppsummeringPdfUrl,
                søknadJsonUrl
            )
        )

        if (melding.type == Meldingstype.FORDELING && melding.fordeling != null && melding.fordeling.samværsavtale.isNotEmpty()) {
            melding.fordeling.samværsavtale.map { komplettDokumentUrls.add(listOf(it.toURI())) }
        }

        logger.trace("Totalt ${komplettDokumentUrls.size} dokumentbolker.")

        val preprossesertMeldingV1 = PreprossesertMelding(
            melding = melding,
            dokumentUrls = komplettDokumentUrls.toList()
        )
        preprossesertMeldingV1.reportMetrics()
        return preprossesertMeldingV1
    }

}
