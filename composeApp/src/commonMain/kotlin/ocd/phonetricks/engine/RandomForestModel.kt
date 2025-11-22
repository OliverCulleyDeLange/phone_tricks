package ocd.phonetricks.engine

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class DecisionTree(
    val children_left: List<Int>,
    val children_right: List<Int>,
    val feature: List<Int>,
    val threshold: List<Double>,
    val value: List<List<List<Double>>>,
    val n_node_samples: List<Int>
)

@Serializable
data class Scaler(
    val mean: List<Double>,
    val scale: List<Double>
)

@Serializable
data class RandomForestModelData(
    val model_type: String,
    val classes: List<String>,
    val n_features: Int,
    val n_classes: Int,
    val n_trees: Int,
    val trees: List<DecisionTree>,
    val scaler: Scaler
)

class RandomForestModel(private val modelData: RandomForestModelData) {

    fun predict(features: FloatArray): Pair<String, Float> {
        val scaledFeatures = scaleFeatures(features)

        val votes = FloatArray(modelData.n_classes) { 0f }

        for (tree in modelData.trees) {
            val prediction = predictTree(tree, scaledFeatures)
            votes[prediction] += 1f
        }

        val maxIndex = votes.indices.maxByOrNull { votes[it] } ?: 0
        val confidence = votes[maxIndex] / modelData.n_trees

        return Pair(modelData.classes[maxIndex], confidence)
    }

    fun predictProba(features: FloatArray): Map<String, Float> {
        val scaledFeatures = scaleFeatures(features)

        val votes = FloatArray(modelData.n_classes) { 0f }

        for (tree in modelData.trees) {
            val prediction = predictTree(tree, scaledFeatures)
            votes[prediction] += 1f
        }

        return modelData.classes.indices.associate { i ->
            modelData.classes[i] to (votes[i] / modelData.n_trees)
        }
    }

    private fun scaleFeatures(features: FloatArray): FloatArray {
        require(features.size == modelData.n_features) {
            "Expected ${modelData.n_features} features, got ${features.size}"
        }

        return FloatArray(features.size) { i ->
            ((features[i] - modelData.scaler.mean[i]) / modelData.scaler.scale[i]).toFloat()
        }
    }

    private fun predictTree(tree: DecisionTree, features: FloatArray): Int {
        var nodeIndex = 0

        while (tree.children_left[nodeIndex] != -1) {
            val featureIndex = tree.feature[nodeIndex]
            val threshold = tree.threshold[nodeIndex]

            nodeIndex = if (features[featureIndex] <= threshold) {
                tree.children_left[nodeIndex]
            } else {
                tree.children_right[nodeIndex]
            }
        }

        val value = tree.value[nodeIndex][0]
        return value.indices.maxByOrNull { value[it] } ?: 0
    }

    companion object {
        fun loadFromJson(jsonString: String): RandomForestModel {
            val json = Json { ignoreUnknownKeys = true }
            val modelData = json.decodeFromString<RandomForestModelData>(jsonString)
            return RandomForestModel(modelData)
        }
    }
}
