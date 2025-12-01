import { requireNativeModule } from "expo-modules-core";

type SaveLivePhotoResult = string; // "Saved"

interface RnLiveWallpaperModule {
  saveLivePhoto(photoUri: string, videoUri: string): Promise<SaveLivePhotoResult>;
}

const RnLiveWallpaper: RnLiveWallpaperModule =
  requireNativeModule<RnLiveWallpaperModule>("RnLiveWallpaper");

export default RnLiveWallpaper;
