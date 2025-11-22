package ocd.phonetricks.training

import android.content.Context
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class AndroidFileWriter(private val context: Context) : FileWriter {
    override suspend fun saveFile(filename: String, content: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // Use external storage Downloads directory if available, otherwise use internal storage
            val directory = if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
                File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "PhoneTricksTraining")
            } else {
                File(context.filesDir, "training_data")
            }

            // Create directory if it doesn't exist
            if (!directory.exists()) {
                directory.mkdirs()
            }

            val file = File(directory, filename)
            FileOutputStream(file).use { output ->
                output.write(content.toByteArray())
            }

            println("Training data saved to: ${file.absolutePath}")
            true
        } catch (e: Exception) {
            println("Failed to save training data: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    override fun getSaveDirectory(): String {
        return if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "PhoneTricksTraining").absolutePath
        } else {
            File(context.filesDir, "training_data").absolutePath
        }
    }
}

actual fun createFileWriter(): FileWriter {
    throw IllegalStateException("Context required for Android. Use createFileWriter(context) instead")
}

fun createFileWriter(context: Context): FileWriter {
    return AndroidFileWriter(context)
}
