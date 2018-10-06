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

/*
fun main(args: Array<String>) {
    val server = "ftp.ncbi.nlm.nih.gov"
    val user = "anonymous"
    val pass = ""

    val ftpClient = FTPClient()
    try {
        ftpClient.connect(server)
        ftpClient.login(user, pass)
        ftpClient.enterLocalPassiveMode()
        ftpClient.setFileType(FTP.BINARY_FILE_TYPE)
        println(ftpClient.status)
//        for (listDirectory in ftpClient.listDirectories()) {
//            println(listDirectory.name)
//        }
//        for (listFile in ftpClient.listFiles()) {
//            println(listFile.name)
//        }
        System.out.println("Connected to $server.")
        System.out.print(ftpClient.replyString)

        // After connection attempt, you should check the reply code to verify
        // success.
        val reply = ftpClient.replyCode

        if (!FTPReply.isPositiveCompletion(reply)) {
            ftpClient.disconnect()
            System.err.println("FTP server refused connection.")
            System.exit(1)
        }
        // APPROACH #1: using retrieveFile(String, OutputStream)
        val remoteFile1 = "/pubmed/baseline/pubmed18n0001.xml.gz"
        val downloadFile1 = File("src/main/resources/pubmed18n0001.xml.gz")
        val outputStream1 = BufferedOutputStream(FileOutputStream(downloadFile1))
        val success = ftpClient.retrieveFile(remoteFile1, outputStream1)
        outputStream1.close()

        if (success) {
            println("File #1 has been downloaded successfully.")
        } else {
            println("Bad luck")
        }
    } catch (ex: IOException) {
        System.out.println("Error: " + ex.toString())
        ex.printStackTrace()
    } finally {
        try {
            if (ftpClient.isConnected) {
                ftpClient.logout()
                ftpClient.disconnect()
            }
        } catch (ex: IOException) {
            ex.printStackTrace()
        }
    }

    val p = XMLParser("src/main/resources/pubmed18n0001.xml")
    p.parseDocument()
    p.printPapers()
    val papers = p.papers
    val file = File("src/main/resources/data.csv")
    var fileWriter: FileWriter? = null
    val aggregate = papers.asSequence().map { p -> p.date to p.containsTags }.groupingBy { it.first }
            .aggregate { _, accumulator: Int?, element, first ->
                if (first) {
                    element.second.toInt()
                } else {
                    accumulator!! + element.second.toInt()
                }
            }
    try {
        fileWriter = FileWriter(file)

        fileWriter.append("date,close")
        fileWriter.append('\n')

        for (stat in aggregate.toSortedMap()) {
            fileWriter.append(stat.key.toString())
            fileWriter.append(',')
            fileWriter.append(stat.value.toString())
//            fileWriter.append(',')
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
            print("KEK")
        } catch (e: IOException) {
            println("Flushing/closing error!")
            e.printStackTrace()
        }
    }
}*/
