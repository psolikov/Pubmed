package ru.hse.spb

import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPReply
import java.util.zip.GZIPInputStream
import java.io.*
import java.io.IOException
import java.io.FileOutputStream
import java.io.FileInputStream


fun Boolean.toInt() = if (this) 1 else 0

class DataManager(private val name: String) {

    private val server = "ftp.ncbi.nlm.nih.gov"
    private val user = "anonymous"
    private val pass = ""
    private val ftpClient = FTPClient()
    private val fileHead = "/pubmed/baseline/pubmed18n"
    private val fileTail = ".xml.gz"
    private val localFileHead = "src/main/resources/pubmed18n"
    private val localFileTail = ".xml.gz"
    private val pathCSV = "src/main/resources/data.csv"

    fun fetchXML() = try {
        ftpClient.connect(server)
        ftpClient.login(user, pass)
        ftpClient.enterLocalPassiveMode()
        ftpClient.setFileType(FTP.BINARY_FILE_TYPE)
        println("Connected to $server.")
        println(ftpClient.replyString)
        val reply = ftpClient.replyCode
        when {
            !FTPReply.isPositiveCompletion(reply) -> {
                ftpClient.disconnect()
                System.err.println("FTP server refused connection.")
                System.exit(1)
            }
        }
        val remoteFile = "$fileHead$name$fileTail"
        val downloadFile = File("$localFileHead$name$localFileTail")
        val outputStream = BufferedOutputStream(FileOutputStream(downloadFile))
        val success = ftpClient.retrieveFile(remoteFile, outputStream)
        outputStream.close()
        when {
            success -> println("File has been downloaded successfully.")
            else -> println("Bad luck")
        }
    } catch (ex: IOException) {
        println("Error: " + ex.toString())
        ex.printStackTrace()
    } finally {
        try {
            when {
                ftpClient.isConnected -> {
                    ftpClient.logout()
                    ftpClient.disconnect()
                }
            }
        } catch (ex: IOException) {
            ex.printStackTrace()
        }
    }

    fun unzip() {
        val buffer = ByteArray(1024)
        try {
            val gzis = GZIPInputStream(FileInputStream("$localFileHead$name$localFileTail"))
            val out = FileOutputStream("$localFileHead$name.xml")
            var len = 0
            do {
                if (len != 0) out.write(buffer, 0, len)
                len = gzis.read(buffer)
            } while (len > 0)
            gzis.close()
            out.close()
            println("Done")
        } catch (ex: IOException) {
            ex.printStackTrace()
        }
    }

    fun makeCSV() {
        val p = XMLParser("$localFileHead$name.xml")
        p.parseDocument()
        p.printPapers()
        val papers = p.papers
        val file = File(pathCSV)
        var fileWriter: FileWriter? = null
        val aggregate = papers.asSequence()
                .map { it.date to it.containsTags }
                .groupingBy { it.first }
                .aggregate { _, accumulator: Int?, element, first ->
                    when {
                        first -> element.second.toInt()
                        else -> accumulator!! + element.second.toInt()
                    }
                }
        try {
            fileWriter = FileWriter(file)
            fileWriter.append("date,close")
            fileWriter.append('\n')
            aggregate.toSortedMap().forEach {
                fileWriter.append(it.key.toString())
                fileWriter.append(',')
                fileWriter.append(it.value.toString())
                fileWriter.append('\n')
            }
            println("Write CSV successfully!")
        } catch (e: Exception) {
            println("Writing CSV error!")
            e.printStackTrace()
        } finally {
            try {
                fileWriter!!.flush()
                fileWriter.close()
            } catch (e: IOException) {
                println("Flushing/closing error!")
                e.printStackTrace()
            }
        }
    }
}
