import java.io.IOException
import java.io.RandomAccessFile
import java.nio.file.Path

/**
 * Integers in the index file are written as big-endian; strings are written as two bytes representing the string
 * length followed by the characters of the string. There are no delimiters between values.
 *
 * For each unique chromosome the index file contains its name followed by a list of its [FeaturePosition]s.
 */
object RamBedReader : BinaryBedReader() {
    override fun createIndex(bedPath: Path, indexPath: Path) {
        val positionsByChromosomes = getPositionsForChromosomes(bedPath)
        RandomAccessFile(indexPath.toFile(), "rw").use { file ->
            for ((chromosome, positions) in positionsByChromosomes) {
                file.writeUTF(chromosome)
                file.writeInt(positions.size)
                for (position in positions.sortedBy { it.start }) {
                    position.write(file)
                }
            }
        }
    }

    override fun loadIndex(indexPath: Path): BedIndex {
        return RandomAccessFile(indexPath.toFile(), "r").use { file ->
            val chromosomeRanges = generateSequence {
                try {
                    val chromosome = file.readUTF()
                    val entryCount = file.readInt()
                    chromosome to List(entryCount) { FeaturePosition.read(file) }
                } catch (e: IOException) {
                    null
                }
            }.toMap()
            RamBedIndex(chromosomeRanges)
        }
    }
}