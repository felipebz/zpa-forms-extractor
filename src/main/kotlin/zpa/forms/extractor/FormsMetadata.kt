//
// SPDX-FileCopyrightText: 2026 Felipe Zorzo
// SPDX-License-Identifier: Apache-2.0
//

package zpa.forms.extractor

import oracle.forms.jdapi.*

class FormsMetadata(module: FormModule) {

    val alerts = module.alerts.asSequence().map { (it as Alert).name }.toList()

    val blocks = module.blocks.asSequence().map {
        val block = it as Block
        BlockData(block.name, block.items.asSequence().map { item -> itemData(item as Item) }.toList())
    }.toList()

    val lovs = module.loVs.asSequence().map { lov -> (lov as LOV).name }.toList()

    val parameters = module.moduleParameters.asSequence().map {
        val p = it as ModuleParameter
        ParameterData(p.name, dataTypeName(p.parameterDataType))
    }.toList()
}

data class BlockData(val name: String, val items: List<ItemData>)
data class ItemData(val name: String, val dataType: String? = null, val maxLength: Int? = null)
data class ParameterData(val name: String, val dataType: String?)

// JdapiTypes constants are runtime values, so these are built lazily after JDAPI is loaded
private val VALUE_ITEM_TYPES by lazy {
    setOf(
        JdapiTypes.ITTY_TI_CTID,
        JdapiTypes.ITTY_DI_CTID,
        JdapiTypes.ITTY_LS_CTID,
        JdapiTypes.ITTY_CB_CTID,
        JdapiTypes.ITTY_RD_CTID
    )
}

private val DATA_TYPE_NAMES by lazy {
    mapOf(
        JdapiTypes.DATY_CHARACTER_CTID to "CHAR",
        JdapiTypes.DATY_NCHAR_CTID to "NCHAR",
        JdapiTypes.DATY_NUMBER_CTID to "NUMBER",
        JdapiTypes.DATY_DATE_CTID to "DATE",
        JdapiTypes.DATY_DATETIME_CTID to "DATETIME",
        JdapiTypes.DATY_LONG_CTID to "LONG",
        JdapiTypes.DATY_ALPHA_CTID to "ALPHA",
        JdapiTypes.DATY_INTEGER_CTID to "INT",
        JdapiTypes.DATY_MONEY_CTID to "MONEY",
        JdapiTypes.DATY_RINTEGER_CTID to "RINT",
        JdapiTypes.DATY_RMONEY_CTID to "RMONEY",
        JdapiTypes.DATY_RNUMBER_CTID to "RNUMBER",
        JdapiTypes.DATY_EDATE_CTID to "EDATE",
        JdapiTypes.DATY_JDATE_CTID to "JDATE",
        JdapiTypes.DATY_TIME_CTID to "TIME",
        JdapiTypes.DATY_OBJECTREF_CTID to "REF"
    )
}

private fun dataTypeName(id: Int): String? = DATA_TYPE_NAMES[id]

private fun itemData(item: Item): ItemData {
    val dataType = if (item.itemType in VALUE_ITEM_TYPES) dataTypeName(item.dataType) else null
    return if (dataType != null) ItemData(item.name, dataType, item.maximumLength) else ItemData(item.name)
}
