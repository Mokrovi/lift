package com.example.myapplication.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp // Added for 0.dp comparison
import androidx.compose.ui.unit.sp
import com.example.myapplication.HorizontalAlignmentMode
import com.example.myapplication.WidgetData

// Helper function to get FontFamily from string (copied from TextEditDialog.kt)
@Composable
internal fun getPlatformFontFamily(fontFamilyName: String?): FontFamily {
    return when (fontFamilyName) {
        "Serif" -> FontFamily.Serif
        "SansSerif" -> FontFamily.SansSerif
        "Monospace" -> FontFamily.Monospace
        else -> FontFamily.Default
    }
}

@Composable
internal fun InternalVerticalStretchedText(
    modifier: Modifier = Modifier,
    text: String,
    textSize: TextUnit, // Changed to TextUnit
    color: Color,
    fontFamilyName: String?,
    fontWeightInt: Int?,
    fontStyle: FontStyle,
    lineHeightScale: Float?
) {
    val currentFontFamily = getPlatformFontFamily(fontFamilyName)
    val currentFontWeight = fontWeightInt?.let { FontWeight(it) }
    val effectiveLineHeight = textSize * (lineHeightScale ?: 1.0f)

    Column(modifier = modifier) {
        if (text.isNotEmpty()) {
            text.forEachIndexed { index, char ->
                Text(
                    text = char.toString(),
                    modifier = Modifier.fillMaxWidth(),
                    style = TextStyle(
                        color = color,
                        fontSize = textSize,
                        fontFamily = currentFontFamily,
                        fontWeight = currentFontWeight,
                        fontStyle = fontStyle,
                        textAlign = TextAlign.Center
                    ),
                    lineHeight = effectiveLineHeight,
                    overflow = TextOverflow.Visible,
                    softWrap = false
                )
                if (index < text.length - 1 && (lineHeightScale ?: 1.0f) > 1.0f) {
                    val rawSpacerValue = (((lineHeightScale ?: 1.0f) - 1.0f).coerceAtLeast(0f) * textSize.value * 0.5f)
                    val spacerHeightInSp = rawSpacerValue.sp
                    val spacerHeightInDp = with(LocalDensity.current) { spacerHeightInSp.toDp() }
                    if (spacerHeightInDp > 0.dp) { // Ensure positive spacer - CORRECTED LINE
                        Spacer(modifier = Modifier.height(spacerHeightInDp))
                    }
                }
            }
        }
    }
}

@Composable
fun ReusableTextDisplayView(
    text: String,
    styleData: WidgetData,
    modifier: Modifier = Modifier,
    defaultFontSizeIfNotSet: TextUnit = 16.sp
) {
    val actualTextSize = styleData.textSize?.sp ?: defaultFontSizeIfNotSet
    val textColor = styleData.textColor?.let { Color(it) } ?: MaterialTheme.colorScheme.onSurface
    val horizontalAlignment = when (styleData.horizontalAlignment) {
        HorizontalAlignmentMode.LEFT -> Alignment.CenterStart
        HorizontalAlignmentMode.CENTER -> Alignment.Center
        HorizontalAlignmentMode.RIGHT -> Alignment.CenterEnd
    }
    val textAlign = when (styleData.horizontalAlignment) {
        HorizontalAlignmentMode.LEFT -> TextAlign.Start
        HorizontalAlignmentMode.CENTER -> TextAlign.Center
        HorizontalAlignmentMode.RIGHT -> TextAlign.End
    }

    Box(
        modifier = modifier,
        contentAlignment = if (styleData.isVertical) Alignment.Center else horizontalAlignment
    ) {
        if (styleData.isVertical) {
            InternalVerticalStretchedText(
                modifier = Modifier.fillMaxSize(), // Typically fills the Box
                text = text,
                textSize = actualTextSize,
                color = textColor,
                fontFamilyName = styleData.fontFamily,
                lineHeightScale = styleData.lineHeightScale,
                fontWeightInt = styleData.fontWeight,
                fontStyle = FontStyle.Normal // Assuming normal, add to WidgetData if an italic option is ever needed
            )
        } else {
            Text(
                text = text,
                style = TextStyle(
                    color = textColor,
                    fontSize = actualTextSize,
                    fontFamily = getPlatformFontFamily(styleData.fontFamily),
                    fontWeight = styleData.fontWeight?.let { FontWeight(it) },
                    textAlign = textAlign,
                    lineHeight = actualTextSize * (styleData.lineHeightScale ?: 1.0f),
                    letterSpacing = (styleData.letterSpacingSp ?: 0.0f).sp
                )
            )
        }
    }
}
