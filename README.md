# Mobilt Java Lifecycle App

## Beskrivning
En Android-app i Kotlin som hanterar användarinloggning och profil med Firebase.  
Stöder email/lösenord och Google-inloggning, registrering och personlig profil.

## Funktioner
- **Inloggning:** Email/lösenord och Google (One-Tap eller klassisk)  
- **Registrering:** Validering av email, lösenord, personnummer och telefon  
- **Profil:** Redigera ålder, kön, körkort, email och bio  
- **Data:** Sparas lokalt (SharedPreferences) och i Firebase (Realtime DB + Firestore)  

## Krav
- Android Studio  
- Firebase-projekt med Authentication, Realtime Database och Firestore  
- Internet-tillstånd i `AndroidManifest.xml`
