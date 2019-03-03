package me.rubendm.kcsv

import java.io.*
import java.nio.file.Path
import java.util.zip.ZipFile


object CsvReader {
    fun from(path: Path): CsvFileReader = CsvFileReader.csvFor(path)
}

class CsvFileReader private constructor(private val file: File, private val withHeaders: Boolean) {
    companion object {
        fun csvFor(path: Path) = CsvFileReader(RegularFile(path), withHeaders = false)
    }

    fun withHeaders() = CsvFileReader(file, withHeaders = true)

    fun zipped() = CsvFileReader(me.rubendm.kcsv.ZipFile(file), withHeaders = withHeaders)

    fun read(): Csv = if (withHeaders) CsvFileWithHeaders(CsvFile(file)) else CsvFile(file)
}

sealed class File(val path: Path) {
    abstract val stream: InputStream
}

class RegularFile(path: Path): File(path) {
    override val stream: InputStream get() = path.toFile().inputStream()
}

class ZipFile(file: File): File(file.path) {
    override val stream: InputStream get() {
        val zipFile = ZipFile(path.toFile())
        return zipFile.entries().nextElement().let { zipFile.getInputStream(it) }
    }
}

interface Csv {
    val headers: List<String>
    val content: Sequence<CsvLine>
}

private class CsvFile(private val file: File): Csv {
    override val headers: List<String> = listOf()
    override val content: Sequence<CsvLine> get() = Lines(file.stream)
}

private class CsvFileWithHeaders(csv: Csv): Csv {
    override val headers: List<String> = csv.content.first().toList()
    override val content: Sequence<CsvLine> = csv.content.drop(1).map { it.copy(headers = headers) }
}

data class CsvLine(private val content: List<String>, private val headers: List<String> = listOf()) {

    fun toList() = content

    operator fun get(i: Int): String? = if (i >= content.size) null else content[i]

    operator fun get(headerName: String): String? = if (headers.contains(headerName)) get(headers.indexOf(headerName)) else null
}

private class Lines(private val inputStream: InputStream): Sequence<CsvLine> {
    override fun iterator(): Iterator<CsvLine> {
        return object: Iterator<CsvLine> {
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
    }
}