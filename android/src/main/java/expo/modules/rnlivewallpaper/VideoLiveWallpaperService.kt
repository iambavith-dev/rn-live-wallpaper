package expo.modules.rnlivewallpaper

import android.content.Context
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log

class VideoLiveWallpaperService : WallpaperService() {

  override fun onCreateEngine(): Engine {
    // Ensure persisted URI is loaded immediately (in case we were killed)
    ensureUriLoaded(this@VideoLiveWallpaperService)
    return VideoEngine()
  }

  internal inner class VideoEngine : Engine(), MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener {

    private val TAG = "VideoEngine"
    private var mediaPlayer: MediaPlayer? = null
    private var isPrepared = false
    private var surfaceReady = false

    override fun onSurfaceCreated(holder: SurfaceHolder) {
      super.onSurfaceCreated(holder)
      surfaceReady = true
      ensureUriLoaded(this@VideoLiveWallpaperService)

      val uriString = videoUri
      if (uriString.isNullOrBlank()) {
        Log.e(TAG, "No videoUri provided to wallpaper service")
        return
      }

      try {
        mediaPlayer?.let { it.reset(); it.release() }
        val mp = MediaPlayer()
        mediaPlayer = mp
        mp.setOnPreparedListener(this)
        mp.setOnErrorListener(this)
        mp.isLooping = true

        val uri = Uri.parse(uriString)

        mp.setDataSource(this@VideoLiveWallpaperService, uri)

        if (holder.surface != null && holder.surface.isValid) {
          mp.setSurface(holder.surface)
        }

        mp.prepareAsync()
      } catch (ex: Exception) {
        Log.e(TAG, "Exception preparing MediaPlayer", ex)
      }
    }

    override fun onSurfaceDestroyed(holder: SurfaceHolder) {
      super.onSurfaceDestroyed(holder)
      surfaceReady = false
      
      mediaPlayer?.let {
        try {
          playheadTime = it.currentPosition
        } catch (_: Exception) {}
        it.setOnPreparedListener(null)
        it.setOnErrorListener(null)
        it.reset()
        it.release()
      }
      mediaPlayer = null
      isPrepared = false
    }

    override fun onPrepared(mp: MediaPlayer) {
      
      isPrepared = true
      try {
        if (playheadTime > 0) {
          mp.seekTo(playheadTime)
          playheadTime = 0
        }
        val holder = surfaceHolder
        if (holder != null && holder.surface != null && holder.surface.isValid) {
          mp.setSurface(holder.surface)
          mp.start()
        } else {
          
        }
      } catch (ex: Exception) {
        Log.e(TAG, "Error starting playback", ex)
      }
    }

    override fun onVisibilityChanged(visible: Boolean) {
      super.onVisibilityChanged(visible)
      val mp = mediaPlayer ?: return
      try {
        if (visible && isPrepared && surfaceReady) mp.start()
        else if (!visible && mp.isPlaying) {
          mp.pause()
          playheadTime = mp.currentPosition
        }
      } catch (ex: Exception) {
        Log.e(TAG, "onVisibilityChanged error", ex)
      }
    }

    override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
      Log.e(TAG, "MediaPlayer error what=$what extra=$extra uri=$videoUri")
      return true
    }
  }

  companion object {
    @Volatile
    var videoUri: String? = null

    @Volatile
    var playheadTime = 0

    private const val PREFS_NAME = "rn_live_wallpaper_prefs"
    private const val PREF_KEY_VIDEO_URI = "video_uri"

    fun ensureUriLoaded(context: Context) {
      if (videoUri.isNullOrBlank()) {
        try {
          val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
          videoUri = prefs.getString(PREF_KEY_VIDEO_URI, null)
        } catch (ex: Exception) {
          Log.w("VideoLiveWallpaperService", "ensureUriLoaded failed: ${ex.message}")
        }
      } else {
        
      }
    }
  }
}
