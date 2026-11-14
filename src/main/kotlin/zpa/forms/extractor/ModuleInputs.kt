package zpa.forms.extractor

import java.io.File
import java.util.Locale

/** A module to extract; [outputPath] is its folder below the output directory. */
data class ModuleInput(val file: File, val outputPath: String)

private val MODULE_EXTENSIONS = setOf("fmb", "mmb", "pll", "olb")

/**
 * Files are taken as given; directories are searched recursively for Forms modules. A module found in a
 * directory keeps its relative path, file name and extension, so `webutil.pll` and `webutil.olb` stay apart.
 */
fun findModules(inputs: List<File>): List<ModuleInput> =
    inputs.flatMap { input ->
        if (input.isDirectory) {
            input.walkTopDown()
                .filter { it.isFile && it.extension.lowercase(Locale.ROOT) in MODULE_EXTENSIONS }
                .map { ModuleInput(it, it.relativeTo(input).path) }
                .sortedBy { it.outputPath }
                .toList()
        } else {
            listOf(ModuleInput(input, input.name))
        }
    }.distinctBy { it.file.canonicalFile }
