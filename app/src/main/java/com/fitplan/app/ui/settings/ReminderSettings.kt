package com.fitplan.app.ui.settings

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings as AndroidSettings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.fitplan.app.data.Prefs
import com.fitplan.app.data.Program
import com.fitplan.app.data.Settings
import com.fitplan.app.data.asClock
import com.fitplan.app.data.indo
import com.fitplan.app.reminders.Notifications
import com.fitplan.app.reminders.ReminderScheduler
import com.fitplan.app.ui.common.BigButton
import com.fitplan.app.ui.common.SectionCard
import com.fitplan.app.ui.theme.Green

/** Bagian "Pengingat" di layar Pengaturan. */
@Composable
fun ReminderSettings(prefs: Prefs, settings: Settings) {
    val context = LocalContext.current
    var refresh by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        refresh++
        onPauseOrDispose { }
    }
    val notifOk = remember(refresh) { Notifications.canPost(context) }
    val exactOk = remember(refresh) { ReminderScheduler.canExact(context) }
    val batteryOk = remember(refresh) { isIgnoringBattery(context) }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { refresh++ }

    // Waktu yang sedang diedit: null = tidak ada dialog
    var editing by remember { mutableStateOf<TimeEdit?>(null) }

    // ---- Status izin ----
    SectionCard {
        Text("Status pengingat", style = MaterialTheme.typography.titleLarge)
        StatusLine(notifOk, if (notifOk) "Izin notifikasi aktif" else "Izin notifikasi belum aktif")
        if (!notifOk) {
            BigButton("Izinkan notifikasi", onClick = {
                if (Build.VERSION.SDK_INT >= 33) {
                    notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    openAppNotificationSettings(context)
                }
            }, icon = Icons.Filled.NotificationsActive)
        }
        StatusLine(exactOk, if (exactOk) "Alarm tepat waktu aktif" else "Alarm tepat waktu belum diizinkan")
        if (!exactOk && Build.VERSION.SDK_INT >= 31) {
            BigButton("Izinkan alarm tepat waktu", onClick = {
                context.startActivity(
                    Intent(AndroidSettings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                        Uri.parse("package:" + context.packageName)),
                )
            }, icon = Icons.Filled.Timer)
        }
        StatusLine(batteryOk, if (batteryOk) "Optimasi baterai dimatikan untuk FitPlan"
        else "Optimasi baterai masih aktif (pengingat bisa terlambat)")
        if (!batteryOk) {
            BigButton("Matikan optimasi baterai", onClick = { requestIgnoreBattery(context) },
                icon = Icons.Filled.BatteryAlert, outlined = true)
        }
    }

    // ---- Pengingat latihan ----
    SectionCard {
        SwitchRow("Pengingat latihan", settings.workoutReminderOn) { on ->
            prefs.update { it.copy(workoutReminderOn = on) }
        }
        if (settings.workoutReminderOn) {
            Text("Ketuk jam untuk mengubah:", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Program.weekly.forEach { (dow, day) ->
                val minutes = settings.workoutTimes[dow] ?: Settings.DEFAULT_WORKOUT_TIME
                TimeRow("${dow.indo()} · ${day.title}", minutes) {
                    editing = TimeEdit("Jam latihan ${dow.indo()}", minutes) { m ->
                        prefs.update { it.copy(workoutTimes = it.workoutTimes + (dow to m)) }
                    }
                }
            }
        }
        if (settings.startDate == null) {
            Text("Pilih tanggal mulai program dulu supaya pengingat latihan berjalan.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.tertiary)
        }
    }

    // ---- Timbang & protein ----
    SectionCard {
        SwitchRow("Timbang badan tiap Minggu pagi", settings.weighReminderOn) { on ->
            prefs.update { it.copy(weighReminderOn = on) }
        }
        if (settings.weighReminderOn) {
            TimeRow("Jam timbang (Minggu)", settings.weighTime) {
                editing = TimeEdit("Jam timbang badan", settings.weighTime) { m ->
                    prefs.update { it.copy(weighTime = m) }
                }
            }
        }
        HorizontalDivider(Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.surfaceVariant)
        SwitchRow("Pengingat protein / makan", settings.proteinReminderOn) { on ->
            prefs.update { it.copy(proteinReminderOn = on) }
        }
        if (settings.proteinReminderOn) {
            TimeRow("Jam pengingat (setiap hari)", settings.proteinTime) {
                editing = TimeEdit("Jam pengingat protein", settings.proteinTime) { m ->
                    prefs.update { it.copy(proteinTime = m) }
                }
            }
            Text("Di hari libur, pesannya berisi pengingat istirahat + protein.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    // ---- Tes ----
    SectionCard {
        Text("Tes pengingat", style = MaterialTheme.typography.titleLarge)
        BigButton("Kirim notifikasi sekarang", outlined = true, onClick = {
            Notifications.show(context, 901, "Tes notifikasi ✅", "Notifikasi FitPlan sudah bisa muncul.")
            if (!Notifications.canPost(context)) {
                Toast.makeText(context, "Izinkan notifikasi dulu", Toast.LENGTH_LONG).show()
            }
        })
        BigButton("Tes alarm 1 menit lagi", outlined = true, onClick = {
            ReminderScheduler.scheduleTest(context, 60)
            Toast.makeText(context, "Tutup aplikasi & kunci layar. Notifikasi muncul dalam 1 menit.",
                Toast.LENGTH_LONG).show()
        })
    }

    editing?.let { e ->
        TimePickerDialog(e.title, e.minutes, onDismiss = { editing = null }) { m ->
            e.onSave(m)
            editing = null
        }
    }
}

private class TimeEdit(val title: String, val minutes: Int, val onSave: (Int) -> Unit)

@Composable
private fun StatusLine(ok: Boolean, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            if (ok) Icons.Filled.CheckCircle else Icons.Filled.Warning,
            contentDescription = null,
            tint = if (ok) Green else Color(0xFFFFCA28),
            modifier = Modifier.padding(end = 8.dp),
        )
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun TimeRow(label: String, minutes: Int, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(minutes.asClock(), style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(title: String, minutes: Int, onDismiss: () -> Unit, onSave: (Int) -> Unit) {
    val state = rememberTimePickerState(minutes / 60, minutes % 60, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { TimePicker(state = state) },
        confirmButton = { TextButton(onClick = { onSave(state.hour * 60 + state.minute) }) { Text("Simpan") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}

private fun isIgnoringBattery(context: Context): Boolean =
    context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName)

@SuppressLint("BatteryLife")
private fun requestIgnoreBattery(context: Context) {
    try {
        context.startActivity(
            Intent(AndroidSettings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                Uri.parse("package:" + context.packageName)),
        )
    } catch (_: Exception) {
        context.startActivity(Intent(AndroidSettings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
    }
}

private fun openAppNotificationSettings(context: Context) {
    context.startActivity(
        Intent(AndroidSettings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(AndroidSettings.EXTRA_APP_PACKAGE, context.packageName),
    )
}
