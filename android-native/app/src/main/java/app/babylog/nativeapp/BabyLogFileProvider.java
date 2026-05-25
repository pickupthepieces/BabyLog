package app.babylog.nativeapp;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

public final class BabyLogFileProvider extends ContentProvider {
    private static final String ROOT_FILES = "files";
    private static final String ROOT_EXTERNAL_FILES = "external-files";

    public static Uri getUriForFile(Context context, File file) throws IOException {
        File canonicalFile = file.getCanonicalFile();
        File filesRoot = context.getFilesDir().getCanonicalFile();
        File externalRoot = context.getExternalFilesDir(null) == null
                ? null
                : context.getExternalFilesDir(null).getCanonicalFile();

        String rootName;
        File root;
        if (isUnderRoot(canonicalFile, filesRoot)) {
            rootName = ROOT_FILES;
            root = filesRoot;
        } else if (externalRoot != null && isUnderRoot(canonicalFile, externalRoot)) {
            rootName = ROOT_EXTERNAL_FILES;
            root = externalRoot;
        } else {
            throw new IOException("文件不在 BabyLog 可共享目录内");
        }

        Uri.Builder builder = new Uri.Builder()
                .scheme("content")
                .authority(context.getPackageName() + ".files")
                .appendPath(rootName);
        String relativePath = root.toURI().relativize(canonicalFile.toURI()).getPath();
        for (String segment : relativePath.split("/")) {
            if (!segment.isEmpty()) {
                builder.appendPath(segment);
            }
        }
        return builder.build();
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public String getType(Uri uri) {
        String name = uri.getLastPathSegment();
        if (name != null && name.toLowerCase().endsWith(".png")) {
            return "image/png";
        }
        if (name != null && name.toLowerCase().endsWith(".apk")) {
            return "application/vnd.android.package-archive";
        }
        return "image/jpeg";
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        try {
            File file = resolveFile(uri);
            String[] columns = projection == null
                    ? new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE}
                    : projection;
            MatrixCursor cursor = new MatrixCursor(columns, 1);
            Object[] row = new Object[columns.length];
            for (int i = 0; i < columns.length; i++) {
                if (OpenableColumns.DISPLAY_NAME.equals(columns[i])) {
                    row[i] = file.getName();
                } else if (OpenableColumns.SIZE.equals(columns[i])) {
                    row[i] = file.length();
                }
            }
            cursor.addRow(row);
            return cursor;
        } catch (IOException ignored) {
            return null;
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException("insert not supported");
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("delete not supported");
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("update not supported");
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        try {
            File file = resolveFile(uri);
            int flags = mode != null && mode.contains("w")
                    ? ParcelFileDescriptor.MODE_CREATE | ParcelFileDescriptor.MODE_TRUNCATE | ParcelFileDescriptor.MODE_WRITE_ONLY
                    : ParcelFileDescriptor.MODE_READ_ONLY;
            return ParcelFileDescriptor.open(file, flags);
        } catch (IOException error) {
            throw new FileNotFoundException(error.getMessage());
        }
    }

    private File resolveFile(Uri uri) throws IOException {
        Context context = getContext();
        if (context == null) {
            throw new IOException("Provider context unavailable");
        }
        List<String> segments = uri.getPathSegments();
        if (segments.isEmpty()) {
            throw new IOException("Invalid BabyLog file uri");
        }

        String rootName = segments.get(0);
        File root;
        if (ROOT_FILES.equals(rootName)) {
            root = context.getFilesDir().getCanonicalFile();
        } else if (ROOT_EXTERNAL_FILES.equals(rootName) && context.getExternalFilesDir(null) != null) {
            root = context.getExternalFilesDir(null).getCanonicalFile();
        } else {
            throw new IOException("Unknown BabyLog file root");
        }

        File file = root;
        for (int i = 1; i < segments.size(); i++) {
            file = new File(file, segments.get(i));
        }
        File canonicalFile = file.getCanonicalFile();
        if (!isUnderRoot(canonicalFile, root)) {
            throw new IOException("Invalid BabyLog file path");
        }
        return canonicalFile;
    }

    private static boolean isUnderRoot(File file, File root) {
        String filePath = file.getPath();
        String rootPath = root.getPath();
        return filePath.equals(rootPath) || filePath.startsWith(rootPath + File.separator);
    }
}
