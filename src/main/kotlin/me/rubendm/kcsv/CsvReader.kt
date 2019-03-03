package me.rubendm.kcsv

import java.io.*
import java.nio.file.Path
import java.util.zip.ZipFile


object CsvReader {
    fun from(path: Path): CsvFileReader = CsvFileReader.csvFor(path)
}

class CsvFileReader private constructor(private val path: Path, private val withHeaders: Boolean, private val zipped: Boolean) {
    companion object {
        fun csvFor(path: Path) = CsvFileReader(path, withHeaders = false, zipped = false)
    }

    fun withHeaders() = CsvFileReader(path, withHeaders = true, zipped = zipped)

    fun zipped() = CsvFileReader(path, withHeaders = withHeaders, zipped = true)

    fun read(): Csv = when {
        zipped -> ZippedCsvFile(path) { if (withHeaders) CsvFileWithHeaders(CsvStream(it)) else CsvStream(it) }
        withHeaders -> CsvFileWithHeaders(CsvFile(path))
        else -> CsvFile(path)
    }
}

interface Csv {
    val headers: List<String>
    val content: Sequence<CsvLine>
}

private class CsvStream(stream: InputStream): Csv {
    override val headers: List<String> = listOf()
    override val content: Sequence<CsvLine> = Lines(stream)
}

private class CsvFile(private val path: Path): Csv by CsvStream(path.toFile().inputStream())

private class CsvFileWithHeaders(csv: Csv): Csv {
    private var temporalHeaders: List<String> = listOf()
    override val headers: List<String> get() = temporalHeaders
    override val content: Sequence<CsvLine> = csv.content.mapNotNull {
        // Ugly trick because the content can only be read once
        if (temporalHeaders.isEmpty()) {
            temporalHeaders = it.toList()
            null
        } else {
            it.copy(headers = headers)
        }
    }
}

class ZippedCsvFile(private val zipped: Path, private val f: (InputStream) -> Csv) : Csv by f(streamOf(zipped)) {
    companion object {
        private fun streamOf(path: Path): InputStream {
            val zipFile = ZipFile(path.toFile())
            return zipFile.entries().nextElement().let { zipFile.getInputStream(it) }
        }
    }
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