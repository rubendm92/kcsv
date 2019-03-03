package me.rubendm.kcsv

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.URI
import java.net.URL
import java.nio.file.Path
import java.util.zip.ZipInputStream


class CsvReader private constructor(private val file: File, private val withHeaders: Boolean = false) {
    companion object {
        fun from(path: Path) = CsvReader(RegularFile(path))

        fun from(url: URL): CsvReader = CsvReader(RemoteFile(url))
    }

    fun withHeaders() = CsvReader(file, withHeaders = true)

    fun zipped() = CsvReader(ZipFile(file), withHeaders = withHeaders)

    fun read(): Csv = if (withHeaders) CsvFileWithHeaders(CsvFile(file)) else CsvFile(file)
}

sealed class File(val uri: URI) {
    abstract val stream: InputStream
}

class RegularFile(path: Path): File(path.toUri()) {
    override val stream: InputStream get() = java.io.File(uri).inputStream()
}

class ZipFile(private val file: File): File(file.uri) {
    override val stream: InputStream get() {
        val stream = ZipInputStream(file.stream)
        stream.nextEntry
        return stream
    }
}

class RemoteFile(url: URL): File(url.toURI()) {
    override val stream: InputStream get() = uri.toURL().openStream()
}

interface Csv {
    val headers: List<String>
    val content: Sequence<CsvLine>
}

private class CsvFile(private val file: File): Csv {
    override val headers: List<String> = listOf()
    override val content: Sequence<CsvLine> get() = CsvIterator(file.stream).asSequence()
}

private class CsvFileWithHeaders(csv: Csv): Csv {
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
