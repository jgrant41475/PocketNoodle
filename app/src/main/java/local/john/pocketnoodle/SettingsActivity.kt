package local.john.pocketnoodle

import android.os.Bundle
import local.john.pocketnoodle.Util.AppCompatPreferenceActivity

internal class SettingsActivity : AppCompatPreferenceActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        @Suppress("DEPRECATION")
        addPreferencesFromResource(R.xml.settings)
    }
}
