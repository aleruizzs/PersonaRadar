package com.personaradar.app.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.PowerManager
import android.util.Log
import com.personaradar.app.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.roundToInt

class SoundManager(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var mediaPlayer: MediaPlayer? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    var targetVolumePercent: Int = 100
        set(value) {
            field = value.coerceIn(0, 100)
        }

    var customAudioUri: Uri? = null
        set(value) {
            field = value
            preloadMediaPlayer()
        }

    private var isPrepared = false
    var onCompletionCallback: (() -> Unit)? = null

    companion object {
        private const val TAG = "SoundManager"
    }

    init {
        preloadMediaPlayer()
    }

    /**
     * Instancia y prepara inmediatamente el MediaPlayer en memoria para latencia cero al disparar.
     */
    @Synchronized
    fun preloadMediaPlayer() {
        releasePlayer()
        try {
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK)

                if (customAudioUri != null) {
                    setDataSource(context, customAudioUri!!)
                } else {
                    val afd = context.resources.openRawResourceFd(R.raw.last_surprise)
                    setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                    afd.close()
                }

                setOnCompletionListener {
                    Log.d(TAG, "Playback completado. Reseteando reproductor y notificando.")
                    _isPlaying.value = false
                    isPrepared = false
                    preloadMediaPlayer()
                    onCompletionCallback?.invoke()
                }

                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "Error en MediaPlayer preloaded: what=$what, extra=$extra")
                    _isPlaying.value = false
                    isPrepared = false
                    releasePlayer()
                    true
                }

                prepare()
            }

            mediaPlayer = player
            isPrepared = true
            Log.d(TAG, "MediaPlayer precargado y preparado con éxito en memoria.")
        } catch (e: Exception) {
            Log.e(TAG, "Error al precargar MediaPlayer", e)
            isPrepared = false
        }
    }

    /**
     * Dispara la reproducción instantánea con el volumen configurado.
     * Retorna true si se ejecutó el disparo, o false si ya estaba sonando.
     */
    @Synchronized
    fun triggerPersonaSurprise(onCompletion: (() -> Unit)? = null): Boolean {
        if (_isPlaying.value) {
            Log.d(TAG, "Trigger omitido: ya se encuentra reproduciendo audio")
            return false
        }

        onCompletionCallback = onCompletion

        Log.i(TAG, "¡DISPARANDO PERSONA! Aplicando volumen al $targetVolumePercent% y reproduciendo")
        applyTargetVolume()

        if (!isPrepared || mediaPlayer == null) {
            Log.w(TAG, "MediaPlayer no estaba preparado. Intentando preparar antes de iniciar.")
            preloadMediaPlayer()
        }

        try {
            mediaPlayer?.start()
            _isPlaying.value = true
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error al iniciar MediaPlayer prealmacenado", e)
            preloadMediaPlayer()
            return false
        }
    }

    /**
     * Calcula y aplica el volumen multimedia:
     * targetVolume = (maxVolume * (volumenSeleccionado / 100f)).roundToInt()
     */
    fun applyTargetVolume() {
        try {
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val targetVolume = (maxVolume * (targetVolumePercent / 100f)).roundToInt()
            audioManager.setStreamVolume(
                AudioManager.STREAM_MUSIC,
                targetVolume,
                AudioManager.FLAG_SHOW_UI
            )
            Log.d(TAG, "Volumen ajustado: $targetVolume / $maxVolume ($targetVolumePercent%)")
        } catch (e: Exception) {
            Log.e(TAG, "Error al ajustar el volumen multimedia", e)
        }
    }

    /**
     * Detiene la música inmediatamente, vuelve a la posición inicial (precarga) y limpia estado
     */
    @Synchronized
    fun stopSound() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al detener MediaPlayer", e)
        } finally {
            _isPlaying.value = false
            preloadMediaPlayer()
        }
    }

    private fun releasePlayer() {
        try {
            mediaPlayer?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error al liberar MediaPlayer", e)
        }
        mediaPlayer = null
        isPrepared = false
    }

    fun release() {
        onCompletionCallback = null
        stopSound()
    }
}


