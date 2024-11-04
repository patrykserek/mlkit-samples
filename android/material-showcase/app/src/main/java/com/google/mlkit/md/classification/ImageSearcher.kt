package com.google.mlkit.md.classification

import android.graphics.Bitmap
import android.util.Log
import kotlin.system.measureTimeMillis

class ImageSearcher(
    private val imageEncoder: ImageEncoder,
    private val textEncoder: TextEncoder,
) {
    val ebayCategoriesEncoded = ebayCategories.map { category ->
        category to textEncoder.encode(category)
    }

    suspend fun searchWithImage(imageBitmap: Bitmap): String {
        val categoriesEncoded = ebayCategoriesEncoded
        var result = ""
        val timeMillis = measureTimeMillis {
            val imageEncoded = imageEncoder.encodeBatch(listOf(imageBitmap)).first()
            val mostMatchingCategory = categoriesEncoded.maxOfWith(
                compareBy { category -> calculateSimilarity(imageEncoded, category.second) },
                { it }
            )
            result = mostMatchingCategory.first
        }
        Log.i("ImageSearcher", "Time it took to find category in milliseconds: $timeMillis")
        return result
    }
}

