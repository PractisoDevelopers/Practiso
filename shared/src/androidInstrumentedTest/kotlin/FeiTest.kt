import com.zhufucdev.practiso.JinaV2SmallEn
import com.zhufucdev.practiso.database.TextFrame
import com.zhufucdev.practiso.datamodel.EmbeddingOutput
import com.zhufucdev.practiso.datamodel.Frame
import com.zhufucdev.practiso.datamodel.MlModel
import com.zhufucdev.practiso.platform.FrameEmbeddingInference
import com.zhufucdev.practiso.platform.getPlatform
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Test
import space.kscience.kmath.PerformancePitfall
import space.kscience.kmath.linear.Float64LinearSpace
import space.kscience.kmath.linear.asMatrix
import space.kscience.kmath.nd.Floa64FieldOpsND.Companion.div
import space.kscience.kmath.nd.Floa64FieldOpsND.Companion.plus
import space.kscience.kmath.nd.Floa64FieldOpsND.Companion.pow
import space.kscience.kmath.nd.Floa64FieldOpsND.Companion.sqrt

class FeiTest {
    @Test
    fun inferenceOne() = runBlocking {
        val inference = FrameEmbeddingInference(JinaV2SmallEn)
        val emb = inference.getEmbeddings(newTextFrame("How's the weather like today?"))
        val embeddingsFeature = inference.model.embeddingsFeature()
        assertEquals("Output dimensions mismatch", embeddingsFeature.dimensions.toInt(), emb.size)
    }

    @Test
    fun inferenceMany() = runBlocking {
        val inference = FrameEmbeddingInference(JinaV2SmallEn)
        repeat(getPlatform().logicalProcessorsCount) {
            async(Dispatchers.Default) {
                val embs = inference.getEmbeddings(
                    listOf(
                        newTextFrame("I love cats the most"),
                        newTextFrame("How can the stock market fall so harshly?"),
                        newTextFrame("Capybaras are cute animal"),
                        newTextFrame("What"),
                        newTextFrame("Core dumped and segmentation fault are bad logcat"),
                    )
                )
                val feature = inference.model.embeddingsFeature()

                embs.forEach {
                    assertEquals("Output dimensions mismatch", feature.dimensions.toInt(), it.size)
                }
            }
        }
    }

    @Test
    fun similarity() = runBlocking {
        val inference = FrameEmbeddingInference(JinaV2SmallEn)
        val similarEmbeddings = listOf(
            inference.getEmbeddings(
                listOf(
                    newTextFrame("How's the weather today?"),
                    newTextFrame("What's the weather like today?")
                )
            )
        )
        println("Similar phrases:")
        for (ebd in similarEmbeddings) {
            val sim = cosineSimilarity(ebd[0], ebd[1])
            println(sim)
            assert(sim > 0.8)
        }
    }

    private fun newTextFrame(content: String) =
        Frame.Text(
            textFrame = TextFrame(
                id = -1,
                content = content
            )
        )

    private fun MlModel.embeddingsFeature() = features.firstNotNullOf { it as? EmbeddingOutput }

    @OptIn(PerformancePitfall::class)
    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        assertEquals(a.size, b.size)
        with(Float64LinearSpace) {
            val matrixA = buildVector(a.size) { a[it].toDouble() }.asMatrix()
            val matrixB = buildVector(b.size) { b[it].toDouble() }.asMatrix()

            return ((matrixA dot matrixB) / sqrt((matrixA pow 2) + (matrixB pow 2))).elements()
                .first().second.toFloat()
        }
    }
}