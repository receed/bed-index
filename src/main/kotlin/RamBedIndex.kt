class RamBedIndex(private val positionsByChromosome: Map<String, List<FeaturePosition>>) :
    BedIndex {
    override fun <T> usePositions(chromosome: String, start: Int, end: Int, block: (Sequence<Long>) -> T): T {
        val pointers = positionsByChromosome[chromosome] ?: return block(sequenceOf())
        val firstIndex = pointers.binarySearch { if (it.start >= start) 1 else -1 }.let { index ->
            if (index >= 0) index
            else -index - 1
        }
        return block(sequence {
            for (index in firstIndex until pointers.size) {
                if (pointers[index].start >= end)
                    break
                if (pointers[index].end <= end)
                    yield(pointers[index].filePointer)
            }
        })
    }
}