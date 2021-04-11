import java.io.RandomAccessFile
import java.nio.file.Path

/**
 * Integers in the bed files are written as big-endian; strings are written as two bytes representing the string length
 * followed by the characters of the string. There are no delimiters between values.
 *
 * Index file has the following format.
 *
 * `chr1` `startPointer1`
 *
 * ...
 *
 * `chrN` startPointerN`
 *
 * "" `endPointerN`
 *
 * `featureStart1` `featureEnd1` `pointer1`
 *
 * `featureStart2` `featureEnd2` `pointer2`
 *
 * where `chr1`, `chr2`, ... are all the unique chromosome names,
 * `startPointer` is the pointer to the start of the first feature in the index file,
 * `endPointerN` is the pointer after the end of the last feature of the last chromosome,
 * `featureStart` and `featureEnd` are the start and the end of the feature in the chromosome,
 * `pointer` is file pointer to the start of the feature description in the BED file.
 * Features for each chromosome are listed in increasing order of their starting positions in the chromosome.
 *
 * @property chromosomeRanges The ranges of the features for the chromosomes in the index file.
 * @property indexPath Path to index file.
 */
class FileBedIndex(private val chromosomeRanges: Map<String, FileRange>, private val indexPath: Path) :
    BedIndex {
    /**
     * A half-open range of bytes in the index file.
     */
    data class FileRange(val start: Long, val end: Long)

    /*
     Finds the first feature in the index for which `start` is at least [start] using binary search, then
     traverses all the features for which `start` is inside the range from [start] inclusive to [end] exclusive
     and yields only features with `end` not greater than [end].
     */
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