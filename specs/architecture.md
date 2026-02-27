# GpsTracker — Architettura Tecnica

## Versione
1.0

## Obiettivo

Definire la struttura architetturale dell’app GpsTracker per garantire:

- affidabilità background
- manutenibilità
- compatibilità Android 9+
- integrazione fluida con agenti Antigravity

---

# 🧱 Pattern architetturale

Architettura: **MVVM + Service Layer**

Strati principali:

- UI Layer
- ViewModel Layer
- Domain/UseCase Layer
- Data Layer
- System Services Layer

---

# 📦 Moduli principali

## 1. UI Layer

Responsabilità:

- rendering schermate
- gestione input utente
- binding con ViewModel

Componenti:

- MainActivity
- SettingsActivity
- WidgetProvider

---

## 2. ViewModel Layer

Responsabilità:

- orchestrazione logica UI
- gestione stato
- chiamata use case

ViewModel previsti:

- MainViewModel
- SettingsViewModel

---

## 3. Domain / Use Cases

Responsabilità:

- logica applicativa pura
- indipendenza da Android framework

Use case principali:

- GetCurrentLocationUseCase
- SendLocationUseCase
- StartTrackingUseCase
- StopTrackingUseCase

---

## 4. Data Layer

Responsabilità:

- accesso rete
- persistenza locale
- mapping dati

Componenti:

- LocationRepository
- ApiService (Retrofit)
- SettingsRepository

---

## 5. System Services Layer

⚠️ **CRITICO PER L’AFFIDABILITÀ**

Componenti:

- TrackingForegroundService
- BootReceiver
- WorkManager fallback
- BatteryOptimizationHelper

---

# 🔄 Flussi principali

## Invio manuale

UI → ViewModel → GetCurrentLocationUseCase → SendLocationUseCase → API

---

## Tracking schedulato

ForegroundService → timer interno → location → API

Fallback:

WorkManager periodico

---

## Widget flow

Widget → BroadcastReceiver → ForegroundService (one-shot) → API

---

# 🔐 Sicurezza

- EncryptedSharedPreferences per credenziali
- HTTPS obbligatorio
- timeout rete configurabile

---

# 🔋 Strategia sopravvivenza background

Ordine di priorità:

1. Foreground Service persistente
2. Richiesta ignore battery optimization
3. WorkManager fallback
4. Ripristino dopo reboot

---

# 🧪 Punti critici noti

- kill aggressivo OEM
- permessi background location
- Doze mode
- latenza GPS indoor

Questi punti sono coperti in `doe/experiments.md`.