import java.io.RandomAccessFile
import java.nio.file.Path

data class Position(val start: Int, val end: Int, val filePointer: Long)
data class FileRange(val start: Long, val end: Long)

object BinaryBedReader : BedReader {
    override fun createIndex(bedPath: Path, indexPath: Path) {
        val index = RandomAccessFile(bedPath.toFile(), "r").use { file ->
            generateSequence { val pointer = file.filePointer; file.readLine()?.let { it.split('\t', ' ') to pointer } }
                .dropWhile { it.first[0] == "browser" || it.first[0] == "track" }
                .groupBy({ it.first[0] }) { Position(it.first[1].toInt(), it.first[2].toInt(), it.second) }
        }
        RandomAccessFile(indexPath.toFile(), "rw").use { file ->
            val firstEntryPointer = index.keys.sumOf { it.toByteArray().size.toLong() } + 10 * (index.size + 1)
            var currentChromosomePointer = 0L
            var currentEntryPointer = firstEntryPointer
            for ((chromosome, positions) in index) {
                file.seek(currentChromosomePointer)
                file.writeUTF(chromosome)
                file.writeLong(currentEntryPointer)
                currentChromosomePointer = file.filePointer

                file.seek(currentEntryPointer)
                for (position in positions.sortedBy { it.start }) {
                    file.writeInt(position.start)
                    file.writeInt(position.end)
                    file.writeLong(position.filePointer)
                }
                currentEntryPointer = file.filePointer

            }
            file.seek(currentChromosomePointer)
            file.writeUTF("")
            file.writeLong(currentEntryPointer)
            currentChromosomePointer = file.filePointer
            require(currentChromosomePointer == firstEntryPointer)
        }
    }

    override fun loadIndex(indexPath: Path): BedIndex {
        return RandomAccessFile(indexPath.toFile(), "r").use { file ->
            val chromosomeRanges = sequence {
                while (true) {
                    val chromosome = file.readUTF()
                    yield(chromosome to file.readLong())
                    if (chromosome.isEmpty())
                        break
                }
            }.zipWithNext { (chromosome, entriesBegin), (_, entriesEnd) ->
                chromosome to FileRange(entriesBegin, entriesEnd)
            }.toMap()
            BinaryBedIndex(chromosomeRanges, indexPath)
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
                    val items = file.readLine().split('\t', ' ')
                    BedEntry(items[0], items[1].toInt(), items[2].toInt(), items.drop(3))
                }.toList()
            }
        }
    }
}