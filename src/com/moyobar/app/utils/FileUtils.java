package com.moyobar.app.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class FileUtils {

	public static InputStream getInputStream(String url){
		try {
			HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
			return conn.getInputStream();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	public static void copyFile(InputStream is,String dest){
		
		if(is != null){
		
			File dir = new File(dest.substring(0,dest.lastIndexOf("/")));
			if(!dir.exists()){
				dir.mkdirs();
			}
			FileOutputStream fos;
			try {
				fos = new FileOutputStream(new File(dest));
				byte[] buffer = new byte[64];
				int length = 0;
				while((length = is.read(buffer)) > 0){
					fos.write(buffer, 0, length);
				}
				fos.flush();
				fos.close();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
}
