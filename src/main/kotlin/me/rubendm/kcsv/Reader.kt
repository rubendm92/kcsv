package me.rubendm.kcsv

import java.io.InputStream
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

private class RegularFile(path: Path): File(path.toUri()) {
    override val stream: InputStream get() = java.io.File(uri).inputStream()
}

private class ZipFile(private val file: File): File(file.uri) {
    override val stream: InputStream get() {
        val stream = ZipInputStream(file.stream)
        stream.nextEntry
        return stream
    }
}

private class RemoteFile(url: URL): File(url.toURI()) {
    override val stream: InputStream get() = uri.toURL().openStream()
}
