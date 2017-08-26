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

internal class LogActivity : AppCompatActivity(), AdapterView.OnItemLongClickListener {

    private var sharedPreferences: SharedPreferences? = null

    private companion object {
        private var logType = -1
        private var log = mutableListOf<String>()
        private lateinit var logAdapter: ArrayAdapter<String>
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log)

        logType = intent?.extras?.getInt("type") ?: -1
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)

        log = sharedPreferences!!.getStringSet(when (logType) {
                                                    MainActivity.TYPE_FEED -> "feeds"
                                                    MainActivity.TYPE_SHED -> "sheds"
                                                    else -> "" }, emptySet()).toMutableList()

        if(log.size > 0)
            log = log
                    .map { MainActivity.dateFormatIn.parse(it) }
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
        if(log[pos] == "N/A")
            return true

        val dialogClickListener = DialogInterface.OnClickListener { _, choice ->
            if (choice == DialogInterface.BUTTON_POSITIVE) {
                log.removeAt(pos)

                PreferenceManager.getDefaultSharedPreferences(applicationContext).edit()
                        .putStringSet(when (logType) {
                                        MainActivity.TYPE_FEED -> "feeds"
                                        MainActivity.TYPE_SHED -> "sheds"
                                        else -> ""
                                    },
                                log.map { MainActivity.dateFormatIn.format(MainActivity.dateFormatOut.parse(it)) }
                                        .toMutableSet()).apply()

                logAdapter.notifyDataSetChanged()
            }
        }

        AlertDialog.Builder(this)
                .setMessage("Delete entry:\n'${log[pos]}'?")
                .setPositiveButton("Yes", dialogClickListener)
                .setNegativeButton("No", dialogClickListener)
                .show()

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
