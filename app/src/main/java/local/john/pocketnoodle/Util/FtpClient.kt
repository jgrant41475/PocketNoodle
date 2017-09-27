package local.john.pocketnoodle.Util

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPClientConfig
import org.apache.commons.net.ftp.FTPReply
import java.io.InputStream

internal class FtpClient(private val server: String, private val user: String, private val pass: String) {
    private var connection: FTPClient

    init {
        require(Regex("""\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}""").matches(server)) {
            "Server address must be in dot-decimal format"
        }
        connection = FTPClient()
        connection.configure(FTPClientConfig())
    }

    private fun connect(): Boolean {
        try {
            connection.connect(server)
            if (!FTPReply.isPositiveCompletion(connection.replyCode)) {
                connection.disconnect()
                throw Exception("Server refused connection.")
            } else return@connect true
        } catch (e: Exception) {
            return@connect false
        }
    }

    internal fun sync(path: String, name: String, stream: InputStream? = null, operation: (String) -> Unit) {
        async(CommonPool) {
            try {
                if (connect()) {
                    if (connection.login(user, pass)) {
                        if (connection.changeWorkingDirectory(path)) {
                            operation(
                                    if (stream != null)                                              // Push
                                        if (connection.storeFile(path + name, stream)) "1"
                                        else "0"
                                    else                                                            // Pull
                                        connection.retrieveFileStream(path + name)
                                                .bufferedReader()
                                                .readText()
                            )
                        } else throw Exception("Could not access directory '$path'")
                    } else throw Exception("Invalid login.")
                } else throw Exception("Connection refused.")
            } catch (e: Exception) {
                operation("0")
            } finally {
                try {
                    connection.logout()
                    connection.disconnect()
                } catch (e: Exception) {
                }
            }
        }
    }
}