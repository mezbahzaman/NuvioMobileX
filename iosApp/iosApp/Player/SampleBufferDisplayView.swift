// Ported from the GPL-3.0 fork luqmanfadlli/NuvioMobile-iOS (branch `enhanced`, 2026-08-18), which
// forks the same NuvioMedia/NuvioMobile upstream this repo does — so the licence is compatible.
//
// Why this approach: AVPictureInPictureController only accepts an AVPlayerLayer or an
// AVSampleBufferDisplayLayer, and we render libmpv -> libplacebo -> MoltenVK into a CAMetalLayer.
// Rather than change the renderer (which would cost gpu-next's HDR tone mapping, EDR, deband and
// interpolation), this taps the RENDERER'S OUTPUT: MetalLayer overrides nextDrawable(), the
// presented drawable's MTLTexture is GPU-blitted into a CVPixelBuffer, wrapped in a CMSampleBuffer
// and enqueued into an AVSampleBufferDisplayLayer that PiP is bound to. mpv is untouched.
//
// KEEP THESE FILES CLOSE TO THE UPSTREAM FORK so they can be re-synced with a clean diff. Local
// adaptations are confined to InAppLogBridge.swift (our logging shim) and the bridge wiring.

import UIKit
import AVFoundation

final class SampleBufferDisplayView: UIView {
    override class var layerClass: AnyClass { AVSampleBufferDisplayLayer.self }

    var displayLayer: AVSampleBufferDisplayLayer {
        layer as! AVSampleBufferDisplayLayer
    }

    private(set) var pictureInPictureController: PictureInPictureController?

    weak var pictureInPictureDelegate: PictureInPictureControllerDelegate? {
        didSet {
            pictureInPictureController?.delegate = pictureInPictureDelegate
        }
    }

    override init(frame: CGRect) {
        super.init(frame: frame)
        commonInit()
    }

    required init?(coder: NSCoder) {
        super.init(coder: coder)
        commonInit()
    }

    private func commonInit() {
        backgroundColor = .black
        isUserInteractionEnabled = false
        displayLayer.videoGravity = .resizeAspect
        displayLayer.backgroundColor = UIColor.black.cgColor
        if #available(iOS 17.0, *) {
            displayLayer.wantsExtendedDynamicRangeContent = false
        }
        pictureInPictureController = PictureInPictureController(displayLayer: displayLayer)
        pictureInPictureController?.delegate = pictureInPictureDelegate
    }

    func flush() {
        if #available(iOS 18.0, *) {
            displayLayer.sampleBufferRenderer.flush(removingDisplayedImage: true, completionHandler: nil)
        } else {
            displayLayer.flushAndRemoveImage()
        }
    }
}
