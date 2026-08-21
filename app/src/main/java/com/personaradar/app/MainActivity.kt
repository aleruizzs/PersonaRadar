package com.personaradar.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.personaradar.app.service.RadarService
import com.personaradar.app.ui.screens.RadarScreen
import com.personaradar.app.ui.theme.PersonaBlack
import com.personaradar.app.ui.theme.PersonaRadarTheme

class MainActivity : ComponentActivity() {

    private var customAudioUri by mutableStateOf<Uri?>(null)
    private var customAudioFileName by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            PersonaRadarTheme {
                val isServiceRunning by RadarService.isServiceRunning.collectAsState()
                val radarState by RadarService.radarState.collectAsState()

                // Permission launcher for RECORD_AUDIO and POST_NOTIFICATIONS
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { permissions ->
                    val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false

                    if (audioGranted) {
                        RadarService.startService(this, customAudioUri)
                        Toast.makeText(this, "Radar activado - Escuchando...", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(
                            this,
                            "Se requiere permiso de micrófono para detectar 'persona'",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                // Audio file picker launcher
                val audioPickerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.GetContent()
                ) { uri: Uri? ->
                    if (uri != null) {
                        try {
                            contentResolver.takePersistableUriPermission(
                                uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                            )
                        } catch (ignored: Exception) {}

                        customAudioUri = uri
                        customAudioFileName = getFileName(uri) ?: "Audio personalizado"
                        RadarService.updateCustomUri(this, uri)
                        Toast.makeText(this, "Audio cargado: $customAudioFileName", Toast.LENGTH_SHORT).show()
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = PersonaBlack
                ) {
                    RadarScreen(
                        isServiceRunning = isServiceRunning,
                        radarState = radarState,
                        customAudioName = customAudioFileName,
                        onToggleRadar = {
                            if (isServiceRunning) {
                                RadarService.stopService(this)
                                Toast.makeText(this, "Radar desactivado", Toast.LENGTH_SHORT).show()
                            } else {
                                val audioGranted = ContextCompat.checkSelfPermission(
                                    this,
                                    Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED

                                val notifGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    ContextCompat.checkSelfPermission(
                                        this,
                                        Manifest.permission.POST_NOTIFICATIONS
                                    ) == PackageManager.PERMISSION_GRANTED
                                } else {
                                    true
                                }

                                if (audioGranted && notifGranted) {
                                    RadarService.startService(this, customAudioUri)
                                    Toast.makeText(this, "Radar activado - Escuchando...", Toast.LENGTH_SHORT).show()
                                } else {
                                    val list = mutableListOf(Manifest.permission.RECORD_AUDIO)
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notifGranted) {
                                        list.add(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                    permissionLauncher.launch(list.toTypedArray())
                                }
                            }
                        },
                        onStopMusic = {
                            RadarService.stopMusic(this)
                            Toast.makeText(this, "Música silenciada", Toast.LENGTH_SHORT).show()
                        },
                        onTestSound = {
                            RadarService.testSound(this)
                            Toast.makeText(this, "¡Probando sonido al 100% de volumen!", Toast.LENGTH_SHORT).show()
                        },
                        onSelectAudio = {
                            audioPickerLauncher.launch("audio/*")
                        },
                        onResetAudio = {
                            customAudioUri = null
                            customAudioFileName = null
                            RadarService.updateCustomUri(this, null)
                            Toast.makeText(this, "Restaurado audio por defecto", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }

    private fun getFileName(uri: Uri): String? {
        var name: String? = null
        if (uri.scheme == "content") {
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) {
                        name = it.getString(index)
                    }
                }
            }
        }
        return name ?: uri.lastPathSegment
    }
}
