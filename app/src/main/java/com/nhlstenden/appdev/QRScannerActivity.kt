package com.nhlstenden.appdev

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import com.budiyev.android.codescanner.AutoFocusMode
import com.budiyev.android.codescanner.CodeScanner
import com.budiyev.android.codescanner.CodeScannerView
import com.budiyev.android.codescanner.DecodeCallback
import com.budiyev.android.codescanner.ErrorCallback
import com.budiyev.android.codescanner.ScanMode

class QRScannerActivity : AppCompatActivity() {
    private lateinit var codeScanner: CodeScanner
    private var uuidRegex = Regex("[a-zA-Z0-9]{8}-[a-zA-Z0-9]{4}-[a-zA-Z0-9]{4}-[a-zA-Z0-9]{4}-[a-zA-Z0-9]{12}")
    private val TAG = "QRScannerActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_friends_scanner)
        val scannerView = findViewById<CodeScannerView>(R.id.CodeScannerView)

        codeScanner = CodeScanner(this, scannerView)

        // Parameters (default values)
        codeScanner.camera = CodeScanner.CAMERA_BACK // or CAMERA_FRONT or specific camera id
        codeScanner.formats = CodeScanner.ALL_FORMATS // list of type BarcodeFormat,

        // ex. listOf(BarcodeFormat.QR_CODE)
        codeScanner.autoFocusMode = AutoFocusMode.SAFE // or CONTINUOUS
        codeScanner.scanMode = ScanMode.SINGLE // or CONTINUOUS or PREVIEW
        codeScanner.isAutoFocusEnabled = true // Whether to enable auto focus or not
        codeScanner.isFlashEnabled = false // Whether to enable flash or not

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 100)
        }

        // Callbacks
        codeScanner.decodeCallback = DecodeCallback {
            runOnUiThread {
                val result = it.text.toString()
                Log.d(TAG, "QR code scanned: $result")
                
                if (!uuidRegex.containsMatchIn(result)) {
                    Log.d(TAG, "Invalid UUID format in QR code")
                    setResult(Activity.RESULT_CANCELED)
                    finish()
                    return@runOnUiThread
                }
                
                // Get the return destination from the intent if available
                val returnTo = intent.getStringExtra("RETURN_TO")
                Log.d(TAG, "Return destination from intent: $returnTo")
                
                // Create result intent with the scanned UUID and navigation info
                val resultIntent = Intent().apply {
                    putExtra("SCANNED_UUID", result)
                    putExtra("NAVIGATE_TO_FRIENDS", true)
                    
                    // Preserve the return destination
                    if (returnTo != null) {
                        putExtra("RETURN_TO", returnTo)
                    }
                }
                
                Log.d(TAG, "Setting result and finishing activity")
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            }
        }

        codeScanner.errorCallback = ErrorCallback { // or ErrorCallback.SUPPRESS
            Log.w(TAG, "Camera initialization error: ${it.message}", it)
            runOnUiThread {
                Toast.makeText(this, "Camera initialization error: ${it.message}",
                    Toast.LENGTH_LONG).show()
            }
        }

        scannerView.setOnClickListener {
            codeScanner.startPreview()
        }
    }


    override fun onResume() {
        super.onResume()
        codeScanner.startPreview()
    }

    override fun onPause() {
        codeScanner.releaseResources()
        super.onPause()
    }
}