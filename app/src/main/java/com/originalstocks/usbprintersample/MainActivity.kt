package com.originalstocks.usbprintersample

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.*
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.originalstocks.usbprintersample.databinding.ActivityMainBinding
import java.nio.ByteBuffer


class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity"
    private lateinit var binding: ActivityMainBinding
    private val ACTION_USB_PERMISSION = "com.originalstocks.usbprintersample.USB_PERMISSION"
    var mPermissionIntent: PendingIntent? = null
    var mUsbManager: UsbManager? = null
    var mDevice: UsbDevice? = null
    private var mEndPoint: UsbEndpoint? = null
    private var detachReceiver: BroadcastReceiver? = null
    private var mInterface: UsbInterface? = null
    private var mConnection: UsbDeviceConnection? = null
    private val forceClaim = true
    var mDeviceList: HashMap<String, UsbDevice>? = null
    var mDeviceIterator: Iterator<UsbDevice>? = null
    var protocol = 0
    var textToPrint = "#254896-11     10/12/2020 12:56:13\n" +
            "\n" +
            "QTE PRODUIT     UNIT   TOTAL\n" +
            "1X Burger BIO   7.50   8.50\n" +
            "    Bacon\n" +
            "    Chili Sauce\n" +
            "    Sup Oeuf    1.00\n" +
            "2X Green Burger 8.00   16.00\n" +
            "    Barbecue\n" +
            "    Sup Oeuf\n" +
            "\n" +
            "Total TTC              28.30\n" +
            "TVA                     8.60\n" +
            "Total HT               19.70"

    private val mUsbReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (ACTION_USB_PERMISSION == action) {
                synchronized(this) {
                    val device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE) as UsbDevice?
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        Log.i(TAG, "onReceive_permissions granted")

                        if (device != null) {
                            // call method to set up device communication
                            mInterface = device.getInterface(0)
                            mEndPoint = mInterface?.getEndpoint(0)
                            mConnection = mUsbManager?.openDevice(device)

                            startPrinting(device, textToPrint, mConnection)
                        }
                    } else {
                        Log.e(TAG, "onReceive_permissions denied")
                        Toast.makeText(
                            context,
                            "PERMISSION DENIED FOR THIS DEVICE",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Log.i("Info", "Activity started")

        mUsbManager = getSystemService(Context.USB_SERVICE) as UsbManager
        mDeviceList = mUsbManager?.deviceList
        mDeviceIterator = mDeviceList?.values?.iterator()

        Toast.makeText(this, "Device List Size: ${mDeviceList?.size}", Toast.LENGTH_SHORT).show()
        Log.i(TAG, "onCreate_device_list = $mDeviceList")

        if (mDeviceList!!.size > 0) {
            mDeviceIterator = mDeviceList!!.values.iterator()
            Toast.makeText(this, "Device List Size: ${mDeviceList?.size}", Toast.LENGTH_SHORT)
                .show()
            var usbDevice = ""
            while (mDeviceIterator!!.hasNext()) {
                val usbDevice1: UsbDevice = mDeviceIterator!!.next()
                usbDevice += """
                            DeviceID: ${usbDevice1.deviceId}
                            DeviceName: ${usbDevice1.deviceName}
                            Protocol: ${usbDevice1.deviceProtocol}
                            Product Name: ${usbDevice1.productName}
                            Manufacturer Name: ${usbDevice1.manufacturerName}
                            DeviceClass: ${usbDevice1.deviceClass} - ${
                    translatedDeviceClass(
                        usbDevice1.deviceClass
                    )
                }
                            DeviceSubClass: ${usbDevice1.deviceSubclass}
                            VendorID: ${usbDevice1.vendorId}
                            ProductID: ${usbDevice1.productId}
                            """
                val interfaceCount = usbDevice1.interfaceCount
                Toast.makeText(
                    this@MainActivity,
                    "INTERFACE COUNT: $interfaceCount",
                    Toast.LENGTH_SHORT
                ).show()

                protocol = usbDevice1.deviceProtocol
                mDevice = usbDevice1
                Toast.makeText(this@MainActivity, "Device is attached", Toast.LENGTH_SHORT).show()
                binding.deviceCount.text = usbDevice
                Log.i(TAG, "onCreate_device_info = $usbDevice")
            }

        } else {
            Log.e("Exception", "Printer not found")
            Toast.makeText(this, "Please attach printer via USB", Toast.LENGTH_SHORT).show()
        }


        // do this on click of button
        binding.buttonPrint.setOnClickListener {
            mPermissionIntent = PendingIntent.getBroadcast(
                this@MainActivity, 0, Intent(
                    ACTION_USB_PERMISSION
                ), 0
            )
            val filter = IntentFilter(ACTION_USB_PERMISSION)
            registerReceiver(mUsbReceiver, filter)
            if (mDevice != null) {
                mUsbManager?.requestPermission(mDevice, mPermissionIntent)
            } else {
                Log.e(TAG, "onCreate_mDevice is null")
                Toast.makeText(this, "Please attach printer via USB", Toast.LENGTH_SHORT).show()
            }
            // Make the call for print
        }


    }

    override fun onStart() {
        super.onStart()

        detachReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val action = intent?.action
                if (action.equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
                    Log.e(TAG, "onReceive_detachReceiver: USB detached")
                    Toast.makeText(context, "USB detached", Toast.LENGTH_LONG).show()
                }
            }
        }

        val filter = IntentFilter()
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        registerReceiver(detachReceiver, filter)
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(detachReceiver)
    }


    private fun translatedDeviceClass(deviceClass: Int): String {
        return when (deviceClass) {
            UsbConstants.USB_CLASS_APP_SPEC -> "Application specific USB class"
            UsbConstants.USB_CLASS_AUDIO -> "USB class for audio devices"
            UsbConstants.USB_CLASS_CDC_DATA -> "USB class for CDC devices (communications device class)"
            UsbConstants.USB_CLASS_COMM -> "USB class for communication devices"
            UsbConstants.USB_CLASS_CONTENT_SEC -> "USB class for content security devices"
            UsbConstants.USB_CLASS_CSCID -> "USB class for content smart card devices"
            UsbConstants.USB_CLASS_HID -> "USB class for human interface devices (for example, mice and keyboards)"
            UsbConstants.USB_CLASS_HUB -> "USB class for USB hubs"
            UsbConstants.USB_CLASS_MASS_STORAGE -> "USB class for mass storage devices"
            UsbConstants.USB_CLASS_MISC -> "USB class for wireless miscellaneous devices"
            UsbConstants.USB_CLASS_PER_INTERFACE -> "USB class indicating that the class is determined on a per-interface basis"
            UsbConstants.USB_CLASS_PHYSICA -> "USB class for physical devices"
            UsbConstants.USB_CLASS_PRINTER -> "USB class for printers"
            UsbConstants.USB_CLASS_STILL_IMAGE -> "USB class for still image devices (digital cameras)"
            UsbConstants.USB_CLASS_VENDOR_SPEC -> "Vendor specific USB class"
            UsbConstants.USB_CLASS_VIDEO -> "USB class for video devices"
            UsbConstants.USB_CLASS_WIRELESS_CONTROLLER -> "USB class for wireless controller devices"
            else -> "Unknown USB class!"
        }
    }

    fun startPrinting(
        printerDevice: UsbDevice,
        textToPrint: String,
        mConnection: UsbDeviceConnection?
    ) {
        val handler = Handler()
        handler.post(object : Runnable {
            var usbDeviceConnection: UsbDeviceConnection? = null
            var usbInterface: UsbInterface? = null
            override fun run() {
                try {
                    Log.i("Info", "Bulk transfer started")
                    // usbInterface = printerDevice.getInterface(0);
                    for (i in 0 until printerDevice.interfaceCount) {
                        usbInterface = printerDevice.getInterface(i)
                        if (usbInterface!!.interfaceClass == UsbConstants.USB_CLASS_PRINTER) {
                            // usbInterface = mDevice;
                        }
                    }
                    val endPoint = usbInterface!!.getEndpoint(0)
                    usbDeviceConnection = mUsbManager!!.openDevice(mDevice)
                    usbDeviceConnection?.claimInterface(usbInterface, true)
                    var myStringData = textToPrint
                    myStringData += "\n"
                    val array = myStringData.toByteArray()
                    val outputBuffer: ByteBuffer = ByteBuffer.allocate(array.size)
                    val request = UsbRequest()
                    request.initialize(usbDeviceConnection, endPoint)
                    request.queue(outputBuffer, array.size)
                    if (usbDeviceConnection?.requestWait() === request) {
                        Log.i("Info", outputBuffer.getChar(0).toString() + "")
                        val m = Message()
                        m.obj = outputBuffer.array()
                        outputBuffer.clear()
                    } else {
                        Log.i("Info", "No request received")
                    }
                    val transferredData = usbDeviceConnection?.bulkTransfer(
                        endPoint,
                        myStringData.toByteArray(),
                        myStringData.toByteArray().size,
                        5000
                    )
                    Log.i("Info", "Amount of data transferred : $transferredData")
                } catch (e: Exception) {
                    Log.e("Exception", "Unable to transfer bulk data")
                    e.printStackTrace()
                } finally {
                    try {
                        usbDeviceConnection!!.releaseInterface(usbInterface)
                        Log.i("Info", "Interface released")
                        usbDeviceConnection!!.close()
                        Log.i("Info", "Usb connection closed")
                        unregisterReceiver(mUsbReceiver)
                        Log.i("Info", "Broadcast receiver unregistered")
                    } catch (e: Exception) {
                        Log.e("Exception", "Unable to release resources because : " + e.message)
                        e.printStackTrace()
                    }
                }
            }
        })
    }
}