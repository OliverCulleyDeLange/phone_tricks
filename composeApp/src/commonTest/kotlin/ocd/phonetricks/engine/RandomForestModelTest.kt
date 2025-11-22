package ocd.phonetricks.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RandomForestModelTest {

    @Test
    fun testModelLoading() {
        val mockModelJson = """
        {
          "model_type": "RandomForest",
          "classes": ["CLASS_A", "CLASS_B"],
          "n_features": 4,
          "n_classes": 2,
          "n_trees": 2,
          "trees": [
            {
              "children_left": [-1, -1],
              "children_right": [-1, -1],
              "feature": [-2, -2],
              "threshold": [0.0, 0.0],
              "value": [[[10.0, 0.0]], [[0.0, 10.0]]],
              "n_node_samples": [10, 10]
            },
            {
              "children_left": [-1, -1],
              "children_right": [-1, -1],
              "feature": [-2, -2],
              "threshold": [0.0, 0.0],
              "value": [[[10.0, 0.0]], [[0.0, 10.0]]],
              "n_node_samples": [10, 10]
            }
          ],
          "scaler": {
            "mean": [0.0, 0.0, 0.0, 0.0],
            "scale": [1.0, 1.0, 1.0, 1.0]
          }
        }
        """.trimIndent()

        val model = RandomForestModel.loadFromJson(mockModelJson)
        assertNotNull(model)
    }

    @Test
    fun testPrediction() {
        val mockModelJson = """
        {
          "model_type": "RandomForest",
          "classes": ["CLASS_A", "CLASS_B"],
          "n_features": 4,
          "n_classes": 2,
          "n_trees": 2,
          "trees": [
            {
              "children_left": [-1, -1],
              "children_right": [-1, -1],
              "feature": [-2, -2],
              "threshold": [0.0, 0.0],
              "value": [[[10.0, 0.0]], [[0.0, 10.0]]],
              "n_node_samples": [10, 10]
            },
            {
              "children_left": [-1, -1],
              "children_right": [-1, -1],
              "feature": [-2, -2],
              "threshold": [0.0, 0.0],
              "value": [[[10.0, 0.0]], [[0.0, 10.0]]],
              "n_node_samples": [10, 10]
            }
          ],
          "scaler": {
            "mean": [0.0, 0.0, 0.0, 0.0],
            "scale": [1.0, 1.0, 1.0, 1.0]
          }
        }
        """.trimIndent()

        val model = RandomForestModel.loadFromJson(mockModelJson)
        val features = floatArrayOf(1f, 2f, 3f, 4f)

        val (predictedClass, confidence) = model.predict(features)

        assertNotNull(predictedClass)
        assertTrue(predictedClass == "CLASS_A" || predictedClass == "CLASS_B")
        assertTrue(confidence >= 0f && confidence <= 1f)
    }

    @Test
    fun testPredictProba() {
        val mockModelJson = """
        {
          "model_type": "RandomForest",
          "classes": ["CLASS_A", "CLASS_B"],
          "n_features": 4,
          "n_classes": 2,
          "n_trees": 2,
          "trees": [
            {
              "children_left": [-1, -1],
              "children_right": [-1, -1],
              "feature": [-2, -2],
              "threshold": [0.0, 0.0],
              "value": [[[10.0, 0.0]], [[0.0, 10.0]]],
              "n_node_samples": [10, 10]
            },
            {
              "children_left": [-1, -1],
              "children_right": [-1, -1],
              "feature": [-2, -2],
              "threshold": [0.0, 0.0],
              "value": [[[10.0, 0.0]], [[0.0, 10.0]]],
              "n_node_samples": [10, 10]
            }
          ],
          "scaler": {
            "mean": [0.0, 0.0, 0.0, 0.0],
            "scale": [1.0, 1.0, 1.0, 1.0]
          }
        }
        """.trimIndent()

        val model = RandomForestModel.loadFromJson(mockModelJson)
        val features = floatArrayOf(1f, 2f, 3f, 4f)

        val probabilities = model.predictProba(features)

        assertEquals(2, probabilities.size)
        assertTrue(probabilities.containsKey("CLASS_A"))
        assertTrue(probabilities.containsKey("CLASS_B"))

        val sumProba = probabilities.values.sum()
        assertEquals(1.0f, sumProba, 0.01f)
    }
}
