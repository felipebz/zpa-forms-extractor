package zpa.forms.extractor

import java.io.File
import java.io.IOException
import java.util.Locale

private val WINDOWS_RESERVED_NAME = Regex("^(CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])$")

/**
 * Writes [files] below [moduleDir]. Every target path is checked before anything is written,
 * so a collision leaves the module folder untouched.
 */
fun writeSources(moduleDir: File, files: List<SourceFile>) {
    val targets = files.map { it to (it.folders.map(::safeName) + safeFileName(it.name)) }

    val seen = HashSet<String>()
    for ((_, segments) in targets) {
        // case-insensitive, so the check also holds on case-insensitive filesystems
        if (!seen.add(segments.joinToString("/").lowercase(Locale.ROOT))) {
            throw IOException("Two sources map to ${File(moduleDir, segments.joinToString(File.separator))}")
        }
    }

    for ((file, segments) in targets) {
        val target = File(moduleDir, segments.joinToString(File.separator))
        target.parentFile.mkdirs()
        target.writeText(file.text)
    }
}

/** Appends `_` to Windows reserved device names, which cannot be used as file or folder names there. */
internal fun safeName(name: String): String =
    if (WINDOWS_RESERVED_NAME.matches(name.uppercase(Locale.ROOT))) name + "_" else name

private fun safeFileName(name: String): String {
    val dot = name.indexOf('.')
    return if (dot < 0) safeName(name) else safeName(name.substring(0, dot)) + name.substring(dot)
}
