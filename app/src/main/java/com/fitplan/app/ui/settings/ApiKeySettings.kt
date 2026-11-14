package com.fitplan.app.ui.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.fitplan.app.FitPlanApp
import com.fitplan.app.ui.common.BigButton
import com.fitplan.app.ui.common.SectionCard
import com.fitplan.app.ui.theme.Green

@Composable
fun ApiKeySettings() {
    val store = FitPlanApp.instance.apiKeys
    val context = LocalContext.current
    var version by remember { mutableIntStateOf(0) }
    val masked = remember(version) { store.masked() }
    var input by remember { mutableStateOf("") }

    SectionCard {
        Text("API key Claude (untuk foto makanan)", style = MaterialTheme.typography.titleLarge)
        if (masked != null) {
            Text("✅ Tersimpan terenkripsi: $masked", style = MaterialTheme.typography.bodyLarge, color = Green)
        } else {
            Text("Belum diisi.", style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.tertiary)
        }
        OutlinedTextField(
            value = input,
            onValueChange = { input = it.trim() },
            label = { Text(if (masked == null) "Tempel API key (sk-ant-…)" else "Ganti dengan key baru") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )
        BigButton("Simpan API key", icon = Icons.Filled.Key, enabled = input.length > 20, onClick = {
            if (!input.startsWith("sk-ant-")) {
                Toast.makeText(context, "API key biasanya diawali sk-ant-. Periksa lagi.", Toast.LENGTH_LONG).show()
            }
            store.set(input)
            input = ""
            version++
            Toast.makeText(context, "API key tersimpan", Toast.LENGTH_SHORT).show()
        })
        if (masked != null) {
            TextButton(onClick = { store.clear(); version++ }) { Text("Hapus API key") }
        }
        TextButton(onClick = {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://console.anthropic.com/settings/keys")))
        }) { Text("Buka console.anthropic.com untuk membuat key") }
    }
}
