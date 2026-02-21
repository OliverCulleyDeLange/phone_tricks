package ocd.phonetricks.data

interface SettingsStore {
    fun read(key: String): String?
    fun write(key: String, value: String)
}

expect fun createSettingsStore(context: Any?): SettingsStore
