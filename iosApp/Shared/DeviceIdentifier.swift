import Foundation
#if canImport(UIKit)
    import UIKit
#endif

public struct DeviceIdentifier: CustomStringConvertible {
    let osName: String
    let osVersion: String
    let deviceName: String
    
    public static var `default`: DeviceIdentifier {
#if canImport(UIKit)
        Self(osName: UIDevice.current.systemName, osVersion: UIDevice.current.systemVersion, deviceName: UIDevice.current.name)
#elseif os(macOS)
        Self(osName: "macOS", osVersion: ProcessInfo.processInfo.operatingSystemVersionString, deviceName: "Mac")
#endif
    }
    
    public var description: String {
        return "\(deviceName) (\(osName) \(osVersion))"
    }
}
