package local.john.pocketnoodle.Util

import android.util.Log
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPClientConfig
import org.apache.commons.net.ftp.FTPReply
import java.io.InputStream

class FtpClient(val server: String, val user: String, val pass: String) {
    var connection: FTPClient

    init {
        require(Regex("""\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}""").matches(server)) {
            "Server address must be in proper dot-decimal format"
        }
        connection = FTPClient()
        connection.configure(FTPClientConfig())
    }

    fun connect(): Boolean {
        try {
            connection.connect(server)
            if (!FTPReply.isPositiveCompletion(connection.replyCode)) {
                connection.disconnect()
                throw Exception("Server refused connection.")
            } else return@connect true
        } catch (e: Exception) {
            Log.e("DEBUGGING_TAG", "Encountered error: ${e.message}")
            return@connect false
        }
    }

    fun sync(path: String, name: String, stream: InputStream? = null, operation: (String) -> Unit) {
        async(CommonPool) {
            try {
                if (connect()) {
                    if (connection.login(user, pass)) {
                        if (connection.changeWorkingDirectory(path)) {
                            val result: String
                            if(stream != null)// Push
                                result = if(connection.storeFile(path + name, stream)) "1" else "0"
                            else // Pull
                                result = connection.retrieveFileStream(path + name).bufferedReader().readText()

                            operation(result)
                        } else throw Exception("Could not access directory '$path'")
                    } else throw Exception("Invalid login.")
                } else throw Exception("Connection refused.")
            } catch (e: Exception) {
                Log.e("DEBUGGING_TAG", "Error caught: ${e.message}")
                operation("0")
            } finally {
                try {
                    connection.logout()
                    connection.disconnect()
                } catch(e: Exception) {}
            }
        }
    }
}