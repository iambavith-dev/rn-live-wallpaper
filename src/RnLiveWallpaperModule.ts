import { NativeModule, requireNativeModule } from 'expo';

import { RnLiveWallpaperModuleEvents } from './RnLiveWallpaper.types';

declare class RnLiveWallpaperModule extends NativeModule<RnLiveWallpaperModuleEvents> {
  PI: number;
  hello(): string;
  setValueAsync(value: string): Promise<void>;
}

// This call loads the native module object from the JSI.
export default requireNativeModule<RnLiveWallpaperModule>('RnLiveWallpaper');
