import java.nio.file.Path

fun main() {
    val bedPath = Path.of("test.bed")
    val indexPath = Path.of("index")
    FileBedReader.createIndex(bedPath, indexPath)
    val index = FileBedReader.loadIndex(indexPath)
    val entries = FileBedReader.findWithIndex(index, bedPath, "chr7", 127475000, 127481000)
    println(entries.joinToString("\n"))
}