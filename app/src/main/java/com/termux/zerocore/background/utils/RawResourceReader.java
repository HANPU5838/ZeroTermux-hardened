package com.termux.zerocore.background.utils;

import android.content.Context;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class RawResourceReader
{
	public static String readTextFileFromRawResource(final Context context,
													 final int resourceId)
	{
		final StringBuilder body = new StringBuilder();

		// Try-with-resources guarantees that the reader and the underlying stream are closed on
		// every path, including when readLine() throws.
		try (InputStream inputStream = context.getResources().openRawResource(resourceId);
			 InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
			 BufferedReader bufferedReader = new BufferedReader(inputStreamReader))
		{
			String nextLine;
			while ((nextLine = bufferedReader.readLine()) != null)
			{
				body.append(nextLine);
				body.append('\n');
			}
		}
		catch (IOException e)
		{
			return null;
		}

		return body.toString();
	}
}
