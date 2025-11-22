package ocd.phonetricks.training

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.*
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
class IOSFileWriter : FileWriter {
    override suspend fun saveFile(filename: String, content: String): Boolean = withContext(Dispatchers.Default) {
        try {
            // Get Documents directory
            val documentsDirectory = NSFileManager.defaultManager.URLsForDirectory(
                NSDocumentDirectory,
                NSUserDomainMask
            ).firstOrNull() as? NSURL ?: return@withContext false

            // Create training_data subdirectory
            val trainingDirectory = documentsDirectory.URLByAppendingPathComponent("training_data", true)
            val trainingPath = trainingDirectory?.path ?: return@withContext false

            // Create directory if it doesn't exist
            if (!NSFileManager.defaultManager.fileExistsAtPath(trainingPath)) {
                NSFileManager.defaultManager.createDirectoryAtPath(
                    trainingPath,
                    true,
                    null,
                    null
                )
            }

            // Create file URL
            val fileURL = trainingDirectory.URLByAppendingPathComponent(filename, false)
            val filePath = fileURL?.path ?: return@withContext false

            // Write content to file
            val nsString = content as NSString
            nsString.writeToFile(filePath, true, NSUTF8StringEncoding, null)

            println("Training data saved to: $filePath")
            true
        } catch (e: Exception) {
            println("Failed to save training data: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    override fun getSaveDirectory(): String {
        val documentsDirectory = NSFileManager.defaultManager.URLsForDirectory(
            NSDocumentDirectory,
            NSUserDomainMask
        ).firstOrNull() as? NSURL

        val trainingDirectory = documentsDirectory?.URLByAppendingPathComponent("training_data", true)
        return trainingDirectory?.path ?: "Unknown"
    }
}

actual fun createFileWriter(): FileWriter {
    return IOSFileWriter()
}
