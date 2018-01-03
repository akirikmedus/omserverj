package mu.utils;

import java.io.*;

public class FileUtils
{
	public static String getHash(String fileName)
	{
		try {
			InputStream is = new FileInputStream(fileName);
			BufferedReader buf = new BufferedReader(new InputStreamReader(is));
			String line = buf.readLine();
			StringBuilder sb = new StringBuilder();
			while(line != null){
				sb.append(line);
				line = buf.readLine();
				if(line != null) sb.append("\n");
			}
			String fileAsString = sb.toString();
			return StrUtils.getHash(fileAsString);
		} catch (IOException e) {
			//e.printStackTrace();
		}
		return "";
	}

}

