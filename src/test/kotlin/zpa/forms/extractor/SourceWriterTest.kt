package zpa.forms.extractor

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException
import kotlin.test.Test

class SourceWriterTest {

    @get:Rule
    val temp = TemporaryFolder()

    @Test
    fun suffixesWindowsReservedNames() {
        writeSources(
            temp.root, listOf(
                SourceFile(listOf("blocks", "AUX"), "PRE-QUERY.tgg", "a"),
                SourceFile(emptyList(), "CON.tgg", "b")
            )
        )

        assertThat(File(temp.root, "blocks/AUX_/PRE-QUERY.tgg")).hasContent("a")
        assertThat(File(temp.root, "CON_.tgg")).hasContent("b")
    }

    @Test
    fun caseInsensitiveCollisionWritesNothing() {
        assertThatThrownBy {
            writeSources(
                temp.root, listOf(
                    SourceFile(listOf("blocks", "A"), "X.tgg", "1"),
                    SourceFile(listOf("blocks", "a"), "x.tgg", "2")
                )
            )
        }.isInstanceOf(IOException::class.java)

        assertThat(temp.root.walk().none { it.isFile }).isTrue()
    }

}
