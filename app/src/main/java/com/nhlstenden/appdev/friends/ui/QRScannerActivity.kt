package com.nhlstenden.appdev.friends.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.BarcodeView
import com.nhlstenden.appdev.R
import java.util.UUID

class QRScannerActivity : AppCompatActivity() {
    private lateinit var barcodeView: BarcodeView
    private var isScanning = false
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startScanning()
        } else {
            Toast.makeText(this, "Camera permission is required to scan QR codes", Toast.LENGTH_LONG).show()
            finish()
        }
    }
    
    private val callback = object : BarcodeCallback {
        override fun barcodeResult(result: BarcodeResult) {
            if (!isScanning) return
            
            isScanning = false
            val scannedData = result.text
            
            // Validate if the scanned data is a valid UUID
            try {
                UUID.fromString(scannedData)
                setResult(RESULT_OK, Intent().apply {
                    putExtra("SCANNED_UUID", scannedData)
                })
                finish()
            } catch (e: IllegalArgumentException) {
                Toast.makeText(this@QRScannerActivity, "Invalid QR code format", Toast.LENGTH_SHORT).show()
                isScanning = true
            }
        }
        
        override fun possibleResultPoints(resultPoints: List<ResultPoint>) {
            // Not needed for our use case
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_scanner)
        
        barcodeView = findViewById(R.id.barcodeView)
        
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startScanning()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
    
    private fun startScanning() {
        isScanning = true
        barcodeView.decodeContinuous(callback)
    }
    
    override fun onResume() {
        super.onResume()
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startScanning()
        }
    }
    
    override fun onPause() {
        super.onPause()
        barcodeView.pause()
    }
}