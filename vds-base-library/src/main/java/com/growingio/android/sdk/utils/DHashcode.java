package com.growingio.android.sdk.utils;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.NinePatch;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import android.os.Build;
import android.view.View;
import android.widget.ImageView;

import com.growingio.android.sdk.collection.CoreInitialize;
import com.growingio.android.sdk.collection.GConfig;

public class DHashcode {
	public static final String TAG = "GIO.DHashcode";
	private static final int HASH_SIZE = 8;
	public static Map<Integer, String> cacheHash = new HashMap<Integer, String>();

	public static String getDHash(View view) {
		if (view == null)
			return "";
		String hashcode = "";
		Bitmap bitmap = null;
		try {
			bitmap = DHashcode.getBitmapFromImageView(view);
			if (bitmap == null){
				LogUtil.e(TAG, "Util.getBitmapFromImageView == null");
				return "";
			}
			hashcode = DHashcode.getDhash(bitmap);
		} catch (Throwable e) {
			e.printStackTrace();
		}
		bitmap = null;
		return hashcode;
	}

	public static String getDhash(Bitmap img) throws Throwable{
		// Resize & Grayscale
		img = Bitmap.createScaledBitmap(img, HASH_SIZE + 1, HASH_SIZE, true);
		img = toGrayscale(img);
		StringBuffer difference = new StringBuffer();
		for(int row=0; row<HASH_SIZE; row++) {
			for(int col=0; col<HASH_SIZE; col++) {
				int pixel_left = img.getPixel(col, row);
				int pixel_right = img.getPixel(col + 1, row);
				difference.append(pixel_left > pixel_right ? "1" : "0");
			}
		}
		img = null;
		return String.valueOf(UnsignedLongs.parseUnsignedLong(difference.toString(), 2));
	}

	private static Bitmap toGrayscale(Bitmap bmpOriginal) {
		final int height = bmpOriginal.getHeight();
		final int width = bmpOriginal.getWidth();

		final Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		final Canvas c = new Canvas(bmpGrayscale);
		final Paint paint = new Paint();
		final ColorMatrix cm = new ColorMatrix();
		cm.setSaturation(0);
		final ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
		paint.setColorFilter(f);
		c.drawBitmap(bmpOriginal, 0, 0, paint);
		return bmpGrayscale;
	}

	public static Bitmap getBitmapFromImageView(View imageView) throws Throwable{
		Bitmap bitmap = null;
		Drawable drawable = null;
		if(imageView instanceof ImageView)
			drawable = ((ImageView) imageView).getDrawable();
		if(drawable == null){
			LogUtil.e(TAG, "imageView.getDrawable() == null");
			return null;
		}
		GConfig config = CoreInitialize.config();
		if(drawable.getIntrinsicHeight() > config.getImageViewCollectionSize()
				|| drawable.getIntrinsicWidth() > config.getImageViewCollectionSize()){
			LogUtil.e(TAG, "drawable.getIntrinsicHeight() > BITMAP_SIZE " + drawable.getIntrinsicHeight() +" " + drawable.getIntrinsicWidth());
			drawable = null;
			return null;
		}

		if (drawable instanceof NinePatchDrawable){
			NinePatchDrawable ninePatchDrawable = (NinePatchDrawable) drawable;
			NinePatch ninePatch = null;
			try {
				Field ninePatchField = NinePatchDrawable.class.getDeclaredField("mNinePatch");
				ninePatchField.setAccessible(true);
				ninePatch = (NinePatch) ninePatchField.get(ninePatchDrawable);
			} catch (Throwable e) {
				e.printStackTrace();
			}

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
				if (ninePatch != null && ninePatch.getBitmap() == null)
					bitmap = ninePatch.getBitmap();
			}
		}else if (drawable instanceof BitmapDrawable){
			BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
			bitmap = bitmapDrawable.getBitmap();

		}

		if(bitmap == null)
			bitmap = drawableToBitmap(drawable);
		drawable = null;
		return bitmap;
	}

	public static Bitmap drawableToBitmap(Drawable drawable) {
		if (drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0)
			return null;
		Bitmap bitmap = Bitmap.createBitmap(
				drawable.getIntrinsicWidth(),
				drawable.getIntrinsicHeight(),
				drawable.getOpacity() != PixelFormat.OPAQUE
						? Bitmap.Config.ARGB_8888
						: Bitmap.Config.RGB_565);

		Canvas canvas = new Canvas(bitmap);
		if (drawable.getBounds() == null)
			return null;
		drawable.draw(canvas);
		canvas = null;
		return bitmap;

	}

}
