package com.fitplan.app.food

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.util.Base64
import androidx.exifinterface.media.ExifInterface
import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.anthropic.core.JsonValue
import com.anthropic.errors.AnthropicIoException
import com.anthropic.errors.AnthropicServiceException
import com.anthropic.errors.PermissionDeniedException
import com.anthropic.errors.RateLimitException
import com.anthropic.errors.UnauthorizedException
import com.anthropic.models.messages.Base64ImageSource
import com.anthropic.models.messages.ContentBlockParam
import com.anthropic.models.messages.ImageBlockParam
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.OutputConfig
import com.anthropic.models.messages.TextBlockParam
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.time.Duration

/** Hasil estimasi dari Claude. */
data class FoodEstimate(
    val name: String,
    val portion: String,
    val kcal: Int,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val confidence: String,
    val note: String,
)

sealed interface AnalyzeResult {
    data class Ok(val estimate: FoodEstimate) : AnalyzeResult
    data class NotFood(val message: String) : AnalyzeResult
    data class Error(val message: String) : AnalyzeResult
}

object FoodAnalyzer {
    /** Model Claude dengan kemampuan membaca gambar. */
    const val MODEL = "claude-opus-5-5"

    private const val PROMPT = """
Kamu ahli gizi. Lihat foto ini dan perkirakan kandungan gizi makanan/minuman di dalamnya
(seluruh porsi yang terlihat). Gunakan pengetahuan tentang makanan Indonesia bila relevan.

Balas HANYA dengan satu objek JSON, tanpa teks lain, dengan format:
{
  "is_food": true,
  "name": "nama makanan dalam bahasa Indonesia",
  "portion": "perkiraan porsi, mis. 1 piring (± 350 g)",
  "kcal": 0,
  "protein_g": 0,
  "carbs_g": 0,
  "fat_g": 0,
  "confidence": "rendah" | "sedang" | "tinggi",
  "note": "catatan singkat, mis. asumsi cara masak"
}
Jika foto bukan makanan atau minuman, balas {"is_food": false, "note": "alasan singkat"}.
"""

    /** Kecilkan foto (maks 1280 px) dan ubah ke JPEG supaya hemat biaya & cepat. */
    fun loadAndCompress(context: Context, uri: Uri): ByteArray? {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) } ?: return null
        val maxSide = maxOf(bounds.outWidth, bounds.outHeight)
        if (maxSide <= 0) return null
        var sample = 1
        while (maxSide / (sample * 2) >= 1280) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        var bmp = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) } ?: return null

        // Putar sesuai orientasi kamera
        val rotation = runCatching {
            resolver.openInputStream(uri)?.use { ExifInterface(it).rotationDegrees } ?: 0
        }.getOrDefault(0)
        val scale = 1280f / maxOf(bmp.width, bmp.height)
        if (rotation != 0 || scale < 1f) {
            val m = Matrix().apply {
                if (scale < 1f) postScale(scale, scale)
                if (rotation != 0) postRotate(rotation.toFloat())
            }
            bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
        }
        return ByteArrayOutputStream().use { out ->
            bmp.compress(Bitmap.CompressFormat.JPEG, 85, out)
            out.toByteArray()
        }
    }

    fun isOnline(context: Context): Boolean {
        val cm = context.getSystemService(ConnectivityManager::class.java)
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    suspend fun analyze(context: Context, apiKey: String, jpeg: ByteArray): AnalyzeResult =
        withContext(Dispatchers.IO) {
            if (!isOnline(context)) {
                return@withContext AnalyzeResult.Error("Tidak ada koneksi internet. Nyalakan data/Wi-Fi lalu coba lagi.")
            }
            val client = AnthropicOkHttpClient.builder()
                .apiKey(apiKey)
                .timeout(Duration.ofSeconds(120))
                .build()
            try {
                val b64 = Base64.encodeToString(jpeg, Base64.NO_WRAP)
                val params = MessageCreateParams.builder()
                    .model(MODEL)
                    .maxTokens(4000L)
                    // Tugas sederhana: effort rendah = lebih cepat & murah.
                    .outputConfig(OutputConfig.builder().effort(OutputConfig.Effort.LOW).build())
                    // Jika permintaan ditolak filter keamanan, server otomatis mencoba model cadangan.
                    .putAdditionalHeader("anthropic-beta", "server-side-fallback-2026-07-01")
                    .putAdditionalBodyProperty("fallbacks", JsonValue.from("default"))
                    .addUserMessageOfContentBlockParams(
                        listOf(
                            ContentBlockParam.ofImage(
                                ImageBlockParam.builder()
                                    .source(
                                        Base64ImageSource.builder()
                                            .data(b64)
                                            .mediaType(Base64ImageSource.MediaType.IMAGE_JPEG)
                                            .build(),
                                    )
                                    .build(),
                            ),
                            ContentBlockParam.ofText(TextBlockParam.builder().text(PROMPT.trim()).build()),
                        ),
                    )
                    .build()
                val response = client.messages().create(params)
                val text = response.content().mapNotNull { it.text().orElse(null)?.text() }.joinToString("")
                parse(text)
            } catch (e: UnauthorizedException) {
                AnalyzeResult.Error("API key salah atau sudah tidak aktif. Periksa lagi di menu Atur.")
            } catch (e: PermissionDeniedException) {
                AnalyzeResult.Error("API key tidak punya izin. Periksa akun Anthropic Anda.")
            } catch (e: RateLimitException) {
                AnalyzeResult.Error("Terlalu banyak permintaan. Tunggu sebentar lalu coba lagi.")
            } catch (e: AnthropicServiceException) {
                val type = e.errorType().map { it.toString() }.orElse("")
                if (type.contains("billing", ignoreCase = true) || e.statusCode() == 402) {
                    AnalyzeResult.Error("Saldo/kredit API habis. Isi saldo di console.anthropic.com.")
                } else {
                    AnalyzeResult.Error("Server Claude bermasalah (kode ${e.statusCode()}). Coba lagi nanti.")
                }
            } catch (e: AnthropicIoException) {
                AnalyzeResult.Error("Gagal terhubung ke internet. Periksa koneksi lalu coba lagi.")
            } catch (e: Exception) {
                AnalyzeResult.Error("Terjadi kesalahan: ${e.message ?: e.javaClass.simpleName}")
            } finally {
                runCatching { client.close() }
            }
        }

    /** Ambil objek JSON dari jawaban Claude. */
    fun parse(text: String): AnalyzeResult {
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        if (start < 0 || end <= start) return AnalyzeResult.Error("Jawaban tidak bisa dibaca. Coba foto ulang.")
        return try {
            val j = JSONObject(text.substring(start, end + 1))
            if (!j.optBoolean("is_food", true)) {
                AnalyzeResult.NotFood(
                    "Foto ini sepertinya bukan makanan. " + j.optString("note", ""),
                )
            } else {
                AnalyzeResult.Ok(
                    FoodEstimate(
                        name = j.optString("name", "Makanan"),
                        portion = j.optString("portion", "1 porsi"),
                        kcal = j.optDouble("kcal", 0.0).toInt(),
                        protein = j.optDouble("protein_g", 0.0).toFloat(),
                        carbs = j.optDouble("carbs_g", 0.0).toFloat(),
                        fat = j.optDouble("fat_g", 0.0).toFloat(),
                        confidence = j.optString("confidence", "sedang"),
                        note = j.optString("note", ""),
                    ),
                )
            }
        } catch (e: Exception) {
            AnalyzeResult.Error("Jawaban tidak bisa dibaca. Coba foto ulang.")
        }
    }
}
