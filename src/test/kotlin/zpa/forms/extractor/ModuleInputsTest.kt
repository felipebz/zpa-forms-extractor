package zpa.forms.extractor

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.Test

class ModuleInputsTest {

    @get:Rule
    val temp = TemporaryFolder()

    private lateinit var root: File

    @Before
    fun createTree() {
        root = temp.root
        for (path in listOf("a/x.fmb", "a/x.pll", "b/y.FMB", "c/z.txt")) {
            File(root, path).apply { parentFile.mkdirs() }.createNewFile()
        }
    }

    private fun path(vararg segments: String) = segments.joinToString(File.separator)

    @Test
    fun findsModulesRecursivelyKeepingRelativePaths() {
        assertThat(findModules(listOf(root)).map { it.outputPath })
            .containsExactly(path("a", "x.fmb"), path("a", "x.pll"), path("b", "y.FMB"))
    }

    @Test
    fun dropsModulesReachedTwice() {
        assertThat(findModules(listOf(root, File(root, "a/x.fmb"))).map { it.outputPath })
            .containsExactly(path("a", "x.fmb"), path("a", "x.pll"), path("b", "y.FMB"))
    }

    @Test
    fun fileInputUsesFileName() {
        assertThat(findModules(listOf(File(root, "b/y.FMB"))).map { it.outputPath }).containsExactly("y.FMB")
    }

}
