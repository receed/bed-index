import java.io.RandomAccessFile
import java.nio.file.Path

object FileBedReader : BinaryBedReader() {
    override fun createIndex(bedPath: Path, indexPath: Path) {
        val positionsByChromosome = getPositionsForChromosomes(bedPath)
        RandomAccessFile(indexPath.toFile(), "rw").use { file ->
            val firstEntryPointer =
                positionsByChromosome.keys.sumOf { it.toByteArray().size.toLong() } + 10 * (positionsByChromosome.size + 1)
            var currentChromosomePointer = 0L
            var currentEntryPointer = firstEntryPointer
            for ((chromosome, positions) in positionsByChromosome) {
                file.seek(currentChromosomePointer)
                file.writeUTF(chromosome)
                file.writeLong(currentEntryPointer)
                currentChromosomePointer = file.filePointer

                file.seek(currentEntryPointer)
                for (position in positions.sortedBy { it.start }) {
                    position.write(file)
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
                chromosome to FileBedIndex.FileRange(entriesBegin, entriesEnd)
            }.toMap()
            FileBedIndex(chromosomeRanges, indexPath)
        }
    }
}