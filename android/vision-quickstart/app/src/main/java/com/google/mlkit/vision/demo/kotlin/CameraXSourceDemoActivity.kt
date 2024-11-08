/*
 * Copyright 2021 Google LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.mlkit.vision.demo.kotlin

import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.CompoundButton
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import android.widget.ToggleButton
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.view.PreviewView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.drawable.toDrawable
import com.google.android.gms.common.annotation.KeepName
import com.google.common.primitives.Ints.min
import com.google.mlkit.common.model.LocalModel
import com.google.mlkit.vision.camera.CameraSourceConfig
import com.google.mlkit.vision.camera.CameraXSource
import com.google.mlkit.vision.camera.DetectionTaskCallback
import com.google.mlkit.vision.demo.GraphicOverlay
import com.google.mlkit.vision.demo.InferenceInfoGraphic
import com.google.mlkit.vision.demo.R
import com.google.mlkit.vision.demo.kotlin.objectdetector.ObjectGraphic
import com.google.mlkit.vision.demo.preference.PreferenceUtils
import com.google.mlkit.vision.demo.preference.SettingsActivity
import com.google.mlkit.vision.demo.preference.SettingsActivity.LaunchSource
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.ObjectDetector
import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions
import java.util.Objects
import kotlin.math.max

/** Live preview demo app for ML Kit APIs using CameraXSource API. */
@KeepName
@RequiresApi(VERSION_CODES.LOLLIPOP)
class CameraXSourceDemoActivity : AppCompatActivity(), CompoundButton.OnCheckedChangeListener {
    private var previewView: PreviewView? = null
    private var graphicOverlay: GraphicOverlay? = null
    private var needUpdateGraphicOverlayImageSourceInfo = false
    private var lensFacing: Int = CameraSourceConfig.CAMERA_FACING_BACK
    private var cameraXSource: CameraXSource? = null
    private var customObjectDetectorOptions: CustomObjectDetectorOptions? = null
    private var targetResolution: Size? = null

    private var clickablesLayout: FrameLayout? = null
    private val clickableOverlayViews: MutableMap<Int, ImageView> = mutableMapOf()

    private var selectedObjectLayout: ConstraintLayout? = null
    private var selectedObjectImage: ImageView? = null
    private var selectedObjectState: SelectionState = SelectionState.NOT_SELECTED

    enum class SelectionState {
        NOT_SELECTED,
        SELECTING,
        SELECTED
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")
        setContentView(R.layout.activity_vision_cameraxsource_demo)
        previewView = findViewById(R.id.preview_view)
        if (previewView == null) {
            Log.d(TAG, "previewView is null")
        }
        graphicOverlay = findViewById(R.id.graphic_overlay)
        if (graphicOverlay == null) {
            Log.d(TAG, "graphicOverlay is null")
        }

        clickablesLayout = findViewById(R.id.clickables_layout)
        selectedObjectLayout = findViewById(R.id.selected_object_layout)
        selectedObjectImage = findViewById(R.id.selected_object_image)
        findViewById<ImageButton>(R.id.selected_object_back).setOnClickListener {
            selectedObjectState = SelectionState.NOT_SELECTED
            selectedObjectImage?.setImageDrawable(null)
            selectedObjectLayout?.visibility = GONE
        }

        val facingSwitch = findViewById<ToggleButton>(R.id.facing_switch)
        facingSwitch.setOnCheckedChangeListener(this)
        val settingsButton = findViewById<ImageView>(R.id.settings_button)
        settingsButton.setOnClickListener {
            val intent = Intent(applicationContext, SettingsActivity::class.java)
            intent.putExtra(SettingsActivity.EXTRA_LAUNCH_SOURCE, LaunchSource.CAMERAXSOURCE_DEMO)
            startActivity(intent)
        }
    }

    override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
        if (lensFacing == CameraSourceConfig.CAMERA_FACING_FRONT) {
            lensFacing = CameraSourceConfig.CAMERA_FACING_BACK
        } else {
            lensFacing = CameraSourceConfig.CAMERA_FACING_FRONT
        }
        createThenStartCameraXSource()
    }

    public override fun onResume() {
        super.onResume()
        if (cameraXSource != null &&
            PreferenceUtils.getCustomObjectDetectorOptionsForLivePreview(this, localModel)
                .equals(customObjectDetectorOptions) &&
            PreferenceUtils.getCameraXTargetResolution(
                getApplicationContext(),
                lensFacing
            ) != null &&
            (Objects.requireNonNull(
                PreferenceUtils.getCameraXTargetResolution(getApplicationContext(), lensFacing)
            ) == targetResolution)
        ) {
            cameraXSource!!.start()
        } else {
            createThenStartCameraXSource()
        }
    }

    override fun onPause() {
        super.onPause()
        if (cameraXSource != null) {
            cameraXSource!!.stop()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (cameraXSource != null) {
            cameraXSource!!.stop()
        }
    }

    private fun createThenStartCameraXSource() {
        if (cameraXSource != null) {
            cameraXSource!!.close()
        }
        customObjectDetectorOptions =
            PreferenceUtils.getCustomObjectDetectorOptionsForLivePreview(
                getApplicationContext(),
                localModel
            )
        val objectDetector: ObjectDetector =
            ObjectDetection.getClient(customObjectDetectorOptions!!)
        var detectionTaskCallback: DetectionTaskCallback<List<DetectedObject>> =
            DetectionTaskCallback<List<DetectedObject>> { detectionTask ->
                detectionTask
                    .addOnSuccessListener { results -> onDetectionTaskSuccess(results) }
                    .addOnFailureListener { e -> onDetectionTaskFailure(e) }
            }
        val builder: CameraSourceConfig.Builder =
            CameraSourceConfig.Builder(
                getApplicationContext(),
                objectDetector!!,
                detectionTaskCallback
            )
                .setFacing(lensFacing)
        targetResolution =
            PreferenceUtils.getCameraXTargetResolution(getApplicationContext(), lensFacing)
        if (targetResolution != null) {
            builder.setRequestedPreviewSize(targetResolution!!.width, targetResolution!!.height)
        }
        cameraXSource = CameraXSource(builder.build(), previewView!!)
        needUpdateGraphicOverlayImageSourceInfo = true
        cameraXSource!!.start()
    }

    private fun onDetectionTaskSuccess(results: List<DetectedObject>) {
        graphicOverlay!!.clear()
        if (needUpdateGraphicOverlayImageSourceInfo) {
            val size: Size = cameraXSource!!.getPreviewSize()!!
            if (size != null) {
                Log.d(TAG, "preview width: " + size.width)
                Log.d(TAG, "preview height: " + size.height)
                val isImageFlipped =
                    cameraXSource!!.getCameraFacing() == CameraSourceConfig.CAMERA_FACING_FRONT
                if (isPortraitMode) {
                    // Swap width and height sizes when in portrait, since it will be rotated by
                    // 90 degrees. The camera preview and the image being processed have the same size.
                    graphicOverlay!!.setImageSourceInfo(size.height, size.width, isImageFlipped)
                } else {
                    graphicOverlay!!.setImageSourceInfo(size.width, size.height, isImageFlipped)
                }
                needUpdateGraphicOverlayImageSourceInfo = false
            } else {
                Log.d(TAG, "previewsize is null")
            }
        }
        Log.v(TAG, "Number of object been detected: " + results.size)
        for (`object` in results) {
            val objectGraphic = ObjectGraphic(graphicOverlay!!, `object`)
            graphicOverlay!!.add(objectGraphic)
            val id: Int = `object`.trackingId!!

            if (!clickableOverlayViews.containsKey(id)) {
                val view = ImageView(this)
                clickablesLayout?.addView(view)
                clickableOverlayViews[id] = view
            }
            val view = clickableOverlayViews[id]
            if (selectedObjectState == SelectionState.SELECTED) {
                clickablesLayout?.removeAllViews()
                clickableOverlayViews.clear()
            } else {
                view?.setOnClickListener { _ ->
                    val wholeBitmap = previewView!!.bitmap!!
                    val left =
                        max(
                            0,
                            objectGraphic.translateX(`object`.boundingBox.left.toFloat()).toInt()
                        )
                    val right = min(
                        objectGraphic.translateX(`object`.boundingBox.right.toFloat()).toInt(),
                        previewView!!.width
                    )
                    val top =
                        max(0, objectGraphic.translateY(`object`.boundingBox.top.toFloat()).toInt())
                    val bottom = min(
                        objectGraphic.translateY(`object`.boundingBox.bottom.toFloat()).toInt(),
                        previewView!!.height
                    )
                    val width = right - left
                    val height = bottom - top
                    val croppedBitmap = Bitmap.createBitmap(wholeBitmap, left, top, width, height)
                    clickableOverlayViews[id]?.setImageDrawable(croppedBitmap.toDrawable(resources))
                    selectedObjectImage?.setImageDrawable(croppedBitmap.toDrawable(resources))
                    selectedObjectState = SelectionState.SELECTED
                    selectedObjectLayout?.visibility = VISIBLE
                        //TODO
//                    view.animate()
//                        .x(previewView!!.width / 2f - width / 2f)
//                        .y(previewView!!.height / 2f - height / 2f)
//                        .setListener(object : AnimatorListener {
//                            override fun onAnimationStart(animation: Animator) {
//                                selectedObjectState = SelectionState.SELECTING
//                            }
//
//                            override fun onAnimationEnd(animation: Animator) {
//                                selectedObjectState = SelectionState.SELECTED
//                                selectedObjectLayout?.visibility = VISIBLE
//                            }
//
//                            override fun onAnimationCancel(animation: Animator) {
//                                selectedObjectState = SelectionState.NOT_SELECTED
//                            }
//
//                            override fun onAnimationRepeat(animation: Animator) {
//                                selectedObjectState = SelectionState.SELECTING
//                            }
//                        })
                }

                val left = objectGraphic.translateX(`object`.boundingBox.left.toFloat())
                val right = objectGraphic.translateX(`object`.boundingBox.right.toFloat())
                val top = objectGraphic.translateY(`object`.boundingBox.top.toFloat())
                val bottom = objectGraphic.translateY(`object`.boundingBox.bottom.toFloat())

                view?.apply {
                    x = left
                    y = top
                }
                val layoutParams = view?.layoutParams
                layoutParams?.apply {
                    width = (right - left).toInt()
                    height = (bottom - top).toInt()
                }
            }
        }

        clickableOverlayViews.keys
            .filter { trackingId -> results.none { it.trackingId == trackingId } }
            .forEach {
                clickablesLayout?.removeView(clickableOverlayViews[it])
                clickableOverlayViews.remove(it)
            }

        graphicOverlay!!.add(InferenceInfoGraphic(graphicOverlay!!))
        graphicOverlay!!.postInvalidate()
    }

    private fun onDetectionTaskFailure(e: Exception) {
        graphicOverlay!!.clear()
        graphicOverlay!!.postInvalidate()
        val error = "Failed to process. Error: " + e.localizedMessage
        Toast.makeText(
            graphicOverlay!!.getContext(),
            """
   $error
   Cause: ${e.cause}
      """.trimIndent(),
            Toast.LENGTH_SHORT
        )
            .show()
        Log.d(TAG, error)
    }

    private val isPortraitMode: Boolean
        private get() =
            (getApplicationContext().getResources().getConfiguration().orientation !==
                    Configuration.ORIENTATION_LANDSCAPE)

    companion object {
        private const val TAG = "CameraXSourcePreview"
        private val localModel: LocalModel =
            LocalModel.Builder().setAssetFilePath("custom_models/object_labeler.tflite").build()
    }
}
