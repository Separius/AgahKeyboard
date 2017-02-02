package com.android.inputmethod.keyboard.emojifast;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.Pair;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.resource.bitmap.BitmapResource;
import com.bumptech.glide.load.resource.bitmap.Downsampler;
import com.bumptech.glide.load.resource.bitmap.FitCenter;

/**
 * Created by sepehr on 2/1/17.
 */

public class BitmapUtil {

    private static final String TAG = BitmapUtil.class.getSimpleName();

    private static final int MAX_COMPRESSION_QUALITY          = 90;
    private static final int MIN_COMPRESSION_QUALITY          = 45;
    private static final int MAX_COMPRESSION_ATTEMPTS         = 5;
    private static final int MIN_COMPRESSION_QUALITY_DECREASE = 5;

    public static <T> Bitmap createScaledBitmap(Context context, T model, int maxWidth, int maxHeight)
            throws BitmapDecodingException
    {
        final Pair<Integer, Integer> dimensions = getDimensions(getInputStreamForModel(context, model));
        final Pair<Integer, Integer> clamped    = clampDimensions(dimensions.first, dimensions.second,
                maxWidth, maxHeight);
        return createScaledBitmapInto(context, model, clamped.first, clamped.second);
    }

    private static <T> InputStream getInputStreamForModel(Context context, T model)
            throws BitmapDecodingException
    {
        try {
            return Glide.buildStreamModelLoader(model, context)
                    .getResourceFetcher(model, -1, -1)
                    .loadData(Priority.NORMAL);
        } catch (Exception e) {
            throw new BitmapDecodingException(e);
        }
    }

    private static <T> Bitmap createScaledBitmapInto(Context context, T model, int width, int height)
            throws BitmapDecodingException
    {
        final Bitmap rough = Downsampler.AT_LEAST.decode(getInputStreamForModel(context, model),
                Glide.get(context).getBitmapPool(),
                width, height,
                DecodeFormat.PREFER_RGB_565);

        final Resource<Bitmap> resource = BitmapResource.obtain(rough, Glide.get(context).getBitmapPool());
        final Resource<Bitmap> result   = new FitCenter(context).transform(resource, width, height);

        if (result == null) {
            throw new BitmapDecodingException("unable to transform Bitmap");
        }
        return result.get();
    }

    public static <T> Bitmap createScaledBitmap(Context context, T model, float scale)
            throws BitmapDecodingException
    {
        Pair<Integer, Integer> dimens = getDimensions(getInputStreamForModel(context, model));
        return createScaledBitmapInto(context, model,
                (int)(dimens.first * scale), (int)(dimens.second * scale));
    }

    private static BitmapFactory.Options getImageDimensions(InputStream inputStream)
            throws BitmapDecodingException
    {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds    = true;
        BufferedInputStream fis       = new BufferedInputStream(inputStream);
        BitmapFactory.decodeStream(fis, null, options);
        try {
            fis.close();
        } catch (IOException ioe) {
            Log.w(TAG, "failed to close the InputStream after reading image dimensions");
        }

        if (options.outWidth == -1 || options.outHeight == -1) {
            throw new BitmapDecodingException("Failed to decode image dimensions: " + options.outWidth + ", " + options.outHeight);
        }

        return options;
    }

    public static Pair<Integer, Integer> getDimensions(InputStream inputStream) throws BitmapDecodingException {
        BitmapFactory.Options options = getImageDimensions(inputStream);
        return new Pair<>(options.outWidth, options.outHeight);
    }

    public static InputStream toCompressedJpeg(Bitmap bitmap) {
        ByteArrayOutputStream thumbnailBytes = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, thumbnailBytes);
        return new ByteArrayInputStream(thumbnailBytes.toByteArray());
    }

    public static byte[] toByteArray(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        return stream.toByteArray();
    }

    public static byte[] createFromNV21(@NonNull final byte[] data,
                                        final int width,
                                        final int height,
                                        int rotation,
                                        final Rect croppingRect)
            throws IOException
    {
        byte[] rotated = rotateNV21(data, width, height, rotation);
        final int rotatedWidth  = rotation % 180 > 0 ? height : width;
        final int rotatedHeight = rotation % 180 > 0 ? width  : height;
        YuvImage previewImage = new YuvImage(rotated, ImageFormat.NV21,
                rotatedWidth, rotatedHeight, null);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        previewImage.compressToJpeg(croppingRect, 80, outputStream);
        byte[] bytes = outputStream.toByteArray();
        outputStream.close();
        return bytes;
    }

    public static byte[] rotateNV21(@NonNull final byte[] yuv,
                                    final int width,
                                    final int height,
                                    final int rotation)
    {
        if (rotation == 0) return yuv;
        if (rotation % 90 != 0 || rotation < 0 || rotation > 270) {
            throw new IllegalArgumentException("0 <= rotation < 360, rotation % 90 == 0");
        }

        final byte[]  output    = new byte[yuv.length];
        final int     frameSize = width * height;
        final boolean swap      = rotation % 180 != 0;
        final boolean xflip     = rotation % 270 != 0;
        final boolean yflip     = rotation >= 180;

        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                final int yIn = j * width + i;
                final int uIn = frameSize + (j >> 1) * width + (i & ~1);
                final int vIn = uIn       + 1;

                final int wOut     = swap ? height              : width;
                final int hOut     = swap ? width               : height;
                final int iSwapped = swap ? j                   : i;
                final int jSwapped = swap ? i                   : j;
                final int iOut     = xflip ? wOut - iSwapped - 1 : iSwapped;
                final int jOut     = yflip ? hOut - jSwapped - 1 : jSwapped;

                final int yOut = jOut * wOut + iOut;
                final int uOut = frameSize + (jOut >> 1) * wOut + (iOut & ~1);
                final int vOut = uOut + 1;

                output[yOut] = (byte)(0xff & yuv[yIn]);
                output[uOut] = (byte)(0xff & yuv[uIn]);
                output[vOut] = (byte)(0xff & yuv[vIn]);
            }
        }
        return output;
    }

    private static Pair<Integer, Integer> clampDimensions(int inWidth, int inHeight, int maxWidth, int maxHeight) {
        if (inWidth > maxWidth || inHeight > maxHeight) {
            final float aspectWidth, aspectHeight;

            if (inWidth == 0 || inHeight == 0) {
                aspectWidth  = maxWidth;
                aspectHeight = maxHeight;
            } else if (inWidth >= inHeight) {
                aspectWidth  = maxWidth;
                aspectHeight = (aspectWidth / inWidth) * inHeight;
            } else {
                aspectHeight = maxHeight;
                aspectWidth  = (aspectHeight / inHeight) * inWidth;
            }

            return new Pair<>(Math.round(aspectWidth), Math.round(aspectHeight));
        } else {
            return new Pair<>(inWidth, inHeight);
        }
    }
}
