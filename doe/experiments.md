# DOE — Esperimenti Pianificati

## Obiettivo

Ridurre i rischi tecnici prima della release 1.0.

---

# 🔴 EXP-001 — Sopravvivenza Foreground Service

## Ipotesi

Il Foreground Service rimane attivo su Android 9+ per almeno 2 ore.

## Metodo

- avviare tracking
- spegnere app in background
- monitorare per 120 minuti

## Metriche

- service alive time
- numero restart
- consumo batteria

## Criterio successo

Service attivo ≥ 95% del tempo.

---

# 🔴 EXP-002 — Accuratezza GPS vs Timeout

## Ipotesi

Timeout 20s produce accuracy < 30m nella maggioranza dei casi.

## Metodo

- 50 rilevazioni outdoor
- 50 indoor
- confrontare accuracy

## Metriche

- accuracy media
- tempo fix
- fallback rate

## Decisione attesa

Definizione timeout ottimale.

---

# 🟡 EXP-003 — Comportamento Widget in Doze

## Ipotesi

Il widget riesce a completare invio posizione in Doze.

## Metodo

- forzare Doze
- premere widget
- verificare invio

## Metriche

- success rate
- tempo risposta
- errori

---

# 🟡 EXP-004 — Retry rete

## Ipotesi

Backoff esponenziale riduce perdita dati.

## Metodo

- simulare rete instabile
- confrontare con/senza retry

## Metriche

- richieste perse
- tempo medio invio

---

# 🟢 EXP-005 — Impatto batteria

## Ipotesi

Tracking ogni 5 minuti è accettabile.

## Metodo

- test 24h
- monitor battery drain

## Target

< 5%/ora (indicativo)

---

# 📊 Note DOE

Ogni esperimento deve produrre:

- log
- screenshot
- decisione in `doe/decisions.md`