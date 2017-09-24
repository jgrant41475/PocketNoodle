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
import kotlinx.android.synthetic.main.activity_main.*
import local.john.pocketnoodle.Util.DatePickerFragment
import local.john.pocketnoodle.Util.Snake
import local.john.pocketnoodle.Util.Snakes
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.*

internal class MainActivity : AppCompatActivity() {

    private var sharedPref: SharedPreferences? = null
        get() = field?.let { it } ?: loadPreferences()

    private var profile = DEFAULT_SNAKE
        set(value) {
            field = value
            loadProfile(value)
            sharedPref?.edit()
                    ?.putString("last_loaded", value)
                    ?.apply()
        }

    // FTP Server default configuration
    private var ftpServerIP             = "192.168.0.5"
    private var ftpServerFileName       = "PocketNoodle.json"
    private var ftpServerPath           = "/Documents/"
    private var ftpServerUser           = "john"
    private var ftpServerPass           = "ftppassword"

    private val snakes                  = Snakes(mutableListOf())
    private var snake: Snake?           = null
        set(value) {
            if(!value?.name.isNullOrBlank()) {
                field = value
                textSnakeName.text = field?.name
            } else textSnakeName.text = "N/A"

            updateViews()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Setup
        loadPreferences()
        loadProfiles()
        profile = sharedPref?.getString("last_loaded", DEFAULT_SNAKE) ?: DEFAULT_SNAKE

        // Collect UI elements and assign event handlers
        buttonFeed.setOnClickListener { confirmYesNo("Add Feed") { addDate(TYPE_FEED) } }
        buttonFeed.setOnLongClickListener { getDate(TYPE_FEED) }

        buttonShed.setOnClickListener { confirmYesNo("Add Shed") { addDate(TYPE_SHED) } }
        buttonShed.setOnLongClickListener { getDate(TYPE_SHED) }

        buttonSettings.setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }

        buttonProfiles.setOnClickListener {
            AlertDialog.Builder(this)
                    .setTitle("Change Profile")
                    .setNegativeButton("Cancel", { _,_ -> })
                    .setItems(snakes.getNames()) { _,pos ->
                        snakes.get(pos)?.let {
                            saveProfiles()
                            profile = it.name
                        }
                    }
                    .create().show()
        }

        buttonFeedLog.setOnClickListener {
            startActivityForResult(
                    Intent(this,LogActivity::class.java)
                            .putExtra("type", TYPE_FEED)
                            .putExtra("snake", profile), TYPE_FEED)
        }

        buttonShedLog.setOnClickListener {
            startActivityForResult(
                    Intent(this,LogActivity::class.java)
                            .putExtra("type", TYPE_SHED)
                            .putExtra("snake", profile), TYPE_SHED)
        }

        // Launch settings activity on first run
        if(firstRun()) {
            sharedPref?.edit()?.putBoolean("first_run", false)?.apply()
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()

        loadProfile(profile)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_settings, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.settings_menu_save -> {
                confirmYesNo { saveProfiles() }
                true
            }
            R.id.settings_menu_load -> {

                true
            }
            R.id.setting_menu_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            R.id.settings_menu_reset -> {
                confirmYesNo { doReset() }
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    private fun updateViews() {
        var lastFeed: Date? = null
        var lastShed: Date? = null

        snake?.let {
            lastFeed = it.feedDates
                    .map { dateFormatIn.parse(it) }
                    .sorted()
                    .lastOrNull()

            lastShed = it.shedDates
                    .map { dateFormatIn.parse(it) }
                    .sorted()
                    .lastOrNull()
        }

        lastFeedDate.setText(if(lastFeed == null) "N/A" else dateFormatOut.format(lastFeed))
        lastShedDate.setText(if(lastShed == null) "N/A" else dateFormatOut.format(lastShed))
    }

    private fun doReset() {
        // Clear SharedPreferences data cache
        sharedPref!!.edit().clear()
                .putBoolean("first_run", true)
                .putString("default_snake", DEFAULT_SNAKE)
                .putString("ftp_server_ip", ftpServerIP)
                .putString("ftp_server_file", ftpServerFileName)
                .putString("ftp_server_path", ftpServerPath)
                .putString("ftp_server_user", ftpServerUser)
                .putString("ftp_server_pass", ftpServerPass)
                .putString("snakes", Snakes(mutableListOf(
                        Snake("Monty Python", mutableListOf("9-11-17", "9-15-17", "9-21-17"), mutableListOf("7-1-17", "8-1-17")),
                        Snake("Ramen Noodle", mutableListOf(), mutableListOf()),
                        Snake("Hank Jr.", mutableListOf(), mutableListOf())
                )).toString())
                .apply()

        loadProfiles()
        profile = ""
    }

    private fun loadPreferences(): SharedPreferences {
        sharedPref = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        return sharedPref!!
    }

    private fun firstRun(): Boolean = sharedPref?.getBoolean("first_run", true) ?: true

    private fun loadProfiles() {
        val snakeList = JSONArray(sharedPref?.getString("snakes", "") ?: "")
        var i = 0
        val temp = mutableListOf<Snake>()

        while(i < snakeList.length()) {
            val cur = snakeList.getJSONObject(i++)
            val name = cur.getString("name")
            val feeds = cur.getJSONArray("feeds")
            val sheds = cur.getJSONArray("sheds")

            val tempFeeds = mutableListOf<String>()
            val tempSheds = mutableListOf<String>()

            var pos = 0
            while(pos < feeds.length())
                tempFeeds.add(feeds[pos++].toString())

            pos = 0
            while(pos < sheds.length())
                tempSheds.add(sheds[pos++].toString())

            temp.add(Snake(name, tempFeeds, tempSheds))
        }

        snakes.updateAll(temp)
    }

    private fun loadProfile(name: String) = snakes.get(name)?.let { snake = it } ?: run { snake = null }

    private fun saveProfiles() = sharedPref!!.edit()
                                    .putString("snakes", snakes.toString())
                                    .apply()

    private fun confirmYesNo(title: String = "Pocket Noodle", operation: () -> Unit) {
        val dialogClickListener = DialogInterface.OnClickListener { _, choice ->
            when (choice) {
                DialogInterface.BUTTON_POSITIVE -> {
                    operation()
                }
                else -> {  }
            }
        }
        AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage("Are you sure?")
                .setPositiveButton("Yes", dialogClickListener)
                .setNegativeButton("No", dialogClickListener)
                .show()
    }

    private fun getDate(type: Int): Boolean {
        DatePickerFragment({
            confirmYesNo("Add " +
                    when (type) {
                        TYPE_FEED -> "Feed"
                        TYPE_SHED -> "Shed"
                        else -> "???"
                    }) { addDate(type, dateFormatIn.parse(it)) }
        }).show(fragmentManager, "datePicker")

        return false
    }

    private fun addDate(type: Int, date: Date = Date()): Boolean {
        when (type) {
            TYPE_FEED -> snake?.feedDates?.add(dateFormatIn.format(date))
            TYPE_SHED -> snake?.shedDates?.add(dateFormatIn.format(date))
            else -> return false
        }

        saveProfiles()
        updateViews()

        return true
    }

    internal companion object {
        private val DEFAULT_SNAKE = "Monty Python"

        internal val dateFormatIn = SimpleDateFormat("MM-dd-yy", Locale.US)
        internal val dateFormatOut = SimpleDateFormat("EE MM/dd/yy", Locale.US)

        internal val TYPE_FEED = 0
        internal val TYPE_SHED = 1
    }
}
