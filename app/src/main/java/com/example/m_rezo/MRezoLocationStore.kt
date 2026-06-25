package com.example.m_rezo

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * LA CHAMBRE FORTE - Stockage ultra-sécurisé des emplacements
 * Chiffrement AES256-GCM - Inviolable
 */
class MRezoLocationStore(context: Context) {

    private val sharedPreferences: SharedPreferences

    init {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        sharedPreferences = EncryptedSharedPreferences.create(
            context,
            "mrezo_location_vault",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /**
     * Sauvegarde les emplacements de tous les opérateurs
     */
    fun saveLocations(mtnLocation: String, orangeLocation: String, moovLocation: String) {
        sharedPreferences.edit().apply {
            putString("location_mtn", mtnLocation)
            putString("location_orange", orangeLocation)
            putString("location_moov", moovLocation)
            apply()
        }
    }

    /**
     * Récupère l'emplacement d'un opérateur spécifique
     */
    fun getLocationForOperator(operator: OperatorBrand): String? {
        return when (operator) {
            OperatorBrand.MTN -> sharedPreferences.getString("location_mtn", null)
            OperatorBrand.ORANGE -> sharedPreferences.getString("location_orange", null)
            OperatorBrand.MOOV -> sharedPreferences.getString("location_moov", null)
        }
    }

    /**
     * Charge tous les emplacements
     */
    fun loadAllLocations(): OperatorLocations {
        return OperatorLocations(
            mtnLocation = sharedPreferences.getString("location_mtn", "") ?: "",
            orangeLocation = sharedPreferences.getString("location_orange", "") ?: "",
            moovLocation = sharedPreferences.getString("location_moov", "") ?: ""
        )
    }

    /**
     * Efface tous les emplacements (pour déconnexion ou réinitialisation)
     */
    fun clearAllLocations() {
        sharedPreferences.edit().clear().apply()
    }

    /**
     * Vérifie si au moins un emplacement est configuré
     */
    fun hasLocationsConfigured(): Boolean {
        val locations = loadAllLocations()
        return locations.mtnLocation.isNotBlank() ||
               locations.orangeLocation.isNotBlank() ||
               locations.moovLocation.isNotBlank()
    }
}

/**
 * Data class pour stocker les emplacements
 */
data class OperatorLocations(
    val mtnLocation: String = "",
    val orangeLocation: String = "",
    val moovLocation: String = ""
)
