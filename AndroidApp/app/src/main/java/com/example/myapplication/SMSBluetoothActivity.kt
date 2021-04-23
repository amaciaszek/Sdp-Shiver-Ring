package com.example.myapplication

import android.Manifest
import android.bluetooth.*
//import android.R
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telephony.SmsManager
import android.util.Log
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.google.android.material.navigation.NavigationView
import kotlinx.android.synthetic.main.content_calendar.*
import kotlinx.android.synthetic.main.content_smsbluetooth.*
import java.io.File
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.schedule


class SMSBluetoothActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private val REQUEST_CODE_NEW_FEATURE_PERMISSIONS = 1
    private val REQUEST_ENABLE_BT = 1
    // Initializes Bluetooth adapter.
//    val bluetoothManager = getSystemService(BluetoothManager::class.java)
//    val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private val bleScanner by lazy{
        bluetoothAdapter.bluetoothLeScanner
    }

    val bleServiceUuid = UUID.fromString("0000FFE0-0000-1000-8000-00805F9B34FB")
    val bleCharacteristicUuid = UUID.fromString("0000FFE1-0000-1000-8000-00805F9B34FB")

    val CCC_DESCRIPTOR_UUID = "00002902-0000-1000-8000-00805F9B34FB"

    var p1 = ""
    var p2 = ""
    var p3 = ""

    val SMSMan:SmsManager = SmsManager.getDefault()

    private val scanSettings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_BALANCED).build()

    private val bluetoothAdapter: BluetoothAdapter by lazy(LazyThreadSafetyMode.NONE) {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val indexQuery = scanResults.indexOfFirst { it.device.address == result.device.address }
            if (indexQuery != -1) { // A scan result already exists with the same address
                scanResults[indexQuery] = result
                scanResultAdapter.notifyItemChanged(indexQuery)
            } else {
                with(result.device) {
                    Log.i("ScanCallback", "Found BLE device! Name: ${name ?: "Unnamed"}, address: $address")
                }
                scanResults.add(result)
                scanResultAdapter.notifyItemInserted(scanResults.size - 1)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e("ScanCallback", "onScanFailed: code $errorCode")
        }
    }

    private var isScanning = false
        set(value) {
            field = value
            runOnUiThread { connect_bluetooth_button.text = if (value) "Stop Scan" else "Connect Bluetooth" }
        }

    //private var mScanning: Boolean = false


    private val scanResults = mutableListOf<ScanResult>()
    private val scanResultAdapter: ScanResultAdapter by lazy {
        ScanResultAdapter(scanResults) { result ->
            // User tapped on a scan result
            if (isScanning) {
                stopBleScan()
            }
            with(result.device) {
                Log.w("ScanResultAdapter", "Connecting to $address")
                connectGatt(this@SMSBluetoothActivity, false, gattCallback)
            }
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val deviceAddress = gatt.device.address

            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.w("BluetoothGattCallback", "Successfully connected to $deviceAddress")
                    bluetoothGatt = gatt
                    Handler(Looper.getMainLooper()).post {
                        bluetoothGatt?.discoverServices()
                    }

                    Timer("WAITforServices", false).schedule(1000) {
                        val bleservice = gatt.getService(bleServiceUuid)
                        if (bleservice == null)
                        {
                            Log.w("BluetoothGattCallback", "Unable to connect to $deviceAddress Service ID UUID $bleServiceUuid")
                        }
                        else
                        {
                            Log.w("BluetoothGattCallback", "Connected to $deviceAddress Service ID UUID $bleServiceUuid")
                        }
                        val blecharacteristic = bleservice.getCharacteristic(bleCharacteristicUuid)
                        if (blecharacteristic == null)
                        {
                            Log.w("BluetoothGattCallback", "Unable to connect to $deviceAddress Characteristic ID UUID $bleCharacteristicUuid")
                        }
                        else
                        {
                            Log.w("BluetoothGattCallback", "Connected to $deviceAddress Characteristic ID UUID $bleCharacteristicUuid")
                        }
                        enableNotifications(blecharacteristic)
                    }
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.w("BluetoothGattCallback", "Successfully disconnected from $deviceAddress")
                    gatt.close()
                }
            } else {
                Log.w("BluetoothGattCallback", "Error $status encountered for $deviceAddress! Disconnecting...")
                gatt.close()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            with(gatt) {
                Log.w("BluetoothGattCallback", "Discovered ${services.size} services for ${device.address}")
                printGattTable() // See implementation just above this section
                // Consider connection setup as complete here
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic)
        {

            with(characteristic) {
                Log.i("BluetoothGattCallback", "Characteristic $uuid changed | value: ${value.toHexString()}")

//                val p1 = sharedPref.getString("Emergency-PhoneNum1", "17818030014")
//                Log.w("PhoneNum1", "Phone1 - $p1")
//
//                val p2 = sharedPref.getString("Emergency-PhoneNum2", "17818030014")
//                Log.w("PhoneNum2", "Phone2 - $p2")
//
//                val p3 = sharedPref.getString("Emergency-PhoneNum3", "17818030014")
//                Log.w("PhoneNum3", "Phone3 - $p3")
                val eDate = Date()
                val datef = SimpleDateFormat("M/d/yyyy")
                val timef = SimpleDateFormat("H:mm")
                val date = datef.format(eDate)
                val time = timef.format(eDate)

                var filename = SimpleDateFormat("MDYYYY").format(eDate)
                var path = filesDir
                var newfile= File(path,filename)
                val isnewfile :Boolean = newfile.createNewFile()
                if (isnewfile){
                    openFileOutput(filename, Context.MODE_PRIVATE).use{
                        it.write(time.toByteArray())
                    }
                }else{
                    //open the file as a list
                    var list = File(path,filename).useLines { it.toList() }
                    list+=time

                }




                //Write data to file
                openFileOutput(filename, Context.MODE_PRIVATE).use{
                    it.write(date.toByteArray())
                }


                SMSMan.sendTextMessage(p1, null, "EMERGENCY! Shiver-Ring has detected trouble! Please assist the user.", null,null)
                SMSMan.sendTextMessage(p2, null, "EMERGENCY! Shiver-Ring has detected trouble! Please assist the user.", null,null)
                SMSMan.sendTextMessage(p3, null, "EMERGENCY! Shiver-Ring has detected trouble! Please assist the user.", null,null)
            }
        }
    }

    private fun BluetoothGatt.printGattTable() {
        if (services.isEmpty()) {
            Log.w("printGattTable", "No service and characteristic available, call discoverServices() first?")
            return
        }
        services.forEach { service ->
            val characteristicsTable = service.characteristics.joinToString(
                separator = "\n|--",
                prefix = "|--"
            ) { it.uuid.toString() }
            Log.w("printGattTable", "\nService ${service.uuid}\nCharacteristics:\n$characteristicsTable"
            )
        }
    }

    private var bluetoothGatt: BluetoothGatt? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_smsbluetooth)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        val hasSMSPermission = checkSelfPermission(Manifest.permission.SEND_SMS)
        val hasBluetoothPermission = checkSelfPermission(Manifest.permission.BLUETOOTH)
        val hasBluetoothAdminPermission = checkSelfPermission(Manifest.permission.BLUETOOTH_ADMIN)
        val hasCoarseLocationPermission = checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
        val hasFineLocationPermission = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val permissionstorequest: MutableList<String> = mutableListOf()
            if (hasSMSPermission != PackageManager.PERMISSION_GRANTED) {
                permissionstorequest.add(Manifest.permission.SEND_SMS)
            }
            if (hasBluetoothPermission != PackageManager.PERMISSION_GRANTED) {
                permissionstorequest.add(Manifest.permission.BLUETOOTH)
            }
            if (hasBluetoothAdminPermission!= PackageManager.PERMISSION_GRANTED) {
                permissionstorequest.add(Manifest.permission.BLUETOOTH_ADMIN)
            }
            if (hasCoarseLocationPermission != PackageManager.PERMISSION_GRANTED) {
                permissionstorequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
            if (hasFineLocationPermission != PackageManager.PERMISSION_GRANTED) {
                permissionstorequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            if(permissionstorequest.isNotEmpty()) {
                requestPermissions(permissionstorequest.toTypedArray(), REQUEST_CODE_NEW_FEATURE_PERMISSIONS)
            }
        }

        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)
        val phone1: EditText = findViewById(R.id.editText)
        val phone2: EditText = findViewById(R.id.editText2)
        val phone3: EditText = findViewById(R.id.editText3)

        val toggle = ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        navView.setNavigationItemSelectedListener(this)

        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }
        var file = File(filesDir,"contacts")
        val isnew :Boolean = file.createNewFile()
        val contacts = file.useLines { it.toString().toList() }
        val connectBluetoothButton: Button = findViewById(R.id.connect_bluetooth_button)
        connectBluetoothButton.setOnClickListener{
            if (bluetoothAdapter.isEnabled) {
                if (isScanning) {
                    stopBleScan()
                } else {
                    scanResults.clear()
                    scanResultAdapter.notifyDataSetChanged()
                    bleScanner.startScan(null, scanSettings,scanCallback)
                    isScanning=true
                }
            }
            else
            {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
            }
        }

        val sendSMSButton: Button = findViewById(R.id.sms_button)
        sendSMSButton.setOnClickListener{
            val sharedPref = getSharedPreferences("ShiverRingShared", Context.MODE_PRIVATE)
            var sharedPrefEditor = sharedPref.edit()
            sharedPrefEditor.putString("PhoneNum1",phone1.text.toString())
            sharedPrefEditor.putString("PhoneNum2",phone2.text.toString())
            sharedPrefEditor.putString("PhoneNum3",phone3.text.toString())

            sharedPrefEditor.apply()

            p1 = sharedPref.getString("PhoneNum1", "17818030014").toString()
            Log.w("PhoneNum1", "Phone1 - $p1")

            p2 = sharedPref.getString("PhoneNum2", "17818030014").toString()
            Log.w("PhoneNum2", "Phone2 - $p2")

            p3 = sharedPref.getString("PhoneNum3", "17818030014").toString()
            Log.w("PhoneNum3", "Phone3 - $p3")

            SMSMan.sendTextMessage(p1, null, "TESTING! Shiver-Ring has been setup.", null,null)
            SMSMan.sendTextMessage(p2, null, "TESTING! Shiver-Ring has been setup.", null,null)
            SMSMan.sendTextMessage(p3, null, "TESTING! Shiver-Ring has been setup.", null,null)
            val contacts = arrayListOf<String>(p1,p2,p3)
            var file = File(filesDir,"contacts")
            val isnew :Boolean = file.createNewFile()
            openFileOutput("contacts", Context.MODE_PRIVATE).use{
                it.write(contacts.toString().toByteArray())}
        }
        setupRecyclerView()
    }

    private fun stopBleScan() {
        bleScanner.stopScan(scanCallback)
        isScanning = false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    private fun setupRecyclerView() {
        scan_results_recycler_view.apply {
            adapter = scanResultAdapter
            layoutManager = LinearLayoutManager(
                this@SMSBluetoothActivity,
                RecyclerView.VERTICAL,
                false
            )
            isNestedScrollingEnabled = false
        }

        val animator = scan_results_recycler_view.itemAnimator
        if (animator is SimpleItemAnimator) {
            animator.supportsChangeAnimations = false
        }
    }

    fun BluetoothGattCharacteristic.isIndicatable(): Boolean = containsProperty(BluetoothGattCharacteristic.PROPERTY_INDICATE)

    fun BluetoothGattCharacteristic.isNotifiable(): Boolean = containsProperty(BluetoothGattCharacteristic.PROPERTY_NOTIFY)

    fun BluetoothGattCharacteristic.isReadable(): Boolean = containsProperty(BluetoothGattCharacteristic.PROPERTY_READ)

    fun BluetoothGattCharacteristic.isWritable(): Boolean = containsProperty(BluetoothGattCharacteristic.PROPERTY_WRITE)

    fun BluetoothGattCharacteristic.isWritableWithoutResponse(): Boolean = containsProperty(BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)

    fun BluetoothGattCharacteristic.containsProperty(property: Int): Boolean {
        return properties and property != 0
    }

    private fun writeDescriptor(descriptor: BluetoothGattDescriptor, payload: ByteArray) {
        bluetoothGatt?.let { gatt ->
            descriptor.value = payload
            gatt.writeDescriptor(descriptor)
        } ?: error("Not connected to a BLE device!")
    }

    fun enableNotifications(characteristic: BluetoothGattCharacteristic) {
        val cccdUuid = UUID.fromString(CCC_DESCRIPTOR_UUID)
        val payload = when {
            //characteristic.isIndicatable() -> BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
            characteristic.isNotifiable() -> BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            else -> {
                Log.e("ConnectionManager", "${characteristic.uuid} doesn't support notifications/indications")
                return
            }
        }

        characteristic.getDescriptor(cccdUuid)?.let { cccDescriptor ->
            if (bluetoothGatt?.setCharacteristicNotification(characteristic, true) == false) {
                Log.e("ConnectionManager", "setCharacteristicNotification failed for ${characteristic.uuid}")
                return
            }
            writeDescriptor(cccDescriptor, payload)
        } ?: Log.e("ConnectionManager", "${characteristic.uuid} doesn't contain the CCC descriptor!")
    }

    fun disableNotifications(characteristic: BluetoothGattCharacteristic) {
        if (!characteristic.isNotifiable() || !characteristic.isIndicatable()) {
            Log.e("ConnectionManager", "${characteristic.uuid} doesn't support indications/notifications")
            return
        }

        val cccdUuid = UUID.fromString(CCC_DESCRIPTOR_UUID)
        characteristic.getDescriptor(cccdUuid)?.let { cccDescriptor ->
            if (bluetoothGatt?.setCharacteristicNotification(characteristic, false) == false) {
                Log.e("ConnectionManager", "setCharacteristicNotification failed for ${characteristic.uuid}")
                return
            }
            writeDescriptor(cccDescriptor, BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE)
        } ?: Log.e("ConnectionManager", "${characteristic.uuid} doesn't contain the CCC descriptor!")
    }

    fun ByteArray.toHexString(): String =
        joinToString(separator = " ", prefix = "0x") { String.format("%02X", it) }

//    private fun scanLeDevice(enable: Boolean) {
//        when (enable) {
//            true -> {
//                // Stops scanning after a pre-defined scan period.
//                Handler().postDelayed({
//                    mScanning = false
//                    bluetoothAdapter?.bluetoothLeScanner?.stopScan(mLeScanCallback)
//                }, 5000)
//                mScanning = true
//                bluetoothAdapter?.bluetoothLeScanner?.startScan(mLeScanCallback)
//            }
//            else -> {
//                mScanning = false
//                bluetoothAdapter?.bluetoothLeScanner?.stopScan(mLeScanCallback)
//            }
//        }
//
//    }
//
//    private var mLeScanCallback: ScanCallback =
//        object : ScanCallback() {
//            override fun onScanResult(callbackType: Int, result: ScanResult?) {
//                super.onScanResult(callbackType, result)
//                tvTestNote.text = getString(R.string.found_ble_device)
//            }
//
//            override fun onBatchScanResults(results: List<ScanResult?>?) {
//                super.onBatchScanResults(results)
//                tvTestNote.text = getString(R.string.found_ble_devices)
//
//            }
//
//            override fun onScanFailed(errorCode: Int) {
//                super.onScanFailed(errorCode)
//                tvTestNote.text = getString(R.string.ble_device_scan_failed)+ errorCode
//
//            }
//        }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        when (item.itemId) {
            R.id.nav_calender -> {
                val calendarIntent = Intent(applicationContext, CalendarActivity::class.java)
                startActivity(calendarIntent)
            }
            R.id.nav_smsbluetooth-> {
                val smsIntent = Intent(applicationContext, SMSBluetoothActivity::class.java)
                startActivity(smsIntent)
            }
        }
        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

}
