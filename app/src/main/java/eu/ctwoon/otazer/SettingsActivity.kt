package eu.ctwoon.otazer

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(android.R.id.content, SettingsFragment())
                .commit()
        }
        //val toolbar: Toolbar = findViewById(R.id.toolbar)
        //setSupportActionBar(toolbar)
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
            val prefs: SharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(context)
            val ed: SharedPreferences.Editor = prefs.edit()
            val add: Preference? = findPreference("add")
            add?.summary = (getString(R.string.add_summary) + prefs.getInt("total", 0).toString())
            if (prefs.getBoolean("alarm", false)) {
                findPreference<Preference>("alarm")?.title = getString(R.string.disable_alarm)
            } else {
                findPreference<Preference>("alarm")?.title = getString(R.string.alarm)
            }
            add?.setOnPreferenceClickListener {
                startActivity(Intent(context, AddRepository::class.java))
                return@setOnPreferenceClickListener true
            }
            findPreference<Preference>("manually")?.setOnPreferenceClickListener {
                Checker().check(requireContext())
                return@setOnPreferenceClickListener true
            }
            val changelistener =
                Preference.OnPreferenceChangeListener { _, newValue ->
                    val myIntent = Intent(requireContext(), AlarmReceiver::class.java)
                    val pendingIntent = PendingIntent.getBroadcast(
                        requireContext(), 1, myIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                    )

                    val alarmManager = context?.getSystemService(ALARM_SERVICE) as AlarmManager?

                    if (newValue == true) {
                        alarmManager?.setRepeating(
                            AlarmManager.RTC_WAKEUP,
                            System.currentTimeMillis(),
                            86400000,
                            pendingIntent
                        )
                        ed.putBoolean("alarmm", true)
                        ed.apply()
                        findPreference<Preference>("alarm")?.title =
                            getString(R.string.disable_alarm)
                    } else {
                        alarmManager?.cancel(pendingIntent)
                        ed.putBoolean("alarmm", false)
                        ed.apply()
                        findPreference<Preference>("alarm")?.title = getString(R.string.alarm)
                    }
                    true
                }
            findPreference<Preference>("alarm")?.onPreferenceChangeListener = changelistener
        }
    }
}

