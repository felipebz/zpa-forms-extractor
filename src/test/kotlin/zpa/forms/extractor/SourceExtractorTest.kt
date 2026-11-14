package zpa.forms.extractor

import oracle.forms.jdapi.*
import org.assertj.core.api.Assertions.assertThat
import kotlin.test.Test

class SourceExtractorTest {

    private fun paths(module: JdapiModule) =
        extractSources(module).associate { (it.folders + it.name).joinToString("/") to it.text }

    @Test
    fun formModuleLayout() {
        val fm = FormModule("TEST")

        Trigger(fm, "WHEN-NEW-FORM-INSTANCE").triggerText = "null;"

        val blk1 = Block(fm, "BLK1")
        Trigger(blk1, "PRE-QUERY").triggerText = "x := 1;"
        val item1 = Item(blk1, "ITEM1")
        Trigger(item1, "WHEN-VALIDATE-ITEM").triggerText = "message('validate');"
        Item(blk1, "TOTAL").formula = ":BLK1.ITEM1 * 2"

        val pc = PropertyClass(fm, "PC")
        Trigger(pc, "WHEN-NEW-ITEM-INSTANCE").triggerText = "y := 2;"

        val ev = Event(fm, "EV")
        Trigger(ev, "WHEN-EVENT-RAISED").triggerText = "z := 3;"

        val menu = Menu(fm, "MAIN")
        MenuItem(menu, "SAVE").menuItemCode = "commit_form;"
        MenuItem(menu, "SUB").apply {
            commandType = JdapiTypes.COTY_MENU_CTID
            menuItemCode = "MAIN"
        }

        val procedure = "PROCEDURE MY_PROC IS\nBEGIN\n  NULL;\nEND;"
        ProgramUnit(fm, "MY_PROC").programUnitText = procedure

        assertThat(paths(fm)).containsExactlyInAnyOrderEntriesOf(
            mapOf(
                "triggers/WHEN-NEW-FORM-INSTANCE.tgg" to "BEGIN\nnull;\nEND;",
                "blocks/BLK1/PRE-QUERY.tgg" to "BEGIN\nx := 1;\nEND;",
                "blocks/BLK1/ITEM1/WHEN-VALIDATE-ITEM.tgg" to "BEGIN\nmessage('validate');\nEND;",
                "blocks/BLK1/TOTAL/FORMULA-CALCULATION.tgg" to "BEGIN\n:BLK1.TOTAL := :BLK1.ITEM1 * 2;\nEND;",
                "property-classes/PC/WHEN-NEW-ITEM-INSTANCE.tgg" to "BEGIN\ny := 2;\nEND;",
                "events/EV/WHEN-EVENT-RAISED.tgg" to "BEGIN\nz := 3;\nEND;",
                "menus/MAIN/SAVE.tgg" to "BEGIN\ncommit_form;\nEND;",
                "program-units/MY_PROC.pcd" to procedure
            )
        )

        fm.destroy()
    }

    @Test
    fun menuModuleLayout() {
        val mm = MenuModule("MENU1")
        mm.startupCode = "null;"
        MenuItem(Menu(mm, "M"), "QUIT").menuItemCode = "exit_form;"

        assertThat(paths(mm)).containsExactlyInAnyOrderEntriesOf(
            mapOf(
                "startup-code.tgg" to "BEGIN\nnull;\nEND;",
                "menus/M/QUIT.tgg" to "BEGIN\nexit_form;\nEND;"
            )
        )

        mm.destroy()
    }

}
