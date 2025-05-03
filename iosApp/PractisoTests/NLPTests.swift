import Testing
import ComposeApp

struct NLPTests {
    @Test
    func feiBasic() async throws {
        let inference = try await FrameEmbeddingInference(model: JinaV2SmallEn.shared)
        let embeddings = try await inference.getEmbeddings(frame: FrameText(id: 0, textFrame: .init(id: 0, embeddingsId: nil, content: "One of the following text is correct. What is it?")))
        #expect(embeddings.size > 0)
    }
    
    @Test
    func languageIdEn() async throws {
        let identifier = LanguageIdentifier()
        let lang = try! await identifier.getLanguage(text: "What's the meaning of life, universe and everything?")
        #expect(lang == .english)
    }
}
