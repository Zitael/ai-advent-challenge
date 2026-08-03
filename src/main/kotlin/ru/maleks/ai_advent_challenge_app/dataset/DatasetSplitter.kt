package ru.maleks.ai_advent_challenge_app.dataset

import kotlin.random.Random

class DatasetSplitter(
    private val trainRatio: Double = 0.8,
    private val random: Random = Random(42)
) {

    init {
        require(trainRatio in 0.0..1.0) {
            "trainRatio must be between 0 and 1"
        }
    }

    fun split(examples: List<FineTuningExample>): Pair<List<FineTuningExample>, List<FineTuningExample>> {
        require(examples.size >= 2) {
            "At least 2 examples required for split"
        }

        val shuffled = examples.shuffled(random)
        val trainSize = (shuffled.size * trainRatio).toInt().coerceAtLeast(1)
        val boundedTrainSize = trainSize.coerceAtMost(shuffled.size - 1)

        val train = shuffled.take(boundedTrainSize)
        val eval = shuffled.drop(boundedTrainSize)
        return train to eval
    }
}
