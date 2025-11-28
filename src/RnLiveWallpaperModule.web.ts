import { registerWebModule, NativeModule } from 'expo';

import { RnLiveWallpaperModuleEvents } from './RnLiveWallpaper.types';

class RnLiveWallpaperModule extends NativeModule<RnLiveWallpaperModuleEvents> {
  PI = Math.PI;
  async setValueAsync(value: string): Promise<void> {
    this.emit('onChange', { value });
  }
  hello() {
    return 'Hello world! 👋';
  }
}

export default registerWebModule(RnLiveWallpaperModule, 'RnLiveWallpaperModule');
