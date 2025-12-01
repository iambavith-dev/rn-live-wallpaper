import { Button, View } from "react-native"
import RnLiveWallpaper from 'rn-live-wallpaper';
import * as FileSystem from 'expo-file-system/legacy';

const App = () => {

    const onProgress = async () => {
        const IMAGE =
            "https://one4wall.nyc3.cdn.digitaloceanspaces.com/dev/uploads/live/2/IMG_2745.HEIC";
        const VIDEO =
            "https://one4wall.nyc3.cdn.digitaloceanspaces.com/dev/uploads/live/2/IMG_2745.MOV";

        try {

            const localVideoPath = FileSystem.documentDirectory + "live1.mov";
            const localImagePath = FileSystem.documentDirectory + "live1.heic";

            const videoDownload = FileSystem.createDownloadResumable(
                VIDEO,
                localVideoPath,
                {}
            );
            const imageDownload = FileSystem.createDownloadResumable(
                IMAGE,
                localImagePath,
                {}
            );

            const [videoRes, imageRes] = await Promise.all([
                videoDownload.downloadAsync(),
                imageDownload.downloadAsync(),
            ]);

            if (!videoRes?.uri || !imageRes?.uri) {
                throw new Error("Download failed - invalid URIs");
            }

            console.log(videoRes.uri, imageRes.uri)

            await RnLiveWallpaper.saveLivePhoto(imageRes.uri, videoRes.uri)

            console.log("done")

        } catch (error: any) {
            console.log("onProgress error:", error);
        }
    };

    return (
        <View style={{ flex: 1, alignItems: 'center', justifyContent: 'center' }} >
            <Button
                title="Convert Video"
                onPress={onProgress}
            />
        </View>
    )
};

export default App;