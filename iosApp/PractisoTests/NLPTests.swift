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
    
    func makeTextFrame(content: String) -> FrameText {
        FrameText(id: 0, textFrame: .init(id: 0, content: content))
    }
    
    func getDistance(frame1: Frame, frame2: Frame, inference: FrameEmbeddingInference) async throws -> Float {
        let embeddings = try await inference.getEmbeddings(frames: [frame1, frame2])
        return calculateCosDistance(v1: embeddings[0], v2: embeddings[1])
    }
    
    func similarityTest(similarPhrases: [(String, String)], dissimilarPhrases: [(String, String)], model: MlModel) async throws {
        let inference = try await FrameEmbeddingInference(model: model)
        
        for phrase in similarPhrases {
            let dis = try await getDistance(frame1: makeTextFrame(content: phrase.0), frame2: makeTextFrame(content: phrase.1), inference: inference)
            #expect(dis < 0.2, "similar phrase is dissimilar")
        }
        
        for phrase in dissimilarPhrases {
            let dis = try await getDistance(frame1: makeTextFrame(content: phrase.0), frame2: makeTextFrame(content: phrase.1), inference: inference)
            #expect(dis > 0.2, "dissimilar phrase is similar")
        }
    }
    
    @Test
    func similarityEn() async throws {
        let similarPhrases = [
            ("I love cats the most.", "Cats are my favorite animals"),
            ("How's the weather currently?", "What's the current weather like?")
        ]
        let dissimilarPhrases = [
            ("The stock market is crashing!", "I love Minecraft"),
            ("Elon Musk wants to commercialize Mars", "Sony just released it's new flagship travel headphone.")
        ]
        try await similarityTest(similarPhrases: similarPhrases, dissimilarPhrases: dissimilarPhrases, model: JinaV2SmallEn.shared)
    }
    
    @Test
    func similarityZh() async throws {
        let similarPhrases = [
            ("此事在《史记》亦有记载", "此事在《史记》中亦有记载"),
            ("我最喜欢的动物是猫。", "猫是我最喜欢的动物"),
            ("现在的天气怎么样?", "当前的天气如何?")
        ]
        let dissimilarPhrases = [
            ("股市正在崩盘！", "我喜欢玩《我的世界》"),
            ("埃隆·马斯克想要商业化火星", "索尼刚发布了最新的旗舰旅行耳机。")
        ]
        try await similarityTest(similarPhrases: similarPhrases, dissimilarPhrases: dissimilarPhrases, model: JinaV2EnZh.shared)
    }

    @Test
    func feiZhModel() async throws {
        let inference = try await FrameEmbeddingInference(model: JinaV2EnZh.shared)
        let embeddings = try await inference.getEmbeddings(frame: FrameText(id: 0, textFrame: .init(id: 0, content: "下列某段文字有问题，请问是哪一段？")))
        #expect(embeddings.size > 0)
    }
}
