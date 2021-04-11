import java.io.IOException
import java.io.RandomAccessFile
import java.nio.file.Path

object RamBedReader : BedReader {
    override fun createIndex(bedPath: Path, indexPath: Path) {
        val positionsByChromosome = RandomAccessFile(bedPath.toFile(), "r").use { file ->
            generateSequence {
                val pointer = file.filePointer; file.readLine()?.let { it.split("\\s+".toRegex()) to pointer }
            }
                .dropWhile { it.first[0] == "browser" || it.first[0] == "track" }
                .groupBy({ it.first[0] }) { FeaturePosition(it.first[1].toInt(), it.first[2].toInt(), it.second) }
        }
        RandomAccessFile(indexPath.toFile(), "rw").use { file ->
            for ((chromosome, positions) in positionsByChromosome) {
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
}