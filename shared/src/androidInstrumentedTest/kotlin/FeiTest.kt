import com.zhufucdev.practiso.JinaV2SmallEn
import com.zhufucdev.practiso.database.TextFrame
import com.zhufucdev.practiso.datamodel.EmbeddingOutput
import com.zhufucdev.practiso.datamodel.Frame
import com.zhufucdev.practiso.datamodel.MlModel
import com.zhufucdev.practiso.platform.FrameEmbeddingInference
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.runBlocking
import org.junit.Test

class FeiTest {
    private fun newTextFrame(content: String) =
        Frame.Text(
            textFrame = TextFrame(
                id = -1,
                content = content
            )
        )

    private fun MlModel.embeddingsFeature() = features.firstNotNullOf { it as? EmbeddingOutput }

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
        val embs = inference.getEmbeddings(
            listOf(
                newTextFrame("I love cats the most"),
                newTextFrame("How can the stock market fall so harshly?"),
                newTextFrame("Capybaras are cute animal")
            )
        )
        val feature = inference.model.embeddingsFeature()
        embs.forEach {
            assertEquals("Output dimensions mismatch", feature.dimensions.toInt(), it.size)
        }
    }
}