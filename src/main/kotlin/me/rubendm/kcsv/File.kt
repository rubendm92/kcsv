package me.rubendm.kcsv

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

interface Csv {
    val headers: List<String>
    val content: Sequence<CsvLine>
}

class CsvFile(private val file: File): Csv {
    override val headers: List<String> = listOf()
    override val content: Sequence<CsvLine> get() = CsvIterator(file.stream).asSequence()
}

class CsvFileWithHeaders(csv: Csv): Csv {
    override val headers: List<String> = csv.content.first().toList()
    override val content: Sequence<CsvLine> = csv.content.drop(1).map { it.copy(headers = headers) }
}

private class CsvIterator(inputStream: InputStream): Iterator<CsvLine> {
    val reader = BufferedReader(InputStreamReader(inputStream))
    var line: String? = reader.readLine()

    override fun hasNext(): Boolean = line != null

    override fun next(): CsvLine = if (hasNext()) {
        val csvLine = CsvLine(line!!.split(";"), listOf())
        line = reader.readLine()
        csvLine
    } else {
        throw NoSuchElementException()
    }
}

data class CsvLine(private val content: List<String>, private val headers: List<String> = listOf()) {
    fun toList() = content
    operator fun get(i: Int): String? = if (i >= content.size) null else content[i]
    operator fun get(headerName: String): String? = if (headers.contains(headerName)) get(headers.indexOf(headerName)) else null
}
