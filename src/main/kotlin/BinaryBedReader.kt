import java.io.RandomAccessFile
import java.nio.file.Path

/**
 * Common superclass for readers which use indexes providing positions in BED file by query.
 */
abstract class BinaryBedReader: BedReader {
    override fun findWithIndex(
        index: BedIndex,
        bedPath: Path,
        chromosome: String,
        start: Int,
        end: Int
    ): List<BedEntry> {
        return RandomAccessFile(bedPath.toFile(), "r").use { file ->
            index.usePositions(chromosome, start, end) { positions ->
                positions.map { position ->
                    file.seek(position)
                    BedEntry(file.readLine())
                }.toList()
            }
        }
    }

    /**
     * Returns a map containing a list of corresponding [FeaturePosition]s for each chromosome name.
     * @param bedPath Path of BED file to read.
     */
    protected fun getPositionsForChromosomes(bedPath: Path): Map<String, List<FeaturePosition>> {
        return RandomAccessFile(bedPath.toFile(), "r").use { file ->
            generateSequence {
                val pointer = file.filePointer; file.readLine()?.let { it.split("\\s+".toRegex()) to pointer }
            }
                .dropWhile { it.first[0] == "browser" || it.first[0] == "track" }
                .groupBy({ it.first[0] }) { FeaturePosition(it.first[1].toInt(), it.first[2].toInt(), it.second) }
        }
    }
}