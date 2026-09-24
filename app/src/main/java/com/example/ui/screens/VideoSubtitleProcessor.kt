package com.example.ui.screens

import java.util.Locale

// ==========================================
// SUBTITLE DATA MODELS
// ==========================================

data class SubtitleCue(
    val id: String = java.util.UUID.randomUUID().toString(),
    val index: Int = 1,
    val startTimeMs: Long = 0L,
    val endTimeMs: Long = 1000L,
    val text: String = "",
    val style: String = "Default",
    val actor: String = "",
    val layer: Int = 0,
    val marginL: Int = 0,
    val marginR: Int = 0,
    val marginV: Int = 0,
    val effect: String = "",
    val overrideTags: String = "",
    val rawText: String = "",
    val fontSize: Int = 18,
    val alignment: Int = 2,
    val fontName: String = "Arial",
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isUnderline: Boolean = false,
    val isStrikeout: Boolean = false,
    val textColorHex: String? = null,
    val outlineColorHex: String? = null,
    val backColorHex: String? = null
)

// ==========================================
// ASS SUBTITLE PARSER & EXPORTER
// ==========================================

object AssParser {
    var lastParsedHeader: String = ""
    val lastParsedStyleMap = mutableMapOf<String, AssStyleDef>()
    var scriptPlayResX: Int = 1920
    var scriptPlayResY: Int = 1080

    data class AssStyleDef(
        val name: String = "Default",
        val fontName: String = "Arial",
        val fontSize: String = "48",
        val primaryColour: String = "&H00FFFFFF",
        val secondaryColour: String = "&H000000FF",
        val outlineColour: String = "&H00000000",
        val backColour: String = "&H80000000",
        val bold: String = "0",
        val italic: String = "0",
        val underline: String = "0",
        val strikeOut: String = "0",
        val scaleX: String = "100",
        val scaleY: String = "100",
        val spacing: String = "0",
        val angle: String = "0",
        val borderStyle: String = "1",
        val outline: String = "2",
        val shadow: String = "0",
        val alignment: Int = 2,
        val marginL: String = "10",
        val marginR: String = "10",
        val marginV: String = "10",
        val encoding: String = "1"
    )

    fun parse(content: String): List<SubtitleCue> {
        val cues = mutableListOf<SubtitleCue>()
        val styleMap = mutableMapOf<String, AssStyleDef>()
        lastParsedStyleMap.clear()

        val eventsIdx = content.indexOf("[Events]", ignoreCase = true)
        if (eventsIdx != -1) {
            val headerPart = content.substring(0, eventsIdx).trim()
            if (headerPart.contains("[Script Info]", ignoreCase = true)) {
                lastParsedHeader = headerPart
            }
        }

        val lines = content.lines()

        // Parse Script Info PlayRes
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("PlayResX:", ignoreCase = true)) {
                trimmed.substringAfter(":").trim().toIntOrNull()?.let { scriptPlayResX = it }
            } else if (trimmed.startsWith("PlayResY:", ignoreCase = true)) {
                trimmed.substringAfter(":").trim().toIntOrNull()?.let { scriptPlayResY = it }
            }
        }

        // 1. Parse Style Definitions
        var inStylesSection = false
        var styleFormatFields = listOf<String>()

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("[V4+ Styles]", ignoreCase = true) || trimmed.startsWith("[V4 Styles]", ignoreCase = true)) {
                inStylesSection = true
                continue
            }
            if (trimmed.startsWith("[") && inStylesSection) {
                inStylesSection = false
            }

            if (inStylesSection) {
                if (trimmed.startsWith("Format:", ignoreCase = true)) {
                    styleFormatFields = trimmed.substringAfter("Format:").split(",").map { it.trim().lowercase() }
                } else if (trimmed.startsWith("Style:", ignoreCase = true)) {
                    val parts = trimmed.substringAfter("Style:").split(",").map { it.trim() }
                    if (parts.isNotEmpty()) {
                        val name = parts[0]

                        fun getValue(fieldName: String, posIdx: Int, defaultVal: String): String {
                            val idx = styleFormatFields.indexOf(fieldName)
                            val actualIdx = if (idx != -1) idx else posIdx
                            return if (actualIdx < parts.size && parts[actualIdx].isNotBlank()) parts[actualIdx] else defaultVal
                        }

                        val fontName = getValue("fontname", 1, "Arial")
                        val fontSize = getValue("fontsize", 2, "48")
                        val primaryColour = getValue("primarycolour", 3, "&H00FFFFFF")
                        val secondaryColour = getValue("secondarycolour", 4, "&H000000FF")
                        val outlineColour = getValue("outlinecolour", 5, "&H00000000")
                        val backColour = getValue("backcolour", 6, "&H80000000")
                        val bold = getValue("bold", 7, "-1")
                        val italic = getValue("italic", 8, "0")
                        val underline = getValue("underline", 9, "0")
                        val strikeOut = getValue("strikeout", 10, "0")
                        val scaleX = getValue("scalex", 11, "100")
                        val scaleY = getValue("scaley", 12, "100")
                        val spacing = getValue("spacing", 13, "0")
                        val angle = getValue("angle", 14, "0")
                        val borderStyle = getValue("borderstyle", 15, "1")
                        val outline = getValue("outline", 16, "2")
                        val shadow = getValue("shadow", 17, "0")
                        val alignVal = getValue("alignment", 18, "2").toIntOrNull() ?: 2
                        val marginL = getValue("marginl", 19, "10")
                        val marginR = getValue("marginr", 20, "10")
                        val marginV = getValue("marginv", 21, "10")
                        val encoding = getValue("encoding", 22, "1")

                        val styleDef = AssStyleDef(
                            name = name,
                            fontName = fontName,
                            fontSize = fontSize,
                            primaryColour = primaryColour,
                            secondaryColour = secondaryColour,
                            outlineColour = outlineColour,
                            backColour = backColour,
                            bold = bold,
                            italic = italic,
                            underline = underline,
                            strikeOut = strikeOut,
                            scaleX = scaleX,
                            scaleY = scaleY,
                            spacing = spacing,
                            angle = angle,
                            borderStyle = borderStyle,
                            outline = outline,
                            shadow = shadow,
                            alignment = alignVal,
                            marginL = marginL,
                            marginR = marginR,
                            marginV = marginV,
                            encoding = encoding
                        )
                        styleMap[name] = styleDef
                        lastParsedStyleMap[name] = styleDef
                    }
                }
            }
        }

        // 2. Parse Dialogue Events
        var index = 1
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("Dialogue:", ignoreCase = true)) {
                val parts = trimmed.substringAfter("Dialogue:").split(",", limit = 10)
                if (parts.size >= 10) {
                    val layer = parts[0].trim().toIntOrNull() ?: 0
                    val startStr = parts[1].trim()
                    val endStr = parts[2].trim()
                    val style = parts[3].trim()
                    val actor = parts[4].trim()
                    val marginL = parts[5].trim().toIntOrNull() ?: 0
                    val marginR = parts[6].trim().toIntOrNull() ?: 0
                    val marginV = parts[7].trim().toIntOrNull() ?: 0
                    val effect = parts[8].trim()
                    val textRaw = parts[9]

                    val startMs = parseAssTimestamp(startStr)
                    val endMs = parseAssTimestamp(endStr)

                    // Extract alignment from tags or style definition
                    var cueAlignment = styleMap[style]?.alignment ?: 2

                    val anMatch = Regex("\\{\\\\an([1-9])\\}").find(textRaw)
                    if (anMatch != null) {
                        cueAlignment = anMatch.groupValues[1].toIntOrNull() ?: cueAlignment
                    } else {
                        val aMatch = Regex("\\{\\\\a([0-9]+)\\}").find(textRaw)
                        if (aMatch != null) {
                            val ssaVal = aMatch.groupValues[1].toIntOrNull() ?: 2
                            cueAlignment = when (ssaVal) {
                                5 -> 7; 6 -> 8; 7 -> 9
                                9 -> 4; 10 -> 5; 11 -> 6
                                1 -> 1; 2 -> 2; 3 -> 3
                                else -> 2
                            }
                        }
                    }

                    val overrideTags = extractOverrideTags(textRaw)
                    val cleanText = textRaw.replace("\\N", "\n").replace("\\n", "\n")

                    cues.add(
                        SubtitleCue(
                            index = index++,
                            startTimeMs = startMs,
                            endTimeMs = endMs,
                            text = cleanText,
                            style = if (style.isNotEmpty()) style else "Default",
                            actor = actor,
                            layer = layer,
                            marginL = marginL,
                            marginR = marginR,
                            marginV = marginV,
                            effect = effect,
                            overrideTags = overrideTags,
                            rawText = textRaw,
                            alignment = cueAlignment
                        )
                    )
                }
            }
        }
        return validateAndVerifySubtitles(cues)
    }

    fun validateAndVerifySubtitles(cues: List<SubtitleCue>): List<SubtitleCue> {
        val verifiedCues = mutableListOf<SubtitleCue>()
        for (cue in cues) {
            var validAlignment = if (cue.alignment in 1..9) cue.alignment else 2
            var textToValidate = cue.text

            // 1. Tag curly braces integrity check & auto-balance
            val openBraces = textToValidate.count { it == '{' }
            val closeBraces = textToValidate.count { it == '}' }
            if (openBraces > closeBraces) {
                textToValidate += "}".repeat(openBraces - closeBraces)
            }

            // 2. Scan override tags for explicit alignment override (\an or \a)
            val anMatch = Regex("\\{\\\\an([1-9])\\}").find(textToValidate)
            if (anMatch != null) {
                validAlignment = anMatch.groupValues[1].toIntOrNull() ?: validAlignment
            } else {
                val aMatch = Regex("\\{\\\\a([0-9]+)\\}").find(textToValidate)
                if (aMatch != null) {
                    val ssaVal = aMatch.groupValues[1].toIntOrNull() ?: 2
                    validAlignment = when (ssaVal) {
                        5 -> 7; 6 -> 8; 7 -> 9
                        9 -> 4; 10 -> 5; 11 -> 6
                        1 -> 1; 2 -> 2; 3 -> 3
                        else -> 2
                    }
                }
            }

            // 3. Scan & verify rotation/angle tags (\frz, \frx, \fry)
            val angleMatches = Regex("\\\\fr[zxy]([-+]?[0-9]*\\.?[0-9]+)").findAll(textToValidate)
            for (match in angleMatches) {
                val rawAngle = match.groupValues[1].toDoubleOrNull()
                if (rawAngle != null && (rawAngle.isNaN() || rawAngle.isInfinite())) {
                    textToValidate = textToValidate.replace(match.value, "\\frz0")
                }
            }

            val updatedOverrideTags = extractOverrideTags(textToValidate)

            verifiedCues.add(
                cue.copy(
                    text = textToValidate,
                    alignment = validAlignment,
                    overrideTags = updatedOverrideTags
                )
            )
        }
        return verifiedCues
    }

    private fun extractOverrideTags(text: String): String {
        val tags = mutableListOf<String>()
        val regex = Regex("\\{\\\\.*?\\}")
        regex.findAll(text).forEach { match ->
            tags.add(match.value)
        }
        return tags.joinToString("")
    }

    private fun parseAssTimestamp(timestamp: String): Long {
        return try {
            val parts = timestamp.split(":")
            if (parts.size < 3) return 0L
            val hours = parts[0].toLong()
            val minutes = parts[1].toLong()
            val secParts = parts[2].split(".")
            val seconds = secParts[0].toLong()
            val cs = if (secParts.size > 1) secParts[1].padEnd(2, '0').take(2).toLong() else 0L
            (hours * 3600_000L) + (minutes * 60_000L) + (seconds * 1000L) + (cs * 10L)
        } catch (e: Exception) {
            0L
        }
    }

    fun formatAssTimestamp(timeMs: Long): String {
        val totalMs = maxOf(0L, timeMs)
        val hours = totalMs / 3600_000L
        val minutes = (totalMs % 3600_000L) / 60_000L
        val seconds = (totalMs % 60_000L) / 1000L
        val cs = (totalMs % 1000L) / 10L
        return String.format(Locale.US, "%d:%02d:%02d.%02d", hours, minutes, seconds, cs)
    }

    fun export(cues: List<SubtitleCue>): String {
        val validatedCues = validateAndVerifySubtitles(cues)
        val sb = StringBuilder()

        val styleGroups = validatedCues.groupBy { if (it.style.isNotBlank()) it.style else "Default" }
        val requiredStyles = mutableSetOf<String>()
        styleGroups.keys.forEach { requiredStyles.add(it) }
        lastParsedStyleMap.keys.forEach { requiredStyles.add(it) }
        if (requiredStyles.isEmpty()) requiredStyles.add("Default")

        if (lastParsedHeader.isNotBlank() && lastParsedHeader.contains("[Script Info]", ignoreCase = true)) {
            var headerToUse = lastParsedHeader
            if (!headerToUse.contains("ScaledBorderAndShadow:", ignoreCase = true)) {
                headerToUse = headerToUse.replace("[Script Info]", "[Script Info]\nScaledBorderAndShadow: yes", ignoreCase = true)
            }
            if (!headerToUse.contains("PlayResX:", ignoreCase = true)) {
                headerToUse = headerToUse.replace("[Script Info]", "[Script Info]\nPlayResX: $scriptPlayResX", ignoreCase = true)
            }
            if (!headerToUse.contains("PlayResY:", ignoreCase = true)) {
                headerToUse = headerToUse.replace("[Script Info]", "[Script Info]\nPlayResY: $scriptPlayResY", ignoreCase = true)
            }
            sb.appendLine(headerToUse)

            if (!headerToUse.contains("[V4+ Styles]", ignoreCase = true) && !headerToUse.contains("[V4 Styles]", ignoreCase = true)) {
                sb.appendLine()
                sb.appendLine("[V4+ Styles]")
                sb.appendLine("Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding")

                if (!requiredStyles.contains("Default")) {
                    sb.appendLine("Style: Default,Arial,48,&H00FFFFFF,&H000000FF,&H00000000,&H80000000,-1,0,0,0,100,100,0,0,1,2,0,2,10,10,10,1")
                }

                requiredStyles.forEach { name ->
                    val st = lastParsedStyleMap[name] ?: AssStyleDef(name = name)
                    sb.appendLine("Style: ${st.name},${st.fontName},${st.fontSize},${st.primaryColour},${st.secondaryColour},${st.outlineColour},${st.backColour},${st.bold},${st.italic},${st.underline},${st.strikeOut},${st.scaleX},${st.scaleY},${st.spacing},${st.angle},${st.borderStyle},${st.outline},${st.shadow},${st.alignment},${st.marginL},${st.marginR},${st.marginV},${st.encoding}")
                }
            }

            sb.appendLine()
            sb.appendLine("[Events]")
            sb.appendLine("Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text")
        } else {
            sb.appendLine("[Script Info]")
            sb.appendLine("ScriptType: v4.00+")
            sb.appendLine("PlayResX: $scriptPlayResX")
            sb.appendLine("PlayResY: $scriptPlayResY")
            sb.appendLine("ScaledBorderAndShadow: yes")
            sb.appendLine()
            sb.appendLine("[V4+ Styles]")
            sb.appendLine("Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding")

            if (!requiredStyles.contains("Default")) {
                sb.appendLine("Style: Default,Arial,48,&H00FFFFFF,&H000000FF,&H00000000,&H80000000,-1,0,0,0,100,100,0,0,1,2,0,2,10,10,10,1")
            }

            requiredStyles.forEach { name ->
                val st = lastParsedStyleMap[name] ?: AssStyleDef(name = name)
                sb.appendLine("Style: ${st.name},${st.fontName},${st.fontSize},${st.primaryColour},${st.secondaryColour},${st.outlineColour},${st.backColour},${st.bold},${st.italic},${st.underline},${st.strikeOut},${st.scaleX},${st.scaleY},${st.spacing},${st.angle},${st.borderStyle},${st.outline},${st.shadow},${st.alignment},${st.marginL},${st.marginR},${st.marginV},${st.encoding}")
            }
            sb.appendLine()
            sb.appendLine("[Events]")
            sb.appendLine("Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text")
        }

        validatedCues.forEach { cue ->
            val start = formatAssTimestamp(cue.startTimeMs)
            val end = formatAssTimestamp(cue.endTimeMs)

            var textToUse = when {
                cue.text.isNotBlank() -> cue.text
                cue.rawText.isNotBlank() -> cue.rawText
                else -> ""
            }

            textToUse = textToUse.replace("\n", "\\N").replace("\r", "")

            val styleName = if (cue.style.isNotBlank()) cue.style else "Default"
            val styleAlignment = lastParsedStyleMap[styleName]?.alignment ?: 2
            val hasAlignmentTag = textToUse.contains("{\\an") || textToUse.contains("{\\a") || textToUse.contains("{\\pos") || textToUse.contains("{\\move")
            if (!hasAlignmentTag && cue.alignment in 1..9 && cue.alignment != styleAlignment) {
                textToUse = "{\\an${cue.alignment}}$textToUse"
            }

            sb.appendLine("Dialogue: ${cue.layer},$start,$end,$styleName,${cue.actor},${cue.marginL},${cue.marginR},${cue.marginV},${cue.effect},$textToUse")
        }
        return sb.toString()
    }
}

// ==========================================
// GENERAL SUBTITLE ENGINE
// ==========================================

