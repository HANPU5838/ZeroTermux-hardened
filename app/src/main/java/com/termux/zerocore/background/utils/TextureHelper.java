package com.termux.zerocore.background.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;

public class TextureHelper
{

	private static final String LOG_TAG = "TextureHelper";

	private static boolean isFirst =true;

	public static int loadTexture(final Context context, final int resourceId)
	{
		final int[] textureHandle = new int[1];

		GLES20.glGenTextures(1, textureHandle, 0);

		if (textureHandle[0] == 0)
		{
			Log.e(LOG_TAG, "glGenTextures failed for resource id: " + resourceId);
			return 0;
		}

		final BitmapFactory.Options options = new BitmapFactory.Options();

		options.inScaled = false;	// No pre-scaling

		// Read in the resource
		final Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), resourceId, options);

		if (bitmap == null)
		{
			// Decoding failed (missing/corrupt resource). Delete the unused texture and return 0
			// so the caller can skip this texture instead of crashing with a NullPointerException.
			Log.e(LOG_TAG, "Failed to decode bitmap for resource id: " + resourceId);
			GLES20.glDeleteTextures(1, textureHandle, 0);
			return 0;
		}

		boolean uploaded = false;
		// Bind to the texture in OpenGL
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0]);

		// Set filtering
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);

		try
		{
			// Load the bitmap into the bound texture.
			GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
			uploaded = true;
		}
		catch (RuntimeException e)
		{
			Log.e(LOG_TAG, "Failed to upload bitmap for resource id: " + resourceId, e);
		}
		finally
		{
			// Recycle the bitmap, since its data has been loaded into OpenGL.
			bitmap.recycle();
		}

		if (!uploaded)
		{
			GLES20.glDeleteTextures(1, textureHandle, 0);
			return 0;
		}

		return textureHandle[0];
	}

	public static int loadTexture(Bitmap bitmap)
	{
		final int[] textureHandle = new int[1];

		GLES20.glGenTextures(1, textureHandle, 0);
		if (textureHandle[0] == 0)
		{
			Log.e(LOG_TAG, "glGenTextures failed while loading a bitmap texture");
			return 0;
		}

		if (bitmap == null || bitmap.isRecycled())
		{
			// Never pass a null/recycled bitmap to GLUtils.texImage2D(); report the failure and
			// return 0 so callers render an untextured quad instead of crashing.
			Log.e(LOG_TAG, "Cannot load texture: bitmap is " + (bitmap == null ? "null" : "recycled"));
			GLES20.glDeleteTextures(1, textureHandle, 0);
			return 0;
		}

		// Bind to the texture in OpenGL
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0]);
		// Set filtering
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
		// Load the bitmap into the bound texture.
		try
		{
			GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
		}
		catch (RuntimeException e)
		{
			Log.e(LOG_TAG, "Failed to upload bitmap texture", e);
			GLES20.glDeleteTextures(1, textureHandle, 0);
			return 0;
		}

		return textureHandle[0];
	}


}
