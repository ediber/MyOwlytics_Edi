package com.e.myowlytics_edi

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.hardware.SensorEvent
import android.net.wifi.WifiManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.location.LocationServices
import java.io.FileOutputStream
import java.util.*
import kotlin.collections.ArrayList


class MeasureViewModel(private val application: Application) : ViewModel() {
    // TODO: Implement the ViewModel

    enum class Index {
        TIME, A_X, A_Y, A_Z, WIFI, GPS_ACCURANCY, GPS_ALTITUDE, GPS_LATITUDE, GPS_LONGTITUDE
    }

    companion object {
        const val SLEEP_TIME = 1000L
    }

    private val _fileRow = arrayOfNulls<String>(9)

    // accumulates all data, later can be added to file
    // data is saved every second or on AcceleratorUpdates, the more frequent between the two
    private var _data = "timestamp (milliseconds), accelerometer X, accelerometer Y, accelerometer Z, WiFiRSSI, GPS accuracy, GPS altitude, GPS latitude, GPS longitude\n"

    private val _wifiLive = MutableLiveData<Int>()
    val wifiLive: LiveData<Int>
    get() {
        return _wifiLive
    }

    // holds all accelerator z values
    private val _zList = ArrayList<Time_Z>()

    private val _soundLive = MutableLiveData<Boolean>()
    val soundLive: LiveData<Boolean>
      get() {
          return _soundLive
      }

    init {
      //  startTests()
    }

    fun startTests() {
        val thread = Thread(Runnable {

            while (true) {
                startWifiUpdates()
                saveGpsLocation()

                Thread.sleep(Companion.SLEEP_TIME)
            }
        })

        thread.start()
    }

    @SuppressLint("MissingPermission")
    fun saveGpsLocation() {

        val mFusedLocationClient  =
            LocationServices.getFusedLocationProviderClient(application);

        mFusedLocationClient.lastLocation.addOnSuccessListener()
        { location ->
            if (location != null) {
                var latitude = location.latitude
                var longitude = location.longitude
                var accuracy = location.accuracy
                var altitude = location.altitude

                _fileRow[Index.GPS_ALTITUDE.ordinal] = altitude.toString()
                _fileRow[Index.GPS_LATITUDE.ordinal] = latitude.toString()
                _fileRow[Index.GPS_LONGTITUDE.ordinal] = longitude.toString()
                _fileRow[Index.GPS_ACCURANCY.ordinal] = accuracy.toDouble().toString()
            }
        }
    }

    private fun startWifiUpdates() {
        val wifiManager = application.getSystemService(Context.WIFI_SERVICE) as WifiManager?
        val info = wifiManager!!.connectionInfo
        val rssi = info.rssi

        _fileRow[Index.WIFI.ordinal] = rssi.toString()
        _wifiLive.postValue(rssi)

    }


     fun onSensorChanged(event: SensorEvent?) {
        val x = event?.values?.get(0)
        val y = event?.values?.get(1)
        val z = event?.values?.get(2)

        _fileRow[Index.A_X.ordinal] = x!!.toString()
        _fileRow[Index.A_Y.ordinal] = y!!.toString()
        _fileRow[Index.A_Z.ordinal] = z!!.toString()

        val time = Calendar.getInstance().timeInMillis
        _fileRow[Index.TIME.ordinal] = time.toString()

        // update data String
        for (item in _fileRow) {
            _data = "$_data$item,"
        }
        _data += '\n'

        var average = twoSecondsAverage(z, time)

        // need to sound
        if(average > 4){
            // happens on main thread
            _soundLive.value = true
        }
    }


    // calculate average for the last 2 seconds
    private fun twoSecondsAverage(z: Float, time: Long): Double {
        _zList.add(Time_Z(time, z))
        var deltaTime = 0L
        var i = _zList.size - 1
        var total = 0.0
        var count = 0
        while (deltaTime <= 2000 && i >= 0){
            var pair = _zList[i]
            total += pair.z
            deltaTime = time - pair.time

            count ++
            i --
        }

        return total / count
    }

     fun createCSV() {

        try {
            //saving the file into device
            val out: FileOutputStream = application.openFileOutput("data.csv", Context.MODE_PRIVATE)
            out.write(_data.toByteArray())
            out.close()


        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    fun soundCreated() {
        _soundLive.value = false
    }

    data class Time_Z(
            var time: Long,
            var z: Float
    )


}