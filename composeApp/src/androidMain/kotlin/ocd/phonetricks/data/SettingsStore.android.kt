package ocd.phonetricks.data

import android.content.Context

private class AndroidSettingsStore(context: Context) : SettingsStore {
    private val prefs = context.getSharedPreferences("phonetricks_settings", Context.MODE_PRIVATE)

    override fun read(key: String): String? = prefs.getString(key, null)

    override fun write(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }
}

actual fun createSettingsStore(context: Any?): SettingsStore =
    AndroidSettingsStore(context as Context)
