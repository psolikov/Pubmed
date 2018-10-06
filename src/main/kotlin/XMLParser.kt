package ru.hse.spb

import org.xml.sax.Attributes
import org.xml.sax.SAXException
import org.xml.sax.helpers.DefaultHandler
import java.io.IOException
import javax.xml.parsers.ParserConfigurationException
import javax.xml.parsers.SAXParserFactory


class XMLParser(private val path: String) : DefaultHandler() {

    val papers = ArrayList<Paper>()
    var inPubDate = false
    private var currentText: String? = null
    private var paper = Paper()
    private var inArticle = false

    fun parseDocument() {
        val factory = SAXParserFactory.newInstance()
        try {
            val parser = factory.newSAXParser()
            parser.parse(path, this)
        } catch (e: ParserConfigurationException) {
            println("ParserConfig error")
        } catch (e: SAXException) {
            println("SAXException : xml not well formed")
        } catch (e: IOException) {
            println("IO error")
        }
    }

    fun printPapers() {
        papers.forEach { p -> println("Date:${p.date} contains:${p.containsTags}") }
    }

    private fun stringContainsItemFromList(items: ArrayList<String>): Boolean {
        return items.stream().parallel().allMatch { currentText?.contains(it) ?: false }
    }

    override fun startElement(uri: String?, localName: String?, qName: String?, attributes: Attributes?) {
        when (qName) {
            "Article" -> {
                paper = Paper()
                inArticle = true
            }
            "DateCompleted" -> {
                if (inArticle) {
                    inPubDate = true
                }
            }
        }
    }

    override fun endElement(uri: String?, localName: String?, qName: String?) {
        when (qName) {
            "Article" -> {
                inArticle = false
                papers.add(paper)
            }
            "DateCompleted" -> {
                inPubDate = false
            }
            "Year" -> {
                paper.date = Integer.parseInt(currentText)
            }
            "Abstract" -> {
                if (inArticle && currentText != null) {
                    paper.containsTags = stringContainsItemFromList(paper.tags)
                    /*paper.containsTags = (currentText!!.contains("aging")
                            || currentText!!.contains("notch"))*/
                }
            }
        }
    }

    @Throws(SAXException::class)
    override fun characters(ac: CharArray, i: Int, j: Int) {
        currentText = String(ac, i, j)
    }

    class Paper {
        val tags = arrayListOf("Aging", "NOTCH")
        var date: Int = 0
        var containsTags = false
    }
}

