package com.jackson4rocks.evexia.ui.components

import android.graphics.BitmapFactory
import kotlin.math.sign
import kotlin.math.abs
import kotlinx.coroutines.launch
import com.jackson4rocks.evexia.ui.components.gestures.DampedDragAnimation
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.Key
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.derivedStateOf
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.focusable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Home
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.foundation.layout.offset
import com.jackson4rocks.evexia.R
import com.jackson4rocks.evexia.ui.AppScreen
import com.jackson4rocks.evexia.ui.RootScreens
import com.jackson4rocks.evexia.ui.theme.DF
import com.jackson4rocks.evexia.ui.theme.LocalColors
import com.jackson4rocks.evexia.ui.theme.LocalGlassOpacity
import com.jackson4rocks.evexia.ui.theme.LocalReduceMotion
import com.jackson4rocks.evexia.ui.theme.LocalType
import com.jackson4rocks.evexia.util.FileStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

object Radii {
    val hero = RoundedCornerShape(32.dp, 32.dp, 32.dp, 12.dp)
    val card = RoundedCornerShape(26.dp)
    val cardSmall = RoundedCornerShape(20.dp)
    val field = RoundedCornerShape(17.dp)
    val pill = CircleShape
    val sheet = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
}

val PagePadding = 20.dp
val BottomContentPadding = 140.dp

fun Modifier.pressable(
    enabled: Boolean = true,
    role: Role? = Role.Button,
    onClick: () -> Unit,
): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val reduceMotion = LocalReduceMotion.current
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled && !reduceMotion) 0.968f else 1f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessHigh),
        label = "pressScale",
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            alpha = if (enabled) 1f else 0.5f
        }
        .clickable(
            interactionSource = interaction,
            indication = null,
            enabled = enabled,
            role = role,
            onClick = onClick,
        )
}

@Composable
fun Icon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
) {
    val colors = DF.colors
    val resolved = if (tint == Color.Unspecified) colors.textPrimary else tint
    Image(
        painter = rememberVectorPainter(imageVector),
        contentDescription = contentDescription,
        modifier = modifier,
        colorFilter = ColorFilter.tint(resolved),
    )
}

@Composable
fun AmbientBackground(modifier: Modifier = Modifier) {    val colors = DF.colors
    Canvas(modifier.fillMaxSize()) {
        drawRect(colors.background)
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(colors.auraA, Color.Transparent),
                center = Offset(size.width * 0.92f, size.height * 0.04f),
                radius = size.width * 0.82f,
            ),
        )
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(colors.auraB, Color.Transparent),
                center = Offset(size.width * 0.05f, size.height * 0.45f),
                radius = size.width * 0.72f,
            ),
        )
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(colors.auraC, Color.Transparent),
                center = Offset(size.width * 0.72f, size.height * 0.96f),
                radius = size.width * 0.75f,
            ),
        )
    }
}

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = Radii.card,
    accentTint: Boolean = false,
    tint: Color? = null,
    strong: Boolean = false,
    bordered: Boolean = true,
    elevated: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = DF.colors
    val opacity = LocalGlassOpacity.current
    val base = tint ?: if (strong) colors.glassStrong else colors.glass
    val fill = (if (accentTint) colors.accentSoft else base).let {
        it.copy(alpha = (it.alpha * opacity).coerceIn(0.08f, 0.96f))
    }
    val outline = if (accentTint) colors.accent.copy(alpha = 0.22f) else colors.border
    val clickModifier = if (onClick != null) Modifier.pressable(onClick = onClick) else Modifier
    val shadow = if (elevated) {
        Modifier.shadow(
            elevation = 18.dp,
            shape = shape,
            clip = false,
            ambientColor = colors.shadow,
            spotColor = colors.shadow,
        )
    } else Modifier
    Box(
        modifier = modifier
            .then(shadow)
            .then(clickModifier)
            .clip(shape)
            .background(
                brush = Brush.linearGradient(
                    listOf(
                        fill.copy(alpha = (fill.alpha * 1.12f).coerceAtMost(1f)),
                        fill.copy(alpha = (fill.alpha * 0.72f).coerceAtLeast(0.04f)),
                    ),
                ),
            )
            .then(
                if (bordered) {
                    Modifier.border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            listOf(
                                Color.White.copy(alpha = if (colors.dark) 0.20f else 0.72f),
                                outline,
                                outline.copy(alpha = 0.35f),
                            ),
                        ),
                        shape = shape,
                    )
                } else Modifier,
            )
            .drawWithContent {
                drawContent()
                drawLine(
                    brush = Brush.horizontalGradient(
                        listOf(
                            Color.White.copy(alpha = if (colors.dark) 0.22f else 0.85f),
                            Color.Transparent,
                        ),
                    ),
                    start = Offset(size.width * 0.06f, 0.7.dp.toPx()),
                    end = Offset(size.width * 0.64f, 0.7.dp.toPx()),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            .padding(contentPadding),
        content = content,
    )
}

@Composable
fun BrandMark(size: Dp = 40.dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val unit = this.size.width / 108f
        val bg = Color(0xFF171916)
        val paper = Color(0xFFF4EFE3)
        val citron = Color(0xFFD8FF63)
        drawRoundRect(color = bg, cornerRadius = androidx.compose.ui.geometry.CornerRadius(24f * unit))
        val back = Path().apply {
            moveTo(44.3f * unit, 27.4f * unit)
            quadraticBezierTo(44.3f * unit, 23.6f * unit, 48.1f * unit, 23.6f * unit)
            lineTo(69.2f * unit, 23.6f * unit)
            lineTo(80.6f * unit, 35f * unit)
            lineTo(80.6f * unit, 69.6f * unit)
            quadraticBezierTo(80.6f * unit, 73.8f * unit, 76.4f * unit, 73.8f * unit)
            lineTo(67.1f * unit, 73.8f * unit)
        }
        drawPath(
            back,
            color = citron,
            style = Stroke(width = 3.4f * unit, cap = StrokeCap.Round),
        )
        val page = Path().apply {
            moveTo(27.4f * unit, 41.3f * unit)
            quadraticBezierTo(27.4f * unit, 36.7f * unit, 32.1f * unit, 36.7f * unit)
            lineTo(60.3f * unit, 36.7f * unit)
            lineTo(73.8f * unit, 50.2f * unit)
            lineTo(73.8f * unit, 78.1f * unit)
            quadraticBezierTo(73.8f * unit, 83.5f * unit, 68.3f * unit, 83.5f * unit)
            lineTo(32.1f * unit, 83.5f * unit)
            quadraticBezierTo(27.4f * unit, 83.5f * unit, 27.4f * unit, 78.9f * unit)
            close()
        }
        drawPath(page, paper)
        val fold = Path().apply {
            moveTo(60.3f * unit, 36.7f * unit)
            lineTo(60.3f * unit, 46.4f * unit)
            quadraticBezierTo(60.3f * unit, 50.2f * unit, 64.1f * unit, 50.2f * unit)
            lineTo(73.8f * unit, 50.2f * unit)
            close()
        }
        drawPath(fold, citron)
        val flow = Path().apply {
            moveTo(39.2f * unit, 61.6f * unit)
            cubicTo(47.7f * unit, 57.8f * unit, 51f * unit, 68.8f * unit, 59.5f * unit, 64.6f * unit)
            cubicTo(63.7f * unit, 62.5f * unit, 65.4f * unit, 60.4f * unit, 66.2f * unit, 58.2f * unit)
        }
        drawPath(
            flow,
            color = bg,
            style = Stroke(width = 3.2f * unit, cap = StrokeCap.Round),
        )
        drawCircle(color = citron, radius = 2.45f * unit, center = Offset(67.9f * unit, 65.8f * unit))
        drawCircle(
            color = bg,
            radius = 2.45f * unit,
            center = Offset(67.9f * unit, 65.8f * unit),
            style = Stroke(width = 1.2f * unit),
        )
    }
}

@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else 8,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Sentences,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {},
    error: String? = null,
    supporting: String? = null,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    val colors = DF.colors
    val type = DF.type
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val reduceMotion = LocalReduceMotion.current
    val borderColor by animateColorAsState(
        if (error != null) colors.danger else if (focused) colors.accentInk else colors.border,
        animationSpec = if (reduceMotion) snap() else tween(180),
        label = "fieldBorder",
    )
    Column(modifier) {
        if (label.isNotBlank()) {
            BasicText(
                text = label,
                style = type.caption.copy(
                    color = colors.textSecondary,
                    fontWeight = FontWeight.Medium,
                ),
                modifier = Modifier.padding(start = 4.dp, bottom = 7.dp),
            )
        }
        CompositionLocalProvider(
            LocalTextSelectionColors provides TextSelectionColors(
                handleColor = colors.accentInk,
                backgroundColor = colors.accentSoft,
            ),
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(Radii.field)
                    .background(colors.glassQuiet)
                    .border(1.dp, borderColor.copy(alpha = if (focused) 0.9f else 0.6f), Radii.field)
                    .padding(horizontal = 15.dp, vertical = if (singleLine) 15.dp else 14.dp),
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    if (leadingIcon != null) {
                        Icon(
                            imageVector = leadingIcon,
                            contentDescription = null,
                            tint = colors.textTertiary,
                            modifier = Modifier.padding(top = 1.dp, end = 10.dp).size(18.dp),
                        )
                    }
                    Box(Modifier.weight(1f)) {
                        if (value.isEmpty()) {
                            BasicText(
                                text = placeholder,
                                style = type.body.copy(color = colors.textTertiary),
                            )
                        }
                        BasicTextField(
                            value = value,
                            onValueChange = onValueChange,
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = type.body.copy(color = colors.textPrimary),
                            singleLine = singleLine,
                            minLines = minLines,
                            maxLines = maxLines,
                            enabled = enabled,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = keyboardType,
                                capitalization = capitalization,
                                imeAction = imeAction,
                            ),
                            keyboardActions = KeyboardActions(onAny = { onImeAction() }),
                            visualTransformation = visualTransformation,
                            cursorBrush = Brush.verticalGradient(listOf(colors.accentInk, colors.accentInk)),
                            interactionSource = interaction,
                        )
                    }
                }
            }
        }
        val assist = error ?: supporting
        if (assist != null) {
            BasicText(
                text = assist,
                style = type.caption.copy(
                    color = if (error != null) colors.danger else colors.textTertiary,
                ),
                modifier = Modifier.padding(start = 4.dp, top = 6.dp),
            )
        }
    }
}

@Composable
fun PrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    loading: Boolean = false,
    danger: Boolean = false,
) {
    val colors = DF.colors
    val type = DF.type
    val buttonColor = if (danger) colors.danger else colors.accent
    val contentColor = if (danger) Color.White else colors.onAccent
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (enabled) buttonColor else colors.glassQuiet)
            .pressable(enabled = enabled && !loading, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (loading) {
                Spinner(size = 18.dp, color = contentColor)
                Spacer(Modifier.width(9.dp))
            } else if (icon != null) {
                Icon(icon, null, tint = contentColor, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
            }
            BasicText(
                text = label,
                style = type.button.copy(color = if (enabled) contentColor else colors.textTertiary),
            )
        }
    }
}

@Composable
fun SecondaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    danger: Boolean = false,
) {
    val colors = DF.colors
    val type = DF.type
    Box(
        modifier = modifier
            .height(50.dp)
            .clip(RoundedCornerShape(17.dp))
            .background(colors.glassQuiet)
            .border(1.dp, if (danger) colors.danger.copy(alpha = 0.35f) else colors.border, RoundedCornerShape(17.dp))
            .pressable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    icon,
                    null,
                    tint = if (danger) colors.danger else colors.textPrimary,
                    modifier = Modifier.size(17.dp),
                )
                Spacer(Modifier.width(7.dp))
            }
            BasicText(
                text = label,
                style = type.button.copy(color = if (danger) colors.danger else colors.textPrimary),
            )
        }
    }
}

@Composable
fun IconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 42.dp,
    tint: Color? = null,
    glass: Boolean = true,
    enabled: Boolean = true,
) {
    val colors = DF.colors
    val base = if (glass) {
        Modifier
            .background(colors.glassQuiet, CircleShape)
            .border(1.dp, colors.border, CircleShape)
    } else Modifier
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .then(base)
            .pressable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint ?: colors.textPrimary,
            modifier = Modifier.size(size * 0.43f),
        )
    }
}

@Composable
fun AccentIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 42.dp,
) {
    val colors = DF.colors
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(colors.accent)
            .pressable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, tint = colors.onAccent, modifier = Modifier.size(size * 0.44f))
    }
}

@Composable
fun BackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {    IconButton(
        icon = Icons.Filled.ArrowBack,
        contentDescription = "Go back",
        onClick = onClick,
        modifier = modifier,
        size = 40.dp,
    )
}

@Composable
fun ScreenHeader(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val colors = DF.colors
    val type = DF.type
    Column(
        modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = PagePadding, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BackButton(onClick = onBack)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                if (eyebrow != null) {
                    BasicText(
                        text = eyebrow.uppercase(),
                        style = type.label.copy(color = colors.textTertiary),
                    )
                }
                BasicText(
                    text = title,
                    style = type.sectionTitle.copy(color = colors.textPrimary),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                content = actions,
            )
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors = DF.colors
    val type = DF.type
    Row(
        modifier.fillMaxWidth().padding(horizontal = PagePadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(
            text = title,
            style = type.sectionTitle.copy(color = colors.textPrimary),
            modifier = Modifier.weight(1f),
        )
        if (trailing != null) trailing()
        if (action != null && onAction != null) {
            BasicText(
                text = action,
                style = type.caption.copy(color = colors.textSecondary, fontWeight = FontWeight.Medium),
                modifier = Modifier
                    .clip(CircleShape)
                    .pressable(onClick = onAction)
                    .padding(horizontal = 10.dp, vertical = 7.dp),
            )
        }
    }
}

@Composable
fun ScreenTitle(
    title: String,
    eyebrow: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    val colors = DF.colors
    val type = DF.type
    Column(modifier.fillMaxWidth().statusBarsPadding().padding(start = PagePadding, top = 14.dp, end = PagePadding)) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                BasicText(
                    text = eyebrow.uppercase(),
                    style = type.label.copy(color = colors.textTertiary),
                    modifier = Modifier.padding(bottom = 7.dp),
                )
                BasicText(
                    text = title,
                    style = type.pageTitle.copy(color = colors.textPrimary, fontSize = 32.sp, lineHeight = 36.sp),
                )
                if (subtitle != null) {
                    BasicText(
                        text = subtitle,
                        style = type.bodySmall.copy(color = colors.textSecondary),
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
            if (trailing != null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    content = trailing,
                )
            }
        }
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val colors = DF.colors
    val type = DF.type
    Column(
        modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 38.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(74.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(colors.accentSoft)
                .border(1.dp, colors.accent.copy(alpha = 0.22f), RoundedCornerShape(26.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = colors.accentInk, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.height(18.dp))
        BasicText(
            text = title,
            style = type.sectionTitle.copy(color = colors.textPrimary, textAlign = TextAlign.Center),
        )
        Spacer(Modifier.height(6.dp))
        BasicText(
            text = message,
            style = type.bodySmall.copy(color = colors.textSecondary, textAlign = TextAlign.Center),
            modifier = Modifier.widthIn(max = 290.dp),
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(18.dp))
            SecondaryButton(label = actionLabel, onClick = onAction)
        }
    }
}

@Composable
fun AppSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = DF.colors
    val haptic = LocalHapticFeedback.current
    val reduceMotion = LocalReduceMotion.current
    val offset by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = if (reduceMotion) {
            snap()
        } else {
            spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMedium)
        },
        label = "switch",
    )
    Box(
        modifier = modifier
            .width(48.dp)
            .height(29.dp)
            .clip(CircleShape)
            .background(if (checked) colors.accent else colors.glassQuiet)
            .border(1.dp, if (checked) colors.accent.copy(alpha = 0.4f) else colors.border, CircleShape)
            .pressable(enabled = enabled, role = Role.Switch) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onCheckedChange(!checked)
            }
            .padding(3.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .size(23.dp)
                .offset(x = (offset * 19).dp)
                .clip(CircleShape)
                .background(if (checked) colors.onAccent else colors.textTertiary),
        )
    }
}

@Composable
fun SegmentedControl(
    items: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = DF.colors
    val type = DF.type
    val reduceMotion = LocalReduceMotion.current
    Row(
        modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(colors.glassQuiet)
            .border(1.dp, colors.border.copy(alpha = 0.6f), CircleShape)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        items.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            val background by animateColorAsState(
                if (selected) colors.glassStrong else Color.Transparent,
                animationSpec = if (reduceMotion) snap() else tween(180),
                label = "segment",
            )
            Box(
                Modifier
                    .weight(1f)
                    .clip(CircleShape)
                    .background(background)
                    .then(
                        if (selected) Modifier.border(1.dp, colors.border, CircleShape) else Modifier,
                    )
                    .pressable(onClick = { onSelected(index) })
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = label,
                    style = type.caption.copy(
                        color = if (selected) colors.textPrimary else colors.textSecondary,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun AppChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    dotColor: Color? = null,
) {
    val colors = DF.colors
    val type = DF.type
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(if (selected) colors.accentSoft else colors.glassQuiet)
            .border(1.dp, if (selected) colors.accent.copy(alpha = 0.4f) else colors.border, CircleShape)
            .pressable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (dotColor != null) {
            Box(
                Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(dotColor),
            )
            Spacer(Modifier.width(7.dp))
        }
        BasicText(
            text = label,
            style = type.caption.copy(
                color = if (selected) colors.textPrimary else colors.textSecondary,
                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            ),
            maxLines = 1,
        )
    }
}

@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 84.dp,
    stroke: Dp = 8.dp,
    color: Color? = null,
    trackColor: Color? = null,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val colors = DF.colors
    val reduceMotion = LocalReduceMotion.current
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = if (reduceMotion) snap() else tween(650),
        label = "ring",
    )
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val width = stroke.toPx()
            drawArc(
                color = trackColor ?: colors.glassQuiet,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = width, cap = StrokeCap.Round),
            )
            drawArc(
                color = color ?: colors.accent,
                startAngle = -90f,
                sweepAngle = 360f * animated,
                useCenter = false,
                style = Stroke(width = width, cap = StrokeCap.Round),
            )
        }
        content()
    }
}

@Composable
fun LinearProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color? = null,
) {
    val colors = DF.colors
    val reduceMotion = LocalReduceMotion.current
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = if (reduceMotion) snap() else tween(500),
        label = "progress",
    )
    Box(
        modifier
            .fillMaxWidth()
            .height(5.dp)
            .clip(CircleShape)
            .background(colors.glassQuiet),
    ) {
        Box(
            Modifier
                .fillMaxWidth(animated)
                .height(5.dp)
                .clip(CircleShape)
                .background(color ?: colors.accent),
        )
    }
}

@Composable
fun AnimatedCheck(
    checked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 26.dp,
) {
    val colors = DF.colors
    val haptic = LocalHapticFeedback.current
    val reduceMotion = LocalReduceMotion.current
    val progress by animateFloatAsState(
        if (checked) 1f else 0f,
        animationSpec = if (reduceMotion) {
            snap()
        } else {
            spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMedium)
        },
        label = "check",
    )
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .pressable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onToggle()
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 1.8.dp.toPx()
            drawCircle(
                color = if (checked) colors.accent else colors.borderStrong,
                style = Stroke(width = stroke),
            )
            if (progress > 0f) {
                drawCircle(color = colors.accent, radius = this.size.minDimension / 2f * progress)
                val path = Path().apply {
                    moveTo(this@Canvas.size.width * 0.28f, this@Canvas.size.height * 0.52f)
                    lineTo(this@Canvas.size.width * 0.44f, this@Canvas.size.height * 0.68f)
                    lineTo(this@Canvas.size.width * 0.74f, this@Canvas.size.height * 0.34f)
                }
                drawPath(
                    path = path,
                    color = colors.onAccent,
                    style = Stroke(
                        width = 2.dp.toPx() * progress.coerceAtLeast(0.4f),
                        cap = StrokeCap.Round,
                    ),
                    alpha = progress,
                )
            }
        }
    }
}

@Composable
fun Spinner(size: Dp = 20.dp, color: Color? = null) {
    val colors = DF.colors
    val reduceMotion = LocalReduceMotion.current
    val angle = if (reduceMotion) 0f else rememberSpinAngle()
    Canvas(Modifier.size(size)) {
        drawArc(
            color = (color ?: colors.accentInk).copy(alpha = 0.22f),
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round),
        )
        drawArc(
            color = color ?: colors.accentInk,
            startAngle = angle,
            sweepAngle = 100f,
            useCenter = false,
            style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round),
        )
    }
}

@Composable
private fun rememberSpinAngle(): Float {
    val animated = androidx.compose.animation.core.rememberInfiniteTransition(label = "spin")
    val angle by animated.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
        ),
        label = "angle",
    )
    return angle
}

@Composable
fun FloatingAddButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = DF.colors
    Box(
        modifier
            .size(56.dp)
            .shadow(14.dp, CircleShape, clip = false, ambientColor = colors.shadow, spotColor = colors.shadow)
            .clip(CircleShape)
            .background(colors.accent)
            .border(1.dp, Color.White.copy(alpha = 0.24f), CircleShape)
            .pressable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Filled.Add, "Create something", tint = colors.onAccent, modifier = Modifier.size(25.dp))
    }
}

@Composable
fun GlassSheet(
    title: String,
    onDismiss: () -> Unit,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = DF.colors
    val type = DF.type
    val configuration = LocalConfiguration.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            decorFitsSystemWindows = false,
        ),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(colors.scrim.copy(alpha = 0.5f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .widthIn(max = 620.dp)
                    .padding(horizontal = 8.dp)
                    .navigationBarsPadding()
                    .imePadding()
                    .heightIn(max = (configuration.screenHeightDp * 0.88f).dp)
                    .shadow(30.dp, Radii.sheet, clip = false, ambientColor = colors.shadow, spotColor = colors.shadow)
                    .clip(Radii.sheet)
                    .background(colors.background.copy(alpha = 0.98f))
                    .border(
                        1.dp,
                        Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.5f), colors.border)),
                        Radii.sheet,
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    )
                    .padding(top = 10.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp)
                    .padding(bottom = 26.dp),
            ) {
                Box(
                    Modifier
                        .width(38.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(colors.borderStrong)
                        .align(Alignment.CenterHorizontally),
                )
                Spacer(Modifier.height(20.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        BasicText(
                            text = title,
                            style = type.pageTitle.copy(color = colors.textPrimary, fontSize = 25.sp),
                        )
                        if (subtitle != null) {
                            BasicText(
                                text = subtitle,
                                style = type.bodySmall.copy(color = colors.textSecondary),
                                modifier = Modifier.padding(top = 5.dp),
                            )
                        }
                    }
                    IconButton(
                        icon = Icons.Filled.Close,
                        contentDescription = "Close",
                        onClick = onDismiss,
                        size = 36.dp,
                        glass = false,
                    )
                }
                Spacer(Modifier.height(20.dp))
                content()
            }
        }
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String = "Delete",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    danger: Boolean = true,
) {
    val colors = DF.colors
    val type = DF.type
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(colors.scrim.copy(alpha = 0.45f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
            contentAlignment = Alignment.Center,
        ) {
            GlassSurface(
                modifier = Modifier
                    .padding(28.dp)
                    .widthIn(max = 380.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    ),
                shape = RoundedCornerShape(28.dp),
                strong = true,
                contentPadding = PaddingValues(22.dp),
            ) {
                Column {
                    BasicText(
                        text = title,
                        style = type.sectionTitle.copy(color = colors.textPrimary),
                    )
                    BasicText(
                        text = message,
                        style = type.bodySmall.copy(color = colors.textSecondary),
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        SecondaryButton(
                            label = "Keep it",
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                        )
                        Box(Modifier.weight(1f)) {
                            PrimaryButton(
                                label = confirmLabel,
                                onClick = onConfirm,
                                modifier = Modifier.fillMaxWidth(),
                                danger = danger,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppSnackbar(
    message: String?,
    onDismissed: () -> Unit,
    modifier: Modifier = Modifier,
) {    val colors = DF.colors
    val type = DF.type
    val reduceMotion = LocalReduceMotion.current
    LaunchedEffect(message) {
        if (message != null) {
            delay(2800)
            onDismissed()
        }
    }
    androidx.compose.animation.AnimatedVisibility(
        visible = message != null,
        enter = if (reduceMotion) {
            androidx.compose.animation.fadeIn()
        } else {
            androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInVertically { it / 2 }
        },
        exit = if (reduceMotion) {
            androidx.compose.animation.fadeOut()
        } else {
            androidx.compose.animation.fadeOut() + androidx.compose.animation.slideOutVertically { it / 2 }
        },
        modifier = modifier,
    ) {
        Box(
            Modifier
                .padding(horizontal = 22.dp)
                .widthIn(max = 520.dp)
                .background(colors.textPrimary, RoundedCornerShape(18.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            BasicText(
                text = message.orEmpty(),
                style = type.bodySmall.copy(color = colors.background),
            )
        }
    }
}

@Composable
fun FileImage(
    path: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    if (path.isNullOrBlank()) return
    val bitmap by produceState<androidx.compose.ui.graphics.ImageBitmap?>(initialValue = null, path) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val options = BitmapFactory.Options().apply {
                    inSampleSize = 2
                }
                BitmapFactory.decodeFile(path, options)?.asImageBitmap()
            }.getOrNull()
        }
    }
    bitmap?.let {
        Image(
            bitmap = it,
            contentDescription = null,
            modifier = modifier.clip(CircleShape),
            contentScale = contentScale,
        )
    }
}

@Composable
fun PersonAvatar(
    name: String,
    path: String?,
    modifier: Modifier = Modifier,
    size: Dp = 42.dp,
    fill: Color? = null,
) {
    val colors = DF.colors
    val type = DF.type
    val hasImage = !path.isNullOrBlank() && File(path).exists()
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(fill ?: colors.accentSoft)
            .border(1.dp, colors.border, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (hasImage) {
            FileImage(path, Modifier.fillMaxSize())
        } else {
            BasicText(
                text = name.trim().firstOrNull()?.uppercase() ?: "J",
                style = type.sectionTitle.copy(
                    color = if (fill != null) Color.White else colors.accentInk,
                    fontSize = (size.value * 0.38f).sp,
                ),
            )
        }
    }
}

@Composable
fun LeonAvatar(
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
) {
    val colors = DF.colors
    Image(
        painter = painterResource(R.drawable.leon_avatar),
        contentDescription = "Leon Sony",
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .border(1.dp, colors.border, CircleShape),
        contentScale = ContentScale.Crop,
    )
}

@Composable
fun JeevanAvatar(
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
) {
    val colors = DF.colors
    Image(
        painter = painterResource(R.drawable.jeevan_avatar),
        contentDescription = "Ambhavaram Jeevan Kumar Reddy",
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .border(1.dp, colors.border, CircleShape),
        contentScale = ContentScale.Crop,
    )
}

@Composable
fun FileGlyph(
    mimeType: String,
    isBookmark: Boolean = false,
    tint: Color? = null,
    modifier: Modifier = Modifier,
) {
    val colors = DF.colors
    val icon = when {
        isBookmark || mimeType.startsWith("text/uri-list") -> Icons.Filled.Link
        mimeType.contains("pdf") -> Icons.Filled.PictureAsPdf
        mimeType.startsWith("image") -> Icons.Filled.Image
        mimeType.contains("word") || mimeType.contains("text") -> Icons.Filled.Description
        else -> Icons.Filled.InsertDriveFile
    }
    Box(
        modifier
            .size(42.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(colors.glassQuiet)
            .border(1.dp, colors.border, RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = tint ?: colors.textSecondary, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun InfoRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    iconTint: Color? = null,
) {
    val colors = DF.colors
    val type = DF.type
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .then(if (onClick != null) Modifier.pressable(onClick = onClick) else Modifier)
            .padding(vertical = 10.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(colors.glassQuiet),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = iconTint ?: colors.textSecondary, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            BasicText(text = title, style = type.cardTitle.copy(color = colors.textPrimary))
            if (subtitle != null) {
                BasicText(
                    text = subtitle,
                    style = type.caption.copy(color = colors.textSecondary),
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        if (trailing != null) trailing()
    }
}

@Composable
fun Dot(dayIndex: Int, active: Boolean) {
    val colors = DF.colors
    Box(
        Modifier
            .size(6.dp)
            .clip(CircleShape)
            .background(if (active) colors.accentInk else colors.borderStrong),
    )
}

@Composable
fun FileKindBadge(label: String, color: Color = DF.colors.accent) {
    val colors = DF.colors
    val type = DF.type
    Box(
        Modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        BasicText(
            text = label.uppercase(),
            style = type.label.copy(color = color.copy(alpha = 0.9f), fontSize = 8.5.sp),
        )
    }
}
