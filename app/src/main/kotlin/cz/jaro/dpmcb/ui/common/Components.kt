package cz.jaro.dpmcb.ui.common

import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Accessible
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotAccessible
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.jaro.dpmcb.data.entities.RegistrationNumber
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.toDelay
import cz.jaro.dpmcb.data.realtions.BusStop
import cz.jaro.dpmcb.data.realtions.StopType
import cz.jaro.dpmcb.ui.common.icons.Empty
import cz.jaro.dpmcb.ui.common.icons.LeftHalfDisk
import cz.jaro.dpmcb.ui.common.icons.RightHalfDisk
import cz.jaro.dpmcb.ui.theme.DPMCBTheme
import cz.jaro.dpmcb.ui.theme.Theme
import kotlin.random.Random
import kotlin.time.Duration.Companion.minutes

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun StopTypeIcon(stopType: StopType, modifier: Modifier = Modifier, color: Color = LocalContentColor.current) {
    when (stopType) {
        StopType.Normal -> IconWithTooltip(
            imageVector = Icons.Default.Empty,
            contentDescription = null,
            modifier,
            tint = color,
        )

        StopType.GetOnOnly -> IconWithTooltip(
            imageVector = Icons.Default.RightHalfDisk,
            contentDescription = "Zastávka pouze pro nástup",
            modifier,
            tint = color,
        )

        StopType.GetOffOnly -> IconWithTooltip(
            imageVector = Icons.Default.LeftHalfDisk,
            contentDescription = "Zastávka pouze pro výstup",
            modifier,
            tint = color,
        )
    }
}
@Composable
@ReadOnlyComposable
fun variantColorFor(color: Color) =
    MaterialTheme.colorScheme.variantColorFor(color).takeOrElse { color }

@Stable
fun ColorScheme.variantColorFor(color: Color): Color =
    when (color) {
        background -> surfaceVariant
        surface -> surfaceVariant
        surfaceVariant -> surface
        onBackground -> onSurfaceVariant
        onSurface -> onSurfaceVariant
        onSurfaceVariant -> onSurface
        outline -> outlineVariant
        outlineVariant -> outline
        else -> Color.Unspecified
    }

@Composable
fun Line(
    stops: List<BusStop?>,
    traveledSegments: Int,
    height: Float,
    isOnline: Boolean,
    modifier: Modifier = Modifier,
) {
    val passedColor = if (isOnline) MaterialTheme.colorScheme.primary else LocalContentColor.current
    val busColor = if (isOnline) MaterialTheme.colorScheme.secondary else LocalContentColor.current
    val bgColor = backgroundColorFor(LocalContentColor.current)
    val lineColor = variantColorFor(backgroundColorFor(LocalContentColor.current))
    val stopCount = stops.count()

    val animatedHeight by animateFloatAsState(height, label = "HeightAnimation")

    Canvas(
        modifier = modifier
            .fillMaxHeight()
            .width(20.dp)
            .padding(horizontal = 8.dp),
        contentDescription = "Poloha spoje"
    ) {
        val canvasHeight = size.height
        val lineWidth = 3.dp.toPx()
        val lineXOffset = 7.dp.toPx()
        val rowHeight = canvasHeight / stopCount
        val circleRadius = 5.5.dp.toPx()
        val circleStrokeWidth = 3.dp.toPx()

        translate(left = lineXOffset, top = rowHeight * .5F) {
            drawLine(
                color = lineColor,
                start = UtilFunctions.Offset(),
                end = UtilFunctions.Offset(y = canvasHeight - rowHeight),
                strokeWidth = lineWidth,
            )

            repeat(stopCount) { i ->
                translate(top = i * rowHeight) {
                    val passed = traveledSegments >= i

                    drawCircle(
                        color = if (passed) passedColor else bgColor,
                        radius = circleRadius,
                        center = UtilFunctions.Offset(),
                        style = Fill
                    )
                    drawCircle(
                        color = if (passed) passedColor else lineColor,
                        radius = circleRadius,
                        center = UtilFunctions.Offset(),
                        style = Stroke(
                            width = circleStrokeWidth
                        )
                    )
                }
            }

            drawLine(
                color = passedColor,
                start = UtilFunctions.Offset(),
                end = UtilFunctions.Offset(y = rowHeight * animatedHeight),
                strokeWidth = lineWidth,
            )

            if (height > 0F) drawCircle(
                color = busColor,
                radius = circleRadius - circleStrokeWidth * .5F,
                center = UtilFunctions.Offset(y = rowHeight * animatedHeight)
            )
        }
    }
}

@Stable
fun ColorScheme.backgroundColorFor(contentColor: Color): Color =
    when (contentColor) {
         onPrimary -> primary
         onSecondary -> secondary
         onTertiary -> tertiary
         onBackground -> background
         onError -> error
         onPrimaryContainer -> primaryContainer
         onSecondaryContainer -> secondaryContainer
         onTertiaryContainer -> tertiaryContainer
         onErrorContainer -> errorContainer
         inverseOnSurface -> inverseSurface
         onSurface -> surface
         onSurfaceVariant -> surfaceVariant
        else -> Color.Unspecified
    }

@Composable
@ReadOnlyComposable
fun backgroundColorFor(contentColor: Color) =
    MaterialTheme.colorScheme.backgroundColorFor(contentColor).takeOrElse { MaterialTheme.colorScheme.surface }

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun Vehicle(vehicle: RegistrationNumber?, showInfoButton: Boolean = true) {
    if (vehicle != null) {
        Text(
            text = "ev. č. $vehicle",
            Modifier.padding(horizontal = 8.dp),
        )
        val context = LocalContext.current
        if (showInfoButton) IconWithTooltip(
            Icons.Default.Info,
            "Zobrazit informace o voze",
            Modifier.clickable {
                CustomTabsIntent.Builder()
                    .setShowTitle(true)
                    .build()
                    .launchUrl(context, Uri.parse("https://seznam-autobusu.cz/seznam?operatorName=DP+města+České+Budějovice&prov=1&evc=$vehicle"))
            },
        )
    }
}

@Composable
fun DelayBubble(delayMin: Float) {
    Badge(
        Modifier,
        containerColor = UtilFunctions.colorOfDelayBubbleContainer(delayMin),
        contentColor = UtilFunctions.colorOfDelayBubbleText(delayMin),
    ) {
        Text(
            text = delayMin.toDouble().minutes.toDelay(),
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun Wheelchair(
    lowFloor: Boolean,
    confirmedLowFloor: Boolean?,
    modifier: Modifier = Modifier,
    enableCart: Boolean = false,
) {
    IconWithTooltip(
        imageVector = remember(lowFloor, confirmedLowFloor) {
            when {
                enableCart && Random.nextFloat() < .01F -> Icons.Default.ShoppingCart
                confirmedLowFloor == true -> Icons.AutoMirrored.Filled.Accessible
                confirmedLowFloor == false -> Icons.Default.NotAccessible
                lowFloor -> Icons.AutoMirrored.Filled.Accessible
                else -> Icons.Default.NotAccessible
            }
        },
        contentDescription = when {
            confirmedLowFloor == true -> "Potvrzený nízkopodlažní vůz"
            confirmedLowFloor == false -> "Potvrzený vysokopodlažní vůz"
            lowFloor -> "Plánovaný nízkopodlažní vůz"
            else -> "Nezaručený nízkopodlažní vůz"
        },
        modifier,
        tint = when {
            confirmedLowFloor == false && lowFloor -> MaterialTheme.colorScheme.error
            confirmedLowFloor != null -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.onSurface
        }
    )
}

@Composable
fun Name(name: String, modifier: Modifier = Modifier, subName: String? = null) {
    Text(buildAnnotatedString {
        withStyle(style = SpanStyle(fontSize = 24.sp)) {
            append(name)
        }
        if (subName != null) withStyle(style = SpanStyle(fontSize = 14.sp)) {
            append(subName)
        }
    }, modifier, color = MaterialTheme.colorScheme.primary)
}

@ExperimentalMaterial3Api
@Composable
fun IconWithTooltip(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tooltipText: String? = contentDescription,
    tint: Color = LocalContentColor.current,
) = if (tooltipText != null) TooltipBox(
    tooltip = {
        DPMCBTheme(
            useDarkTheme = isSystemInDarkTheme(),
            useDynamicColor = true,
            theme = Theme.Yellow,
            doTheThing = false,
        ) {
            PlainTooltip {
                Text(text = tooltipText)
            }
        }
    },
    state = rememberTooltipState(),
    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider()
) {
    Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        modifier = modifier,
        tint = tint
    )
}
else
    Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        modifier = modifier,
        tint = tint
    )