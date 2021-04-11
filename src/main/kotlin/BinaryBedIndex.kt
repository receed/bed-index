import java.io.RandomAccessFile
import java.nio.file.Path

class BinaryBedIndex(private val chromosomeRanges: Map<String, FileRange>, private val indexPath: Path) :
    BedIndex {
    override fun <T> usePositions(chromosome: String, start: Int, end: Int, block: (Sequence<Long>) -> T): T {
        val range = chromosomeRanges[chromosome] ?: return block(sequenceOf())
        return RandomAccessFile(indexPath.toFile(), "r").use { file ->
            val entriesCount = (range.end - range.start) / FeaturePosition.SIZE_BYTES
            val firstEntryNumber = binarySearch(0L, entriesCount) { position ->
                file.seek(range.start + position * FeaturePosition.SIZE_BYTES)
                file.readInt() >= start
            }
            file.seek(range.start + firstEntryNumber * FeaturePosition.SIZE_BYTES)
            block(generateSequence {
                if (file.filePointer >= range.end)
                    null
                else
                    FeaturePosition.read(file)
            }.takeWhile { it.start < end }.filter { it.end <= end }.map { it.filePointer })
        }
    }

    companion object {
        /**
         * Finds the first value in the range from [start] (inclusive) to [end] (exclusive) matching
         * [predicate], assuming all values matching [predicate] form a suffix of the range.
         * If there is no such value, returns [end].
         */
        fun binarySearch(start: Long, end: Long, predicate: (Long) -> Boolean): Long {
            var left = start - 1
            var right = end
            while (right - left > 1) {
                val middle = (left + right) / 2
                if (predicate(middle))
                    right = middle
                else
                    left = middle
            }
            return right
        }
    }
}