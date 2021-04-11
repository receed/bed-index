import java.io.DataInput
import java.io.DataOutput

/**
 * A position of a feature in a BED file.
 * @property start The starting position of the feature in the chromosome.
 * @property end The ending position of the feature in the chromosome.
 * @property filePointer The starting position of the feature in the BED file.
 */
data class FeaturePosition(val start: Int, val end: Int, val filePointer: Long) {
    fun write(file: DataOutput) {
        file.writeInt(start)
        file.writeInt(end)
        file.writeLong(filePointer)
    }

    companion object {
        fun read(file: DataInput) =
            FeaturePosition(file.readInt(), file.readInt(), file.readLong())
    }
}