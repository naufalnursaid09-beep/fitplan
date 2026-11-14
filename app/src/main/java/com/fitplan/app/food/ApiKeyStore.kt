package com.fitplan.app.food

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/** Menyimpan API key Claude secara terenkripsi di HP (tidak pernah ditulis di kode). */
class ApiKeyStore(context: Context) {
    private val sp: SharedPreferences = run {
        val key = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "fitplan_secure",
            key,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    fun get(): String? = sp.getString(K_API_KEY, null)?.takeIf { it.isNotBlank() }

    fun set(value: String) = sp.edit().putString(K_API_KEY, value.trim()).apply()

    fun clear() = sp.edit().remove(K_API_KEY).apply()

    /** Contoh: "sk-ant-…a1B2" */
    fun masked(): String? = get()?.let { k ->
        if (k.length <= 12) "••••" else k.take(7) + "…" + k.takeLast(4)
    }

    private companion object {
        const val K_API_KEY = "anthropic_api_key"
    }
}
