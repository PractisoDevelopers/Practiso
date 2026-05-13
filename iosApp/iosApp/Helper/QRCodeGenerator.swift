import CoreImage.CIFilterBuiltins

enum CorrectionLevel: String {
    /// L (Low) - recovers about 7% of the data
    case low = "L"

    /// M (Medium) - recovers about 15% of the data
    case medium = "M"

    /// Q (Quartile) - recovers about 25% of the data
    case quartile = "Q"

    /// H (High) - recovers about 30% of the data
    case high = "H"
}

func qrCode(
    from inputMessage: String,
    correctionLevel: CorrectionLevel = .medium,
    targetSize: CGSize,
    displayScale: CGFloat
) -> CGImage? {
    let data = Data(inputMessage.utf8)

    let qrCodeGenerator = CIFilter.qrCodeGenerator()
    qrCodeGenerator.message = data
    qrCodeGenerator.correctionLevel = correctionLevel.rawValue

    guard let outputImage = qrCodeGenerator.outputImage else {
        return nil
    }

    // Convert size in points → pixels based on the device's display scale
    let pixelWidth = targetSize.width * displayScale
    let pixelHeight = targetSize.height * displayScale

    // Scale the `CIImage` in pixel space
    let scaleX = pixelWidth / outputImage.extent.size.width
    let scaleY = pixelHeight / outputImage.extent.size.height
    let transform = CGAffineTransform(scaleX: scaleX, y: scaleY)
    let scaledImage = outputImage.transformed(by: transform)

    let context = CIContext()
    let cgImage = context.createCGImage(scaledImage, from: scaledImage.extent)

    return cgImage
}
