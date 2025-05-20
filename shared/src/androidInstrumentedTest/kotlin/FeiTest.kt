import android.util.Log
import com.zhufucdev.practiso.JinaV2SmallEn
import com.zhufucdev.practiso.database.TextFrame
import com.zhufucdev.practiso.datamodel.EmbeddingOutput
import com.zhufucdev.practiso.datamodel.Frame
import com.zhufucdev.practiso.datamodel.MlModel
import com.zhufucdev.practiso.helper.calculateCosSimilarity
import com.zhufucdev.practiso.platform.FrameEmbeddingInference
import com.zhufucdev.practiso.platform.getPlatform
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.math.abs

class FeiTest {
    @Test
    fun inferenceOne() = runBlocking {
        val inference = FrameEmbeddingInference(JinaV2SmallEn)
        val emb = inference.getEmbeddings(newTextFrame("How's the weather like today?"))
        val embeddingsFeature = inference.model.embeddingsFeature()
        assertEquals("Output dimensions mismatch", embeddingsFeature.dimensions.toInt(), emb.size)
        emb.forEach { d -> assert(d.isFinite()) { "Got non-finite output" } }
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
                    it.forEach { d -> assert(d.isFinite()) { "Got non-finite output" } }
                }
            }
        }
    }

    @Test
    fun similarity() = runBlocking {
        val tag = "Similarity test"
        val inference = FrameEmbeddingInference(JinaV2SmallEn)
        val similarPhrases = listOf(
            "How's the weather today?" to "What's the weather like today?",
            "I love cats the most." to "Cats are my favorite"
        )
        Log.d(tag, "Similar phrases:")
        for (phrase in similarPhrases) {
            val ebd = inference.getEmbeddings(
                listOf(
                    newTextFrame(phrase.first),
                    newTextFrame(phrase.second)
                )
            )
            val sim = calculateCosSimilarity(ebd[0], ebd[1])
            Log.d(tag, sim.toString())
            assert(abs(sim) < 0.2)
        }
        val dissimilarPhrases = listOf(
            "The stock market is crashing" to "Pizza doesn't make any sense to me",
            "God loves people" to "Lava is hot",
            "Ground floor? We call it 1st floor here." to "Jesus Christ, why are we here"
        )

        Log.d(tag, "Dissimilar phrases:")
        for (phrase in dissimilarPhrases) {
            val ebd = inference.getEmbeddings(
                listOf(
                    newTextFrame(phrase.first),
                    newTextFrame(phrase.second)
                )
            )
            val sim = calculateCosSimilarity(ebd[0], ebd[1])
            Log.d(tag, sim.toString())
            assert(abs(sim) > 0.2)
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
}