import React from "react";
import { Button, View } from "react-native";
import * as FileSystem from 'expo-file-system/legacy';
import { requireNativeModule } from 'expo-modules-core';
const RnLiveWallpaper = requireNativeModule('RnLiveWallpaper');

export default function App() {
  const onProgress = async () => {
    const VIDEO =
      "https://www.pexels.com/download/video/9669111/";

    try {
      const localVideoPath = FileSystem.documentDirectory + "sample.mp4";

      const videoDownload = FileSystem.createDownloadResumable(
        VIDEO,
        localVideoPath,
        {}
      );

      const videoRes = await videoDownload.downloadAsync();

      if (!videoRes?.uri) {
        throw new Error("Download failed - invalid URIs");
      }

      console.log("downloaded uri:", videoRes.uri);
      await RnLiveWallpaper.applyVideoWallpaper(videoRes.uri);
      console.log("applyVideoWallpaper called");
    } catch (error: any) {
      console.log("onProgress error:", error);
    }
  };

  return (
    <View style={{ flex: 1, alignItems: 'center', justifyContent: 'center' }} >
      <Button
        title="Apply Downloaded Video as Live Wallpaper"
        onPress={onProgress}
      />
    </View>
  );
}
