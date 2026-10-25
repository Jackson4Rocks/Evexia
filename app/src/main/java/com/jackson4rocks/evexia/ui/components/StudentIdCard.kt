package com.jackson4rocks.evexia.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jackson4rocks.evexia.data.AppPreferences
import com.jackson4rocks.evexia.ui.theme.DF

@Composable
fun StudentIdCard(
    preferences: AppPreferences,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val colors = DF.colors
    val accent = colors.accent
    val dark = colors.dark
    val cardText = if (dark) Color(0xFFF6F1E4) else Color(0xFF171916)
    val cardSecondary = if (dark) cardText.copy(alpha = 0.68f) else Color(0xFF63665D)
    val cardMuted = if (dark) cardText.copy(alpha = 0.45f) else Color(0xFF8C8F85)
    val cardBackground = if (dark) {
        Brush.linearGradient(listOf(Color(0xFF1D201A), Color(0xFF0D0F0B)))
    } else {
        Brush.linearGradient(listOf(Color(0xFFFFFEFA), Color(0xFFF1EFE8)))
    }
    val school = preferences.schoolName.ifBlank { "Your school" }
    val name = preferences.displayName.trim().ifBlank { "Your name" }
    val favouriteSubjects = preferences.program.trim().ifBlank { "Add favourite subjects" }
    val year = preferences.year.trim()
    val id = preferences.identityNumber.trim()
    val email = preferences.schoolEmail.trim()

    Box(
        modifier
            .fillMaxWidth()
            .aspectRatio(1.56f)
            .clip(RoundedCornerShape(28.dp))
            .then(if (onClick != null) Modifier.pressable(onClick = onClick) else Modifier)
            .background(cardBackground)
            .border(
                1.dp,
                if (dark) Color.White.copy(alpha = 0.12f) else Color(0xFF171916).copy(alpha = 0.08f),
                RoundedCornerShape(28.dp),
            ),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(accent.copy(alpha = 0.36f), Color.Transparent),
                    center = Offset(size.width * 0.92f, size.height * 0.05f),
                    radius = size.width * 0.56f,
                ),
                radius = size.width * 0.56f,
                center = Offset(size.width * 0.92f, size.height * 0.05f),
            )
            val lineColor = if (dark) {
                Color.White.copy(alpha = 0.055f)
            } else {
                Color(0xFF171916).copy(alpha = 0.045f)
            }
            for (i in 0..5) {
                drawLine(
                    color = lineColor,
                    start = Offset(size.width * 0.62f, size.height * (0.52f + i * 0.062f)),
                    end = Offset(size.width * 0.94f, size.height * (0.52f + i * 0.062f)),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            drawCircle(
                color = accent.copy(alpha = 0.25f),
                radius = size.width * 0.16f,
                center = Offset(size.width * 0.06f, size.height * 1.05f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.2.dp.toPx()),
            )
        }
        Column(Modifier.fillMaxSize().padding(17.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PersonAvatar(
                    name = school,
                    path = preferences.schoolLogoPath,
                    size = 28.dp,
                    fill = accent,
                )
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    BasicText(
                        text = "EVEXIA",
                        style = DF.type.label.copy(color = accent, fontSize = 7.5.sp),
                    )
                    BasicText(
                        text = school,
                        style = DF.type.caption.copy(
                            color = cardText,
                            fontWeight = FontWeight.Medium,
                            fontSize = 10.5.sp,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                BasicText(
                    text = "STUDENT ID",
                    style = DF.type.label.copy(color = cardMuted, fontSize = 7.5.sp),
                )
            }
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                PersonAvatar(
                    name = name,
                    path = preferences.profileImagePath,
                    size = 58.dp,
                    fill = accent,
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    BasicText(
                        text = name,
                        style = DF.type.serif.copy(
                            color = cardText,
                            fontSize = 25.sp,
                            lineHeight = 27.sp,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(3.dp))
                    BasicText(
                        text = listOf(favouriteSubjects, year)
                            .filter { it.isNotBlank() }
                            .joinToString(" · "),
                        style = DF.type.caption.copy(color = cardSecondary, fontSize = 10.5.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        if (dark) Color.White.copy(alpha = 0.1f)
                        else Color(0xFF171916).copy(alpha = 0.1f),
                    ),
            )
            Spacer(Modifier.height(9.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    BasicText(
                        text = "STUDENT NO.",
                        style = DF.type.label.copy(
                            color = if (dark) cardText.copy(alpha = 0.38f) else cardMuted,
                            fontSize = 6.5.sp,
                        ),
                    )
                    BasicText(
                        text = id.ifBlank { "— — — —" },
                        style = DF.type.cardTitle.copy(
                            color = cardText,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Normal,
                        ),
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    BasicText(
                        text = email.ifBlank { "PERSONAL COPY" },
                        style = DF.type.caption.copy(
                            color = cardMuted,
                            fontSize = 9.sp,
                        ),
                        maxLines = 1,
                    )
                    Spacer(Modifier.height(2.dp))
                    BasicText(
                        text = "EVEXIA · LOCAL",
                        style = DF.type.label.copy(color = accent.copy(alpha = 0.8f), fontSize = 6.5.sp),
                    )
                }
            }
        }
    }
}
