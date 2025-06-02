package com.nhlstenden.appdev.shared.components

import android.app.Activity
import android.content.Intent
import android.os.Bundle

class ImageCropActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Stub: No UI, just finish immediately
        setResult(Activity.RESULT_CANCELED)
        finish()
    }

    companion object {
        fun start(activity: Activity, imageUri: String, requestCode: Int) {
            val intent = Intent(activity, ImageCropActivity::class.java)
            intent.putExtra("IMAGE_URI", imageUri)
            activity.startActivityForResult(intent, requestCode)
        }
    }
} 