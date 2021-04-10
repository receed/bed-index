import java.io.RandomAccessFile

class BinaryBedIndex(private val chromosomeRanges: Map<String, LongRange>, private val indexFile: RandomAccessFile) :
    BedIndex, AutoCloseable {
    fun getRange(chromosome: String) {
    }

    override fun close() {
        indexFile.close()
    }
}