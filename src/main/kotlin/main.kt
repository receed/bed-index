import java.nio.file.Path

fun main() {
    val bedPath = Path.of("test.bed")
    val indexPath = Path.of("index")
    BinaryBedReader.createIndex(bedPath, indexPath)
    val index = BinaryBedReader.loadIndex(indexPath)
    val entries = BinaryBedReader.findWithIndex(index, bedPath, "chr7", 127475000, 127481000)
    println(entries.joinToString("\n"))
}