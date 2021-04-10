import java.io.RandomAccessFile
import java.nio.file.Path

data class Position(val start: Int, val end: Int, val filePointer: Long)

object BinaryBedReader : BedReader {
    override fun createIndex(bedPath: Path, indexPath: Path) {
        val index = mutableMapOf<String, MutableList<Position>>()
        RandomAccessFile(bedPath.toFile(), "r").use { file ->
            generateSequence { val pointer = file.filePointer; file.readLine()?.let { it.split('\t', ' ') to pointer } }
                .dropWhile { it.first[0] == "browser" || it.first[0] == "track" }
                .groupByTo(index, { it.first[0] }) { Position(it.first[1].toInt(), it.first[2].toInt(), it.second) }
        }
        RandomAccessFile(indexPath.toFile(), "rw").use { file ->
            val firstEntryPointer = index.keys.sumOf { it.toByteArray().size.toLong() } + 10 * index.size + 2
            var currentChromosomePointer = 0L
            var currentEntryPointer = firstEntryPointer
            for ((chromosome, positions) in index) {
                file.seek(currentEntryPointer)
                for (position in positions) {
                    file.writeInt(position.start)
                    file.writeInt(position.end)
                    file.writeLong(position.filePointer)
                }
                currentEntryPointer = file.filePointer

                file.seek(currentChromosomePointer)
                file.writeUTF(chromosome)
                file.writeLong(currentEntryPointer)
                currentChromosomePointer = file.filePointer
            }
            file.seek(currentChromosomePointer)
            file.writeUTF("")
            currentChromosomePointer = file.filePointer
            require(currentChromosomePointer == firstEntryPointer)
        }
    }

    override fun loadIndex(indexPath: Path): BedIndex {
        val file = RandomAccessFile(indexPath.toFile(), "r")
        val chromosomeRanges = mutableMapOf<String, LongRange>()
        generateSequence {
            file.readUTF().takeIf { it.isNotEmpty() }?.let {
                it to file.readLong()
            }
        }.fold(file.filePointer) { entriesBegin, (chromosome, entriesEnd) ->
            chromosomeRanges[chromosome] = entriesBegin until entriesEnd
            entriesEnd
        }
        return BinaryBedIndex(chromosomeRanges, file)
    }

    override fun findWithIndex(
        index: BedIndex,
        bedPath: Path,
        chromosome: String,
        start: Int,
        end: Int
    ): List<BedEntry> {
        TODO("Not yet implemented")
    }
}