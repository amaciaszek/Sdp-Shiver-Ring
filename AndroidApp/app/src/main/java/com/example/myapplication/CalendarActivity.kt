package com.example.myapplication

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.CalendarView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import kotlinx.android.synthetic.main.content_calendar.*
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FileReader
import java.text.SimpleDateFormat
import java.util.*

class CalendarActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private val REQUEST_CODE_NEW_FEATURE_PERMISSIONS = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendar)
        val toolbar: Toolbar = findViewById(R.id.toolbar2)
        setSupportActionBar(toolbar)
        val calendar = findViewById<CalendarView>(R.id.calendarView)

        //val hasWritePermission = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        //val hasReadPermission = checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val permissionstorequest: MutableList<String> = mutableListOf()

            if(permissionstorequest.isNotEmpty()) {
                requestPermissions(permissionstorequest.toTypedArray(), REQUEST_CODE_NEW_FEATURE_PERMISSIONS)
            }
        }

        var path = filesDir



        val eDate = Date()
        val datef = SimpleDateFormat("M/d/yyyy")
        val timef = SimpleDateFormat("H:mm")
        var filename = SimpleDateFormat("MDYYYY").format(eDate)
        val date = datef.format(eDate)


        var file = File(path,filename)
        val isnewfile :Boolean = file.createNewFile()
        val list = file.useLines { it.toList() }
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, list)
        listview.adapter = adapter



        calendar.setOnDateChangeListener(CalendarView.OnDateChangeListener { _, year, month , day ->
            val month= month +1
            val filename = "$month$day$year"

            //var filename = SimpleDateFormat("MDYYYY").format(sdate)

            var file = File(path,filename)
            val isnewfile :Boolean = file.createNewFile()
            val list = file.useLines { it.toList() }
            val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, list)
            listview.adapter = adapter


        })





        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)
        val toggle = ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        navView.setNavigationItemSelectedListener(this)


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