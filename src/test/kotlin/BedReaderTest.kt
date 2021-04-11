import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.File
import java.nio.file.Path
import kotlin.random.Random
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime
import kotlin.time.nanoseconds

@ExperimentalTime
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BedReaderTest {
    private val tempDirectory = File("tmp")

    @BeforeAll
    fun setup() {
        tempDirectory.mkdir()
        tempDirectory.deleteOnExit()
    }

    class IndexContext(private val index: BedIndex, private val bedPath: Path) {
        fun findWithIndex(chromosome: String, start: Int, end: Int) =
            BinaryBedReader.findWithIndex(index, bedPath, chromosome, start, end)

        fun assertFind(chromosome: String, start: Int, end: Int, expected: List<BedEntry>) {
            val actual = findWithIndex(chromosome, start, end)
            assertEquals(expected.toSet(), actual.toSet())
        }
    }

    private fun getResourcePath(fileName: String) =
        Path.of(this::class.java.getResource(fileName).toURI())

    private fun withIndex(bedPath: Path, block: IndexContext.() -> Unit) {
        val indexPath = Path.of("index")
        val indexCreationTime = measureTime { BinaryBedReader.createIndex(bedPath, indexPath) }
        println("Index creation took $indexCreationTime")
        val start = System.nanoTime()
        val index = BinaryBedReader.loadIndex(indexPath)
        val loadingTime = (System.nanoTime() - start).nanoseconds
        println("Index loading took $loadingTime")
        try {
            IndexContext(index, bedPath).block()
        } finally {
            indexPath.toFile().delete()
        }
    }

    @Test
    fun small() {
        withIndex(getResourcePath("test.bed")) {
            assertFind("chr3", 1, 100, listOf())
            assertFind("chr1", 2, 9, listOf())
            assertFind("chr2", 10, 30, listOf(BedEntry("chr2 10 30"), BedEntry("chr2 17 29")))
            assertFind("chr1", 18, 52, listOf(BedEntry("chr1 20 40"), BedEntry("chr1 20 25")))
        }
    }

    @Test
    fun additionalData() {
        withIndex(getResourcePath("sample.bed")) {
            assertFind(
                "chr7", 127477031, 127478198, listOf(
                    BedEntry(
                        "chr7", 127477031, 127478198, listOf("Neg2", "0", "-", "127477031", "127478198", "0,0,255")
                    )
                )
            )
        }
    }

    data class ChromosomeSpan(val chromosome: String, val start: Int, val end: Int) {
        companion object {
            fun random(chromosomeCount: Int, maxPosition: Int): ChromosomeSpan {
                val chromosome = Random.nextInt(chromosomeCount).toString()
                val (start, end) = List(2) { Random.nextInt(maxPosition) }.sorted()
                return ChromosomeSpan(chromosome, start, end + 1)
            }
        }
    }

    private fun generateBed(
        bedPath: Path,
        entryCount: Int,
        chromosomeCount: Int,
        maxPosition: Int
    ) {
        bedPath.toFile().bufferedWriter().use { writer ->
            repeat(entryCount) {
                val (chromosome, start, end) = ChromosomeSpan.random(chromosomeCount, maxPosition)
                writer.appendLine("$chromosome $start ${end + 1}")
            }
        }
    }

    @Test
    fun stress() {
        val bedPath = tempDirectory.toPath().resolve("large.bed")
        val entryCount = 1000
        val maxPosition = 1000
        val chromosomeCount = 10
        generateBed(bedPath, entryCount, chromosomeCount, maxPosition)
        val simpleIndex = SimpleIndex(bedPath)
        withIndex(bedPath) {
            repeat(10000) {
                val (chromosome, start, end) = ChromosomeSpan.random(chromosomeCount, maxPosition)
                val expected = simpleIndex.find(chromosome, start, end)
                assertFind(chromosome, start, end, expected)
            }
        }
    }

    @Test
    fun measureFindTime() {
        val bedPath = tempDirectory.toPath().resolve("large.bed")
        val entryCount = 100000
        val chromosomeCount = 10
        val maxPosition = 1_000_000_000
        val queries = 1000
        val generationTime = measureTime {
            generateBed(bedPath, entryCount, chromosomeCount, maxPosition)
        }
        println("Generation of the BED file took $generationTime")
        withIndex(bedPath) {
            val time = measureTime {
                repeat(queries) {
                    val (chromosome, start, end) = ChromosomeSpan.random(chromosomeCount, maxPosition)
                    findWithIndex(chromosome, start, end)
                }
            }
            println("$queries queries to an index of $entryCount entries took $time, ${time / queries} per query")
        }
    }
}