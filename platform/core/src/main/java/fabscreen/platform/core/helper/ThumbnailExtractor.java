package fabscreen.platform.core.helper;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import com.orhanobut.logger.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import fabscreen.platform.base.instantiation.ServiceContainer;
import fabscreen.platform.base.lib.file.IFile;
import fabscreen.platform.base.lib.parser.OrcaThumbnailBlockParser;
import fabscreen.platform.base.service.IAppService;
import fabscreen.platform.base.service.IFileManagerService;
import okio.BufferedSource;
import okio.Okio;

/**
 * Extract gcode thumbnail and save the image to cache.
 */
public class ThumbnailExtractor {
    // Be cautious of the space in ";thumbnail: "!!!
    private static final String HEADER_THUMBNAIL_PATTERN_V0 = ";thumbnail: ";
    private static final String HEADER_THUMBNAIL_PATTERN_V1 = ";Thumbnail:";
    public static String extract(IFile iFile) {
        if (iFile == null) return null;
        if (!isPathGcode(iFile.getName())) return null;
        File thumbnailFile = getCacheThumbnailFile(iFile);
        if (thumbnailFile != null) {
//            Logger.d("Thumbnail already cached!");
            return thumbnailFile.getAbsolutePath();
        }
        try (BufferedSource source = Okio.buffer(Okio.source(iFile.getInputStream()))) {
            Bitmap thumbnail = readLargestThumbnail(source);
            return thumbnail == null ? null : saveThumbnailToCache(thumbnail, iFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Read gcode file from path and extract the thumbnail to cache folder.
     *
     * @param gcodePath Path of the gcode file.
     * @param isLocal   The gcode file is stored locally or in USB disk.
     * @return The thumbnail file path on screen device.
     */
    public static String extract(String gcodePath, boolean isLocal) {
        if (!isPathGcode(gcodePath)) return null;
        IFile iFile = ServiceContainer.getInstance().getService(IFileManagerService.class).getDevice(isLocal).search(gcodePath);
        return extract(iFile);
    }

    private static Bitmap readLargestThumbnail(BufferedSource source) throws IOException {
        OrcaThumbnailBlockParser orcaParser = new OrcaThumbnailBlockParser();
        Bitmap largest = null;

        while (true) {
            String line = source.readUtf8Line();
            if (line == null) {
                break;
            }

            String trimmed = line.trim();
            if (!trimmed.isEmpty() && !trimmed.startsWith(";")) {
                break;
            }

            if (orcaParser.consumeLine(line)) {
                OrcaThumbnailBlockParser.Result result = orcaParser.takeCompleted();
                if (result != null) {
                    largest = chooseLargest(largest, getBitmapFromBase64(result.getEncodedData()));
                }
                continue;
            }

            if (line.startsWith(HEADER_THUMBNAIL_PATTERN_V0)) {
                largest = chooseLargest(largest,
                        getBitmapFromBase64(line.substring(HEADER_THUMBNAIL_PATTERN_V0.length())));
            } else if (line.startsWith(HEADER_THUMBNAIL_PATTERN_V1)) {
                largest = chooseLargest(largest,
                        getBitmapFromBase64(line.substring(HEADER_THUMBNAIL_PATTERN_V1.length())));
            }
        }

        return largest;
    }

    private static Bitmap chooseLargest(Bitmap current, Bitmap candidate) {
        if (candidate == null) {
            return current;
        }
        long candidateArea = (long) candidate.getWidth() * candidate.getHeight();
        long currentArea = current == null ? -1L : (long) current.getWidth() * current.getHeight();
        if (candidateArea > currentArea) {
            if (current != null) {
                current.recycle();
            }
            return candidate;
        }
        candidate.recycle();
        return current;
    }

    private static File getCacheThumbnailFile(IFile iFile) {
        File cacheDir = ServiceContainer.getInstance().getService(IAppService.class).getAppContext().getCacheDir();
        String folderPath = cacheDir.getAbsolutePath() + "/gcode_thumbnail" + (iFile.isLocal() ? "/Local" : "/USB");
        File folder = new File(folderPath);
        if (!folder.exists()) {
            folder.mkdirs();
            return null;
        }
        if (iFile.isLocal()) {

        }
        return search(folder, getThumbnailFileName(iFile.getAbsolutePath()));
    }

    private static File search(File nowFile, String path) {
        if (path.isEmpty()) {
            return null;
        }
        int startIndex = path.indexOf(File.separatorChar);
        int endIndex = path.indexOf(File.separatorChar, startIndex + 1);
        String pathName = path;
        if (endIndex != -1) {
            pathName = path.substring(startIndex + 1, endIndex);
        } else if (startIndex != -1) {
            pathName = path.substring(startIndex + 1);
        }
        for (File file : nowFile.listFiles()) {
            if (pathName.equals(file.getName())) {
                if (endIndex == -1) {
                    return file;
                }
                return search(file, path.substring(endIndex));
            }
        }
        return null;
    }


    private static boolean isPathGcode(String gcodePath) {
        return gcodePath.contains(".gcode") || gcodePath.contains(".nc") || gcodePath.contains(".cnc");
    }

    private static String getThumbnailFileName(String filePath) {
        return filePath.substring(0, filePath.lastIndexOf(".")) + ".png";
    }

    private static String saveThumbnailToCache(Bitmap thumbnail, IFile iFile) throws IOException {
        if (thumbnail == null) {
            return null;
        }
        File cacheDir = ServiceContainer.getInstance().getService(IAppService.class).getAppContext().getCacheDir();
        String folderPath = cacheDir.getAbsolutePath() + "/gcode_thumbnail" + (iFile.isLocal() ? "/Local" : "/USB");
        String absolutePath = iFile.getAbsolutePath();
        int endIndex = absolutePath.lastIndexOf(File.separatorChar);
        if (endIndex != -1) {
            folderPath += absolutePath.substring(0, endIndex);
        }
        File folder = new File(folderPath);
        if (!folder.exists()) {
            folder.mkdirs();
        }
        String fileName = absolutePath.substring(absolutePath.lastIndexOf("/"), absolutePath.lastIndexOf(".")) + ".png";
        File thumbnailFile = new File(folder, fileName);
        if (thumbnailFile.exists() && !thumbnailFile.delete()) {
            return thumbnailFile.getAbsolutePath();
        }
        if (!thumbnailFile.createNewFile()) {
            return null;
        }

        try (FileOutputStream out = new FileOutputStream(thumbnailFile)) {
            if (thumbnail.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                return thumbnailFile.getAbsolutePath();
            } else {
                return null;
            }
        }
    }

    private static Bitmap getBitmapFromBase64(String base64img) {
        Bitmap bitmap = null;
        try {
            int commaIndex = base64img.indexOf(',');
            String encodedData = commaIndex >= 0 ? base64img.substring(commaIndex + 1) : base64img;
            if (encodedData.isEmpty()
                    || encodedData.length() > OrcaThumbnailBlockParser.MAX_ENCODED_CHARACTERS) {
                return null;
            }
            byte[] bitmapArray = Base64.decode(encodedData, Base64.DEFAULT);
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(bitmapArray, 0, bitmapArray.length, bounds);
            long area = (long) bounds.outWidth * bounds.outHeight;
            if (bounds.outWidth <= 0
                    || bounds.outHeight <= 0
                    || bounds.outWidth > OrcaThumbnailBlockParser.MAX_IMAGE_DIMENSION
                    || bounds.outHeight > OrcaThumbnailBlockParser.MAX_IMAGE_DIMENSION
                    || area <= 0
                    || area > OrcaThumbnailBlockParser.MAX_IMAGE_PIXELS) {
                return null;
            }
            bitmap = BitmapFactory.decodeByteArray(bitmapArray, 0, bitmapArray.length);
        } catch (Exception e) {
            Logger.e(e.toString());
        }
        return bitmap;

    }

    public static void deleteExtractCache(IFile iFile) {
        if (iFile == null) {
            return;
        }
        if (!isPathGcode(iFile.getName())) {
            return;
        }
        File thumbnailFile = getCacheThumbnailFile(iFile);
        if (thumbnailFile != null) {
            thumbnailFile.delete();
        }
    }
}
