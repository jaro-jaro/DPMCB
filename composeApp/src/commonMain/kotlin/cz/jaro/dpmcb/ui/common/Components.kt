package cz.jaro.dpmcb.ui.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Accessible
import androidx.compose.material.icons.automirrored.filled.ArrowLeft
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material.icons.automirrored.filled.ArrowRightAlt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotAccessible
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Badge
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import cz.jaro.dpmcb.data.entities.RegistrationNumber
import cz.jaro.dpmcb.data.helperclasses.Offset
import cz.jaro.dpmcb.data.helperclasses.SystemClock
import cz.jaro.dpmcb.data.helperclasses.Traction
import cz.jaro.dpmcb.data.helperclasses.awaitFrame
import cz.jaro.dpmcb.data.helperclasses.colorOfDelayBubbleContainer
import cz.jaro.dpmcb.data.helperclasses.colorOfDelayBubbleText
import cz.jaro.dpmcb.data.helperclasses.cz
import cz.jaro.dpmcb.data.helperclasses.isTypeOf
import cz.jaro.dpmcb.data.helperclasses.plus
import cz.jaro.dpmcb.data.helperclasses.toDelay
import cz.jaro.dpmcb.data.helperclasses.todayHere
import cz.jaro.dpmcb.data.realtions.BusStop
import cz.jaro.dpmcb.data.realtions.StopType
import cz.jaro.dpmcb.ui.common.icons.LeftHalfDisk
import cz.jaro.dpmcb.ui.common.icons.RightHalfDisk
import cz.jaro.dpmcb.ui.theme.DPMCBTheme
import cz.jaro.dpmcb.ui.theme.LocalIsDarkThemeUsed
import cz.jaro.dpmcb.ui.theme.LocalIsDynamicThemeUsed
import cz.jaro.dpmcb.ui.theme.LocalTheme
import cz.jaro.dpmcb.ui.theme.Theme
import dpmcb.composeapp.generated.resources.Res
import dpmcb.composeapp.generated.resources.bus
import dpmcb.composeapp.generated.resources.diesel
import dpmcb.composeapp.generated.resources.ebus
import dpmcb.composeapp.generated.resources.hybrid
import dpmcb.composeapp.generated.resources.parcial
import dpmcb.composeapp.generated.resources.trolejbus
import dpmcb.composeapp.generated.resources.zemeplyn
import kotlinx.coroutines.delay
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.random.Random
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun StopTypeIcon(stopType: StopType, modifier: Modifier = Modifier, color: Color = LocalContentColor.current) {
    when (stopType) {
        StopType.Normal -> Unit

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
                start = Offset(),
                end = Offset(y = canvasHeight - rowHeight),
                strokeWidth = lineWidth,
            )

            repeat(stopCount) { i ->
                translate(top = i * rowHeight) {
                    val passed = traveledSegments >= i

                    drawCircle(
                        color = if (passed) passedColor else bgColor,
                        radius = circleRadius,
                        center = Offset(),
                        style = Fill
                    )
                    drawCircle(
                        color = if (passed) passedColor else lineColor,
                        radius = circleRadius,
                        center = Offset(),
                        style = Stroke(
                            width = circleStrokeWidth
                        )
                    )
                }
            }

            drawLine(
                color = passedColor,
                start = Offset(),
                end = Offset(y = rowHeight * animatedHeight),
                strokeWidth = lineWidth,
            )

            if (height > 0F) drawCircle(
                color = busColor,
                radius = circleRadius - circleStrokeWidth * .5F,
                center = Offset(y = rowHeight * animatedHeight)
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
fun VehicleIcon(
    lineTraction: Traction,
    vehicleTraction: Traction? = null,
    modifier: Modifier = Modifier,
    colorOverride: Color? = null,
) {
    val icon = remember(vehicleTraction, lineTraction) {
        when (vehicleTraction ?: lineTraction) {
            Traction.Electro -> Res.drawable.ebus
            Traction.Hybrid -> Res.drawable.hybrid
            Traction.PartialTrolleybus -> Res.drawable.parcial
            Traction.Gas -> Res.drawable.zemeplyn
            Traction.Diesel if vehicleTraction == null -> Res.drawable.bus
            Traction.Diesel -> Res.drawable.diesel
            Traction.Trolleybus -> Res.drawable.trolejbus
            Traction.Other -> null
        }
    }
    if (icon != null) IconWithTooltip(
        painter = painterResource(icon),
        contentDescription = when (vehicleTraction ?: lineTraction) {
            Traction.Electro -> "Elektrobus"
            Traction.Hybrid -> "Hybrid"
            Traction.PartialTrolleybus -> "Parciální trolejbus"
            Traction.Gas -> "Zeměplynový autobus"
            Traction.Diesel if vehicleTraction == null -> "Autobus"
            Traction.Diesel -> "Dieselový autobus"
            Traction.Trolleybus -> "Trolejbus"
            Traction.Other -> null
        },
        modifier.size(48.dp),
        tint = when {
            colorOverride != null -> colorOverride
            vehicleTraction != null && !vehicleTraction.isTypeOf(lineTraction) ->
                MaterialTheme.colorScheme.error

            vehicleTraction != null -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.onSurface
        },
    )
}

@Composable
fun VI(lineTraction: Traction, vehicleTraction: Traction) = Row(verticalAlignment = Alignment.CenterVertically) {
    VehicleIcon(lineTraction, null)
    Icon(Icons.AutoMirrored.Default.ArrowRightAlt, null)
    VehicleIcon(lineTraction, vehicleTraction)
}

@Preview
@Composable
fun IconsPreview() = Surface {
    Column(Modifier.width(IntrinsicSize.Min)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Icon(Icons.Default.WifiOff, null, Modifier.size(48.dp))
            Icon(Icons.AutoMirrored.Default.ArrowRightAlt, null)
            Icon(Icons.Default.Wifi, null, Modifier.size(48.dp))
        }
        VI(Traction.Electro, Traction.Electro)
        VI(Traction.Electro, Traction.Diesel)
        VI(Traction.Diesel, Traction.Diesel)
        VI(Traction.Diesel, Traction.Gas)
        VI(Traction.Diesel, Traction.Hybrid)
        VI(Traction.PartialTrolleybus, Traction.PartialTrolleybus)
        VI(Traction.PartialTrolleybus, Traction.Diesel)
        VI(Traction.PartialTrolleybus, Traction.Gas)
        VI(Traction.PartialTrolleybus, Traction.Hybrid)
        VI(Traction.Trolleybus, Traction.Trolleybus)
        VI(Traction.Trolleybus, Traction.PartialTrolleybus)
        VI(Traction.Trolleybus, Traction.Diesel)
        VI(Traction.Trolleybus, Traction.Gas)
        VI(Traction.Trolleybus, Traction.Hybrid)
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun Vehicle(vehicle: RegistrationNumber?, name: String?, showInfoButton: Boolean = true) {
    if (vehicle != null) {
        if (name != null) {
            Text(
                text = name,
                Modifier.padding(start = 8.dp),
            )
            Text(
                text = " (ev. č. $vehicle)",
                Modifier.padding(end = 8.dp),
                style = MaterialTheme.typography.bodySmall,
            )
        } else Text(
            text = "ev. č. $vehicle",
            Modifier.padding(horizontal = 8.dp),
        )
        val openWebsite = openWebsiteLauncher
        if (showInfoButton) IconWithTooltip(
            Icons.Default.Info,
            "Zobrazit informace o voze",
            Modifier.clickable {
                openWebsite("https://seznam-autobusu.cz/seznam?operatorName=DP+města+České+Budějovice&prov=1&evc=$vehicle")
            },
        )
    }
}

@Composable
fun DelayBubble(delayMin: Float) {
    Badge(
        Modifier,
        containerColor = colorOfDelayBubbleContainer(delayMin),
        contentColor = colorOfDelayBubbleText(delayMin),
    ) {
        Text(
            text = delayMin.toDouble().minutes.toDelay(),
        )
    }
}

@Suppress("unused")
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
fun Name(name: String, modifier: Modifier = Modifier, suffix: String? = null, prefix: String? = null) {
    Text(buildAnnotatedString {
        if (prefix != null) withStyle(style = MaterialTheme.typography.titleMedium.toSpanStyle()) {
            append(prefix)
        }
        withStyle(style = MaterialTheme.typography.headlineMedium.toSpanStyle()) {
            append(name)
        }
        if (suffix != null) withStyle(style = MaterialTheme.typography.titleSmall.toSpanStyle()) {
            append(suffix)
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
            useDarkTheme = LocalIsDarkThemeUsed.current,
            useDynamicColor = LocalIsDynamicThemeUsed.current,
            theme = LocalTheme.current ?: Theme.Default,
        ) {
            PlainTooltip {
                Text(text = tooltipText)
            }
        }
    },
    state = rememberTooltipState(),
    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
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

@ExperimentalMaterial3Api
@Composable
fun IconWithTooltip(
    painter: Painter,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tooltipText: String? = contentDescription,
    tint: Color = LocalContentColor.current,
) = if (tooltipText != null) TooltipBox(
    tooltip = {
        DPMCBTheme(
            useDarkTheme = LocalIsDarkThemeUsed.current,
            useDynamicColor = LocalIsDynamicThemeUsed.current,
            theme = LocalTheme.current ?: Theme.Default,
        ) {
            PlainTooltip {
                Text(text = tooltipText)
            }
        }
    },
    state = rememberTooltipState(),
    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
) {
    Icon(
        painter = painter,
        contentDescription = contentDescription,
        modifier = modifier,
        tint = tint
    )
}
else
    Icon(
        painter = painter,
        contentDescription = contentDescription,
        modifier = modifier,
        tint = tint
    )

@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
fun DateSelector(
    date: LocalDate,
    onDateChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    showDialog: MutableState<Boolean> = rememberSaveable { mutableStateOf(false) },
) = Row(
    modifier,
) {
    var show by showDialog
    val state = rememberDatePickerState(
        initialSelectedDateMillis = date.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds(),
        initialDisplayMode = DisplayMode.Picker,
    )
    LaunchedEffect(date) {
        state.selectedDateMillis = date.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
    }
    if (show) DatePickerDialog(
        onDismissRequest = {
            show = false
        },
        confirmButton = {
            TextButton(
                onClick = {
                    show = false
                    onDateChange(
                        Instant.fromEpochMilliseconds(state.selectedDateMillis!!)
                            .toLocalDateTime(TimeZone.UTC).date
                    )
                }
            ) {
                Text("OK")
            }
        },
    ) {
        DatePicker(
            state = state,
            title = {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DatePickerDefaults.DatePickerTitle(
                        displayMode = state.displayMode,
                        Modifier.padding(PaddingValues(start = 24.dp, end = 12.dp, top = 16.dp))
                    )
                    TextButton(
                        onClick = {
                            show = false
                            onDateChange(SystemClock.todayHere())
                        },
                        Modifier.padding(PaddingValues(end = 24.dp, start = 12.dp, top = 16.dp))
                    ) {
                        Text("Dnes")
                    }
                }
            },
        )
    }

    IconButton(
        onClick = {
            onDateChange(date - DatePeriod(days = 1))
        },
    ) {
        IconWithTooltip(Icons.AutoMirrored.Default.ArrowLeft, "O den dříve")
    }
    TextButton(
        onClick = {
            show = true
        },
        contentPadding = PaddingValues(vertical = 8.dp, horizontal = 2.dp),
    ) {
        IconWithTooltip(Icons.Default.CalendarMonth, "Změnit datum", Modifier.size(ButtonDefaults.IconSize))
        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
        Text("${date.dayOfWeek.cz().take(2)} ${date.day}. ${date.month.number}.")
    }
    IconButton(
        onClick = {
            onDateChange(date + DatePeriod(days = 1))
        },
    ) {
        IconWithTooltip(Icons.AutoMirrored.Default.ArrowRight, "O den později")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextField(
    state: TextFieldState,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    prefix: @Composable (() -> Unit)? = null,
    suffix: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    singleLine: Boolean = false,
    shape: Shape = TextFieldDefaults.shape,
    colors: TextFieldColors = TextFieldDefaults.colors(),
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    inputTransformation: InputTransformation? = null,
    onKeyboardAction: KeyboardActionHandler? = null,
    lineLimits: TextFieldLineLimits = TextFieldLineLimits.Default,
    onTextLayout: (Density.(getResult: () -> TextLayoutResult?) -> Unit)? = null,
    outputTransformation: OutputTransformation? = null,
    scrollState: ScrollState = rememberScrollState(),
) {
    // If color is not provided via the text style, use content color as a default
    val textColor = textStyle.color.takeOrElse {
        colors.textColor(enabled, isError, interactionSource).value
    }
    val mergedTextStyle = textStyle.merge(TextStyle(color = textColor))

    CompositionLocalProvider(LocalTextSelectionColors provides colors.selectionColors) {
        BasicTextField(
            state = state,
            modifier = modifier
                .defaultMinSize(
                    minWidth = TextFieldDefaults.MinWidth,
                    minHeight = TextFieldDefaults.MinHeight
                ),
            enabled = enabled,
            readOnly = readOnly,
            textStyle = mergedTextStyle,
            cursorBrush = SolidColor(colors.cursorColor(isError).value),
            keyboardOptions = keyboardOptions,
            interactionSource = interactionSource,
            inputTransformation = inputTransformation,
            onKeyboardAction = onKeyboardAction,
            lineLimits = lineLimits,
            onTextLayout = onTextLayout,
            outputTransformation = outputTransformation,
            scrollState = scrollState,
            decorator = { innerTextField ->
                TextFieldDefaults.DecorationBox(
                    value = state.text.toString(),
                    visualTransformation = visualTransformation,
                    innerTextField = innerTextField,
                    placeholder = placeholder,
                    label = label,
                    leadingIcon = leadingIcon,
                    trailingIcon = trailingIcon,
                    prefix = prefix,
                    suffix = suffix,
                    supportingText = supportingText,
                    shape = shape,
                    singleLine = singleLine,
                    enabled = enabled,
                    isError = isError,
                    interactionSource = interactionSource,
                    colors = colors
                )
            }
        )
    }
}

@Composable
private fun TextFieldColors.textColor(
    enabled: Boolean,
    isError: Boolean,
    interactionSource: InteractionSource,
): State<Color> {
    val focused by interactionSource.collectIsFocusedAsState()

    val targetValue = when {
        !enabled -> disabledTextColor
        isError -> errorTextColor
        focused -> focusedTextColor
        else -> unfocusedTextColor
    }
    return rememberUpdatedState(targetValue)
}

@Composable
private fun TextFieldColors.cursorColor(isError: Boolean): State<Color> {
    return rememberUpdatedState(if (isError) errorCursorColor else cursorColor)
}

private val TextFieldColors.selectionColors: TextSelectionColors
    @Composable get() = textSelectionColors

fun Modifier.autoFocus(vararg keys: Any? = arrayOf()) = composed {
    val focusRequester = remember { FocusRequester() }
    val windowInfo = LocalWindowInfo.current

    LaunchedEffect(windowInfo, *keys) {
        snapshotFlow { windowInfo.isWindowFocused }.collect { isWindowFocused ->
            if (isWindowFocused) {
                awaitFrame()
                delay(250)
                focusRequester.requestFocus()
            }
        }
    }

    focusRequester(focusRequester)
}