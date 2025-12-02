package expo.modules.rnlivewallpaper

import android.app.WallpaperManager
import android.content.ClipData
import android.content.ComponentName
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.FileProvider
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

class RnLiveWallpaperModule : Module() {

  companion object {
    private const val TAG = "RnLiveWallpaper"
    private const val PREFS_NAME = "rn_live_wallpaper_prefs"
    private const val PREF_KEY_VIDEO_URI = "video_uri"
  }

  override fun definition() = ModuleDefinition {
    Name("RnLiveWallpaper")

    /**
     * JS: RnLiveWallpaper.applyVideoWallpaper(uriString)
     * Accepts file://, content://, https:// (URI string)
     */
    Function("applyVideoWallpaper") { uriString: String ->
      try {
        val ctx = appContext.reactContext ?: appContext.activityProvider?.currentActivity
          ?: throw Exception("Context is null")

        if (uriString.isBlank()) throw Exception("Empty uri supplied")

        // Convert file:// -> content:// via FileProvider when needed (for files in app private storage)
        var originalFile: File? = null
        var finalUri: Uri = when {
          uriString.startsWith("file://") -> {
            val path = uriString.removePrefix("file://")
            val file = File(path)
            if (!file.exists()) throw Exception("File does not exist: $path")
            originalFile = file
            val authority = "${ctx.packageName}.fileprovider"
            FileProvider.getUriForFile(ctx, authority, file)
          }
          else -> Uri.parse(uriString)
        }

        // Persist URI (so wallpaper service can recover if process dies)
        try {
          val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
          prefs.edit().putString(PREF_KEY_VIDEO_URI, finalUri.toString()).apply()
        } catch (ex: Exception) {
          Log.w(TAG, "Failed to persist video uri to prefs: ${ex.message}")
        }

        // Try to grant URI permissions broadly (resolved activities + known system packages)
        val changeWallpaperIntent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
          putExtra(
            WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
            ComponentName(ctx.packageName, "expo.modules.rnlivewallpaper.VideoLiveWallpaperService")
          )
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        // Attach ClipData (helps some OEMs)
        changeWallpaperIntent.clipData = ClipData.newUri(ctx.contentResolver, "video", finalUri)

        // Grant permission to all resolved activities (the wallpaper picker, etc.)
        val pm = ctx.packageManager
        val resolvedActivities: List<ResolveInfo> = pm.queryIntentActivities(changeWallpaperIntent, 0)
        for (ri in resolvedActivities) {
          val pkg = ri.activityInfo.packageName
          try {
            ctx.grantUriPermission(pkg, finalUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
          } catch (ex: Exception) {
            Log.w(TAG, "Failed to grantUriPermission to $pkg: ${ex.message}")
          }
        }

        // ALSO grant to known system packages that may be used for lock-screen wallpaper application
        val systemPkgs = listOf(
          "com.android.systemui",
          "com.android.keyguard",
          "com.android.wallpaper",
          "com.google.android.apps.wallpaper",
          "com.android.launcher3",
          "com.google.android.apps.nexuslauncher",
          "com.oneplus.launcher",
          "com.sec.android.app.launcher"
        )
        for (pkg in systemPkgs) {
          try {
            ctx.grantUriPermission(pkg, finalUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
          } catch (ex: Exception) {
            Log.w(TAG, "Grant to $pkg failed: ${ex.message}")
          }
        }

        // If the wallpaper picker (or system) still can't access the content:// from FileProvider,
        // fallback: copy the original file to public Movies and use that public URI.
        // This is more broadly readable by system processes (may require storage permission on older Android).
        if (finalUri.scheme == "content" && originalFile != null) {
          val publicUri = tryCopyToPublic(ctx, originalFile)
          if (publicUri != null) {
            finalUri = publicUri
            // Persist the public URI
            try {
              ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putString(PREF_KEY_VIDEO_URI, finalUri.toString()).apply()
            } catch (_: Exception) {}
            // Re-grant permissions to resolved activities and system pkgs (if needed)
            changeWallpaperIntent.clipData = ClipData.newUri(ctx.contentResolver, "video", finalUri)
            for (ri in resolvedActivities) {
              try {
                ctx.grantUriPermission(ri.activityInfo.packageName, finalUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
              } catch (_: Exception) {}
            }
            for (pkg in systemPkgs) {
              try {
                ctx.grantUriPermission(pkg, finalUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
              } catch (_: Exception) {}
            }
          }
        }

        // Finally start the change-live-wallpaper UI
        ctx.startActivity(changeWallpaperIntent)

      } catch (e: Exception) {
        Log.e(TAG, "applyVideoWallpaper failed", e)
        throw e
      }
    }
  }

  /**
   * Copy originalFile into MediaStore (preferred on Android Q+) or into external Movies folder
   * and return a public Uri that system processes can read.
   */
  private fun tryCopyToPublic(ctx: Context, originalFile: File): Uri? {
    try {
      // Android 10+ -> insert into MediaStore (no storage permission needed if using proper APIs)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val values = ContentValues().apply {
          put(MediaStore.Video.Media.DISPLAY_NAME, originalFile.name)
          put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
          put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES)
          put(MediaStore.Video.Media.IS_PENDING, 1)
        }
        val resolver = ctx.contentResolver
        val collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val itemUri = resolver.insert(collection, values) ?: return null
        // Write bytes
        resolver.openOutputStream(itemUri)?.use { out ->
          FileInputStream(originalFile).use { input ->
            input.copyTo(out)
          }
        }
        // mark not pending
        values.clear()
        values.put(MediaStore.Video.Media.IS_PENDING, 0)
        resolver.update(itemUri, values, null, null)
        return itemUri
      } else {
        // Pre-Q: copy to Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
        val moviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
        if (!moviesDir.exists()) moviesDir.mkdirs()
        val dest = File(moviesDir, originalFile.name)
        FileInputStream(originalFile).use { inStream ->
          FileOutputStream(dest).use { outStream ->
            inStream.copyTo(outStream)
          }
        }
        // Return file:// uri (system can read)
        return Uri.fromFile(dest)
      }
    } catch (ex: IOException) {
      Log.w(TAG, "tryCopyToPublic failed: ${ex.message}")
      return null
    } catch (ex: Exception) {
      Log.w(TAG, "tryCopyToPublic exception: ${ex.message}")
      return null
    }
  }
}
