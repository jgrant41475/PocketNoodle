package local.john.pocketnoodle

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.AdapterView
import android.widget.ArrayAdapter
import kotlinx.android.synthetic.main.activity_log.*
import android.content.DialogInterface
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.support.v4.app.NavUtils
import android.support.v7.app.AlertDialog
import android.view.MenuItem
import android.view.View
import local.john.pocketnoodle.Util.Snake
import local.john.pocketnoodle.Util.Snakes
import local.john.pocketnoodle.Util.parseJSON
import org.json.JSONArray


internal class LogActivity : AppCompatActivity(), AdapterView.OnItemLongClickListener {

    private var sharedPreferences: SharedPreferences? = null
    private var name: String? = null

    private companion object {
        private var logType = -1
        private var log = mutableListOf<String>()
        private lateinit var logAdapter: ArrayAdapter<String>
        private var allSnakes: Snakes? = null
        private var snake: Snake? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log)

        logType = intent?.extras?.getInt("type") ?: -1
        name = intent?.extras?.getString("snake") ?: ""

        if(name.isNullOrBlank())
            return

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)

        allSnakes = Snakes(parseJSON(JSONArray(sharedPreferences?.getString("snakes", "") ?: "")))
        snake = allSnakes?.get(name!!)

        log = when(logType) {
            MainActivity.TYPE_FEED -> snake?.feedDates ?: mutableListOf()
            MainActivity.TYPE_SHED -> snake?.shedDates ?: mutableListOf()
            else -> mutableListOf()
        }

        if(log.size > 0)
            log = log.map { MainActivity.dateFormatIn.parse(it) }
                     .sortedDescending()
                     .map { MainActivity.dateFormatOut.format(it) }
                     .toMutableList()
        else
            log.add("N/A")

        logAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, log)
        logList.adapter = logAdapter
        logList.onItemLongClickListener = this
    }

    override fun onItemLongClick(parent: AdapterView<*>, view: View, pos: Int, id: Long): Boolean {
        if(log[pos] != "N/A") {
            val dialogClickListener = DialogInterface.OnClickListener { _, choice ->
                if (choice == DialogInterface.BUTTON_POSITIVE) {
                    val match = MainActivity.dateFormatIn.format(MainActivity.dateFormatOut.parse(log[pos]))

                    when (logType) {
                        MainActivity.TYPE_FEED -> { snake?.feedDates?.remove(match) }
                        MainActivity.TYPE_SHED -> { snake?.shedDates?.remove(match) }
                    }

                    log.removeAt(pos)
                    if(log.size == 0)
                        log.add("N/A")

                    allSnakes?.update(snake!!)
                    logAdapter.notifyDataSetChanged()

                    sharedPreferences?.edit()
                            ?.putString("snakes", allSnakes.toString())
                            ?.apply()
                }
            }

            AlertDialog.Builder(this)
                    .setMessage("Delete entry:\n'${log[pos]}'?")
                    .setPositiveButton("Yes", dialogClickListener)
                    .setNegativeButton("No", dialogClickListener)
                    .show()
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.itemId == android.R.id.home) {
            NavUtils.navigateUpTo(this, NavUtils.getParentActivityIntent(this))
            return true
        }
        return false
    }
}
