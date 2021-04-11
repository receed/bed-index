import java.io.RandomAccessFile
import java.nio.file.Path

class BinaryBedIndex(private val chromosomeRanges: Map<String, FileRange>, private val indexPath: Path) :
    BedIndex {
    override fun <T> usePositions(chromosome: String, start: Int, end: Int, block: (Sequence<Long>) -> T): T {
        val range = chromosomeRanges[chromosome] ?: return block(sequenceOf())
        return RandomAccessFile(indexPath.toFile(), "r").use { file ->
            var left = -1L
            var right = (range.end - range.start) / ENTRY_SIZE - 1
            while (right - left > 1) {
                val middle = (left + right) / 2
                file.seek(range.start + middle * ENTRY_SIZE)
                val middleStart = file.readInt()
                if (middleStart < start)
                    left = middle
                else
                    right = middle
            }
            file.seek(range.start + right * ENTRY_SIZE)
            block(generateSequence {
                if (file.filePointer >= range.end)
                    null
                else
                    FeaturePosition.read(file)
            }.takeWhile { it.start < end }.filter { it.end <= end }.map { it.filePointer })
        }
    }

    companion object {
        const val ENTRY_SIZE = Long.SIZE_BYTES * 2
    }
}