@file:Suppress("DEPRECATION")

package com.e.myowlytics_edi

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.android.synthetic.main.measure_fragment.*
import java.io.File

// TODO
//////////////////////////////////////////////////////////////////////////////////////////////////////
// to improve
// 1 send data to server
// save it with room DB
// retrieve data from CSV file
// 2 do it in the background
// 3 add gyroscope
// add better UI
// use string resource
// make shorter sound
//////////////////////////////////////////////////////////////////////////////////////////////////////


class MeasureFragment : Fragment(), SensorEventListener {

    companion object {
        fun newInstance() = MeasureFragment()
    }

    private lateinit var _sensorManager: SensorManager

    private val _viewModel: MeasureViewModel by lazy {
        val activity = requireNotNull(this.activity) {
            "You can only access the viewModel after onActivityCreated()"
        }
        ViewModelProviders.of(this, MeasureViewModelFactory(activity.application))
                .get(MeasureViewModel::class.java)
    }

    private lateinit var mFusedLocationClient: FusedLocationProviderClient

    private val locationRequestCode = 1000



    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.measure_fragment, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _sensorManager = context?.getSystemService(Context.SENSOR_SERVICE) as SensorManager

        _viewModel.startTests()
        startLocationPermission()

        create_csv.setOnClickListener(View.OnClickListener {
            _viewModel.createCSV()
        })

        send_csv.setOnClickListener(View.OnClickListener {
            sendCSV()
        })

        _viewModel.wifiLive.observe(viewLifecycleOwner, Observer {
            var drawabalePath: Int
            var text:String

            if(it > -50){
                text = "excellent"
                drawabalePath = R.drawable.excelent
            } else if(  it > -60 && it <= -50){
                text = "good"
                drawabalePath = R.drawable.good
            } else if(  it > -70 && it <= -60){
                text = "fair"
                drawabalePath = R.drawable.fair
            } else {
                text = "weak"
                drawabalePath = R.drawable.weak
            }
            wifi_image.setImageDrawable(
                ContextCompat.getDrawable(requireContext(), drawabalePath)
            )
            wifi_text.text = text
        })

        _viewModel.soundLive.observe(viewLifecycleOwner, Observer {
            if(it){
                makeSound()
                _viewModel.soundCreated()
            }
        })


    }

    override fun onResume() {
        super.onResume()

        startAcceleratorUpdates()
    }

    override fun onPause() {
        super.onPause()

        _sensorManager.unregisterListener(this);
    }

    private fun startAcceleratorUpdates() {

        val sensorAcceleration: Sensor? = _sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)

        _sensorManager.registerListener(
            this, sensorAcceleration,
            SensorManager.SENSOR_DELAY_NORMAL
        )
    }

    private fun startLocationPermission() {

        mFusedLocationClient  =
            LocationServices.getFusedLocationProviderClient(requireContext());

        // ask permission
        if (ActivityCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
        ) {
            if(shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)){
                Toast.makeText(context, "give me permission now !!!", Toast.LENGTH_LONG).show()
            }

            requestPermissions(

                    arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    ),
                    locationRequestCode
            )
        } else {
            // already permission granted
            _viewModel.saveGpsLocation()
        }
    }

    @SuppressLint("MissingPermission")
    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<String?>,
            grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            1000 -> {

                // If request is cancelled, the result arrays are empty.
                if (grantResults.size > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED
                ) {
                    _viewModel.saveGpsLocation()
                } else {
                    Toast.makeText(context, "Permission denied location", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun sendCSV() {
        //exporting
         val context: Context = requireContext()
        val filelocation: File = File(context.filesDir, "data.csv")
        val path: Uri = FileProvider.getUriForFile(context, "com.e.myowlytics_edi.fileprovider", filelocation)
        val fileIntent = Intent(Intent.ACTION_SEND)
        fileIntent.type = "text/csv"
        fileIntent.putExtra(Intent.EXTRA_SUBJECT, "Data")
        fileIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        fileIntent.putExtra(Intent.EXTRA_STREAM, path)
       // fileIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK;
        startActivity(Intent.createChooser(fileIntent, "Send mail"))
    }


    private fun makeSound() {
        val mp: MediaPlayer = MediaPlayer.create(requireContext(), R.raw.birds_sms)
        mp.start()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        _viewModel.onSensorChanged(event)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

    }

}

