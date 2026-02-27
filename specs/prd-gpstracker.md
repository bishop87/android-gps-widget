# 📱 GpsTracker — Requisiti Funzionali e Tecnici

**Versione documento:** 1.0
**App version:** 1.0.0
**Piattaforma target:** Android (min SDK 27 Android 8.1)

---

# 1. 🎯 Obiettivo dell’applicazione

L’applicazione **GpsTracker** consente di rilevare la posizione GPS del dispositivo Android e inviarla a un backend remoto tramite API HTTP.

L’invio può avvenire:

* su richiesta utente
* tramite schedulazione temporizzata in background
* tramite widget sulla home di sistema
* tramite un bottone rotondo in overlay sempre in primo piano anche su altre app

L’app deve privilegiare la massima affidabilità del tracking su dispositivi Android moderni.

---

# 2. 🧱 Requisiti tecnici di sviluppo

## 2.1 Linguaggio e stack

* Linguaggio: **Kotlin**
* Codice: **nativo Android**
* IDE supportato: **Android Studio su macOS**
* Min SDK: **Android 9 (API 28)**
* Target SDK: ultima stabile disponibile
* Architettura consigliata: **MVVM**

---

## 2.2 Dipendenze principali

L’app deve utilizzare:

* FusedLocationProviderClient per la geolocalizzazione
* WorkManager (fallback scheduling)
* Foreground Service per tracking affidabile
* Retrofit/OkHttp per chiamate API
* EncryptedSharedPreferences per credenziali

---

## 2.3 Versioning

* applicationId: `com.bishop87.gpstracker`
* versionName iniziale: **1.0.0**
* versionCode incrementale
* Build types:

  * debug
  * release con minify (R8)

---

# 3. 🔐 Gestione permessi

L’app deve gestire dinamicamente i permessi runtime.

## 3.1 Permessi richiesti

* ACCESS_FINE_LOCATION
* ACCESS_COARSE_LOCATION
* ACCESS_BACKGROUND_LOCATION (Android 10+)
* FOREGROUND_SERVICE
* POST_NOTIFICATIONS (Android 13+)
* SYSTEM_ALERT_WINDOW

---

## 3.2 Comportamento richiesto

L’app deve:

* richiedere i permessi al primo avvio
* spiegare all’utente il motivo della richiesta
* gestire il caso di permesso negato permanentemente
* fornire deep link alle impostazioni di sistema
* verificare i permessi ad ogni utilizzo critico

---

# 4. 📡 Logica di geolocalizzazione

## 4.1 Strategia di rilevazione

L’app deve:

* usare **FusedLocationProviderClient**
* combinare posizione:

  * coarse (rapida)
  * fine GPS (precisa)
* privilegiare la massima accuratezza possibile

---

## 4.2 Regole di qualità

Devono essere implementati:

* timeout fix GPS configurabile (default 20s)
* fallback a posizione coarse se GPS non disponibile
* scarto posizioni con accuracy oltre soglia configurabile
* gestione assenza segnale

---

# 5. 🔄 Modalità di invio posizione

## 5.1 Invio manuale

L’utente può avviare manualmente l’invio tramite:

* bottone nella home
* widget

---

## 5.2 Invio schedulato

L’app deve supportare tracking temporizzato.

### Requisiti:

* intervallo configurabile in secondi
* attivazione/disattivazione da impostazioni
* esecuzione affidabile anche con app chiusa

---

## 5.3 Robustezza background (REQUISITO CRITICO)

Il tracking temporizzato deve usare:

* **Foreground Service con notifica persistente**
* timer interno al service
* fallback con WorkManager
* gestione BOOT_COMPLETED per ripristino

Obiettivo: minimizzare la terminazione da parte del sistema.

---

# 6. 🔋 Ottimizzazioni batteria

L’app deve gestire le limitazioni energetiche Android.

## Requisiti:

* rilevare battery optimization attiva
* possibilità di richiedere esclusione
* schermata informativa per l’utente
* gestione Doze mode

---

# 7. 🌐 Comunicazione con backend

L’app invia i dati alla API descritta in `gps-save-api.yaml`.

---

## 7.1 Client HTTP

Requisiti:

* uso HTTPS obbligatorio
* timeout configurabile
* gestione errori di rete
* parsing robusto delle risposte

---

## 7.2 Resilienza rete

Devono essere implementati:

* retry con backoff esponenziale
* gestione timeout
* gestione HTTP 4xx/5xx
* nessun crash in caso di errore

---

## 7.3 Anti-spam e ottimizzazione

Per evitare sovraccarico:

* intervallo minimo tra invii
* deduplicazione posizioni ravvicinate
* (opzionale) soglia minima di movimento

---

# 8. 🔐 Sicurezza credenziali

Le credenziali API devono essere protette.

## Requisiti:

* uso EncryptedSharedPreferences
* nessun salvataggio in chiaro
* supporto solo HTTPS
* mascheramento password in UI

---

# 9. 🏠 Schermata Home

La home deve contenere:

* bottone **Test chiamata API**
* bottone **Impostazioni**
* label versione app a fondo pagina

---

## 9.1 Comportamento bottone test

Alla pressione:

* rileva posizione
* invia all’API
* mostra stato operazione
* gestisce errori senza crash

---

# 10. ⚙️ Schermata Impostazioni

La schermata deve permettere di configurare:

* nome device
* URL API
* username
* password
* toggle tracking background
* intervallo tracking (secondi)
* bottone salva

---

## 10.1 Persistenza impostazioni

* salvataggio su SharedPreferences cifrate
* caricamento automatico all’avvio
* prima apertura: campi vuoti con placeholder
* validazione input

---

# 11. 🧩 Widget Home Android

L’app deve fornire un widget installabile.

---

## 11.1 UI widget

Il widget deve mostrare:

* bottone rotondo
* etichetta nome app

---

## 11.2 Comportamento

Alla pressione:

1. mostra stato "acquiring GPS"
2. rileva posizione precisa
3. invia dati all’API
4. mostra esito

---

## 11.3 Stati widget

Devono essere gestiti:

* idle
* acquiring
* sending
* success
* error

---

## 11.4 Robustezza widget

* debounce tap multipli
* timeout operazione
* ripristino dopo reboot

---

# 12. 🧠 Gestione errori

L’app non deve mai crashare per errori prevedibili.

Devono essere gestiti:

* errori rete
* GPS non disponibile
* permessi mancanti
* API error
* timeout

---

# 13. 📜 Logging

Richiesto logging strutturato.

## Requisiti:

* livelli: DEBUG, INFO, WARN, ERROR
* tag coerenti
* toggle debug mode
* log utili per troubleshooting

---

# 14. 🔒 Privacy e compliance

L’app deve includere:

* informativa privacy
* spiegazione uso posizione
* consenso esplicito utente
* conformità Play Store (se pubblicata)

---

# 15. ⭐ Requisiti opzionali (future release)

Non obbligatori per v1.0 ma consigliati:

* buffer offline posizioni
* invio batch
* health check API
* supporto tema chiaro/scuro
* localizzazione multilingua
* test automatici
* esportazione log

---

# 16. vista con Bottone Overlay

L'app deve includere:

* nella schermata principale uno switch che attiva o disattiva la comparsa di una vista in overlay con un singolo bottone
* il bottone in overlay deve essere visibile anche su altre applicazioni
* il bottone in overlay funziona come quello del widget
* alla pressione del bottone viene invocata l'api con la posizione gps 

---

# ✅ Criteri di accettazione MVP

La versione 1.0.0 è considerata valida se:

* l’app non crasha in condizioni normali
* il widget invia correttamente la posizione
* il tracking schedulato sopravvive in background
* le credenziali sono salvate in modo sicuro
* i permessi sono gestiti correttamente

---

**Fine documento**
