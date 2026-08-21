package com.personaradar.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.personaradar.app.MainActivity
import com.personaradar.app.R
import com.personaradar.app.audio.SoundManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

class RadarService : Service(), RecognitionListener {

    private var speechRecognizer: SpeechRecognizer? = null
    private var recognizerIntent: Intent? = null
    private var soundManager: SoundManager? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private val mainHandler = Handler(Looper.getMainLooper())
    private val serviceScope = CoroutineScope(Dispatchers.Main)
    private var resetStateJob: Job? = null

    private var isListeningLoopActive = false
    private var restartAttemptCount = 0

    companion object {
        private const val TAG = "RadarService"
        const val CHANNEL_ID = "persona_radar_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.personaradar.action.START"
        const val ACTION_STOP = "com.personaradar.action.STOP"
        const val ACTION_STOP_MUSIC = "com.personaradar.action.STOP_MUSIC"
        const val ACTION_TEST_SOUND = "com.personaradar.action.TEST_SOUND"
        const val ACTION_UPDATE_URI = "com.personaradar.action.UPDATE_URI"
        const val EXTRA_AUDIO_URI = "extra_audio_uri"

        private val _radarState = MutableStateFlow(RadarState.INACTIVE)
        val radarState: StateFlow<RadarState> = _radarState.asStateFlow()

        private val _isServiceRunning = MutableStateFlow(false)
        val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

        fun startService(context: Context, audioUri: Uri? = null) {
            val intent = Intent(context, RadarService::class.java).apply {
                action = ACTION_START
                if (audioUri != null) {
                    putExtra(EXTRA_AUDIO_URI, audioUri.toString())
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, RadarService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun stopMusic(context: Context) {
            val intent = Intent(context, RadarService::class.java).apply {
                action = ACTION_STOP_MUSIC
            }
            context.startService(intent)
        }

        fun testSound(context: Context) {
            val intent = Intent(context, RadarService::class.java).apply {
                action = ACTION_TEST_SOUND
            }
            context.startService(intent)
        }

        fun updateCustomUri(context: Context, uri: Uri?) {
            val intent = Intent(context, RadarService::class.java).apply {
                action = ACTION_UPDATE_URI
                putExtra(EXTRA_AUDIO_URI, uri?.toString())
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")
        soundManager = SoundManager(this)
        createNotificationChannel()
        acquireWakeLock()
        initSpeechRecognizer()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START
        Log.d(TAG, "onStartCommand: action=$action")

        val uriString = intent?.getStringExtra(EXTRA_AUDIO_URI)
        if (uriString != null) {
            soundManager?.customAudioUri = Uri.parse(uriString)
        }

        when (action) {
            ACTION_START -> {
                startForegroundWithNotification()
                _isServiceRunning.value = true
                startContinuousListening()
            }
            ACTION_STOP -> {
                stopContinuousListening()
                stopForeground(STOP_FOREGROUND_REMOVE)
                _isServiceRunning.value = false
                _radarState.value = RadarState.INACTIVE
                stopSelf()
            }
            ACTION_STOP_MUSIC -> {
                soundManager?.stopSound()
                if (isListeningLoopActive) {
                    _radarState.value = RadarState.LISTENING
                    updateNotification(RadarState.LISTENING)
                }
            }
            ACTION_TEST_SOUND -> {
                _radarState.value = RadarState.TRIGGERED
                updateNotification(RadarState.TRIGGERED)
                soundManager?.triggerPersonaSurprise {
                    scheduleStateResetToListening()
                }
            }
            ACTION_UPDATE_URI -> {
                if (uriString != null) {
                    soundManager?.customAudioUri = Uri.parse(uriString)
                } else {
                    soundManager?.customAudioUri = null
                }
            }
        }

        return START_STICKY
    }

    private fun acquireWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "PersonaRadar:ListeningWakeLock"
            ).apply {
                setReferenceCounted(false)
                acquire(24 * 60 * 60 * 1000L) // 24h max safeguard
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error acquiring wake lock", e)
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing wake lock", e)
        }
        wakeLock = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(state: RadarState): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, RadarService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopMusicIntent = Intent(this, RadarService::class.java).apply {
            action = ACTION_STOP_MUSIC
        }
        val stopMusicPendingIntent = PendingIntent.getService(
            this,
            2,
            stopMusicIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentText = when (state) {
            RadarState.INACTIVE -> "Radar inactivo"
            RadarState.LISTENING -> getString(R.string.notification_listening)
            RadarState.TRIGGERED -> getString(R.string.notification_triggered)
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_radar_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(R.drawable.ic_radar_notification, "Silenciar", stopMusicPendingIntent)
            .addAction(R.drawable.ic_radar_notification, "Apagar Radar", stopPendingIntent)
            .build()
    }

    private fun startForegroundWithNotification() {
        val notification = buildNotification(_radarState.value)
        val foregroundType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, foregroundType)
    }

    private fun updateNotification(state: RadarState) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, buildNotification(state))
    }

    private fun initSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Log.e(TAG, "SpeechRecognizer no está disponible en este dispositivo")
            return
        }

        mainHandler.post {
            try {
                speechRecognizer?.destroy()
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
                    setRecognitionListener(this@RadarService)
                }

                recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                    )
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "es-ES")
                    putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "es-ES")
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
                    // Optimizations for continuous listening
                    putExtra("android.speech.extra.DICTATION_MODE", true)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error inicializando SpeechRecognizer", e)
            }
        }
    }

    private fun startContinuousListening() {
        isListeningLoopActive = true
        _radarState.value = RadarState.LISTENING
        updateNotification(RadarState.LISTENING)
        restartAttemptCount = 0
        scheduleRecognizerStart(0)
    }

    private fun stopContinuousListening() {
        isListeningLoopActive = false
        mainHandler.removeCallbacksAndMessages(null)
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
        } catch (e: Exception) {
            Log.e(TAG, "Error al detener SpeechRecognizer", e)
        }
    }

    private fun scheduleRecognizerStart(delayMs: Long) {
        if (!isListeningLoopActive) return

        mainHandler.postDelayed({
            if (!isListeningLoopActive) return@postDelayed
            try {
                if (speechRecognizer == null) {
                    initSpeechRecognizer()
                }
                recognizerIntent?.let {
                    speechRecognizer?.startListening(it)
                    Log.d(TAG, "SpeechRecognizer startListening invocado")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Excepción al iniciar reconocimiento de voz", e)
                // Watchdog restart fallback
                scheduleRecognizerStart(1000)
            }
        }, delayMs)
    }

    private fun processTranscribedText(texts: List<String>?) {
        if (texts.isNullOrEmpty()) return

        for (text in texts) {
            val lower = text.lowercase(Locale.getDefault())
            Log.d(TAG, "Audio transcrito: \"$lower\"")

            // Comprobación de la palabra clave "persona" o variaciones fonéticas comunes
            if (lower.contains("persona") || lower.contains("personaje") || lower.contains("personas")) {
                Log.i(TAG, "¡¡¡PALABRA CLAVE DETECTADA: $lower !!!")
                handlePersonaDetected()
                break
            }
        }
    }

    private fun handlePersonaDetected() {
        _radarState.value = RadarState.TRIGGERED
        updateNotification(RadarState.TRIGGERED)

        val triggered = soundManager?.triggerPersonaSurprise {
            scheduleStateResetToListening()
        } ?: false

        if (!triggered) {
            // Si estaba en cooldown, regresamos el estado visual a LISTENING
            _radarState.value = RadarState.LISTENING
            updateNotification(RadarState.LISTENING)
        }
    }

    private fun scheduleStateResetToListening() {
        resetStateJob?.cancel()
        resetStateJob = serviceScope.launch {
            delay(SoundManager.COOLDOWN_DURATION_MS)
            if (isListeningLoopActive) {
                _radarState.value = RadarState.LISTENING
                updateNotification(RadarState.LISTENING)
            }
        }
    }

    // --- RecognitionListener Callbacks & Watchdog Auto-Restart ---

    override fun onReadyForSpeech(params: Bundle?) {
        Log.d(TAG, "onReadyForSpeech")
        restartAttemptCount = 0
    }

    override fun onBeginningOfSpeech() {
        Log.d(TAG, "onBeginningOfSpeech")
    }

    override fun onRmsChanged(rmsdB: Float) {}

    override fun onBufferReceived(buffer: ByteArray?) {}

    override fun onEndOfSpeech() {
        Log.d(TAG, "onEndOfSpeech -> reiniciando ciclo de escucha")
        if (isListeningLoopActive) {
            scheduleRecognizerStart(300)
        }
    }

    override fun onError(error: Int) {
        Log.w(TAG, "onError: código $error -> reiniciando ciclo de escucha (Watchdog)")
        if (isListeningLoopActive) {
            restartAttemptCount++
            val backoff = when {
                error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> 500L
                error == SpeechRecognizer.ERROR_NO_MATCH -> 100L
                error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> 100L
                restartAttemptCount > 5 -> 1500L
                else -> 300L
            }

            try {
                speechRecognizer?.cancel()
            } catch (e: Exception) {
                Log.e(TAG, "Error cancelando speechRecognizer tras error", e)
            }

            // Re-instanciar el recognizer si es necesario tras errores graves
            if (error == SpeechRecognizer.ERROR_CLIENT || restartAttemptCount > 10) {
                initSpeechRecognizer()
                restartAttemptCount = 0
            }

            scheduleRecognizerStart(backoff)
        }
    }

    override fun onResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        processTranscribedText(matches)

        // Continuar escuchando inmediatamente
        if (isListeningLoopActive) {
            scheduleRecognizerStart(150)
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        processTranscribedText(matches)
    }

    override fun onEvent(eventType: Int, params: Bundle?) {}

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        isListeningLoopActive = false
        mainHandler.removeCallbacksAndMessages(null)
        resetStateJob?.cancel()

        try {
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.e(TAG, "Error destruyendo SpeechRecognizer", e)
        }
        speechRecognizer = null

        soundManager?.release()
        soundManager = null

        releaseWakeLock()
        _isServiceRunning.value = false
        _radarState.value = RadarState.INACTIVE
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
