package com.example.player

import android.content.Context
import android.util.Base64
import android.util.Log
import java.io.File
import java.io.FileOutputStream

object SubtitleFontManager {

    private const val TAG = "SubtitleFontManager"
    private const val FONTS_DIR_NAME = "sub_fonts"

    // Bundled multi-script fallback font (assets/fonts/GoNotoCurrent-Regular.ttf).
    // Covers Devanagari + most other Unicode scripts, so subtitles never show
    // "tofu" (missing glyph boxes) when a video/subtitle doesn't carry its own font.
    private const val BUNDLED_FALLBACK_FONT_FILE = "GoNotoCurrent-Regular.ttf"
    const val BUNDLED_FALLBACK_FONT_FAMILY = "Go Noto Current-Regular"

    // Set of known font names embedded in or attached to the currently playing video (MKV attachments, etc.)
    private val videoEmbeddedFontNames = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

    fun registerVideoEmbeddedFont(fontName: String) {
        if (fontName.isNotBlank()) {
            val clean = fontName.trim()
            videoEmbeddedFontNames.add(clean.lowercase(java.util.Locale.ROOT))
            val withoutExt = clean.substringBeforeLast('.').trim()
            if (withoutExt.isNotBlank()) {
                videoEmbeddedFontNames.add(withoutExt.lowercase(java.util.Locale.ROOT))
            }
            val normalized = normalizeFontName(clean)
            if (normalized.isNotBlank()) {
                videoEmbeddedFontNames.add(normalized)
            }
            val normWithoutExt = normalizeFontName(withoutExt)
            if (normWithoutExt.isNotBlank()) {
                videoEmbeddedFontNames.add(normWithoutExt)
            }
        }
    }

    fun clearVideoEmbeddedFonts() {
        videoEmbeddedFontNames.clear()
    }

    private fun normalizeFontName(name: String): String {
        return name.lowercase(java.util.Locale.ROOT).replace(Regex("[\\s_\\-]+"), "")
    }

    fun getSubFontsDir(context: Context): File {
        val dir = File(context.cacheDir, FONTS_DIR_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    private var assetsFontsSynced = false
    private var legacyGeneratedFontsCleaned = false
    private val fontScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO + kotlinx.coroutines.SupervisorJob())
    private val processedVideoFontPaths = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

    /**
     * Synchronously prepares base fallback fonts and launches comprehensive background synchronization.
     */
    fun initializeFontEnvironment(context: Context): File {
        val fontsDir = getSubFontsDir(context)
        try {
            ensureDefaultFontAliases(fontsDir)
        } catch (_: Throwable) {}
        syncFonts(context)
        return fontsDir
    }

    /**
     * Synchronizes and populates the MPV/libass fonts directory from:
     * 1. System fonts (/system/fonts, etc.) & creates fallback aliases (default.ttf, Arial.ttf, sans-serif.ttf)
     * 2. App Assets (assets/fonts and assets/)
     * 3. Video file folder & subfolders (fonts/, Fonts/)
     * 4. Embedded ASS script [Fonts] attachments
     * 5. Custom font files picked by user
     */
    fun syncFonts(
        context: Context,
        videoPath: String? = null,
        assContent: String? = null,
        customFontFiles: List<File> = emptyList()
    ): File {
        val fontsDir = getSubFontsDir(context)

        // 1. Asset Fallback Fonts
        try {
            if (!assetsFontsSynced) {
                copyAssetFonts(context, fontsDir)
                assetsFontsSynced = true
            }
            ensureDefaultFontAliases(fontsDir)
        } catch (t: Throwable) {
            Log.e(TAG, "Error in synchronous font setup", t)
        }

        // 2. Synchronously extract & sync fonts for immediate ASS/SSA rendering
        try {
            if (!legacyGeneratedFontsCleaned) {
                cleanupLegacyGeneratedFonts(fontsDir)
                legacyGeneratedFontsCleaned = true
            }

            // Extract Embedded ASS Script Fonts if ASS script contains [Fonts] section
            if (!assContent.isNullOrBlank() && assContent.contains("[Fonts]", ignoreCase = true)) {
                extractEmbeddedAssFonts(assContent, fontsDir)
            }

            if (!videoPath.isNullOrBlank()) {
                val shouldProcessPath = processedVideoFontPaths.add(videoPath)
                if (shouldProcessPath) {
                    // Copy Video / Subtitle Folder Fonts if any
                    copyVideoFolderFonts(videoPath, fontsDir)

                    // Extract Embedded MKV Video Fonts & attachments if playing MKV container
                    extractEmbeddedMkvFonts(videoPath, fontsDir)
                }
            }

            // Copy Custom Picked Font Files
            for (file in customFontFiles) {
                if (file.exists() && file.length() > 0) {
                    try {
                        file.copyTo(File(fontsDir, file.name), overwrite = true)
                        registerVideoEmbeddedFont(file.name)
                        registerVideoEmbeddedFont(file.nameWithoutExtension)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to copy custom font: ${file.name}", e)
                    }
                }
            }

            // Register all font files present in fontsDir so style detection knows they are available
            fontsDir.listFiles()?.forEach { fontFile ->
                if (fontFile.isFile) {
                    registerVideoEmbeddedFont(fontFile.name)
                    registerVideoEmbeddedFont(fontFile.nameWithoutExtension)
                }
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Error in synchronous syncFonts", t)
        }

        return fontsDir
    }

    /**
     * Ensures fallback aliases (default.ttf, sans-serif.ttf, Arial.ttf, Roboto.ttf) exist in sub_fonts.
     * This prevents libass from dropping text glyphs when an unknown font is requested.
     */
    private fun ensureDefaultFontAliases(destDir: File) {
        // No duplicate alias files: the single GoNoto fallback is enough.
    }

    /**
     * Sanitizes ASS subtitle content so that any style with a missing custom font
     * falls back to the default system font ("sans-serif" / "Arial"), while PRESERVING
     * 100% of the style's colors, sizes, borders, shadows, alignments, karaoke tags, and animations!
     */
    fun sanitizeAssForFallback(context: Context, assContent: String): String {
        if (assContent.isBlank()) return assContent
        if (!assContent.contains("[V4+ Styles]", ignoreCase = true) && !assContent.contains("[V4 Styles]", ignoreCase = true)) {
            return assContent
        }

        try {
            val lines = assContent.lines()
            val newLines = mutableListOf<String>()
            var inStylesSection = false
            var styleFormatFields = listOf<String>()

            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.startsWith("[V4+ Styles]", ignoreCase = true) || trimmed.startsWith("[V4 Styles]", ignoreCase = true)) {
                    inStylesSection = true
                    newLines.add(line)
                    continue
                }
                if (trimmed.startsWith("[") && inStylesSection) {
                    inStylesSection = false
                }

                if (inStylesSection) {
                    if (trimmed.startsWith("Format:", ignoreCase = true)) {
                        styleFormatFields = trimmed.substringAfter("Format:").split(",").map { it.trim().lowercase() }
                        newLines.add(line)
                    } else if (trimmed.startsWith("Style:", ignoreCase = true)) {
                        val parts = trimmed.substringAfter("Style:").split(",").map { it.trim() }.toMutableList()
                        if (parts.isNotEmpty()) {
                            val fontIdx = if (styleFormatFields.isNotEmpty()) styleFormatFields.indexOf("fontname") else 1
                            val actualFontIdx = if (fontIdx != -1) fontIdx else 1

                            if (actualFontIdx < parts.size) {
                                val fontName = parts[actualFontIdx]
                                if (!isFontAvailable(context, fontName)) {
                                    // Replace missing font with our bundled multi-script fallback
                                    // (covers Devanagari + other scripts) while keeping all colors,
                                    // borders, shadow, etc. untouched.
                                    parts[actualFontIdx] = BUNDLED_FALLBACK_FONT_FAMILY
                                }
                            }
                            newLines.add("Style: " + parts.joinToString(","))
                        } else {
                            newLines.add(line)
                        }
                    } else {
                        newLines.add(line)
                    }
                } else {
                    newLines.add(line)
                }
            }
            return newLines.joinToString("\n")
        } catch (e: Exception) {
            Log.w(TAG, "Error sanitizing ASS styles for fallback", e)
            return assContent
        }
    }

    /**
     * Imports a custom user-uploaded font file (e.g. .ttf, .otf, .ttc) into the MPV fonts directory.
     */
    fun importFontFromUri(context: Context, uri: android.net.Uri): String? {
        return try {
            val contentResolver = context.contentResolver
            var fileName = "custom_font_${System.currentTimeMillis()}.ttf"
            try {
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIdx != -1 && cursor.moveToFirst()) {
                        val name = cursor.getString(nameIdx)
                        if (!name.isNullOrBlank()) fileName = name
                    }
                }
            } catch (_: Throwable) {}

            val destDir = getSubFontsDir(context)
            val destFile = File(destDir, fileName)
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            if (destFile.exists() && destFile.length() > 0) {
                Log.d(TAG, "Successfully imported font: $fileName (${destFile.length()} bytes)")
                fileName
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to import font from URI: $uri", e)
            null
        }
    }

    /**
     * Returns only user-uploaded fonts. The bundled fallback and generated system aliases
     * are intentionally not shown as removable custom fonts.
     */
    fun importFontsFromTree(context: Context, treeUri: android.net.Uri): Int {
        var imported = 0
        try {
            val resolver = context.contentResolver
            val childrenUri = android.provider.DocumentsContract.buildChildDocumentsUriUsingTree(
                treeUri,
                android.provider.DocumentsContract.getTreeDocumentId(treeUri)
            )
            val projection = arrayOf(
                android.provider.DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                android.provider.DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                android.provider.DocumentsContract.Document.COLUMN_MIME_TYPE
            )
            resolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                val idIndex = cursor.getColumnIndex(android.provider.DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameIndex = cursor.getColumnIndex(android.provider.DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeIndex = cursor.getColumnIndex(android.provider.DocumentsContract.Document.COLUMN_MIME_TYPE)
                val fontsDir = getSubFontsDir(context)
                while (cursor.moveToNext()) {
                    val id = if (idIndex >= 0) cursor.getString(idIndex) else null
                    val name = if (nameIndex >= 0) cursor.getString(nameIndex) else null
                    val mime = if (mimeIndex >= 0) cursor.getString(mimeIndex) else null
                    if (id.isNullOrBlank() || name.isNullOrBlank()) continue
                    val ext = name.substringAfterLast('.', "").lowercase()
                    if (ext !in setOf("ttf", "otf", "ttc", "woff", "woff2")) continue
                    val docUri = android.provider.DocumentsContract.buildDocumentUriUsingTree(treeUri, id)
                    resolver.openInputStream(docUri)?.use { input ->
                        File(fontsDir, name).outputStream().use { output -> input.copyTo(output) }
                    }
                    if (File(fontsDir, name).length() > 0L) imported++
                }
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to import fonts from selected directory", t)
        }
        return imported
    }

    /**
     * Returns the font's real internal family name (TTF/OTF "name" table, nameID 1,
     * falling back to nameID 4 "Full Font Name" then nameID 6 "PostScript Name").
     *
     * This is REQUIRED for mpv/libass: libass/fontconfig index fonts placed in
     * sub-fonts-dir by the name declared *inside* the font file, not by filename.
     * Many font files (especially ones users manually download/upload) have a
     * filename that doesn't match the font's declared family (e.g.
     * "NotoSansDevanagari-Regular.ttf" is internally "Noto Sans Devanagari").
     * Passing the filename to sub-font / FontName= in that case silently fails
     * to match anything, and libass falls back to the default font — this is
     * the root cause of "my selected font doesn't apply", and it's worse for
     * ASS/SSA where our forced FontName= override then replaces a previously
     * *working* embedded font reference with a name that matches nothing.
     */
    fun getPreferredFamilyName(bytes: ByteArray): String? {
        try {
            if (bytes.size < 12) return null
            val buffer = java.nio.ByteBuffer.wrap(bytes).order(java.nio.ByteOrder.BIG_ENDIAN)
            val numTables = buffer.getShort(4).toInt() and 0xFFFF
            if (numTables <= 0 || numTables > 200) return null

            var nameTableOffset = -1
            var nameTableLength = -1
            for (i in 0 until numTables) {
                val recordPos = 12 + i * 16
                if (recordPos + 16 > bytes.size) break
                val tag = buffer.getInt(recordPos)
                if (tag == 0x6E616D65) { // 'name'
                    nameTableOffset = buffer.getInt(recordPos + 8)
                    nameTableLength = buffer.getInt(recordPos + 12)
                    break
                }
            }
            if (nameTableOffset !in 0 until bytes.size || nameTableLength <= 6 ||
                nameTableOffset + nameTableLength > bytes.size
            ) return null

            val count = buffer.getShort(nameTableOffset + 2).toInt() and 0xFFFF
            val stringOffset = buffer.getShort(nameTableOffset + 4).toInt() and 0xFFFF
            val storageOffset = nameTableOffset + stringOffset

            // Collect candidates per nameID, preferring Windows/Unicode platform
            // records (most reliably encoded) over Macintosh ones.
            val byNameId = mutableMapOf<Int, String>()
            for (i in 0 until count) {
                val nrPos = nameTableOffset + 6 + i * 12
                if (nrPos + 12 > bytes.size) break
                val platformId = buffer.getShort(nrPos).toInt() and 0xFFFF
                val encodingId = buffer.getShort(nrPos + 2).toInt() and 0xFFFF
                val nameId = buffer.getShort(nrPos + 6).toInt() and 0xFFFF
                val length = buffer.getShort(nrPos + 8).toInt() and 0xFFFF
                val strOffset = buffer.getShort(nrPos + 10).toInt() and 0xFFFF
                if (nameId != 1 && nameId != 4 && nameId != 6) continue
                val actualPos = storageOffset + strOffset
                if (actualPos + length > bytes.size || length <= 0) continue
                val str = if (platformId == 0 || platformId == 3 || (platformId == 2 && encodingId == 1)) {
                    String(bytes, actualPos, length, java.nio.charset.StandardCharsets.UTF_16BE)
                } else {
                    String(bytes, actualPos, length, java.nio.charset.StandardCharsets.UTF_8)
                }.trim()
                if (str.isBlank()) continue
                // Prefer Windows(3)/Unicode(0) platform records; don't overwrite
                // an already-found preferred-platform name with a Mac one.
                val isPreferredPlatform = platformId == 0 || platformId == 3
                val existing = byNameId[nameId]
                if (existing == null || isPreferredPlatform) {
                    byNameId[nameId] = str
                }
            }
            return byNameId[1] ?: byNameId[4] ?: byNameId[6]
        } catch (_: Throwable) {
            return null
        }
    }

    fun getFontFamilyName(file: File): String {
        try {
            if (file.exists() && file.isFile && file.length() in 100..(30 * 1024 * 1024)) {
                getPreferredFamilyName(file.readBytes())?.let { return it }
            }
        } catch (_: Throwable) {}
        return file.nameWithoutExtension
    }

    fun getInstalledFonts(context: Context): List<File> {
        val destDir = getSubFontsDir(context)
        val fontExtensions = setOf("ttf", "otf", "ttc", "woff", "woff2")
        val bundled = BUNDLED_FALLBACK_FONT_FILE.lowercase()
        val generatedAliases = setOf(
            "default.ttf", "sans-serif.ttf", "sans.ttf", "arial.ttf",
            "arial-regular.ttf", "roboto.ttf", "roboto-regular.ttf"
        )
        val knownGeneratedPrefixes = listOf(
            "adobearabic", "arial", "bebas", "chamberi", "comic", "commutersans", "coca-cola",
            "comingsoon", "dancing", "carrois", "androidclock", "beihai", "c800", "cutivemono"
        )
        return destDir.listFiles()?.filter { file ->
            val lower = file.name.lowercase()
            file.isFile && file.extension.lowercase() in fontExtensions &&
                lower != bundled && lower !in generatedAliases &&
                knownGeneratedPrefixes.none { lower.startsWith(it) }
        }?.sortedBy { it.name.lowercase() } ?: emptyList()
    }

    /**
     * Deletes a custom font from the fonts directory.
     */
    fun deleteCustomFont(context: Context, fileName: String): Boolean {
        val destDir = getSubFontsDir(context)
        val file = File(destDir, fileName)
        return if (file.exists()) {
            file.delete()
        } else false
    }

    /**
     * Deletes all custom imported fonts from the fonts directory.
     */
    fun deleteAllCustomFonts(context: Context): Int {
        val fonts = getInstalledFonts(context)
        var deletedCount = 0
        for (font in fonts) {
            try {
                if (font.exists() && font.delete()) {
                    deletedCount++
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to delete font: ${font.name}", e)
            }
        }
        return deletedCount
    }

    private fun copyAssetFonts(context: Context, destDir: File) {
        val assetManager = context.assets
        val fontExtensions = setOf("ttf", "otf", "ttc", "woff", "woff2")

        fun scanAssetPath(path: String) {
            try {
                val list = assetManager.list(path) ?: return
                for (item in list) {
                    val subPath = if (path.isEmpty()) item else "$path/$item"
                    val ext = item.substringAfterLast('.', "").lowercase()
                    if (ext in fontExtensions) {
                        val destFile = File(destDir, item)
                        if (!destFile.exists() || destFile.length() == 0L) {
                            assetManager.open(subPath).use { input ->
                                FileOutputStream(destFile).use { output ->
                                    input.copyTo(output)
                                }
                            }
                        }
                    } else if (!item.contains('.')) {
                        scanAssetPath(subPath)
                    }
                }
            } catch (_: Exception) {}
        }

        scanAssetPath("fonts")
        scanAssetPath("")
    }

    private fun copyVideoFolderFonts(videoPath: String, destDir: File) {
        try {
            val videoFile = File(videoPath)
            val parentDir = videoFile.parentFile ?: return
            if (!parentDir.exists() || !parentDir.isDirectory) return

            val fontExtensions = setOf("ttf", "otf", "ttc", "woff", "woff2")
            val candidateDirs = mutableListOf(parentDir)

            val subFolders = listOf("fonts", "Fonts", "font", "Font", "FONTS")
            for (sub in subFolders) {
                val f = File(parentDir, sub)
                if (f.exists() && f.isDirectory) {
                    candidateDirs.add(f)
                }
            }

            for (dir in candidateDirs) {
                val files = dir.listFiles() ?: continue
                for (file in files) {
                    if (file.isFile) {
                        val ext = file.extension.lowercase()
                        if (ext in fontExtensions) {
                            val dest = File(destDir, file.name)
                            if (!dest.exists() || dest.length() != file.length()) {
                                file.copyTo(dest, overwrite = true)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error copying video folder fonts", e)
        }
    }

    /**
     * Extracts [Fonts] embedded attachments from ASS script and decodes them into destDir.
     */
    fun extractEmbeddedAssFonts(assContent: String, destDir: File) {
        if (!assContent.contains("[Fonts]", ignoreCase = true)) return

        val lines = assContent.lines()
        var inFontsSection = false
        var currentFontName: String? = null
        val encodedDataBuilder = StringBuilder()

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("[Fonts]", ignoreCase = true)) {
                inFontsSection = true
                continue
            }

            if (inFontsSection && trimmed.startsWith("[")) {
                if (currentFontName != null && encodedDataBuilder.isNotEmpty()) {
                    saveEncodedFontFile(currentFontName, encodedDataBuilder.toString(), destDir)
                }
                break
            }

            if (inFontsSection) {
                if (trimmed.startsWith("fontname:", ignoreCase = true)) {
                    if (currentFontName != null && encodedDataBuilder.isNotEmpty()) {
                        saveEncodedFontFile(currentFontName, encodedDataBuilder.toString(), destDir)
                        encodedDataBuilder.clear()
                    }
                    currentFontName = trimmed.substringAfter("fontname:").trim()
                } else if (currentFontName != null && trimmed.isNotEmpty()) {
                    encodedDataBuilder.append(trimmed)
                }
            }
        }

        if (inFontsSection && currentFontName != null && encodedDataBuilder.isNotEmpty()) {
            saveEncodedFontFile(currentFontName, encodedDataBuilder.toString(), destDir)
        }
    }

    private fun saveEncodedFontFile(fontName: String, encodedData: String, destDir: File) {
        try {
            val fontFile = File(destDir, fontName)
            if (fontFile.exists() && fontFile.length() > 0) return

            val decodedBytes = decodeAssFontData(encodedData)
            if (decodedBytes != null && decodedBytes.isNotEmpty()) {
                fontFile.writeBytes(decodedBytes)
                Log.d(TAG, "Extracted embedded ASS font: $fontName (${decodedBytes.size} bytes)")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to save embedded font: $fontName", e)
        }
    }

    /**
     * Decodes ASS UUencoded font attachments.
     */
    private fun decodeAssFontData(data: String): ByteArray? {
        val cleanData = data.replace("\r", "").replace("\n", "").replace(" ", "")
        if (cleanData.isEmpty()) return null

        return try {
            val bytes = ArrayList<Byte>(cleanData.length * 3 / 4)
            var i = 0
            val len = cleanData.length
            while (i < len) {
                val chunkLen = minOf(4, len - i)
                val c1 = (cleanData[i].code - 33) and 0x3F
                val c2 = if (chunkLen > 1) (cleanData[i + 1].code - 33) and 0x3F else 0
                val c3 = if (chunkLen > 2) (cleanData[i + 2].code - 33) and 0x3F else 0
                val c4 = if (chunkLen > 3) (cleanData[i + 3].code - 33) and 0x3F else 0

                val val24 = (c1 shl 18) or (c2 shl 12) or (c3 shl 6) or c4

                bytes.add(((val24 shr 16) and 0xFF).toByte())
                if (chunkLen > 2) bytes.add(((val24 shr 8) and 0xFF).toByte())
                if (chunkLen > 3) bytes.add((val24 and 0xFF).toByte())

                i += 4
            }
            bytes.toByteArray()
        } catch (e: Exception) {
            try {
                Base64.decode(cleanData, Base64.DEFAULT)
            } catch (_: Exception) {
                null
            }
        }
    }



    private fun cleanupLegacyGeneratedFonts(destDir: File) {
        try {
            val generatedAliases = setOf(
                "default.ttf", "sans-serif.ttf", "sans.ttf", "arial.ttf",
                "arial-regular.ttf", "roboto.ttf", "roboto-regular.ttf"
            )
            val systemFontNames = mutableSetOf<String>()
            val systemDirs = listOf(
                File("/system/fonts"),
                File("/system/font"),
                File("/product/fonts"),
                File("/apex/com.android.runtime/fonts")
            )
            systemDirs.forEach { dir ->
                try {
                    dir.listFiles()?.forEach { file ->
                        if (file.isFile) systemFontNames.add(file.name.lowercase(java.util.Locale.ROOT))
                    }
                } catch (_: Throwable) {}
            }

            destDir.listFiles()?.forEach { file ->
                if (!file.isFile) return@forEach
                val lower = file.name.lowercase(java.util.Locale.ROOT)
                if (lower in generatedAliases || lower in systemFontNames) {
                    try { file.delete() } catch (_: Throwable) {}
                }
            }
        } catch (_: Throwable) {}
    }

    /**
     * Checks if a specified font name is available in video-embedded/sub_fonts/system locations.
     * Follows strict matching priority:
     * 1. Video's own embedded/attached fonts (MKV attachments, track-list attachments)
     * 2. sub_fonts cache directory
     * 3. System font directories (/system/fonts, /product/fonts, /apex/...)
     * 4. Returns false if not found in 1-3 (triggering fallback to bundled Go Noto Current-Regular in sanitizeAssForFallback).
     */
    fun isFontAvailable(context: Context, fontName: String): Boolean {
        if (fontName.isBlank() || fontName.equals(BUNDLED_FALLBACK_FONT_FAMILY, ignoreCase = true)) {
            return true
        }

        val cleanName = fontName.trim()
        val lowerName = cleanName.lowercase(java.util.Locale.ROOT)
        val normalizedTarget = normalizeFontName(cleanName)

        // 1. PRIORITY 1: Check Video's own embedded / attached fonts
        for (embedded in videoEmbeddedFontNames) {
            if (embedded.equals(lowerName, ignoreCase = true) ||
                embedded.equals(normalizedTarget, ignoreCase = true) ||
                normalizeFontName(embedded) == normalizedTarget
            ) {
                return true
            }
        }

        // 2. PRIORITY 2: Check sub_fonts cache directory
        val fontsDir = getSubFontsDir(context)
        val files = fontsDir.listFiles() ?: emptyArray()
        for (file in files) {
            val nameWithoutExt = file.nameWithoutExtension
            if (nameWithoutExt.equals(cleanName, ignoreCase = true) ||
                file.name.equals(cleanName, ignoreCase = true) ||
                normalizeFontName(nameWithoutExt) == normalizedTarget
            ) {
                return true
            }
            // Also check font family names extracted from TTF/OTF name table
            val parsedNames = extractFontNamesFromTtfFile(file)
            for (parsed in parsedNames) {
                if (parsed.equals(cleanName, ignoreCase = true) || normalizeFontName(parsed) == normalizedTarget) {
                    return true
                }
            }
        }

        // 3. PRIORITY 3: Check system font directories directly
        val sysDirs = listOf(
            File("/system/fonts"),
            File("/system/font"),
            File("/product/fonts"),
            File("/apex/com.android.runtime/fonts")
        )
        for (sysDir in sysDirs) {
            if (sysDir.exists() && sysDir.isDirectory) {
                val sysFiles = sysDir.listFiles() ?: continue
                for (file in sysFiles) {
                    val nameWithoutExt = file.nameWithoutExtension
                    if (nameWithoutExt.equals(cleanName, ignoreCase = true) ||
                        file.name.equals(cleanName, ignoreCase = true) ||
                        normalizeFontName(nameWithoutExt) == normalizedTarget
                    ) {
                        return true
                    }
                }
            }
        }

        // 4. PRIORITY 4: Not available in video embedded, sub_fonts, or system fonts -> return false
        return false
    }

    /**
     * Extracts embedded font attachments (.ttf, .otf, .ttc, etc.) from an MKV video container into destDir
     * and registers all extracted font family names into videoEmbeddedFontNames.
     */
    fun extractEmbeddedMkvFonts(videoPath: String, destDir: File) {
        try {
            val videoFile = File(videoPath)
            if (!videoFile.exists() || !videoFile.canRead() || videoFile.length() < 100) return

            java.io.RandomAccessFile(videoFile, "r").use { raf ->
                val magic = ByteArray(4)
                raf.readFully(magic)
                // EBML Header 0x1A 0x45 0xDF 0xA3
                if (magic[0] != 0x1A.toByte() || magic[1] != 0x45.toByte() || magic[2] != 0xDF.toByte() || magic[3] != 0xA3.toByte()) {
                    return
                }

                val fileLen = raf.length()
                val scanLimit = minOf(fileLen, 4L * 1024L * 1024L) // Matroska headers and font attachments reside in initial header segment
                val bufferSize = 64 * 1024
                val buffer = ByteArray(bufferSize)
                var filePos = 0L

                while (filePos < scanLimit) {
                    raf.seek(filePos)
                    val bytesRead = raf.read(buffer)
                    if (bytesRead <= 8) break

                    var i = 0
                    while (i < bytesRead - 4) {
                        // AttachedFile element ID in Matroska: 0x61 0xA7
                        if (buffer[i] == 0x61.toByte() && buffer[i + 1] == 0xA7.toByte()) {
                            val elementAbsPos = filePos + i
                            try {
                                parseAndSaveMkvAttachedFile(raf, elementAbsPos, destDir)
                            } catch (_: Throwable) {}
                            i += 2
                        } else {
                            i++
                        }
                    }
                    filePos += (bytesRead - 8)
                }
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Error extracting MKV embedded fonts from $videoPath", t)
        }
    }

    private fun parseAndSaveMkvAttachedFile(raf: java.io.RandomAccessFile, elementPos: Long, destDir: File) {
        raf.seek(elementPos + 2) // Skip 0x61 0xA7
        val size = readEbmlVint(raf)
        if (size <= 0 || size > 50 * 1024 * 1024) return // Guard against corrupt sizes (max 50MB per font)

        val endPos = raf.filePointer + size
        var fileName: String? = null
        var fileMimeType: String? = null
        var fontData: ByteArray? = null

        while (raf.filePointer < endPos) {
            val childId = readEbmlId(raf) ?: break
            val childSize = readEbmlVint(raf)
            if (childSize < 0 || raf.filePointer + childSize > endPos + 16) break

            when (childId) {
                0x466E -> { // FileName (UTF-8 string)
                    val nameBytes = ByteArray(childSize.toInt())
                    raf.readFully(nameBytes)
                    fileName = String(nameBytes, java.nio.charset.StandardCharsets.UTF_8).trim()
                }
                0x4660 -> { // FileMimeType (ASCII string)
                    val mimeBytes = ByteArray(childSize.toInt())
                    raf.readFully(mimeBytes)
                    fileMimeType = String(mimeBytes, java.nio.charset.StandardCharsets.US_ASCII).trim()
                }
                0x465C -> { // FileData (Binary font data)
                    if (childSize in 100..(30 * 1024 * 1024)) {
                        fontData = ByteArray(childSize.toInt())
                        raf.readFully(fontData)
                    } else {
                        raf.skipBytes(childSize.toInt())
                    }
                }
                else -> {
                    raf.skipBytes(childSize.toInt())
                }
            }
        }

        if (fontData != null && (!fileName.isNullOrBlank() || !fileMimeType.isNullOrBlank())) {
            val validFontExts = listOf(".ttf", ".otf", ".ttc", ".woff", ".woff2")
            val isFont = (fileName != null && validFontExts.any { fileName.endsWith(it, ignoreCase = true) }) ||
                    (fileMimeType != null && (fileMimeType.contains("font", ignoreCase = true) ||
                            fileMimeType.contains("truetype", ignoreCase = true) ||
                            fileMimeType.contains("opentype", ignoreCase = true)))

            if (isFont) {
                val targetFileName = fileName?.takeIf { it.isNotBlank() }
                    ?: "embedded_font_${System.currentTimeMillis() % 10000}.ttf"
                val outFile = File(destDir, targetFileName)
                if (!outFile.exists() || outFile.length() != fontData.size.toLong()) {
                    try {
                        FileOutputStream(outFile).use { it.write(fontData) }
                    } catch (_: Throwable) {}
                }

                // Register file name and without extension
                registerVideoEmbeddedFont(targetFileName)
                val withoutExt = targetFileName.substringBeforeLast('.')
                registerVideoEmbeddedFont(withoutExt)

                // Extract TTF/OTF internal font family & full names from binary bytes
                val familyNames = extractFontNamesFromTtfBytes(fontData)
                for (name in familyNames) {
                    registerVideoEmbeddedFont(name)
                }
            }
        }
    }

    private fun readEbmlId(raf: java.io.RandomAccessFile): Int? {
        val b0 = raf.read()
        if (b0 == -1) return null
        var id = b0
        val numBytes = when {
            (b0 and 0x80) != 0 -> 1
            (b0 and 0x40) != 0 -> 2
            (b0 and 0x20) != 0 -> 3
            (b0 and 0x10) != 0 -> 4
            else -> 1
        }
        for (k in 1 until numBytes) {
            val b = raf.read()
            if (b == -1) return null
            id = (id shl 8) or (b and 0xFF)
        }
        return id
    }

    private fun readEbmlVint(raf: java.io.RandomAccessFile): Long {
        val b0 = raf.read()
        if (b0 == -1) return -1L
        var mask = 0x80
        var length = 1
        while (length <= 8 && (b0 and mask) == 0) {
            mask = mask shr 1
            length++
        }
        if (length > 8) return -1L
        var value = (b0 and (mask xor 0xFF)).toLong()
        for (i in 1 until length) {
            val b = raf.read()
            if (b == -1) return -1L
            value = (value shl 8) or (b.toLong() and 0xFFL)
        }
        return value
    }

    /**
     * Parses TrueType / OpenType font table records to extract declared Font Family (Name ID 1)
     * and Full Font Name (Name ID 4).
     */
    fun extractFontNamesFromTtfBytes(bytes: ByteArray): List<String> {
        val names = mutableListOf<String>()
        try {
            if (bytes.size < 12) return names
            val buffer = java.nio.ByteBuffer.wrap(bytes).order(java.nio.ByteOrder.BIG_ENDIAN)
            val numTables = buffer.getShort(4).toInt() and 0xFFFF
            if (numTables <= 0 || numTables > 200) return names

            var nameTableOffset = -1
            var nameTableLength = -1
            for (i in 0 until numTables) {
                val recordPos = 12 + i * 16
                if (recordPos + 16 > bytes.size) break
                val tag = buffer.getInt(recordPos)
                // Tag 'name' == 0x6E616D65
                if (tag == 0x6E616D65) {
                    nameTableOffset = buffer.getInt(recordPos + 8)
                    nameTableLength = buffer.getInt(recordPos + 12)
                    break
                }
            }

            if (nameTableOffset in 0 until bytes.size && nameTableLength > 6 && nameTableOffset + nameTableLength <= bytes.size) {
                val count = buffer.getShort(nameTableOffset + 2).toInt() and 0xFFFF
                val stringOffset = buffer.getShort(nameTableOffset + 4).toInt() and 0xFFFF
                val storageOffset = nameTableOffset + stringOffset

                for (i in 0 until count) {
                    val nrPos = nameTableOffset + 6 + i * 12
                    if (nrPos + 12 > bytes.size) break
                    val platformId = buffer.getShort(nrPos).toInt() and 0xFFFF
                    val encodingId = buffer.getShort(nrPos + 2).toInt() and 0xFFFF
                    val nameId = buffer.getShort(nrPos + 6).toInt() and 0xFFFF
                    val length = buffer.getShort(nrPos + 8).toInt() and 0xFFFF
                    val strOffset = buffer.getShort(nrPos + 10).toInt() and 0xFFFF

                    // nameId 1 = Font Family Name, nameId 4 = Full Font Name, nameId 6 = PostScript Name
                    if (nameId == 1 || nameId == 4 || nameId == 6) {
                        val actualPos = storageOffset + strOffset
                        if (actualPos + length <= bytes.size && length > 0) {
                            val str = if (platformId == 0 || platformId == 3 || (platformId == 2 && encodingId == 1)) {
                                String(bytes, actualPos, length, java.nio.charset.StandardCharsets.UTF_16BE)
                            } else {
                                String(bytes, actualPos, length, java.nio.charset.StandardCharsets.UTF_8)
                            }.trim()
                            if (str.isNotBlank() && !names.contains(str)) {
                                names.add(str)
                            }
                        }
                    }
                }
            }
        } catch (_: Throwable) {}
        return names
    }

    /**
     * Reads a TTF/OTF font file and returns its declared font family/full names.
     */
    fun extractFontNamesFromTtfFile(file: File): List<String> {
        return try {
            if (file.exists() && file.isFile && file.length() in 100..(30 * 1024 * 1024)) {
                extractFontNamesFromTtfBytes(file.readBytes())
            } else {
                emptyList()
            }
        } catch (_: Throwable) {
            emptyList()
        }
    }
}

