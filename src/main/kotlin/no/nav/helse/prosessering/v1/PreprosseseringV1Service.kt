package no.nav.helse.prosessering.v1

import no.nav.helse.dokument.DokumentGateway
import no.nav.helse.dokument.DokumentService
import no.nav.helse.felles.CorrelationId
import no.nav.helse.felles.Metadata
import no.nav.helse.felles.SøknadId
import no.nav.helse.prosessering.v1.melding.Melding
import no.nav.helse.prosessering.v1.melding.Meldingstype.*
import no.nav.helse.prosessering.v1.melding.PreprossesertMelding
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.URL

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
        logger.trace("Preprosesserer $søknadId")

        val correlationId = CorrelationId(metadata.correlationId)
        val dokumentEier = DokumentGateway.DokumentEier(melding.søker.fødselsnummer)

        logger.info("Genererer Oppsummerings-PDF av søknaden.")
        val oppsummeringPdf = pdfV1Generator.generateSoknadOppsummeringPdf(melding)

        logger.info("Mellomlagrer Oppsummerings-PDF.")
        val oppsummeringPdfDokumentId = dokumentService.lagreSoknadsOppsummeringPdf(
            pdf = oppsummeringPdf,
            correlationId = correlationId,
            dokumentEier = dokumentEier,
            dokumentbeskrivelse = when(melding.type) {
                OVERFORING -> "Melding om deling av omsorgsdager"
                FORDELING -> "Melding om fordeling av omsorgsdager"
                KORONA -> "Melding om overføring av omsorgsdager"
            }
        ).dokumentId()


        logger.info("Mellomlagrer Oppsummerings-JSON")
        val søknadJsonDokumentId = dokumentService.lagreSoknadsMelding(
            melding = melding,
            dokumentEier = dokumentEier,
            correlationId = correlationId
        ).dokumentId()

        val komplettDokumentId = mutableListOf(
            listOf(
                oppsummeringPdfDokumentId,
                søknadJsonDokumentId
            )
        )

        if(melding.type == FORDELING && melding.fordeling != null){
            if(melding.fordeling.samværsavtaleVedleggId.isNotEmpty()){
                melding.fordeling.samværsavtaleVedleggId.forEach { vedleggId ->
                    komplettDokumentId.add(listOf(vedleggId))
                }
            }
        }

        logger.info("Totalt ${komplettDokumentId.size} dokumentbolker med totalt ${komplettDokumentId.flatten().size} dokumenter.")

        val preprossesertMeldingV1 = PreprossesertMelding(
            melding = melding,
            dokumentId = komplettDokumentId.toList()
        )
        preprossesertMeldingV1.reportMetrics()
        return preprossesertMeldingV1
    }

}

fun URI.dokumentId(): String = this.toString().substringAfterLast("/")
fun URL.dokumentId(): String = this.toString().substringAfterLast("/")