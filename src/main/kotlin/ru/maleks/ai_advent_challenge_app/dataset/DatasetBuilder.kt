package ru.maleks.ai_advent_challenge_app.dataset

import java.nio.file.Path

class DatasetBuilder(
    private val realTicketSource: RealTicketSource,
    private val syntheticGenerator: SyntheticTicketGenerator = SyntheticTicketGenerator(),
    private val cleaner: DatasetCleaner = DatasetCleaner(),
    private val splitter: DatasetSplitter = DatasetSplitter(),
    private val jsonlIo: JsonlDatasetIO = JsonlDatasetIO(),
    private val minimumExamples: Int = 50
) {

    fun build(
        datasetDirectory: Path,
        syntheticTargetCount: Int = 40
    ): DatasetBuildReport {
        val realExamples = realTicketSource.load()
        val syntheticExamples = syntheticGenerator.generate(syntheticTargetCount)
        val combined = cleaner.clean(realExamples + syntheticExamples)

        require(combined.size >= minimumExamples) {
            "Dataset has ${combined.size} examples after cleaning, need at least $minimumExamples"
        }

        val realInCombined = combined.count { it.real }
        require(realInCombined >= (combined.size * 0.2).toInt()) {
            "Real examples must be at least 20% of dataset, got $realInCombined/${combined.size}"
        }

        val (train, eval) = splitter.split(combined)
        val trainPath = datasetDirectory.resolve("train.jsonl")
        val evalPath = datasetDirectory.resolve("eval.jsonl")

        jsonlIo.write(trainPath, train)
        jsonlIo.write(evalPath, eval)

        return DatasetBuildReport(
            totalRaw = realExamples.size + syntheticExamples.size,
            afterCleaning = combined.size,
            realCount = realInCombined,
            syntheticCount = combined.size - realInCombined,
            trainCount = train.size,
            evalCount = eval.size,
            trainPath = trainPath.toString(),
            evalPath = evalPath.toString()
        )
    }
}
