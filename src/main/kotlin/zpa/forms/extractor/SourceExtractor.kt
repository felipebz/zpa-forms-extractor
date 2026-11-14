//
// SPDX-FileCopyrightText: 2026 Felipe Zorzo
// SPDX-License-Identifier: Apache-2.0
//

package zpa.forms.extractor

import oracle.forms.jdapi.*

data class SourceFile(val folders: List<String>, val name: String, val text: String)

// An object reachable through several Object Library groups yields identical files; keep one
fun extractSources(module: JdapiModule): List<SourceFile> = SourceCollector().apply { collect(module) }.files.distinct()

private class SourceCollector {
    val files = mutableListOf<SourceFile>()

    private val programUnitExtensions = mapOf(
        JdapiTypes.PGTY_FUNCTION_CTID to "fun",
        JdapiTypes.PGTY_PROCEDURE_CTID to "pcd",
        JdapiTypes.PGTY_PACKAGESPEC_CTID to "pks",
        JdapiTypes.PGTY_PACKAGEBODY_CTID to "pkb"
    )

    fun collect(module: JdapiModule) {
        when (module) {
            is FormModule -> {
                for (o in module.programUnits) programUnit(o as ProgramUnit)
                for (o in module.triggers) trigger(o as Trigger, listOf("triggers"))
                for (o in module.blocks) block(o as Block)
                for (o in module.propertyClasses) propertyClass(o as PropertyClass)
                for (o in module.events) event(o as Event)
                for (o in module.recordGroups) recordGroup(o as RecordGroup)
                for (o in module.menus) menu(o as Menu)
            }
            is MenuModule -> {
                add(emptyList(), "startup-code.tgg", module.startupCode, ::anonymousBlock)
                for (o in module.programUnits) programUnit(o as ProgramUnit)
                for (o in module.menus) menu(o as Menu)
            }
            is PlsqlModule -> for (o in module.programUnits) programUnit(o as ProgramUnit)
            is ObjectLibrary -> for (o in module.objectLibraryObjects) libraryObject(o as JdapiObject)
        }
    }

    // Other Object Library object types (canvases, windows, alerts, visual attributes...) hold no code.
    private fun libraryObject(o: JdapiObject) {
        when (o) {
            is ProgramUnit -> programUnit(o)
            is Trigger -> trigger(o, listOf("triggers"))
            is Block -> block(o)
            is Item -> item(o, listOf("items", o.name))
            is PropertyClass -> propertyClass(o)
            is Event -> event(o)
            is RecordGroup -> recordGroup(o)
            is Menu -> menu(o)
            is MenuItem -> menuItem(o, listOf("menus"))
            // Library objects are typically grouped; the group's children reference the real objects
            is ObjectGroup -> for (c in o.objectGroupChilds) (c as ObjectGroupChild).objectGroupChildRealObject?.let(::libraryObject)
        }
    }

    private fun block(block: Block) {
        val folder = listOf("blocks", block.name)
        for (o in block.triggers) trigger(o as Trigger, folder)
        for (o in block.items) {
            val item = o as Item
            item(item, folder + item.name)
        }
    }

    private fun item(item: Item, folder: List<String>) {
        add(folder, "FORMULA-CALCULATION.tgg", item.formula) { "BEGIN\n:${item.getQualifiedName(false)} := $it;\nEND;" }
        for (o in item.triggers) {
            val trigger = o as Trigger
            // JDAPI also exposes the item formula as this trigger; it is exported above as an assignment
            if (trigger.name != "FORMULA-CALCULATION") trigger(trigger, folder)
        }
    }

    private fun propertyClass(pc: PropertyClass) {
        for (o in pc.triggers) trigger(o as Trigger, listOf("property-classes", pc.name))
    }

    private fun event(event: Event) {
        for (o in event.triggers) trigger(o as Trigger, listOf("events", event.name))
    }

    private fun menu(menu: Menu) {
        for (o in menu.menuItems) menuItem(o as MenuItem, listOf("menus", menu.name))
    }

    // Only PL/SQL items carry code. It lives in Menu Item Code; Command Text is an independent property
    // that the DOAG sample modules leave empty.
    private fun menuItem(item: MenuItem, folder: List<String>) {
        if (item.commandType == JdapiTypes.COTY_PLSQL_CTID) add(folder, "${item.name}.tgg", item.menuItemCode, ::anonymousBlock)
    }

    private fun trigger(trigger: Trigger, folder: List<String>) =
        add(folder, "${trigger.name}.tgg", trigger.triggerText, ::anonymousBlock)

    private fun programUnit(pu: ProgramUnit) {
        val extension = programUnitExtensions[pu.programUnitType] ?: return
        add(listOf("program-units"), "${pu.name}.$extension", pu.programUnitText) { it }
    }

    private fun recordGroup(rg: RecordGroup) = add(listOf("record-groups"), "${rg.name}.sql", rg.recordGroupQuery) { it }

    private fun add(folders: List<String>, name: String, text: String?, render: (String) -> String) {
        if (!text.isNullOrEmpty()) files += SourceFile(folders, name, render(text))
    }
}

private fun anonymousBlock(body: String) = "BEGIN\n$body\nEND;"
