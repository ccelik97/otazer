package eu.ctwoon.otazer

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class AddRepository : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    class SettingsFragment : PreferenceFragmentCompat(), CoroutineScope by MainScope() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.add_repository, rootKey)
            val prefs: SharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(context)
            val ed: SharedPreferences.Editor = prefs.edit()
            findPreference<Preference>("save")?.setOnPreferenceClickListener {
                if (prefs.getString("name", null) == null || prefs.getString("url", null) == null) {
                    Toast.makeText(
                        context,
                        getString(R.string.something_went_wrong) + "0",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnPreferenceClickListener true
                }
                try {
                    launch {
                        withContext(Dispatchers.IO) {
                            val string = prefs.getString("url", null)
                            var str: Array<String>? = string?.split("/")?.toTypedArray()
                            val request: Request =
                                Request.Builder()
                                    .url(
                                        "https://api.github.com/repos/" + str?.get(3) + "/" + str?.get(
                                            4
                                        ) + "/releases/latest"
                                    )
                                    .build()
                            val response = OkHttpClient().newCall(request).execute()
                            if (response.code != 200) {
                                Toast.makeText(
                                    context,
                                    getString(R.string.something_went_wrong) + "1",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@withContext
                            }
                            val parsedString = JSONObject(response.body!!.string())
                            ed.putString(
                                "version_" + (prefs.getInt("total", 0) + 1).toString(),
                                parsedString.getString("name")
                            )
                            ed.apply()
                        }
                        ed.putString(
                            ("url_" + (prefs.getInt("total", 0) + 1).toString()),
                            prefs.getString("url", null)
                        )
                        ed.putString(
                            ("name_" + (prefs.getInt("total", 0) + 1).toString()),
                            prefs.getString("name", null)
                        )
                        ed.putInt("total", prefs.getInt("total", 0) + 1)
                        ed.apply()
                        Toast.makeText(context, prefs.getInt("total", 0).toString(), Toast.LENGTH_SHORT).show()
                        Toast.makeText(context, getString(R.string.succes) + " currrent version: " +  prefs.getString("version_" + prefs.getInt("total", 0).toString(), null), Toast.LENGTH_SHORT).show()
                    }
                }
                catch (e: Exception) {
                    Toast.makeText(
                        context,
                        e.message,
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnPreferenceClickListener true
                }
                return@setOnPreferenceClickListener true
            }
        }
    }
}