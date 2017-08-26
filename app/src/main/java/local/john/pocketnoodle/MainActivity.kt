package local.john.pocketnoodle

import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v7.app.AlertDialog
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import local.john.pocketnoodle.Util.DatePickerFragment
import local.john.pocketnoodle.Util.FtpClient
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

internal class MainActivity : AppCompatActivity() {

    private var snakeName: String = DEFAULT_SNAKE_NAME
        set(value) {
            textSnakeName.text = value
            field = value
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        loadData()

        buttonFeed.setOnClickListener { confirmYesNo { updateField(TYPE_FEED) } }
        buttonFeed.setOnLongClickListener { addDate(TYPE_FEED) }

        buttonShed.setOnClickListener { confirmYesNo { updateField(TYPE_SHED) } }
        buttonShed.setOnLongClickListener { addDate(TYPE_SHED) }

        buttonSync.setOnClickListener {
            val dialogClickListener = DialogInterface.OnClickListener { _, choice ->
                when (choice) {
                    DialogInterface.BUTTON_POSITIVE -> { save() }
                    DialogInterface.BUTTON_NEGATIVE -> { load() }
                }
            }
            AlertDialog.Builder(this)
                    .setMessage("Push or Pull?")
                    .setPositiveButton("Push", dialogClickListener)
                    .setNegativeButton("Pull", dialogClickListener)
                    .setCancelable(true)
                    .show()
        }

        buttonSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        buttonFeedLog.setOnClickListener {
            startActivityForResult(Intent(this,
                    LogActivity::class.java).putExtra("type", TYPE_FEED), TYPE_FEED)
        }
        buttonShedLog.setOnClickListener {
            startActivityForResult(Intent(this,
                    LogActivity::class.java).putExtra("type", TYPE_SHED), TYPE_SHED)
        }
    }

    override fun onResume() {
        super.onResume()

        loadData()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_settings, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.settings_menu_save -> {
                confirmYesNo { save() }
                true
            }
            R.id.settings_menu_load -> {
                val dialogClickListener = DialogInterface.OnClickListener { _, choice ->
                    when (choice) {
                        DialogInterface.BUTTON_POSITIVE -> { confirmYesNo { load("PocketNoodle - Monty Python.json") } }
                        DialogInterface.BUTTON_NEGATIVE -> { confirmYesNo { load("PocketNoodle - Ramen Noodle.json") } }
                        DialogInterface.BUTTON_NEUTRAL -> {  }
                    }
                }
                AlertDialog.Builder(this)
                        .setMessage("Quick Load Preset")
                        .setPositiveButton("Monty Python", dialogClickListener)
                        .setNegativeButton("Ramen Noodle", dialogClickListener)
                        .setNeutralButton("Cancel", dialogClickListener)
                        .setCancelable(true)
                        .show()
                true
            }
            R.id.setting_menu_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            R.id.settings_menu_reset -> {
                confirmYesNo {
                    doReset()
                    loadData()
                }
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    private fun loadData() {
        sharedPref = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        feedDates.clear()
        shedDates.clear()

        if(checkForPrefs()) {
            snakeName = sharedPref.getString("snake_name", DEFAULT_SNAKE_NAME)
            if(sharedPref.contains("feeds"))
                feedDates.addAll(sharedPref.getStringSet("feeds", mutableSetOf())
                        .map { dateFormatIn.parse(it) }.sorted() )
            if(sharedPref.contains("sheds"))
                shedDates.addAll(sharedPref.getStringSet("sheds", mutableSetOf())
                        .map { dateFormatIn.parse(it) }.sorted())
        }
        else
            sharedPref.edit().putString("snake_name", DEFAULT_SNAKE_NAME).apply()

        updateDateViews()
    }

    private fun confirmYesNo(operation: () -> Unit) {
        val dialogClickListener = DialogInterface.OnClickListener { _, choice ->
            when (choice) {
                DialogInterface.BUTTON_POSITIVE -> {
                    operation()
                }
                else -> {  }
            }
        }
        AlertDialog.Builder(this)
                .setMessage("Are you sure?")
                .setPositiveButton("Yes", dialogClickListener)
                .setNegativeButton("No", dialogClickListener)
                .show()
    }

    private fun addDate(type: Int): Boolean {
        DatePickerFragment({
            confirmYesNo { updateField(type, dateFormatIn.parse(it)) }
        }).show(fragmentManager, "datePicker")

        return true
    }

    private fun updateField(field: Int, date: Date = dateFormatIn.parse(dateFormatIn.format(Date()))) {
        val editor = sharedPref.edit()

        if(field == TYPE_FEED) {
            feedDates.add(date)
            editor.putStringSet("feeds", feedDates.map { dateFormatIn.format(it) }.toMutableSet())
        }
        else if(field == TYPE_SHED) {
            shedDates.add(date)
            editor.putStringSet("sheds", shedDates.map { dateFormatIn.format(it) }.toMutableSet())
        }

        editor.apply()
        updateDateViews()
    }

    private fun updateDateViews() {
        val lastFeed = feedDates.sorted().lastOrNull()
        val lastShed = shedDates.sorted().lastOrNull()

        lastFeedDate.setText(
                if(lastFeed != null) dateFormatOut.format(lastFeed)
                else "N/A")
        lastShedDate.setText(
                if(lastShed != null) dateFormatOut.format(lastShed)
                else "N/A")
    }

    private fun checkForPrefs() = sharedPref.all.isNotEmpty() && sharedPref.contains("snake_name")

    private fun doReset() {
        feedDates.clear()
        shedDates.clear()
        sharedPref.edit()
                .clear()
                .putString("snake_name", DEFAULT_SNAKE_NAME)
                .putString("ip_address", DEFAULT_IP_ADDRESS)
                .putString("filename", DEFAULT_FILE_NAME)
                .putString("remote_path", DEFAULT_REMOTE_PATH)
                .putString("user", DEFAULT_USER)
                .putString("password", DEFAULT_PASSWORD)
                .putStringSet("feeds", mutableSetOf())
                .putStringSet("sheds", mutableSetOf())
                .apply()

    }

    private fun save() {
        FtpClient(sharedPref.getString("ip_address", DEFAULT_IP_ADDRESS),
                sharedPref.getString("user", DEFAULT_USER),
                sharedPref.getString("pass", DEFAULT_PASSWORD))
                .sync(sharedPref.getString("remote_path", DEFAULT_REMOTE_PATH),
                        sharedPref.getString("filename", DEFAULT_FILE_NAME), getSaveStream()) {
                    runOnUiThread({
                        Toast.makeText(applicationContext,
                                if(it == "1") "File pushed to server."
                                else "Unable to push file to server.", Toast.LENGTH_SHORT).show()
                    })
                }
    }

    private fun load(name: String = sharedPref.getString("filename", DEFAULT_FILE_NAME)) {
        FtpClient(sharedPref.getString("ip_address", DEFAULT_IP_ADDRESS),
                sharedPref.getString("user", DEFAULT_USER),
                sharedPref.getString("pass", DEFAULT_PASSWORD))
                .sync(sharedPref.getString("remote_path", DEFAULT_REMOTE_PATH),name) {
                    runOnUiThread({
                        if (it == "0")
                            Toast.makeText(applicationContext, "Unable to load file from server.", Toast.LENGTH_SHORT).show()
                        else {
                            Toast.makeText(applicationContext, "File pulled from server.", Toast.LENGTH_SHORT).show()
                            try {
                                parseJson(JSONObject(JSONTokener(it)))
                            } catch (e: Exception) {
                                Toast.makeText(applicationContext, "Error parsing file.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    })
                }
    }

    private fun getSaveStream(): InputStream {
        return JSONObject()
                .put("snake_name", snakeName)
                .put("ip_address", sharedPref.getString("ip_address", DEFAULT_IP_ADDRESS))
                .put("filename", sharedPref.getString("filename", DEFAULT_FILE_NAME))
                .put("remote_path", sharedPref.getString("remote_path", DEFAULT_REMOTE_PATH))
                .put("user", sharedPref.getString("user", DEFAULT_USER))
                .put("pass", sharedPref.getString("pass", DEFAULT_PASSWORD))
                .put("feeds", JSONArray(feedDates.map { dateFormatIn.format(it) }))
                .put("sheds", JSONArray(shedDates.map { dateFormatIn.format(it) }))
                .toString().byteInputStream()
    }

    private fun parseJson(data: JSONObject) {
        val snake = data.getString("snake_name")
        val ip_address = data.getString("ip_address")
        val filename = data.getString("filename")
        val remote_path = data.getString("remote_path")
        val user = data.getString("user")
        val pass = data.getString("pass")
        val feeds = data.getJSONArray("feeds")
        val sheds = data.getJSONArray("sheds")

        val feedSet = mutableSetOf<Date>()
        val shedSet = mutableSetOf<Date>()

        var pos = 0
        while(!feeds.isNull(pos)) feedSet.add(dateFormatIn.parse(feeds.getString(pos++)))

        pos = 0
        while(!sheds.isNull(pos)) shedSet.add(dateFormatIn.parse(sheds.getString(pos++)))

        saveData(snake, ip_address, filename, remote_path, user, pass, feedSet, shedSet)
    }

    private fun saveData(snake: String, ip: String, filename: String, remote_path: String, user: String,
                 pass: String, feeds: Set<Date>, sheds: Set<Date>) {
        sharedPref.edit()
                .putString("snake_name", snake)
                .putString("ip_address", ip)
                .putString("filename", filename)
                .putString("remote_path", remote_path)
                .putString("user", user)
                .putString("pass", pass)
                .putStringSet("feeds", feeds.map { dateFormatIn.format(it) }.toSet())
                .putStringSet("sheds", sheds.map { dateFormatIn.format(it) }.toSet())
                .apply()

        loadData()
    }

    internal companion object {
        private lateinit var sharedPref: SharedPreferences

        private var feedDates = mutableSetOf<Date>()
        private var shedDates = mutableSetOf<Date>()

        internal val dateFormatIn = SimpleDateFormat("MM-dd-yy", Locale.US)
        internal val dateFormatOut = SimpleDateFormat("EE MM/dd/yy", Locale.US)

        private val DEFAULT_SNAKE_NAME = "Noodle"
        private val DEFAULT_IP_ADDRESS = "192.168.0.5"
        private val DEFAULT_FILE_NAME = "Pocket Noodle.json"
        private val DEFAULT_REMOTE_PATH = "/Documents/"
        private val DEFAULT_USER = "anon"
        private val DEFAULT_PASSWORD = ""

        internal val TYPE_FEED = 0
        internal val TYPE_SHED = 1
    }
}
