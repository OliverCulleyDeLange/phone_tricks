package ocd.phonetricks.data

import platform.Foundation.NSUserDefaults

private class IosSettingsStore : SettingsStore {
    private val defaults = NSUserDefaults.standardUserDefaults

    override fun read(key: String): String? = defaults.stringForKey(key)

    override fun write(key: String, value: String) {
        defaults.setObject(value, key)
    }
}

actual fun createSettingsStore(context: Any?): SettingsStore = IosSettingsStore()
