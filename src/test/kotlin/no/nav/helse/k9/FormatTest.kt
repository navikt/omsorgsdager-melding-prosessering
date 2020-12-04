package no.nav.helse.k9

import org.json.JSONObject
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

internal fun String.assertK9RapidFormat(id: String) {
    val rawJson = JSONObject(this)

    assertEquals(rawJson.getJSONArray("@behovsrekkefølge").getString(0), "OverføreOmsorgsdager")
    assertEquals(rawJson.getString("@type"),"Behovssekvens")
    assertEquals(rawJson.getString("@id"), id)

    assertNotNull(rawJson.getString("@correlationId"))
    assertNotNull(rawJson.getJSONObject("@behov"))
}
