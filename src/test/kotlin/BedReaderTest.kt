import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.file.Path

internal class BedReaderTest {
    class IndexContext(val index: BedIndex, val bedPath: Path) {
        fun findWithIndex(chromosome: String, start: Int, end: Int) =
            BinaryBedReader.findWithIndex(index, bedPath, chromosome, start, end)

        fun assertFind(chromosome: String, start: Int, end: Int, expected: Set<BedEntry>) {
            val actual = findWithIndex(chromosome, start, end)
            assertEquals(expected, actual.toSet())
        }
    }

    fun withIndex(bedFileName: String, block: IndexContext.() -> Unit) {
        val bedPath = Path.of(this::class.java.getResource(bedFileName).toURI())
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
        withIndex("test.bed") {
            assertFind("chr3", 1, 100, setOf())
            assertFind("chr1", 2, 9, setOf())
            assertFind("chr2", 10, 30, setOf(BedEntry("chr2 10 30"), BedEntry("chr2 17 29")))
            assertFind("chr1", 18, 52, setOf(BedEntry("chr1 20 40"), BedEntry("chr1 20 25")))
        }
    }

    @Test
    fun additionalData() {
        withIndex("sample.bed") {
            assertFind("chr7", 127477031, 127478198, setOf(BedEntry(
                "chr7", 127477031, 127478198,  listOf("Neg2", "0", "-", "127477031",  "127478198", "0,0,255"))))
        }
    }
}