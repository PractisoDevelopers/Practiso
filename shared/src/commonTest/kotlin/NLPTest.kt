import com.zhufucdev.practiso.JinaV2SmallEn
import com.zhufucdev.practiso.database.TextFrame
import com.zhufucdev.practiso.datamodel.Frame
import com.zhufucdev.practiso.platform.FrameEmbeddingInference
import com.zhufucdev.practiso.platform.Language
import com.zhufucdev.practiso.platform.LanguageIdentifier
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NLPTest {
    @Test
    fun language_id() {
        val id = LanguageIdentifier()
        runBlocking {
            assertEquals(
                Language.Chinese,
                id.getLanguage("兄弟最近怎么样？")
            )
            assertEquals(
                Language.English,
                id.getLanguage("What's good bro?")
            )
            assertEquals(
                Language.German,
                id.getLanguage("Was geht ab?")
            )
            assertEquals(
                Language.Spanish,
                id.getLanguage("¿Qué tal, compadre?")
            )
        }
    }

    @Test
    fun frame_embedding_inference() = runBlocking {
        val model = FrameEmbeddingInference(JinaV2SmallEn)
        val emb = model.getEmbeddings(
            Frame.Text(
                textFrame = TextFrame(
                    id = 0,
                    embeddingsId = null,
                    content = "One of the following text is correct. What is it?"
                )
            )
        )
        assertTrue(emb.isNotEmpty(), "Got empty embeddings.")
    }
}