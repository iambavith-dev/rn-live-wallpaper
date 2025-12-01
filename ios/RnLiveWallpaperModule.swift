import Foundation
import ExpoModulesCore
import Photos

public class RnLiveWallpaperModule: Module {
  public func definition() -> ModuleDefinition {
    
    Name("RnLiveWallpaper")

    // JS: await RnLiveWallpaper.saveLivePhoto(photoUri, videoUri)
    AsyncFunction("saveLivePhoto") { (photoUri: String, videoUri: String) -> String in
      return try await saveLivePhoto(photoUri: photoUri, videoUri: videoUri)
    }
  }
}

// MARK: - Save Live Photo Implementation
extension RnLiveWallpaperModule {

  func saveLivePhoto(photoUri: String, videoUri: String) async throws -> String {
    guard let photoURL = URL(string: photoUri),
          let videoURL = URL(string: videoUri) else {
      throw NSError(domain: "RnLiveWallpaper", code: 1, userInfo: [
        NSLocalizedDescriptionKey: "Invalid URI for photo or video."
      ])
    }

    // Request permission first
    let status = PHPhotoLibrary.authorizationStatus(for: .addOnly)
    if status == .notDetermined {
      let _ = await PHPhotoLibrary.requestAuthorization(for: .addOnly)
    }

    let newStatus = PHPhotoLibrary.authorizationStatus(for: .addOnly)
    guard newStatus == .authorized || newStatus == .limited else {
      throw NSError(domain: "RnLiveWallpaper", code: 2, userInfo: [
        NSLocalizedDescriptionKey: "Photo library permission not granted."
      ])
    }

    return try await withCheckedThrowingContinuation { continuation in
      PHPhotoLibrary.shared().performChanges({

        let request = PHAssetCreationRequest.forAsset()

        // Add the still image
        request.addResource(with: .photo, fileURL: photoURL, options: nil)

        // Add the paired video for Live Photo
        let options = PHAssetResourceCreationOptions()
        options.shouldMoveFile = false

        request.addResource(with: .pairedVideo, fileURL: videoURL, options: options)

      }, completionHandler: { success, error in
        if let error = error {
          continuation.resume(throwing: error)
        } else if success {
          continuation.resume(returning: "Saved")
        } else {
          continuation.resume(throwing: NSError(
            domain: "RnLiveWallpaper",
            code: 3,
            userInfo: [NSLocalizedDescriptionKey: "Unknown error while saving Live Photo."]
          ))
        }
      })
    }
  }
}
