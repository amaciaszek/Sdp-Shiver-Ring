package com.example.myapplication

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.MotionEvent
import android.widget.ArrayAdapter
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import kotlinx.android.synthetic.main.content_calendar.*

class CalendarActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private val REQUEST_CODE_NEW_FEATURE_PERMISSIONS = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendar)
        val toolbar: Toolbar = findViewById(R.id.toolbar2)
        setSupportActionBar(toolbar)

        //add in how the events are determined

        val eventList = arrayOf(0,1,2,4,5,6,7,8,9,10)
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1,eventList)
        listview.adapter = adapter



        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val permissionstorequest: MutableList<String> = mutableListOf()

            if(permissionstorequest.isNotEmpty()) {
                requestPermissions(permissionstorequest.toTypedArray(), REQUEST_CODE_NEW_FEATURE_PERMISSIONS)
            }
        }

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