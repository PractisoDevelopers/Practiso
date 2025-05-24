import Foundation
import ComposeApp

extension String {
    static var showAccuracy: String {
       "show_accuracy" 
    }
    
    static var feiModel: String {
        "fei_model"
    }
}

extension UserDefaults {
    @objc dynamic var feiModel: MlModel {
        return KnownModel(index: Int32(integer(forKey: .feiModel)))
    }
}
