package zpa.forms.extractor

import oracle.forms.jdapi.*
import org.assertj.core.api.Assertions.assertThat
import kotlin.test.Test

class FormsMetadataTest {

    @Test
    fun test() {
        val formModule = FormModule("TEST")

        Alert(formModule, "ALR1")
        Alert(formModule, "ALR2")

        val blk1 = Block(formModule, "BLK1")
        Item(blk1, "ITEM1")
        Item(blk1, "ITEM2")
        Item(blk1, "BTN").itemType = JdapiTypes.ITTY_PB_CTID
        Item(blk1, "DT").apply {
            dataType = JdapiTypes.DATY_DATE_CTID
            maximumLength = 11
        }

        LOV(formModule, "LOV1")
        LOV(formModule, "LOV2")

        ModuleParameter(formModule, "P1").parameterDataType = JdapiTypes.DATY_NUMBER_CTID

        val metadata = FormsMetadata(formModule)

        assertThat(metadata.alerts).containsExactly("ALR1", "ALR2")
        assertThat(metadata.blocks).containsExactly(
            BlockData(
                "BLK1", listOf(
                    ItemData("ITEM1", "CHAR", 30),
                    ItemData("ITEM2", "CHAR", 30),
                    ItemData("BTN"),
                    ItemData("DT", "DATE", 11)
                )
            )
        )
        assertThat(metadata.lovs).containsExactly("LOV1", "LOV2")
        assertThat(metadata.parameters).containsExactly(ParameterData("P1", "NUMBER"))

        formModule.destroy()
    }

}
