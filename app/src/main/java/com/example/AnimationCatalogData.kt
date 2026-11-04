package com.example

internal data class RawDesignItem(
    val number: Int,
    val name: String
)

internal data class RawCategorySpec(
    val number: Int,
    val id: String,
    val title: String,
    val subtitle: String,
    val accentHex: Long,
    val secondaryHex: Long,
    val names: List<String> = emptyList(),
    val items: List<RawDesignItem> = names.mapIndexed { idx, s -> RawDesignItem(idx + 1, s) }
)

/**
 * Legacy non-Pro animation sheets have been removed per user request so that
 * only the Pro Folder HD animations remain in the app.
 */
internal object AnimationCatalogData {
    val specs: List<RawCategorySpec> = emptyList()
}
