package ru.hse.spb

fun main(args: Array<String>) {
    val files = mapOf(
            1 to "0001",
            2 to "0002",
            3 to "0003",
            4 to "0004",
            5 to "0005",
            6 to "0006",
            7 to "0007",
            8 to "0008",
            9 to "0008",
            9 to "0009",
            10 to "0010"
    )

    var n: Int
    while (true) {
        println("Write number 1-10 to pick one of 10 available XMLs.")
        n = Integer.parseInt(readLine())
        if (n in 1..10) {
            break
        }
        println("Wrong range!")
    }

    val dm = DataManager(files[n]!!)
    dm.fetchXML()
    dm.unzip()
    dm.makeCSV()

    println("If you running this in IDEA write this URL into browser to get graph: " +
            "http://localhost:63342/pubmed/pubmed_main/out.html")
}