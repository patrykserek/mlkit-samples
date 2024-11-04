package com.google.mlkit.md.classification

import android.content.Context
import com.google.mlkit.md.objectdetection.CategoryDetector

object CategoryDetectorFactory {
    private var categoryDetector: CategoryDetector? = null
    fun get(context: Context): CategoryDetector {
        if (categoryDetector == null) {
            categoryDetector = CategoryDetector(
                ImageSearcher(
                    ImageEncoderMobileCLIP(context, PreprocessorMobileCLIP()),
                    TextEncoderMobileCLIP(context),
                )
            )
        }
        return categoryDetector!!
    }
}
