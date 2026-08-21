package com.personaradar.app.ui.screens

import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.personaradar.app.service.RadarState
import com.personaradar.app.ui.theme.PersonaBlack
import com.personaradar.app.ui.theme.PersonaCrimson
import com.personaradar.app.ui.theme.PersonaDarkGray
import com.personaradar.app.ui.theme.PersonaGraphite
import com.personaradar.app.ui.theme.PersonaLightGray
import com.personaradar.app.ui.theme.PersonaMutedText
import com.personaradar.app.ui.theme.PersonaRed
import com.personaradar.app.ui.theme.PersonaWhite
import com.personaradar.app.ui.theme.PersonaYellow

@Composable
fun RadarScreen(
    isServiceRunning: Boolean,
    radarState: RadarState,
    targetVolumePercent: Int,
    customAudioName: String?,
    onToggleRadar: () -> Unit,
    onStopMusic: () -> Unit,
    onTestSound: () -> Unit,
    onVolumeChange: (Int) -> Unit,
    onSelectAudio: () -> Unit,
    onResetAudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    // Pulse animation when listening or triggered
    val infiniteTransition = rememberInfiniteTransition(label = "RadarPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isServiceRunning) 1.14f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    val haloAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = if (isServiceRunning) 0.8f else 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "HaloAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PersonaBlack)
            .drawBehind {
                // Persona 5 Angular Dynamic Background Stripes
                val path = Path().apply {
                    moveTo(0f, size.height * 0.28f)
                    lineTo(size.width, size.height * 0.18f)
                    lineTo(size.width, size.height * 0.38f)
                    lineTo(0f, size.height * 0.48f)
                    close()
                }
                drawPath(path, color = PersonaCrimson.copy(alpha = 0.35f))

                val accentPath = Path().apply {
                    moveTo(0f, size.height * 0.75f)
                    lineTo(size.width, size.height * 0.65f)
                    lineTo(size.width, size.height * 0.67f)
                    lineTo(0f, size.height * 0.77f)
                    close()
                }
                drawPath(accentPath, color = PersonaYellow.copy(alpha = 0.5f))
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // HEADER: Persona 5 Stylized Banner
            PersonaHeader()

            Spacer(modifier = Modifier.height(24.dp))

            // RADAR STATUS BADGE
            StatusBadge(radarState = radarState, isServiceRunning = isServiceRunning)

            Spacer(modifier = Modifier.height(28.dp))

            // GIANT CENTER RADAR BUTTON
            GiantRadarButton(
                isServiceRunning = isServiceRunning,
                radarState = radarState,
                pulseScale = pulseScale,
                haloAlpha = haloAlpha,
                onClick = onToggleRadar
            )

            Spacer(modifier = Modifier.height(32.dp))

            // TARGET VOLUME SLIDER CARD
            VolumeControlCard(
                targetVolumePercent = targetVolumePercent,
                onVolumeChange = onVolumeChange
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ACTION BUTTONS: Stop Music & Test Sound
            ActionButtonsRow(
                onStopMusic = onStopMusic,
                onTestSound = onTestSound
            )

            Spacer(modifier = Modifier.height(24.dp))

            // AUDIO SOURCE SELECTOR CARD
            AudioSelectionCard(
                customAudioName = customAudioName,
                onSelectAudio = onSelectAudio,
                onResetAudio = onResetAudio
            )

            Spacer(modifier = Modifier.height(24.dp))

            // INSTRUCTIONS / INFO CARD
            InfoCard()

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}


@Composable
private fun PersonaHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .rotate(-2.5f),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .background(
                    color = PersonaRed,
                    shape = CutCornerShape(topStart = 14.dp, bottomEnd = 14.dp)
                )
                .border(
                    width = 2.dp,
                    color = PersonaWhite,
                    shape = CutCornerShape(topStart = 14.dp, bottomEnd = 14.dp)
                )
                .padding(horizontal = 24.dp, vertical = 10.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "PERSONA RADAR",
                    color = PersonaWhite,
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 32.sp,
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.Black
                    ),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "VOICE TRIGGER // LAST SURPRISE",
                    color = PersonaYellow,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.5.sp
                    )
                )
            }
        }
    }
}

@Composable
private fun StatusBadge(
    radarState: RadarState,
    isServiceRunning: Boolean
) {
    val badgeBg = when {
        radarState == RadarState.TRIGGERED -> PersonaYellow
        isServiceRunning -> PersonaRed
        else -> PersonaDarkGray
    }

    val textColor = when {
        radarState == RadarState.TRIGGERED -> PersonaBlack
        isServiceRunning -> PersonaWhite
        else -> PersonaMutedText
    }

    val stateText = when {
        !isServiceRunning -> "INACTIVO"
        radarState == RadarState.TRIGGERED -> "¡PERSONA DETECTADA!"
        else -> "ESCUCHANDO..."
    }

    val icon = when {
        !isServiceRunning -> Icons.Default.MicOff
        radarState == RadarState.TRIGGERED -> Icons.AutoMirrored.Filled.VolumeUp
        else -> Icons.Default.Mic
    }

    Box(
        modifier = Modifier
            .rotate(1.5f)
            .shadow(12.dp, shape = CutCornerShape(topEnd = 12.dp, bottomStart = 12.dp))
            .background(
                color = badgeBg,
                shape = CutCornerShape(topEnd = 12.dp, bottomStart = 12.dp)
            )
            .border(
                width = 2.dp,
                color = if (radarState == RadarState.TRIGGERED) PersonaRed else PersonaWhite,
                shape = CutCornerShape(topEnd = 12.dp, bottomStart = 12.dp)
            )
            .padding(horizontal = 28.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = stateText,
                color = textColor,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp
                )
            )
        }
    }
}

@Composable
private fun GiantRadarButton(
    isServiceRunning: Boolean,
    radarState: RadarState,
    pulseScale: Float,
    haloAlpha: Float,
    onClick: () -> Unit
) {
    val buttonColor by animateColorAsState(
        targetValue = when {
            radarState == RadarState.TRIGGERED -> PersonaYellow
            isServiceRunning -> PersonaRed
            else -> PersonaDarkGray
        },
        label = "ButtonColor"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(230.dp)
    ) {
        // Outer Glowing Halo
        if (isServiceRunning) {
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                (if (radarState == RadarState.TRIGGERED) PersonaYellow else PersonaRed).copy(
                                    alpha = haloAlpha
                                ),
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        // Secondary angular decorative diamond behind the circle
        Box(
            modifier = Modifier
                .size(190.dp)
                .rotate(45f)
                .background(
                    if (isServiceRunning) PersonaCrimson else PersonaGraphite,
                    CutCornerShape(24.dp)
                )
                .border(2.dp, if (isServiceRunning) PersonaYellow else PersonaMutedText.copy(alpha = 0.3f), CutCornerShape(24.dp))
        )

        // Main Giant Circular Button
        Box(
            modifier = Modifier
                .size(165.dp)
                .shadow(16.dp, CircleShape)
                .clip(CircleShape)
                .background(buttonColor)
                .border(4.dp, PersonaWhite, CircleShape)
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = if (isServiceRunning) Icons.Default.Mic else Icons.Default.MicOff,
                    contentDescription = null,
                    tint = if (radarState == RadarState.TRIGGERED) PersonaBlack else PersonaWhite,
                    modifier = Modifier.size(44.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isServiceRunning) "RADAR ON" else "RADAR OFF",
                    color = if (radarState == RadarState.TRIGGERED) PersonaBlack else PersonaWhite,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    ),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = if (isServiceRunning) "TOCA PARA APAGAR" else "TOCA PARA ACTIVAR",
                    color = if (radarState == RadarState.TRIGGERED) PersonaBlack.copy(alpha = 0.8f) else PersonaYellow,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun VolumeControlCard(
    targetVolumePercent: Int,
    onVolumeChange: (Int) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, PersonaRed, CutCornerShape(topStart = 12.dp, bottomEnd = 12.dp)),
        shape = CutCornerShape(topStart = 12.dp, bottomEnd = 12.dp),
        colors = CardDefaults.cardColors(containerColor = PersonaGraphite)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = null,
                        tint = PersonaYellow,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "VOLUMEN DE DISPARO",
                        color = PersonaWhite,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    )
                }

                // Large numeric visor badge
                Box(
                    modifier = Modifier
                        .rotate(-2f)
                        .background(PersonaRed, CutCornerShape(4.dp))
                        .border(1.dp, PersonaWhite, CutCornerShape(4.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "$targetVolumePercent%",
                        color = PersonaWhite,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Slider(
                value = targetVolumePercent.toFloat(),
                onValueChange = { onVolumeChange(it.toInt()) },
                valueRange = 0f..100f,
                colors = SliderDefaults.colors(
                    thumbColor = PersonaYellow,
                    activeTrackColor = PersonaRed,
                    inactiveTrackColor = PersonaDarkGray,
                    activeTickColor = PersonaYellow
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Nivel de audio que se forzará al detectar \"persona\"",
                color = PersonaMutedText,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                textAlign = TextAlign.Start
            )
        }
    }
}

@Composable
private fun ActionButtonsRow(
    onStopMusic: () -> Unit,
    onTestSound: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // DETENER MÚSICA (Emergency Silence)
        Button(
            onClick = onStopMusic,
            modifier = Modifier
                .weight(1f)
                .height(54.dp)
                .rotate(-1f),
            shape = CutCornerShape(topStart = 10.dp, bottomEnd = 10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PersonaGraphite,
                contentColor = PersonaWhite
            ),
            border = androidx.compose.foundation.BorderStroke(2.dp, PersonaRed)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeOff,
                    contentDescription = null,
                    tint = PersonaRed,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "SILENCIAR",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black
                    )
                )
            }
        }

        // PROBAR SONIDO (Test Trigger)
        Button(
            onClick = onTestSound,
            modifier = Modifier
                .weight(1f)
                .height(54.dp)
                .rotate(1f),
            shape = CutCornerShape(topEnd = 10.dp, bottomStart = 10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PersonaRed,
                contentColor = PersonaWhite
            ),
            border = androidx.compose.foundation.BorderStroke(2.dp, PersonaYellow)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = PersonaYellow,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "PROBAR",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black
                    )
                )
            }
        }
    }
}

@Composable
private fun AudioSelectionCard(
    customAudioName: String?,
    onSelectAudio: () -> Unit,
    onResetAudio: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, PersonaCrimson, CutCornerShape(8.dp)),
        shape = CutCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = PersonaGraphite)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Audiotrack,
                    contentDescription = null,
                    tint = PersonaYellow,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "PISTA DE AUDIO",
                    color = PersonaWhite,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (customAudioName != null) "Archivo actual: $customAudioName" else "Pista activa: Last Surprise (Integrada)",
                color = if (customAudioName != null) PersonaYellow else PersonaLightGray,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onSelectAudio,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PersonaYellow),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PersonaYellow)
                ) {
                    Text("Elegir MP3", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                if (customAudioName != null) {
                    OutlinedButton(
                        onClick = onResetAudio,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PersonaMutedText),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PersonaLightGray)
                    ) {
                        Text("Restaurar Default", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, PersonaDarkGray, RoundedCornerShape(10.dp)),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = PersonaDarkGray.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = PersonaRed,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "¿CÓMO FUNCIONA LA BROMA?",
                    color = PersonaWhite,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "1. Pulsa 'RADAR ON' y acepta los permisos.\n2. Minimiza o apaga la pantalla del móvil.\n3. Cuando alguien diga la palabra \"persona\" o hable de ella, el volumen subirá al 100% y sonará Last Surprise.\n4. Incluye 15s de cooldown tras el susto.",
                color = PersonaMutedText,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp, lineHeight = 18.sp)
            )
        }
    }
}
