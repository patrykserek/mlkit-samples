/*
 * Copyright 2020 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.mlkit.md.objectdetection

import android.util.Log
import androidx.annotation.MainThread
import com.google.android.gms.tasks.Task
import com.google.mlkit.common.model.LocalModel
import com.google.mlkit.md.InputInfo
import com.google.mlkit.md.camera.FrameProcessorBase
import com.google.mlkit.md.camera.GraphicOverlay
import com.google.mlkit.md.camera.WorkflowModel
import com.google.mlkit.md.overlaps
import com.google.mlkit.md.settings.PreferenceUtils
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.ObjectDetector
import com.google.mlkit.vision.objects.ObjectDetectorOptionsBase
import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import java.io.IOException

/** A processor to run object detector in multi-objects mode.  */
class MultiObjectProcessor(
    graphicOverlay: GraphicOverlay,
    private val workflowModel: WorkflowModel,
    private val customModelPath: String? = null
) : FrameProcessorBase<List<DetectedObject>>() {

    private val detector: ObjectDetector

    init {
        val options: ObjectDetectorOptionsBase
        val isClassificationEnabled = PreferenceUtils.isClassificationEnabled(graphicOverlay.context)

        if (customModelPath != null) {
            val localModel = LocalModel.Builder()
                .setAssetFilePath(customModelPath)
                .build()
            options = CustomObjectDetectorOptions.Builder(localModel)
                .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
                .enableClassification() // Always enable classification for custom models
                .build()
        } else {
            val optionsBuilder = ObjectDetectorOptions.Builder()
                .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
                .enableMultipleObjects()
            if (isClassificationEnabled) {
                optionsBuilder.enableClassification()
            }
            options = optionsBuilder.build()
        }

        this.detector = ObjectDetection.getClient(options)
    }

    override fun stop() {
        super.stop()
        try {
            detector.close()
        } catch (e: IOException) {
            Log.e(TAG, "Failed to close object detector!", e)
        }
    }

    override fun detectInImage(image: InputImage): Task<List<DetectedObject>> {
        return detector.process(image)
    }

    @MainThread
    override fun onSuccess(
        inputInfo: InputInfo,
        results: List<DetectedObject>,
        graphicOverlay: GraphicOverlay
    ) {
        val objects = filterNonOverlappingObjects(results)
        if (!workflowModel.isCameraLive) {
            return
        }

        graphicOverlay.clear()

        objects.forEach {
            graphicOverlay.add(ObjectGraphic(graphicOverlay, it))
        }

        graphicOverlay.invalidate()
    }

    private fun filterNonOverlappingObjects(detectedObjects: List<DetectedObject>): List<DetectedObject> {
        val result = mutableListOf<DetectedObject>()
        for (detectedObject in detectedObjects) {
            if (result.none { it.boundingBox.overlaps(detectedObject.boundingBox) }) {
                result.add(detectedObject)
            }
        }
        return result
    }

    override fun onFailure(e: Exception) {
        Log.e(TAG, "Object detection failed!", e)
    }

    companion object {

        private const val TAG = "MultiObjectProcessor"
    }
}
