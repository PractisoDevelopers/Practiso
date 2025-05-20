import Testing
import Foundation
import ComposeApp

struct NLPTests {
    @Test
    func feiBasic() async throws {
        let inference = try await FrameEmbeddingInference(model: JinaV2SmallEn.shared)
        let embeddings = try await inference.getEmbeddings(frame: FrameText(id: 0, textFrame: .init(id: 0, content: "One of the following text is correct. What is it?")))
        #expect(embeddings.size > 0)
    }
    
    @Test
    func languageIdEn() async throws {
        let identifier = LanguageIdentifier()
        let lang = try! await identifier.getLanguage(text: "What's the meaning of life, universe and everything?")
        #expect(lang == .english)
    }
    
    @Test
    func similarity() async throws {
        let inference = try await FrameEmbeddingInference(model: JinaV2SmallEn.shared)
        func makeTextFrame(content: String) -> FrameText {
            FrameText(id: 0, textFrame: .init(id: 0, content: content))
        }
        
        let embeddings = try await inference.getEmbeddings(frames: [
            makeTextFrame(content: "I love cats the most."),
            makeTextFrame(content: "Cats are my favorite")
        ])
        let e0 = URL.documentsDirectory.appending(component: "e0")
        let e1 = URL.documentsDirectory.appending(component: "e1")
        var buf = ""
        for i in 0..<embeddings[0].size {
            buf += "\(embeddings[0].get(index: i)) "
        }
        try buf.write(to: e0, atomically: true, encoding: .utf8)
        buf = ""
        for i in 0..<embeddings[1].size {
            buf += "\(embeddings[1].get(index: i)) "
        }
        try buf.write(to: e1, atomically: true, encoding: .utf8)
    }
    
    @Test
    func feiZhModel() async throws {
        let inference = try await FrameEmbeddingInference(model: JinaV2EnZh.shared)
        let embeddings = try await inference.getEmbeddings(frame: FrameText(id: 0, textFrame: .init(id: 0, content: "下列某段文字有问题，请问是哪一段？")))
        #expect(embeddings.size > 0)
    }
}
