import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.random.Random

internal class BedReaderTest {
    class IndexContext(val index: BedIndex, val bedPath: Path) {
        private fun findWithIndex(chromosome: String, start: Int, end: Int) =
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
        BinaryBedReader.createIndex(bedPath, indexPath)
        val index = BinaryBedReader.loadIndex(indexPath)
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

    private fun generateBed(
        bedPath: Path,
        entryCount: Int,
        maxPosition: Int = 1_000_000_000,
        chromosomeCount: Int = 10
    ) {
        bedPath.toFile().bufferedWriter().use { writer ->
            repeat(entryCount) {
                val chromosome = Random.nextInt(chromosomeCount)
                val (start, end) = List(2) { Random.nextInt(maxPosition) }.sorted()
                writer.appendLine("$chromosome $start $end")
            }
        }
    }

    @Test
    fun stress() {
        val bedPath = Path.of("large.bed")
        val entryCount = 1000
        val maxPosition = 1000
        val chromosomeCount = 10
        generateBed(bedPath, entryCount, maxPosition, chromosomeCount)
        val simpleIndex = SimpleIndex(bedPath)
        withIndex(bedPath) {
            repeat(10000) {
                val chromosome = Random.nextInt(chromosomeCount).toString()
                val (start, end) = List(2) { Random.nextInt(maxPosition) }.sorted()
                val expected = simpleIndex.find(chromosome, start, end)
                assertFind(chromosome, start, end, expected)
            }
        }
    }
}