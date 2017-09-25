package local.john.pocketnoodle

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v7.app.AlertDialog
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import kotlinx.android.synthetic.main.activity_main.*
import local.john.pocketnoodle.Util.*
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.io.InputStream
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
    private var ftpServerIP = "192.168.0.5"
    private var ftpServerFileName = "PocketNoodle.json"
    private var ftpServerPath = "/Documents/"
    private var ftpServerUser = "anon"
    private var ftpServerPass = ""

    private val snakes = Snakes(mutableListOf())
    private var snake: Snake? = null
        set(value) {
            if (!value?.name.isNullOrBlank()) {
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

        // Collect UI elements and assign event handlers (Inside run block so IntelliJ will collapse it)
        run {
            buttonFeed.setOnClickListener { confirmYesNo("Add Feed") { addDate(TYPE_FEED) } }
            buttonFeed.setOnLongClickListener { getDate(TYPE_FEED) }

            buttonShed.setOnClickListener { confirmYesNo("Add Shed") { addDate(TYPE_SHED) } }
            buttonShed.setOnLongClickListener { getDate(TYPE_SHED) }

            buttonSettings.setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }

            buttonProfiles.setOnClickListener {
                AlertDialog.Builder(this)
                        .setTitle("Change Profile")
                        .setNeutralButton("Cancel", { _, _ -> })
                        .setItems(snakes.getNames()) { _, pos ->
                            snakes.get(pos)?.let {
                                saveProfiles()
                                profile = it.name
                            }
                        }
                        .create().show()
            }

            buttonFeedLog.setOnClickListener {
                startActivity(
                        Intent(this, LogActivity::class.java)
                                .putExtra("type", TYPE_FEED)
                                .putExtra("snake", profile))
            }

            buttonShedLog.setOnClickListener {
                startActivity(
                        Intent(this, LogActivity::class.java)
                                .putExtra("type", TYPE_SHED)
                                .putExtra("snake", profile))
            }

            buttonSync.setOnClickListener {
                val dialogClickListener = DialogInterface.OnClickListener { _, choice ->
                    when (choice) {
                        DialogInterface.BUTTON_POSITIVE -> {
                            confirmYesNo("Push to server") { pushToServer() }
                        }
                        DialogInterface.BUTTON_NEGATIVE -> {
                            confirmYesNo("Pull from server") { pullFromServer() }
                        }
                        DialogInterface.BUTTON_NEUTRAL -> {
                        }
                    }
                }
                AlertDialog.Builder(this)
                        .setTitle("Sync")
                        .setMessage("Push or Pull?")
                        .setPositiveButton("Push", dialogClickListener)
                        .setNegativeButton("Pull", dialogClickListener)
                        .setNeutralButton("Cancel", dialogClickListener)
                        .setCancelable(true)
                        .show()
            }
        }

        // Launch settings activity on first run
        if (firstRun()) {
            sharedPref?.edit()?.putBoolean("first_run", false)?.apply()
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        loaded = true
    }

    override fun onResume() {
        super.onResume()

        ftpServerIP = sharedPref?.getString("ftp_server_ip", ftpServerIP) ?: ftpServerIP
        ftpServerFileName = sharedPref?.getString("ftp_server_file", ftpServerFileName) ?: ftpServerFileName
        ftpServerPath = sharedPref?.getString("ftp_server_path", ftpServerPath) ?: ftpServerPath
        ftpServerUser = sharedPref?.getString("ftp_server_user", ftpServerUser) ?: ftpServerUser
        ftpServerPass = sharedPref?.getString("ftp_server_pass", ftpServerPass) ?: ftpServerPass

        // Reload from local, trigger update
        if (loaded) {
            loadProfiles()
            profile = profile
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_settings, menu)
        return super.onCreateOptionsMenu(menu)
    }

    @SuppressLint("InflateParams")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.settings_menu_save -> {
                confirmYesNo("Quick Save") { pushToServer() }
                true
            }
            R.id.settings_menu_add -> {
                val view = layoutInflater.inflate(R.layout.dialog_prompt, null)
                AlertDialog.Builder(this)
                        .setView(view)
                        .setTitle("Create New Profile")
                        .setPositiveButton("Add", { _, _ ->
                            val name = view.findViewById<EditText>(R.id.dialog_prompt_input).text.toString()

                            if (name != "" && snakes.get(name) == null) {
                                confirmYesNo("Create Profile") {
                                    if (snakes.add(name)) {
                                        profile = name

                                        toast("Created profile '$name'")
                                    } else
                                        toast("Error creating profile.")
                                }
                            } else
                                toast("Profile already exists or input was blank.")
                        }).create().show()

                true
            }
            R.id.settings_menu_remove -> {
                confirmYesNo("Delete '$profile'?") {
                    if (snakes.remove(profile)) {
                        saveProfiles()
                        loadProfiles()
                        profile = ""

                        toast("Profile deleted.")
                    } else {
                        // This should never happen...
                        toast("Could not find '$profile'.")
                    }
                }
                true
            }
            R.id.setting_menu_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            R.id.settings_menu_reset -> {
                confirmYesNo("Reset Data") { doReset() }
                true
            }
            else -> super.onOptionsItemSelected(item)
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

        lastFeedDate.setText(if (lastFeed == null) "N/A" else dateFormatOut.format(lastFeed))
        lastShedDate.setText(if (lastShed == null) "N/A" else dateFormatOut.format(lastShed))
    }

    private fun doReset() {
        sharedPref!!.edit().clear()
                .putBoolean("first_run", true)
                .putString("last_loaded", DEFAULT_SNAKE)
                .putString("ftp_server_ip", ftpServerIP)
                .putString("ftp_server_file", ftpServerFileName)
                .putString("ftp_server_path", ftpServerPath)
                .putString("ftp_server_user", ftpServerUser)
                .putString("ftp_server_pass", ftpServerPass)
                .putString("snakes", Snakes(mutableListOf()).toString())
                .apply()

        loadProfiles()
        profile = ""

        startActivity(Intent(this, SettingsActivity::class.java))
    }

    private fun loadPreferences(): SharedPreferences {
        sharedPref = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        return sharedPref!!
    }

    private fun firstRun(): Boolean = sharedPref?.getBoolean("first_run", true) ?: true

    private fun loadProfiles() =
            snakes.updateAll(parseJSON(JSONArray(sharedPref?.getString("snakes", "") ?: "")))

    private fun loadProfile(name: String) = snakes.get(name)?.let { snake = it } ?: run { snake = null }

    private fun saveProfiles() = sharedPref?.edit()
            ?.putString("snakes", snakes.toString())
            ?.apply()

    private fun confirmYesNo(title: String = "Pocket Noodle", operation: () -> Unit) {
        AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage("Are you sure?")
                .setPositiveButton("Yes", { _, _ -> operation() })
                .setNegativeButton("No", null)
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

    private fun getSaveStream(): InputStream {
        return JSONObject()
                .put("first_run", false)
                .put("last_loaded", profile)
                .put("ftp_server_ip", ftpServerIP)
                .put("ftp_server_file", ftpServerFileName)
                .put("ftp_server_path", ftpServerPath)
                .put("ftp_server_user", ftpServerUser)
                .put("ftp_server_pass", ftpServerPass)
                .put("snakes", snakes)
                .toString()
                .byteInputStream()
    }

    private fun pushToServer() {
        FtpClient(ftpServerIP, ftpServerUser, ftpServerPass)
                .sync(ftpServerPath, ftpServerFileName, getSaveStream()) {
                    runOnUiThread {
                        toast(if (it == "1") "File pushed to server."
                        else "Unable to push file to server.")
                    }
                }
    }

    private fun pullFromServer() {
        FtpClient(ftpServerIP, ftpServerUser, ftpServerPass)
                .sync(ftpServerPath, ftpServerFileName) {
                    runOnUiThread {
                        if (it == "0")
                            toast("Unable to load file from server.")
                        else {
                            toast("File pulled from server.")
                            try {
                                loadFromServer(JSONObject(JSONTokener(it)))
                            } catch (e: Exception) {
                                toast("Error parsing file.")
                            }
                        }
                    }
                }
    }

    private fun loadFromServer(data: JSONObject) {
        val lastLoaded = data.getString("last_loaded")

        sharedPref?.edit()
                ?.clear()
                ?.putBoolean("first_run", data.getBoolean("first_run"))
                ?.putString("last_loaded", lastLoaded)
                ?.putString("ftp_server_ip", data.getString("ftp_server_ip"))
                ?.putString("ftp_server_file", data.getString("ftp_server_file"))
                ?.putString("ftp_server_path", data.getString("ftp_server_path"))
                ?.putString("ftp_server_user", data.getString("ftp_server_user"))
                ?.putString("ftp_server_pass", data.getString("ftp_server_pass"))
                ?.putString("snakes", data.getString("snakes"))
                ?.apply()

        loadProfiles()
        profile = lastLoaded
    }

    internal companion object {
        private val DEFAULT_SNAKE = "Noodle"
        private var loaded = false

        internal val dateFormatIn = SimpleDateFormat("MM-dd-yy", Locale.US)
        internal val dateFormatOut = SimpleDateFormat("EE MM/dd/yy", Locale.US)

        internal val TYPE_FEED = 0
        internal val TYPE_SHED = 1
    }
}
