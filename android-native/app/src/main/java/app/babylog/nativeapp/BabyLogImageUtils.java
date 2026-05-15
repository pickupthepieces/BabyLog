package app.babylog.nativeapp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public final class BabyLogImageUtils {
    public static final int MAX_IMAGE_EDGE_PX = 2048;
    public static final int JPEG_QUALITY = 82;

    private BabyLogImageUtils() {
    }

    public static int calculateInSampleSize(int width, int height, int maxEdgePx) {
        if (width <= 0 || height <= 0 || maxEdgePx <= 0) {
            return 1;
        }
        int inSampleSize = 1;
        while (Math.max(width / inSampleSize, height / inSampleSize) > maxEdgePx) {
            inSampleSize *= 2;
        }
        return Math.max(1, inSampleSize);
    }

    public static void compressFileToJpeg(File source, File output) throws IOException {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(source.getAbsolutePath(), bounds);
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw new IOException("无法解码图片");
        }

        BitmapFactory.Options decode = new BitmapFactory.Options();
        decode.inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, MAX_IMAGE_EDGE_PX);
        Bitmap bitmap = BitmapFactory.decodeFile(source.getAbsolutePath(), decode);
        if (bitmap == null) {
            throw new IOException("无法解码图片");
        }

        File parent = output.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            bitmap.recycle();
            throw new IOException("无法创建图片目录");
        }

        try (FileOutputStream out = new FileOutputStream(output)) {
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)) {
                throw new IOException("图片压缩失败");
            }
        } finally {
            bitmap.recycle();
        }
    }
}
