package snakeinmyboot.soundboard

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.PersistableBundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.android.synthetic.main.recording_activity.*
import java.io.IOException
import android.system.Os.mkdir
import android.util.Base64
import android.view.View
import com.google.gson.GsonBuilder
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.internal.schedulers.IoScheduler
import io.reactivex.schedulers.Schedulers
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.nio.file.Files.exists



class RecordingActivity: AppCompatActivity() {

    private val TAG = "RecordingDebug"


    private var output: String? = null
    private var mediaRecorder: MediaRecorder? = null
    private var running: Boolean = false
    private var recordingStopped: Boolean = false
    private var modeManual = true
    private var baseurl = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.recording_activity)
        val base = intent.getStringExtra("base")
        baseurl = Environment.getExternalStorageDirectory().toString() + "/soundboard/"+base+"/"

        mediaRecorder = MediaRecorder()

        recording_manual.setOnClickListener{
            modeManual = true
            recording_layout_manual.visibility = View.VISIBLE
            recording_layout_welcome.visibility = View.INVISIBLE
        }

        recording_auto.setOnClickListener{
            recording_layout_manual.visibility = View.VISIBLE
            recording_layout_welcome.visibility = View.INVISIBLE
            recording_text.visibility = View.GONE
            recording_layout_welcome.removeView(recording_text)
            modeManual = false
        }
        button_start_recording.setOnClickListener {
            if (modeManual && recording_text.text.toString() == "") {
                Toast.makeText(this, "Please add a filename.", Toast.LENGTH_SHORT).show()
            }
            else if (!running) {
                button_start_recording.text = "Stop"
                startRecording()
            }
            else {
                stopRecording()
            }
        }

        button_pause_recording.setOnClickListener {
            pauseRecording()
        }
    }

    private fun startRecording() {
        var file: File
        if(modeManual) {
            val fileName = recording_name.text.toString()
            recording_name.text.clear()
            file = File(baseurl+fileName+".mp3")
            if(file.exists()) {
                file.delete()
            }
            output = file.toString()
        } else {
            recording_name.text.clear()
            file = File(baseurl + "/soundboard/temp.mp3")
            if(file.exists()) {
                file.delete()
            }
            output = baseurl + "temp.mp3"
        }
        mediaRecorder?.setAudioSource(MediaRecorder.AudioSource.MIC)
        mediaRecorder?.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        mediaRecorder?.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        mediaRecorder?.setOutputFile(output)
        try {
            mediaRecorder?.prepare()
            mediaRecorder?.start()
            running = true
            Toast.makeText(this, "Recording started!", Toast.LENGTH_SHORT).show()
        } catch (e: IllegalStateException) {
            Log.d(TAG, e.toString())
        } catch (e: IOException) {
            Log.d(TAG, e.toString())
        }
    }

    @SuppressLint("RestrictedApi", "SetTextI18n")
    @TargetApi(Build.VERSION_CODES.N)
    private fun pauseRecording() {
        if(running) {
            if(!recordingStopped){
                Toast.makeText(this,"Stopped!", Toast.LENGTH_SHORT).show()
                mediaRecorder?.pause()
                recordingStopped = true
                button_pause_recording.text = "Resume"
            }else{
                resumeRecording()
            }
        }
    }

    @SuppressLint("RestrictedApi", "SetTextI18n")
    @TargetApi(Build.VERSION_CODES.N)
    private fun resumeRecording() {
        Toast.makeText(this,"Resume!", Toast.LENGTH_SHORT).show()
        mediaRecorder?.resume()
        button_pause_recording.text = "Pause"
        recordingStopped = false
    }

    private fun stopRecording(){
        if(running){
            mediaRecorder?.stop()
            mediaRecorder?.release()
            running = false
            Toast.makeText(this, "Recording completed!", Toast.LENGTH_SHORT).show()
            finish()
            if(!modeManual) {
                sendAudio(output as String)
            }
        }else{
            Toast.makeText(this, "You are not recording right now!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendAudio(fileName: String) {
        var file = File(fileName)
        var fileContents = File(fileName).readBytes()
        var encoded = Base64.encodeToString(fileContents, Base64.DEFAULT)
        Log.d(TAG, encoded)

        val retrofit = Retrofit.Builder().addConverterFactory(GsonConverterFactory.create(GsonBuilder().create()))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .baseUrl(getString(R.string.url)).build()

        val soundAPI = retrofit.create(WebInterface.SoundAPI::class.java)

        //Log.d(TAG, "Attempting to send.")
        var response = soundAPI.postAudio(WebInterface.Data("file", encoded))

        response.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { response -> Log.d(TAG, response.toString())
                },
                { error ->
                    Toast.makeText(this, error.message,Toast.LENGTH_SHORT).show()
                }
            )

        file.delete()
    }
}