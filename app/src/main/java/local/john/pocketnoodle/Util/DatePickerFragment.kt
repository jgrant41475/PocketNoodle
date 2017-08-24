package local.john.pocketnoodle.Util

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.Dialog
import android.app.DialogFragment
import android.os.Bundle
import android.widget.DatePicker
import local.john.pocketnoodle.MainActivity
import java.text.SimpleDateFormat
import java.util.*

@SuppressLint("ValidFragment")
internal class DatePickerFragment() : DialogFragment(), DatePickerDialog.OnDateSetListener {

    private var callBack: (String) -> Unit = {  }

    internal constructor(cb: (String) -> Unit) : this() { callBack = cb }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val c = Calendar.getInstance()

        return DatePickerDialog(activity, this,
                c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH))
    }

    override fun onDateSet(view: DatePicker, year: Int, month: Int, day: Int) =
        callBack(MainActivity.dateFormatIn.format(
                SimpleDateFormat("MM/dd/yyyy", Locale.US).parse("${month+1}/$day/$year")))
}