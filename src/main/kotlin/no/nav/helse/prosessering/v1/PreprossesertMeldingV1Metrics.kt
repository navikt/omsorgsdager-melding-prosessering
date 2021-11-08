package no.nav.helse.prosessering.v1

import io.prometheus.client.Counter
import no.nav.helse.prosessering.v1.melding.PreprossesertMelding

private val generellCounter = Counter.build()
    .name("generell_counter")
    .help("Generell counter")
    .labelNames("spm", "svar")
    .register()

internal fun PreprossesertMelding.reportMetrics(){
    if(this.fordeling != null){
        if(fordeling.samværsavtale.isEmpty()) generellCounter.labels("vedlegg", "Nei").inc()
        else generellCounter.labels("vedlegg", "Ja").inc()
    }
}
