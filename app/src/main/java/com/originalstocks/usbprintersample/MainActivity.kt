package com.originalstocks.usbprintersample

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.*
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.originalstocks.usbprintersample.converter.Printable
import com.originalstocks.usbprintersample.converter.RawPrintable
import com.originalstocks.usbprintersample.data.DefaultPrinter
import com.originalstocks.usbprintersample.data.Printer
import com.originalstocks.usbprintersample.data.TextPrintable
import com.originalstocks.usbprintersample.databinding.ActivityMainBinding


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
    var textToPrintByteArray: ByteArray? = null
    private lateinit var printer: Printer
    var extraLinesAtEnd: Byte = 0

    val mUsbReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (ACTION_USB_PERMISSION == action) {
                synchronized(this) {
                    val device =
                        intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            //call method to set up device communication
                            mInterface = device.getInterface(0)
                            mEndPoint = mInterface!!.getEndpoint(1) // 0 IN and  1 OUT to printer.
                            mConnection = mUsbManager!!.openDevice(device)
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

        //binding.dummyTextView.text = textToPrint

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

            mPermissionIntent =
                PendingIntent.getBroadcast(this, 0, Intent(ACTION_USB_PERMISSION), 0)
            val filter = IntentFilter(ACTION_USB_PERMISSION)
            registerReceiver(mUsbReceiver, filter)
            mUsbManager?.requestPermission(mDevice, mPermissionIntent)
        } else {
            Log.e("Exception", "Printer not found")
            Toast.makeText(this, "Please attach printer via USB", Toast.LENGTH_SHORT).show()
        }


        // do this on click of button
        binding.buttonPrint.setOnClickListener {
            if (mDevice != null) {
                startPrinting(mConnection, mInterface)
            } else {
                //startPrinting(mConnection, mInterface)
                Log.e(TAG, "onCreate_mDevice is null")
                Toast.makeText(this, "Please attach printer via USB", Toast.LENGTH_SHORT).show()
            }
        }

    }

    private fun startPrinting(connection: UsbDeviceConnection?, usbInterface: UsbInterface?) {
        printer = DefaultPrinter()

        val printableArrayList: ArrayList<Printable> = ArrayList()
        printableArrayList.add(RawPrintable.Builder(byteArrayOf(27, 100, 4)).build())

        val textBoldHeader = "SUR PLACE\nESP123"
        val textNormalContent =
            "RESTAURANT NAME\n5 rue sala,\nTel.04.74.98.22.22\nSTREET 43201425400035 - APE 5610C\n RCS LYON TVA INTRA FR27432078939"

        val textTableContent = "#254896-11     10/12/2020 12:56:13\n" +
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

        val printableHeader = TextPrintable.Builder()
            .setText(textBoldHeader) //The text you want to print
            .setAlignment(DefaultPrinter.ALIGNMENT_CENTER)
            .setEmphasizedMode(DefaultPrinter.EMPHASIZED_MODE_BOLD) //Bold or normal
            .setFontSize(DefaultPrinter.FONT_SIZE_LARGE)
            .setUnderlined(DefaultPrinter.UNDERLINED_MODE_OFF) // Underline on/off
            .setCharacterCode(DefaultPrinter.CHARCODE_PC437) // Character code to support languages /** CHARCODE_PC863 for Canadian-French*/
            .setLineSpacing(DefaultPrinter.LINE_SPACING_60)
            .setNewLinesAfter(2) // To provide n lines after sentence
            .build()

        val printableContent = TextPrintable.Builder()
            .setText(textNormalContent) //The text you want to print
            .setAlignment(DefaultPrinter.ALIGNMENT_CENTER)
            .setEmphasizedMode(DefaultPrinter.EMPHASIZED_MODE_NORMAL) //Bold or normal
            .setFontSize(DefaultPrinter.FONT_SIZE_NORMAL)
            .setUnderlined(DefaultPrinter.UNDERLINED_MODE_OFF) // Underline on/off
            .setCharacterCode(DefaultPrinter.CHARCODE_PC437) // Character code to support languages /** CHARCODE_PC863 for Canadian-French*/
            .setLineSpacing(DefaultPrinter.LINE_SPACING_60)
            .setNewLinesAfter(1) // To provide n lines after sentence
            .build()

        val printableTable = TextPrintable.Builder()
            .setText(textTableContent) //The text you want to print
            .setAlignment(DefaultPrinter.ALIGNMENT_CENTER)
            .setEmphasizedMode(DefaultPrinter.EMPHASIZED_MODE_NORMAL) //Bold or normal
            .setFontSize(DefaultPrinter.FONT_SIZE_NORMAL)
            .setUnderlined(DefaultPrinter.UNDERLINED_MODE_OFF) // Underline on/off
            .setCharacterCode(DefaultPrinter.CHARCODE_PC437) // Character code to support languages /** CHARCODE_PC863 for Canadian-French*/
            .setLineSpacing(DefaultPrinter.LINE_SPACING_60)
            .setNewLinesAfter(1) // To provide n lines after sentence
            .build()

        printableArrayList.add(printableHeader)
        printableArrayList.add(printableContent)
        printableArrayList.add(printableTable)

        /** Need to convert ArrayList into a byte array*/
        Log.i(TAG, "startPrinting_converted_list_into_byteArray = $printableArrayList")

        /*printableArrayList.forEach {
            it.getPrintableByteArray(printer).forEach { data ->
                Log.i(TAG, "startPrinting_byte_array_data_to_send = $data")
            }
        }*/

        when {
            usbInterface == null -> {
                Toast.makeText(this, "INTERFACE IS NULL", Toast.LENGTH_SHORT).show()
            }
            connection == null -> {
                Toast.makeText(this, "CONNECTION IS NULL", Toast.LENGTH_SHORT).show()
            }
            forceClaim == null -> {
                Toast.makeText(this, "FORCE CLAIM IS NULL", Toast.LENGTH_SHORT).show()
            }
            else -> {
                connection.claimInterface(usbInterface, forceClaim)
                val thread = Thread {
                    //val cutPaper = byteArrayOf(0x1D, 0x56, 0x41, 0x10)
                    printableArrayList.forEach {
                        it.getPrintableByteArray(printer).forEach { data ->
                            connection.bulkTransfer(
                                mEndPoint,
                                data,
                                data.size,
                                0
                            )
                            Log.i(TAG, "startPrinting_byte_array_data_to_send = $data")
                        }
                    }

                    //Feed 2 lines to cut the paper
                    if (extraLinesAtEnd > 0) {
                        printer.feedLineCommand.plus(extraLinesAtEnd)
                    }
                    //connection.bulkTransfer(mEndPoint, cutPaper, cutPaper.size, 0)
                }
                thread.run()
            }
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

}