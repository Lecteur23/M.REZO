# M-REZO - Application de Gestion Mobile Money

[![Android CI Build](https://github.com/Lecteur23/M.REZO/actions/workflows/android-build.yml/badge.svg)](https://github.com/Lecteur23/M.REZO/actions)

Application Android pour automatiser les opérations USSD des opérateurs mobile money en Afrique.

## 🚀 Fonctionnalités

- ✅ Support multi-opérateurs (MTN, Orange, Moov)
- ✅ Enregistrement sécurisé des codes PIN (chiffrement AES256-GCM)
- ✅ Auto-injection des PINs lors des opérations USSD
- ✅ Service d'accessibilité pour navigation USSD automatique
- ✅ Overlay sécurisé avec timeout de 40 secondes
- ✅ Historique des opérations
- ✅ Interface Material Design 3 (Jetpack Compose)

## 📱 Prérequis

- Android 8.0 (API 26) minimum
- Android 14 (API 36) cible
- Permissions: CALL_PHONE, SYSTEM_ALERT_WINDOW, Service Accessibilité

## 🔐 Sécurité

- Chiffrement AES256-GCM via Android Keystore
- Stockage sécurisé avec EncryptedSharedPreferences
- Code secret à 6 chiffres pour verrouiller l'app
- Auto-verrouillage après 15 secondes d'inactivité

## 🛠️ Technologies

- Kotlin
- Jetpack Compose
- Material 3
- AndroidX Security Crypto
- Accessibility Service API
- USSD API (Android 8+)

## 📦 Compilation

### Depuis Android Studio
```bash
./gradlew assembleDebug
```

### CI/CD
Le projet est configuré pour AppVeyor. Chaque push déclenche une compilation automatique.

## 📂 Structure

```
app/src/main/java/com/example/m_rezo/
├── MainActivity.kt                  # Interface principale
├── MRezoPinStore.kt                # Coffre-fort PINs
├── MRezoLocationStore.kt           # Coffre-fort emplacements
├── OperatorBrand.kt                # Enum opérateurs
├── MRezoOverlayController.kt       # Gestion overlay
├── MRezoUssdAccessibilityService.kt # Service USSD
└── PermissionsSetupScreen.kt       # Configuration permissions
```

## 🔄 Flux de l'application

1. Configuration des permissions (Téléphone, Overlay, Accessibilité)
2. Écran d'introduction
3. Création du code secret (6 chiffres)
4. Sélection des réseaux opérateurs
5. **Enregistrement des codes PIN** (automatique selon réseaux)
6. Écran principal avec catalogues d'offres
7. Exécution USSD automatique avec injection PIN

## 📝 License

Tous droits réservés.

## 👨‍💻 Auteur

M-REZO Team
