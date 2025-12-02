import { requireNativeModule } from "expo-modules-core";

type SaveLivePhotoResult = string; // "Saved"

interface RnLiveWallpaperModule {
  saveLivePhoto(photoUri: string, videoUri: string): Promise<SaveLivePhotoResult>;
  applyVideoWallpaper(videoUri: string): void;
}

const RnLiveWallpaper: RnLiveWallpaperModule =
  requireNativeModule<RnLiveWallpaperModule>("RnLiveWallpaper");

export default RnLiveWallpaper;
