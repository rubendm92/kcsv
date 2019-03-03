package me.rubendm.kcsv

import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import java.net.URL
import java.nio.file.Paths

class CsvReaderSpec: StringSpec({

    "can read the content of a csv by column index" {
        val csv = CsvReader.from(csvFile("example.csv")).read()

        val firstLine = csv.content.first()

        csv.headers shouldBe emptyList()
        firstLine[0] shouldBe "Hello"
        firstLine[1] shouldBe "World"
    }

    "can read the header and content" {
        val csv = CsvReader.from(csvFile("example_with_header.csv")).withHeaders().read()
        val firstLine = csv.content.first()

        csv.headers shouldBe listOf("header1", "header2")
        firstLine[0] shouldBe "Hello"
        firstLine[1] shouldBe "World"
    }

    "can read content by header name" {
        val csv = CsvReader.from(csvFile("example_with_header.csv")).withHeaders().read()

        val firstLine = csv.content.first()

        firstLine["header1"] shouldBe "Hello"
        firstLine["header2"] shouldBe "World"
    }

    "can read a compressed file" {
        val csv = CsvReader.from(csvFile("example_with_header.zip")).withHeaders().zipped().read()

        val firstLine = csv.content.first()
        firstLine[0] shouldBe "Hello"
        firstLine[1] shouldBe "World"
    }

    "can read from a url" {
        val csv = CsvReader.from(csvUrl("example_with_header.csv")).withHeaders().read()

        val firstLine = csv.content.first()
        firstLine[0] shouldBe "Hello"
        firstLine[1] shouldBe "World"
    }

    "can read from a url a zipped file" {
        val csv = CsvReader.from(csvUrl("example_with_header.zip")).withHeaders().zipped().read()

        val firstLine = csv.content.first()
        firstLine[0] shouldBe "Hello"
        firstLine[1] shouldBe "World"
    }
})

val staticUrl: String = System.getenv().getOrDefault("STATIC_URL", "http://localhost:9999")

private fun csvUrl(name: String) = URL("$staticUrl/$name")

private fun csvFile(name: String) = Paths.get("src/test/resources/$name")
