package snakeinmyboot.soundboard

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.support.design.widget.Snackbar
import android.support.design.widget.NavigationView
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.content_main.*
import java.io.IOException
import android.R.attr.fragment
import android.app.Dialog
import android.content.Intent
import android.media.MediaPlayer
import android.support.v7.app.AlertDialog
import android.text.Editable
import android.text.TextUtils.replace
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.nav_header_main.*
import java.io.File


class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener, BoardAdapter.BoardMediaInterface {

    private var current = "default"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)


        val toggle = ActionBarDrawerToggle(
            this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        nav_view.setNavigationItemSelectedListener(this)

        launch_recording.setOnClickListener({
            val intent = Intent(this, RecordingActivity::class.java)
            intent.putExtra("base", current)
            startActivity(intent)
        })

        board_search.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
                addAdapter()
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }
        })

        directoryActions()
        addAdapter()

    }

    fun directoryActions() {
        if(isPermitted()) {
            //initial directory
            val rootdir = File(Environment.getExternalStorageDirectory().toString() + "/soundboard")
            if (!rootdir.exists()) {
                rootdir.mkdir()
            }
            val defdir = File(Environment.getExternalStorageDirectory().toString() + "/soundboard/default")
            if (!defdir.exists()) {
                defdir.mkdir()
            }
            if (rootdir.exists()) {
                rootdir.walk().forEach {
                    if(it.isDirectory) {
                        if(it.toString() != Environment.getExternalStorageDirectory().toString()+"/soundboard") {
                            Log.d("DirectoryDebug", it.toString())
                            var dir = it.toString().replace(Environment.getExternalStorageDirectory().toString() + "/soundboard/", "").substringBefore('.')
                            nav_view.menu.add(dir)
                        }
                    }
                }
            }
        }

    }

    fun addAdapter() {
        val sounds = mutableListOf<String>()
        val file = File(Environment.getExternalStorageDirectory().toString() + "/soundboard/"+current+"/")
        if (file.exists()) {
            if(board_search.text.toString() == "") {
                file.walk().forEach {
                    if(it.toString() != Environment.getExternalStorageDirectory().toString()+"/soundboard/"+current) {
                        Log.d("FilesDebug", it.toString())
                        sounds.add(it.toString())
                    }
                }
            }
            else {
                val words = board_search.text.split(" ")
                file.walk().forEach {
                    if(it.toString() != Environment.getExternalStorageDirectory().toString()+"/soundboard/"+current) {
                        for(word: String in words) {
                            if(word == "") {
                                continue
                            }
                            if (it.toString().toLowerCase().contains(word.toLowerCase())) {
                                Log.d("FilesDebug", it.toString())
                                sounds.add(it.toString())
                                break
                            }
                        }
                    }
                }
            }
        }
        val baseurl = file.toString()+"/"
        board.adapter = BoardAdapter(this, sounds, this, baseurl)
    }

    fun isPermitted(): Boolean {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            val permissions = arrayOf(android.Manifest.permission.RECORD_AUDIO, android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.READ_EXTERNAL_STORAGE)
            ActivityCompat.requestPermissions(this, permissions,0)
            return false
        }
        else {
            return true
        }
    }

    fun createBoardDialog() {
        val builder = AlertDialog.Builder(this@MainActivity)

        builder.setTitle("Create New Soundboard")
        builder.setMessage("Soundboard Name")
        val editText = EditText(this)
        builder.setView(editText)
        builder.setPositiveButton("Create"){dialog, which ->
            Log.d("DialogDebug", editText.text.toString())
        }
        builder.setNeutralButton("Cancel") {dialog, which ->
            Log.d("DialogDebug", editText.text.toString())
        }
        val dialog: AlertDialog = builder.create()

        // Display the alert dialog on app interface
        dialog.show()
    }

    override fun playMedia(file: File) {
        val mp = MediaPlayer()
        mp.setDataSource(file.path)
        mp.prepare()
        mp.start()
        return
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        when (item.itemId) {
            R.id.action_settings -> return true
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        if(item.itemId == R.id.new_board) {
            createBoardDialog()
        }
        else {
            Log.d("NavDebug",item.toString())
        }

        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onResume() {
        super.onResume()

        addAdapter()
    }

    override fun onRestart() {
        super.onRestart()

        addAdapter()
    }
}
