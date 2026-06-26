package com.lecteur23.mrezo

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * LA CHAMBRE FORTE - Stockage ultra-sécurisé des codes PIN
 * Chiffrement AES256-GCM - Inviolable
 */
class MRezoPinStore(context: Context) {

    private val sharedPreferences: SharedPreferences

    init {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        sharedPreferences = EncryptedSharedPreferences.create(
            context,
            "mrezo_pin_vault",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /**
     * Sauvegarde les PINs de tous les opérateurs
     */
    fun savePins(mtnPin: String, orangePin: String, moovPin: String) {
        sharedPreferences.edit().apply {
            putString("pin_mtn", mtnPin)
            putString("pin_orange", orangePin)
            putString("pin_moov", moovPin)
            apply()
        }
    }

    /**
     * Récupère le PIN d'un opérateur spécifique
     */
    fun getPinForOperator(operator: OperatorBrand): String? {
        return when (operator) {
            OperatorBrand.MTN -> sharedPreferences.getString("pin_mtn", null)
            OperatorBrand.ORANGE -> sharedPreferences.getString("pin_orange", null)
            OperatorBrand.MOOV -> sharedPreferences.getString("pin_moov", null)
        }
    }

    /**
     * Charge tous les PINs
     */
    fun loadAllPins(): OperatorPins {
        return OperatorPins(
            mtnPin = sharedPreferences.getString("pin_mtn", "") ?: "",
            orangePin = sharedPreferences.getString("pin_orange", "") ?: "",
            moovPin = sharedPreferences.getString("pin_moov", "") ?: ""
        )
    }

    /**
     * Efface tous les PINs (pour déconnexion ou réinitialisation)
     */
    fun clearAllPins() {
        sharedPreferences.edit().clear().apply()
    }

    /**
     * Vérifie si au moins un PIN est configuré
     */
    fun hasPinsConfigured(): Boolean {
        val pins = loadAllPins()
        return pins.mtnPin.isNotBlank() ||
               pins.orangePin.isNotBlank() ||
               pins.moovPin.isNotBlank()
    }
}

/**
 * Data class pour stocker les PINs
 */
data class OperatorPins(
    val mtnPin: String = "",
    val orangePin: String = "",
    val moovPin: String = ""
)
