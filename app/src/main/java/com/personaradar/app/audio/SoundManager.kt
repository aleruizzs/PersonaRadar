package com.personaradar.app.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.PowerManager
import android.util.Log
import com.personaradar.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SoundManager(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var mediaPlayer: MediaPlayer? = null
    private val scope = CoroutineScope(Dispatchers.Main)
    private var cooldownJob: Job? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isInCooldown = MutableStateFlow(false)
    val isInCooldown: StateFlow<Boolean> = _isInCooldown.asStateFlow()

    var customAudioUri: Uri? = null

    companion object {
        private const val TAG = "SoundManager"
        const val COOLDOWN_DURATION_MS = 15_000L
    }

    /**
     * Dispara la reproducción al 100% de volumen multimedia con cooldown de 15s.
     * Retorna true si se ejecutó el disparo, o false si estaba en cooldown.
     */
    @Synchronized
    fun triggerPersonaSurprise(onTriggered: (() -> Unit)? = null): Boolean {
        if (_isInCooldown.value) {
            Log.d(TAG, "Trigger omitido: en período de cooldown")
            return false
        }

        Log.i(TAG, "¡DISPARANDO PERSONA! Maximizando volumen y reproduciendo música")
        maximizeVolume()
        playSound()
        startCooldown()
        onTriggered?.invoke()
        return true
    }

    /**
     * Sube el volumen del canal multimedia al 100% inmediatamente
     */
    fun maximizeVolume() {
        try {
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            audioManager.setStreamVolume(
                AudioManager.STREAM_MUSIC,
                maxVolume,
                AudioManager.FLAG_SHOW_UI
            )
            Log.d(TAG, "Volumen ajustado al máximo: $maxVolume")
        } catch (e: Exception) {
            Log.e(TAG, "Error al ajustar el volumen multimedia", e)
        }
    }

    /**
     * Reproduce el archivo de audio (custom URI o recurso raw por defecto)
     */
    fun playSound() {
        stopSound()
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
                    _isPlaying.value = false
                    releasePlayer()
                }

                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "Error en MediaPlayer: what=$what, extra=$extra")
                    _isPlaying.value = false
                    releasePlayer()
                    true
                }

                prepare()
                start()
            }

            mediaPlayer = player
            _isPlaying.value = true
        } catch (e: Exception) {
            Log.e(TAG, "Error al reproducir audio", e)
            _isPlaying.value = false
        }
    }

    /**
     * Detiene la música inmediatamente
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
            releasePlayer()
            _isPlaying.value = false
        }
    }

    private fun releasePlayer() {
        try {
            mediaPlayer?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error al liberar MediaPlayer", e)
        }
        mediaPlayer = null
    }

    private fun startCooldown() {
        _isInCooldown.value = true
        cooldownJob?.cancel()
        cooldownJob = scope.launch {
            delay(COOLDOWN_DURATION_MS)
            _isInCooldown.value = false
            Log.d(TAG, "Cooldown finalizado")
        }
    }

    fun release() {
        stopSound()
        cooldownJob?.cancel()
    }
}
