package ocd.phonetricks.training

/**
 * Platform-specific file writer for saving training data.
 */
interface FileWriter {
    /**
     * Save content to a file in the app's documents/downloads directory.
     *
     * @param filename The name of the file to save
     * @param content The content to write to the file
     * @return true if successful, false otherwise
     */
    suspend fun saveFile(filename: String, content: String): Boolean

    /**
     * Get the directory where files are being saved.
     */
    fun getSaveDirectory(): String
}

expect fun createFileWriter(): FileWriter
