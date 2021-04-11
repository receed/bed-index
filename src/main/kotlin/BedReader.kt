import java.nio.file.Path

data class BedEntry(val chromosome: String, val start: Int, val end: Int, val other: List<Any>)

fun BedEntry(line: String): BedEntry {
    val items = line.split("\\s+".toRegex())
    return BedEntry(items[0], items[1].toInt(), items[2].toInt(), items.drop(3))
}

interface BedIndex {
    /**
     * Calls the [block] callback giving it a sequence of positions in the BED file of all the features
     * located on the given [chromosome] inside the range from [start] inclusive to [end] exclusive.
     */
    fun <T> usePositions(chromosome: String, start: Int, end: Int, block: (Sequence<Long>) -> T): T
}

interface BedReader {

    /**
     * Creates index for [bedPath] and saves it to [indexPath]
     */
    fun createIndex(bedPath: Path, indexPath: Path)

    /**
     * Loads [BedIndex] instance from file [indexPath]
     */
    fun loadIndex(indexPath: Path): BedIndex

    /**
     * Loads list of [BedEntry] from file [bedPath] using [index].
     * All the loaded entries should be located on the given [chromosome],
     * and be inside the range from [start] inclusive to [end] exclusive.
     * E.g. entry [1, 2) is inside [0, 2), but not inside [0, 1).
     */
    fun findWithIndex(
        index: BedIndex, bedPath: Path,
        chromosome: String, start: Int, end: Int
    ): List<BedEntry>

}

fun BedReader(): BedReader = RamBedReader