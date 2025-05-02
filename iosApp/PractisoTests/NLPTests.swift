import Testing
import ComposeApp

struct NLPTests {
    @Test
    func feiWorks() async throws {
        let inference = try await FrameEmbeddingInference(model: JinaV2En.shared)
        let embeddings = try await inference.getEmbeddings(frame: FrameText(id: 0, textFrame: .init(id: 0, embeddingsId: nil, content: "One of the following text is correct. What is it?")))
        #expect(embeddings.size > 0)
    }
}
