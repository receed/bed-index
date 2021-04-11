import java.nio.file.Path

internal class SimpleIndex(path: Path) {
    private val entries = path.toFile().useLines { lines ->
        lines.map { BedEntry(it) }.toList()
    }

    fun find(chromosome: String, start: Int, end: Int): List<BedEntry> =
        entries.filter { it.chromosome == chromosome && start <= it.start && it.end <= end }
}