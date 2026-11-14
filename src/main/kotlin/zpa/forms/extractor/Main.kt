//
// SPDX-FileCopyrightText: 2026 Felipe Zorzo
// SPDX-License-Identifier: Apache-2.0
//

package zpa.forms.extractor

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.file
import com.google.gson.Gson
import oracle.forms.jdapi.FormModule
import oracle.forms.jdapi.Jdapi
import oracle.forms.jdapi.JdapiException
import oracle.forms.jdapi.JdapiModule
import java.io.File
import java.io.IOException
import java.util.Locale

class Main : CliktCommand(name = "zpa-forms-extractor") {
    private val inputs by option(
        "--input",
        help = "Oracle Forms module (.fmb, .mmb, .pll, .olb) or directory searched recursively; repeatable"
    ).file(mustExist = true).multiple(required = true)
    private val output by option("--output", help = "output directory").file(canBeFile = false).required()

    override fun run() {
        val modules = findModules(inputs)
        if (modules.isEmpty()) {
            echo("No Oracle Forms modules found", err = true)
            throw ProgramResult(1)
        }

        val claimed = HashMap<String, File>()
        var failures = 0
        try {
            for (m in modules) {
                val key = m.outputPath.lowercase(Locale.ROOT)
                val owner = claimed[key]
                if (owner != null) {
                    echo("Skipping ${m.file}: output folder ${m.outputPath} already used by $owner", err = true)
                    failures++
                    continue
                }
                claimed[key] = m.file

                val moduleDir = File(output, m.outputPath)
                // also protects the source module when --output is one of the input directories
                if (moduleDir.exists() && !moduleDir.isDirectory) {
                    echo("Skipping ${m.file}: $moduleDir exists and is not a directory", err = true)
                    failures++
                    continue
                }

                try {
                    extract(m.file, moduleDir)
                    echo("${m.file} -> $moduleDir")
                } catch (e: JdapiException) {
                    echo("Failed to extract ${m.file}: ${e.message}", err = true)
                    failures++
                } catch (e: IOException) {
                    echo("Failed to extract ${m.file}: ${e.message}", err = true)
                    failures++
                }
            }
        } finally {
            Jdapi.shutdown()
        }

        if (failures > 0) throw ProgramResult(1)
    }

    private fun extract(file: File, moduleDir: File) {
        val module = JdapiModule.openModule(file)
        try {
            val files = extractSources(module).toMutableList()
            if (module is FormModule) {
                files += SourceFile(emptyList(), "metadata.json", Gson().toJson(FormsMetadata(module)))
            }
            writeSources(moduleDir, files)
        } finally {
            module.destroy()
        }
    }
}

fun main(args: Array<String>) = Main().main(args)
