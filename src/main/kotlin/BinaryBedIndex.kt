import java.io.RandomAccessFile
import java.nio.file.Path

class BinaryBedIndex(private val chromosomeRanges: Map<String, FileRange>, private val indexPath: Path) :
    BedIndex {
    override fun <T> usePositions(chromosome: String, start: Int, end: Int, block: (Sequence<Long>) -> T): T {
        val range = chromosomeRanges[chromosome] ?: return block(sequenceOf())
        return RandomAccessFile(indexPath.toFile(), "r").use { file ->
            var left = -1L
            var right = (range.end - range.start) / 16 - 1
            while (right - left > 1) {
                val middle = (left + right) / 2
                file.seek(range.start + middle * 16 + 4)
                val middleStart = file.readInt()
                if (middleStart < start)
                    left = middle
                else
                    right = middle
            }
            file.seek(left)
            block(sequence {
                if (file.filePointer >= range.end)
                    return@sequence
                val position = Position(file.readInt(), file.readInt(), file.readLong())
                if (position.start >= end)
                    return@sequence
                if (position.end <= end)
                    yield(position.filePointer)
            })
        }
    }
}