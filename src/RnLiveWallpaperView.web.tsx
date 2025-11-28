import * as React from 'react';

import { RnLiveWallpaperViewProps } from './RnLiveWallpaper.types';

export default function RnLiveWallpaperView(props: RnLiveWallpaperViewProps) {
  return (
    <div>
      <iframe
        style={{ flex: 1 }}
        src={props.url}
        onLoad={() => props.onLoad({ nativeEvent: { url: props.url } })}
      />
    </div>
  );
}
