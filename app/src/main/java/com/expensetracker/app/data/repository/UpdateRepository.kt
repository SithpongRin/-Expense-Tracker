package com.expensetracker.app.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.expensetracker.app.BuildConfig
import com.expensetracker.app.core.util.SecurityUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class UpdateInfo(
    val hasUpdate: Boolean,
    val latestVersionName: String,
    val latestVersionCode: Int,
    val releaseNotes: String,
    val downloadUrl: String,
    val expectedSha256: String?,
    val publishedAt: String
)

sealed class UpdateDownloadState {
    object Idle : UpdateDownloadState()
    data class Downloading(val progressPercent: Int) : UpdateDownloadState()
    data class ReadyToInstall(val apkFile: File) : UpdateDownloadState()
    data class Error(val message: String) : UpdateDownloadState()
}

@Singleton
class UpdateRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient
) {

    private val releasesApiUrl = "https://api.github.com/repos/SithpongRin/-Expense-Tracker/releases/latest"

    /**
     * Checks GitHub Releases API for the latest version metadata.
     * Compares version tags against current app [BuildConfig.VERSION_NAME].
     */
    suspend fun checkForUpdates(): Result<UpdateInfo> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(releasesApiUrl)
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "ExpenseTracker-Android")
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                if (response.code == 404) {
                    return@withContext Result.success(
                        UpdateInfo(
                            hasUpdate = false,
                            latestVersionName = BuildConfig.VERSION_NAME,
                            latestVersionCode = BuildConfig.VERSION_CODE,
                            releaseNotes = "No GitHub releases published yet.",
                            downloadUrl = "",
                            expectedSha256 = null,
                            publishedAt = ""
                        )
                    )
                }
                return@withContext Result.failure(Exception("GitHub API returned HTTP ${response.code}"))
            }

            val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response body"))
            val json = JSONObject(body)

            val tagName = json.optString("tag_name", "").removePrefix("v")
            val releaseNotes = json.optString("body", "No release notes provided.")
            val publishedAt = json.optString("published_at", "")

            var downloadUrl = ""
            var expectedSha256: String? = null

            val assets = json.optJSONArray("assets")
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk")) {
                        downloadUrl = asset.optString("browser_download_url", "")
                    } else if (name.endsWith(".sha256") || name.contains("checksum")) {
                        // Optional checksum asset
                    }
                }
            }

            // Also check release notes for SHA-256 pattern
            val shaRegex = Regex("[a-fA-F0-9]{64}")
            val match = shaRegex.find(releaseNotes)
            if (match != null) {
                expectedSha256 = match.value.lowercase()
            }

            val hasUpdate = isVersionNewer(tagName, BuildConfig.VERSION_NAME)

            Result.success(
                UpdateInfo(
                    hasUpdate = hasUpdate,
                    latestVersionName = tagName.ifEmpty { BuildConfig.VERSION_NAME },
                    latestVersionCode = 2,
                    releaseNotes = releaseNotes,
                    downloadUrl = downloadUrl,
                    expectedSha256 = expectedSha256,
                    publishedAt = publishedAt
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Downloads the APK file, reports progress, and verifies its SHA-256 checksum.
     */
    suspend fun downloadAndVerifyApk(
        downloadUrl: String,
        expectedSha256: String?,
        onProgress: (Int) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(downloadUrl).build()
            val response = okHttpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Failed to download APK: HTTP ${response.code}"))
            }

            val body = response.body ?: return@withContext Result.failure(Exception("Empty APK download body"))
            val contentLength = body.contentLength()

            val apkDir = File(context.cacheDir, "updates")
            if (!apkDir.exists()) apkDir.mkdirs()
            val apkFile = File(apkDir, "update.apk")

            body.byteStream().use { input ->
                FileOutputStream(apkFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalRead = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        if (contentLength > 0) {
                            val progress = ((totalRead * 100) / contentLength).toInt()
                            onProgress(progress)
                        }
                    }
                    output.flush()
                }
            }

            // Checksum verification
            if (!expectedSha256.isNullOrBlank()) {
                val apkBytes = apkFile.readBytes()
                val calculatedSha256 = SecurityUtils.computeSha256(apkBytes)
                if (!calculatedSha256.equals(expectedSha256, ignoreCase = true)) {
                    apkFile.delete()
                    return@withContext Result.failure(
                        SecurityException("SHA-256 Checksum mismatch! Downloaded package may be corrupted or tampered with.")
                    )
                }
            }

            Result.success(apkFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Creates an Intent to install the validated APK via Android PackageInstaller.
     */
    fun getInstallIntent(apkFile: File): Intent {
        val apkUri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    companion object {
        fun isVersionNewer(remoteVersion: String, currentVersion: String): Boolean {
            if (remoteVersion.isBlank()) return false
            val remoteParts = remoteVersion.split(".").mapNotNull { it.toIntOrNull() }
            val currentParts = currentVersion.split(".").mapNotNull { it.toIntOrNull() }

            val length = maxOf(remoteParts.size, currentParts.size)
            for (i in 0 until length) {
                val r = remoteParts.getOrElse(i) { 0 }
                val c = currentParts.getOrElse(i) { 0 }
                if (r > c) return true
                if (r < c) return false
            }
            return false
        }
    }
}
