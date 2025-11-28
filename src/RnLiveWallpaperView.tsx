import { requireNativeView } from 'expo';
import * as React from 'react';

import { RnLiveWallpaperViewProps } from './RnLiveWallpaper.types';

const NativeView: React.ComponentType<RnLiveWallpaperViewProps> =
  requireNativeView('RnLiveWallpaper');

export default function RnLiveWallpaperView(props: RnLiveWallpaperViewProps) {
  return <NativeView {...props} />;
}
