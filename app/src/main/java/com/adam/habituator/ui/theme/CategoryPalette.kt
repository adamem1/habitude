package com.adam.habituator.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Curated swatches users pick from when creating/editing a category. Stored as the
 * category's ARGB int; new swatches can be appended without invalidating old data.
 */
data class CategorySwatch(val name: String, val color: Color)

val CategoryColorPalette = listOf(
    CategorySwatch("Red", Color(0xFFE57373)),
    CategorySwatch("Orange", Color(0xFFFFB74D)),
    CategorySwatch("Amber", Color(0xFFFFD54F)),
    CategorySwatch("Lime", Color(0xFFAED581)),
    CategorySwatch("Green", Color(0xFF81C784)),
    CategorySwatch("Teal", Color(0xFF4DB6AC)),
    CategorySwatch("Cyan", Color(0xFF4DD0E1)),
    CategorySwatch("Blue", Color(0xFF64B5F6)),
    CategorySwatch("Indigo", Color(0xFF7986CB)),
    CategorySwatch("Purple", Color(0xFF9575CD)),
    CategorySwatch("Pink", Color(0xFFF06292)),
    CategorySwatch("Brown", Color(0xFFA1887F)),
    CategorySwatch("Blue Gray", Color(0xFF90A4AE)),
    CategorySwatch("Deep Orange", Color(0xFFFF8A65)),
    CategorySwatch("Deep Purple", Color(0xFFBA68C8)),
    CategorySwatch("Yellow", Color(0xFFFFF176)),
)
