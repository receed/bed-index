import java.io.DataInput
import java.io.DataOutput

/**
 * A position of a feature in a BED file.
 * @property start The starting position of the feature in the chromosome.
 * @property end The ending position of the feature in the chromosome.
 * @property filePointer The starting position of the feature in the BED file.
 */
data class FeaturePosition(val start: Int, val end: Int, val filePointer: Long) {
    /**
     * Writes the position to a binary index file.
     */
    fun write(file: DataOutput) {
        file.writeInt(start)
        file.writeInt(end)
        file.writeLong(filePointer)
    }
    companion object {
        /**
         * Number of bytes an entry occupies in an index file.
         */
        val SIZE_BYTES = Long.SIZE_BYTES * 2
        /**
         * Reads a position from a binary index file.
         */
        fun read(file: DataInput) =
            FeaturePosition(file.readInt(), file.readInt(), file.readLong())
    }
}