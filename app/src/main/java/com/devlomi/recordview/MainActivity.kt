package com.devlomi.recordview

import android.Manifest
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.devlomi.record_view.OnBasketAnimationEnd
import com.devlomi.record_view.OnRecordClickListener
import com.devlomi.record_view.OnRecordListener
import com.devlomi.record_view.RecordButton
import com.devlomi.record_view.RecordPermissionHandler
import com.devlomi.record_view.RecordView
import java.io.File
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private var audioRecorder: AudioRecorder? = null
    private var recordFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        audioRecorder = AudioRecorder()

        val recordView = findViewById<RecordView>(R.id.record_view)
        val recordButton = findViewById<RecordButton>(R.id.record_button)
        val btnChangeOnclick = findViewById<Button>(R.id.btn_change_onclick)

        // To Enable Record Lock
//        recordView.setLockEnabled(true);
//        recordView.setRecordLockImageView(findViewById(R.id.record_lock));
        //IMPORTANT
        recordButton.setRecordView(recordView)

        // if you want to click the button (in case if you want to make the record button a Send Button for example..)
//        recordButton.setListenForRecord(false);
        btnChangeOnclick.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                if (recordButton.isListenForRecord) {
                    recordButton.isListenForRecord = false
                    Toast.makeText(this@MainActivity, "onClickEnabled", Toast.LENGTH_SHORT).show()
                } else {
                    recordButton.isListenForRecord = true
                    Toast.makeText(this@MainActivity, "onClickDisabled", Toast.LENGTH_SHORT).show()
                }
            }
        })

        //ListenForRecord must be false ,otherwise onClick will not be called
        recordButton.setOnRecordClickListener(object : OnRecordClickListener {
            override fun onClick(v: View?) {
                Toast.makeText(this@MainActivity, "RECORD BUTTON CLICKED", Toast.LENGTH_SHORT)
                    .show()
                Log.d("RecordButton", "RECORD BUTTON CLICKED")
            }
        })


        //Cancel Bounds is when the Slide To Cancel text gets before the timer . default is 8
        recordView.setCancelBounds(8f)


        recordView.setSmallMicColor(Color.parseColor("#c2185b"))

        //prevent recording under one Second
        recordView.setLessThanSecondAllowed(false)


        recordView.setSlideToCancelText("Slide To Cancel")


        // recordView.setCustomSounds(R.raw.record_start, R.raw.record_finished, 0);
        recordView.setOnRecordListener(object : OnRecordListener {
            override fun onStart() {
                recordFile = File(getFilesDir(), UUID.randomUUID().toString() + ".3gp")
                try {
                    audioRecorder!!.start(recordFile!!.getPath())
                } catch (e: IOException) {
                    e.printStackTrace()
                }
                Log.d("RecordView", "onStart")
                Toast.makeText(this@MainActivity, "OnStartRecord", Toast.LENGTH_SHORT).show()
            }

            override fun onCancel() {
                stopRecording(true)

                Toast.makeText(this@MainActivity, "onCancel", Toast.LENGTH_SHORT).show()

                Log.d("RecordView", "onCancel")
            }

            override fun onFinish(recordTime: Long, limitReached: Boolean) {
                stopRecording(false)


                val time = getHumanTimeText(recordTime)
                Toast.makeText(
                    this@MainActivity,
                    "onFinishRecord - Recorded Time is: " + time + " File saved at " + recordFile!!.getPath(),
                    Toast.LENGTH_SHORT
                ).show()
                Log.d("RecordView", "onFinish" + " Limit Reached? " + limitReached)
                Log.d("RecordTime", time)
            }

            override fun onLessThanSecond() {
                stopRecording(true)

                Toast.makeText(this@MainActivity, "OnLessThanSecond", Toast.LENGTH_SHORT).show()
                Log.d("RecordView", "onLessThanSecond")
            }

            override fun onLock() {
                Toast.makeText(this@MainActivity, "onLock", Toast.LENGTH_SHORT).show()
                Log.d("RecordView", "onLock")
            }
        })


        recordView.setOnBasketAnimationEndListener(object : OnBasketAnimationEnd {
            override fun onAnimationEnd() {
                Log.d("RecordView", "Basket Animation Finished")
            }
        })

        recordView.setRecordPermissionHandler(object : RecordPermissionHandler {
            override val isPermissionGranted: Boolean
                get() {
                    val recordPermissionAvailable = ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.RECORD_AUDIO
                    ) == PermissionChecker.PERMISSION_GRANTED
                    if (recordPermissionAvailable) {
                        return true
                    }


                    ActivityCompat.requestPermissions(this@MainActivity,
                        arrayOf<String>(Manifest.permission.RECORD_AUDIO),
                        0)

                    return false
                }
        })
    }

    private fun stopRecording(deleteFile: Boolean) {
        audioRecorder!!.stop()
        if (recordFile != null && deleteFile) {
            recordFile!!.delete()
        }
    }


    private fun getHumanTimeText(milliseconds: Long): String {
        return String.format(
            "%02d:%02d",
            TimeUnit.MILLISECONDS.toMinutes(milliseconds),
            TimeUnit.MILLISECONDS.toSeconds(milliseconds) -
                    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(milliseconds))
        )
    }
}
