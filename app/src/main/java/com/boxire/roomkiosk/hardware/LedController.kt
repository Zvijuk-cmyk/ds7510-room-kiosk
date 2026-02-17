package com.boxire.roomkiosk.hardware

import android.util.Log

interface LedController {
    fun setAvailable()
    fun setBusy()
    fun setOff()
}

class NoOpLedController : LedController {
    override fun setAvailable() {
        Log.d("LedController", "LED -> GREEN (Available)")
    }

    override fun setBusy() {
        Log.d("LedController", "LED -> RED (Busy)")
    }

    override fun setOff() {
        Log.d("LedController", "LED -> OFF")
    }
}

class VendorLedController : LedController {
    // Placeholder for specific hardware SDKs (e.g., ProDVX, Qbic)
    // Implement actual reflection or SDK calls here.
    
    override fun setAvailable() {
        try {
            // Example: VendorSDK.setLed(Color.GREEN)
            Log.d("VendorLedController", "Setting Hardware LED to GREEN")
        } catch (e: Exception) {
            Log.e("VendorLedController", "Failed to set LED Green", e)
        }
    }

    override fun setBusy() {
        try {
            // Example: VendorSDK.setLed(Color.RED)
            Log.d("VendorLedController", "Setting Hardware LED to RED")
        } catch (e: Exception) {
            Log.e("VendorLedController", "Failed to set LED Red", e)
        }
    }

    override fun setOff() {
         try {
            // Example: VendorSDK.setLed(Color.OFF)
            Log.d("VendorLedController", "Setting Hardware LED to OFF")
        } catch (e: Exception) {
            Log.e("VendorLedController", "Failed to set LED Off", e)
        }
    }
}
