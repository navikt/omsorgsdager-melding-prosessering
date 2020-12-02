# Omsorgsdager melding prosessering

![CI / CD](https://github.com/navikt/omsorgsdager-melding-prosessering/workflows/CI%20/%20CD/badge.svg)
![NAIS Alerts](https://github.com/navikt/omsorgsdager-melding-prosessering/workflows/Alerts/badge.svg)

# Innholdsoversikt
* [1. Kontekst](#1-kontekst)
* [2. Funksjonelle Krav](#2-funksjonelle-krav)
* [2.1 Feil i prosessering](#feil-i-prosessering)
* [3. Utviklingsmiljø](#10-utviklingsmilj)
* [4. Drift og støtte](#11-drift-og-sttte)

# 1. Kontekst
Prosesseringstjeneste for melding om deling/fordeling/koronaoverføring av omsorgsdager.

# 2. Funksjonelle krav
Tjenesten konsumerer meldinger fra topicen "privat-omsorgsdager-melding-mottatt" som 
[omsorgsdager-melding-api](https://github.com/navikt/omsorgsdager-melding-api) har produsert. 

Videre blir søknaden preprosessert, pdf generert og lagret i [K9-mellomlagring](https://github.com/navikt/k9-mellomlagring). 

Tjenesten journalfører så søknaden mot [K9-Joark](https://github.com/navikt/k9-joark). Deretter blir pdf og andre dokumenter som ble lagret 
i k9-mellomlagring slettet og til slutt legges søknaden på topic "k9-rapid-v2" som en behovssekvens som omsorgspenger-rammemeldinger konsumerer.

All kontakt med K9-mellomlagring går gjennom [service-discovery](https://doc.nais.io/clusters/team-namespaces#service-discovery-in-kubernetes), i.e "http://k9-mellomlagring"

## Feil i prosessering
Ved feil i en av streamene som håndterer prosesseringen vil streamen stoppe, og tjenesten gi 503 response på liveness etter 15 minutter.
Når tjenenesten restarter vil den forsøke å prosessere søknaden på ny og fortsette slik frem til den lykkes.

## Alarmer
Vi bruker [nais-alerts](https://doc.nais.io/observability/alerts) for å sette opp alarmer. Disse finner man konfigurert i [nais/alerterator.yml](nais/alerterator.yml).

## Henvendelser
Spørsmål knyttet til koden eller prosjektet kan stilles som issues her på GitHub.

Interne henvendelser kan sendes via Slack i kanalen #team-düsseldorf.
