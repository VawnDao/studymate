package com.studymate.app.ui.components

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import com.studymate.app.ui.text.LatexToUnicode

/**
 * Văn bản có thể chứa công thức `$...$`. MVP chuyển LaTeX đơn giản sang Unicode
 * (xem [LatexToUnicode]); phương án WebView + KaTeX để dành cho công thức phức tạp.
 */
@Composable
fun MathText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
) {
    val rendered = remember(text) { LatexToUnicode.convert(text) }
    Text(text = rendered, modifier = modifier, style = style, color = color, textAlign = textAlign, maxLines = maxLines)
}
