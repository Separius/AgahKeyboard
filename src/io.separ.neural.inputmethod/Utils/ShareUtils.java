package io.separ.neural.inputmethod.Utils;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.facebook.binaryresource.BinaryResource;
import com.facebook.binaryresource.FileBinaryResource;
import com.facebook.cache.common.CacheKey;
import com.facebook.imagepipeline.cache.DefaultCacheKeyFactory;
import com.facebook.imagepipeline.core.ImagePipelineFactory;
import com.facebook.imagepipeline.request.ImageRequest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by sepehr on 3/7/17.
 */

public class ShareUtils {

    private static void deleteContent(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory() && fileOrDirectory.exists()) {
            for (File child : fileOrDirectory.listFiles()) {
                if (child.isFile()) {
                    child.delete();
                }
            }
        }
    }

    public static void copyFile(File file, File toFile) {
        FileNotFoundException fnfe1;
        InputStream inputStream;
        Exception e;
        OutputStream outputStream;
        try {
            OutputStream out = null;
            File dir = new File(toFile.getParent());
            if (dir.exists()) {
                deleteContent(dir);
            } else {
                dir.mkdirs();
            }
            InputStream in = new FileInputStream(file.getAbsolutePath());
            try {
                out = new FileOutputStream(toFile.getAbsolutePath());
            } catch (FileNotFoundException e2) {
                fnfe1 = e2;
                inputStream = in;
                Log.e("tag", fnfe1.getMessage());
            } catch (Exception e3) {
                e = e3;
                inputStream = in;
                Log.e("tag", e.getMessage());
            }
            try {
                byte[] buffer = new byte[1024];
                while (true) {
                    int read = in.read(buffer);
                    if (read != -1) {
                        out.write(buffer, 0, read);
                    } else {
                        in.close();
                        try {
                            out.flush();
                            out.close();
                            return;
                        } catch (FileNotFoundException e4) {
                            fnfe1 = e4;
                            outputStream = out;
                            Log.e("tag", fnfe1.getMessage());
                        } catch (Exception e5) {
                            e = e5;
                            outputStream = out;
                            Log.e("tag", e.getMessage());
                        }
                    }
                }
            } catch (FileNotFoundException e6) {
                fnfe1 = e6;
                outputStream = out;
                inputStream = in;
                Log.e("tag", fnfe1.getMessage());
            } catch (Exception e7) {
                e = e7;
                outputStream = out;
                inputStream = in;
                Log.e("tag", e.getMessage());
            }
        } catch (FileNotFoundException e8) {
            fnfe1 = e8;
            Log.e("tag", fnfe1.getMessage());
        } catch (Exception e9) {
            e = e9;
            Log.e("tag", e.getMessage());
        }
    }

    public static File getCachedImageOnDisk(Context context, Uri loadUri) {
        if (loadUri == null) {
            return null;
        }
        CacheKey cacheKey = DefaultCacheKeyFactory.getInstance().getEncodedCacheKey(ImageRequest.fromUri(loadUri), context);
        BinaryResource resource;
        if (ImagePipelineFactory.getInstance().getMainFileCache().hasKey(cacheKey)) {
            resource = ImagePipelineFactory.getInstance().getMainFileCache().getResource(cacheKey);
            if (resource != null) {
                return ((FileBinaryResource) resource).getFile();
            }
            return null;
        } else if (!ImagePipelineFactory.getInstance().getSmallImageFileCache().hasKey(cacheKey)) {
            return null;
        } else {
            resource = ImagePipelineFactory.getInstance().getSmallImageFileCache().getResource(cacheKey);
            if (resource != null) {
                return ((FileBinaryResource) resource).getFile();
            }
            return null;
        }
    }
}
