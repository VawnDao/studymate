package com.studymate.app.ui.text

/**
 * Chuyển công thức LaTeX đơn giản trong `$...$` sang ký tự Unicode để hiển thị bằng `Text`
 * (CO₂, x², →, √). Đủ cho Sinh, Hoá, Toán cơ bản; công thức phức tạp hơn giữ dạng đọc được.
 * Phần nằm ngoài `$...$` giữ nguyên.
 */
object LatexToUnicode {

    private val inlineMath = Regex("""\$([^$]+)\$""")
    private val fraction = Regex("""\\frac\{([^{}]*)\}\{([^{}]*)\}""")
    private val sqrt = Regex("""\\sqrt\{([^{}]*)\}""")
    private val groupedScript = Regex("""([_^])\{([^{}]*)\}""")
    private val singleScript = Regex("""([_^])([A-Za-z0-9+\-=()])""")
    private val command = Regex("""\\([A-Za-z]+)""")

    private val symbols = mapOf(
        "rightarrow" to "→", "to" to "→", "leftarrow" to "←", "leftrightarrow" to "↔", "Rightarrow" to "⇒",
        "times" to "×", "cdot" to "·", "div" to "÷", "pm" to "±", "mp" to "∓",
        "leq" to "≤", "le" to "≤", "geq" to "≥", "ge" to "≥", "neq" to "≠", "approx" to "≈", "equiv" to "≡",
        "infty" to "∞", "degree" to "°", "circ" to "°", "sum" to "∑", "int" to "∫", "partial" to "∂",
        "alpha" to "α", "beta" to "β", "gamma" to "γ", "delta" to "δ", "Delta" to "Δ", "epsilon" to "ε",
        "theta" to "θ", "lambda" to "λ", "mu" to "μ", "pi" to "π", "rho" to "ρ", "sigma" to "σ",
        "Sigma" to "Σ", "phi" to "φ", "omega" to "ω", "Omega" to "Ω",
        "left" to "", "right" to "", "quad" to " ", "qquad" to "  ", "text" to "", "mathrm" to "",
    )

    private val subscripts = mapOf(
        '0' to '₀', '1' to '₁', '2' to '₂', '3' to '₃', '4' to '₄', '5' to '₅', '6' to '₆', '7' to '₇',
        '8' to '₈', '9' to '₉', '+' to '₊', '-' to '₋', '=' to '₌', '(' to '₍', ')' to '₎',
        'a' to 'ₐ', 'e' to 'ₑ', 'o' to 'ₒ', 'x' to 'ₓ', 'h' to 'ₕ', 'k' to 'ₖ', 'l' to 'ₗ', 'm' to 'ₘ',
        'n' to 'ₙ', 'p' to 'ₚ', 's' to 'ₛ', 't' to 'ₜ', 'i' to 'ᵢ', 'j' to 'ⱼ', 'r' to 'ᵣ', 'u' to 'ᵤ', 'v' to 'ᵥ',
    )

    private val superscripts = mapOf(
        '0' to '⁰', '1' to '¹', '2' to '²', '3' to '³', '4' to '⁴', '5' to '⁵', '6' to '⁶', '7' to '⁷',
        '8' to '⁸', '9' to '⁹', '+' to '⁺', '-' to '⁻', '=' to '⁼', '(' to '⁽', ')' to '⁾', 'n' to 'ⁿ', 'i' to 'ⁱ',
    )

    fun convert(text: String): String = inlineMath.replace(text) { match -> convertMath(match.groupValues[1]) }

    fun containsMath(text: String): Boolean = inlineMath.containsMatchIn(text)

    private fun convertMath(source: String): String {
        // Chỉ số một ký tự (x^2, H_2) xử lý trước, để dạng dự phòng "^(…)" sinh ra sau không bị đổi lần nữa.
        var out = singleScript.replace(source) { script(it.groupValues[1], it.groupValues[2]) }
        // Lặp để xử lý lồng nhau đơn giản như \frac{x^{2}}{2}.
        repeat(3) {
            out = groupedScript.replace(out) { script(it.groupValues[1], it.groupValues[2]) }
            out = fraction.replace(out) { "${wrap(it.groupValues[1])}/${wrap(it.groupValues[2])}" }
            out = sqrt.replace(out) { "√${wrap(it.groupValues[1])}" }
        }
        out = command.replace(out) { symbols[it.groupValues[1]] ?: it.groupValues[1] }
        return out.replace("{", "").replace("}", "").replace(Regex(" {2,}"), " ").trim()
    }

    private fun script(marker: String, content: String): String {
        val table = if (marker == "_") subscripts else superscripts
        val mapped = content.map { table[it] }
        return if (mapped.all { it != null }) {
            mapped.joinToString("")
        } else {
            "$marker(${content})"
        }
    }

    private fun wrap(value: String): String = if (value.length > 1 && value.any { it in "+-×· " }) "($value)" else value
}
