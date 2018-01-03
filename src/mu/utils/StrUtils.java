package mu.utils;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

public class StrUtils
{
	public static String getHash(String str)
	{
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("SHA-1");
			return byteArrayToHexString(md.digest(str.getBytes("UTF-8")));
		}
		catch(NoSuchAlgorithmException e) {
			//e.printStackTrace();
		}
		catch (UnsupportedEncodingException e2) {
			//e2.printStackTrace();
		}
		return "";
	}

	private static String byteArrayToHexString(byte[] b) {
		String result = "";
		for (int i=0; i < b.length; i++) {
			result += Integer.toString( ( b[i] & 0xff ) + 0x100, 16).substring( 1 );
		}
		return result;
	}

	public static Map<String, String> parseLicenseReturn(String data)
	{
		String message = "", messagecode = "", messagestring = "", status = "", licensecoderm = "", license = "", licenselen = "", messtimestamp = "";

		if(null != data) {
			String[] arrOfStr = data.split("\n");

			for (String a : arrOfStr) {
				if (0 == a.indexOf("msg:^")) {
					message = a.substring(5);
					String[] subarrOfStr = message.split("\\|", 2);
					messagestring = subarrOfStr[0];
					messagecode = subarrOfStr[1];
				} else if (0 == a.indexOf("status:^")) {
					status = a.substring(8);
				} else if (0 == a.indexOf("dt:^")) {
					messtimestamp = a.substring(4);
				} else if (0 == a.indexOf("t:^")) {
					messtimestamp = a.substring(3);
				} else if (0 == a.indexOf("licHashCode:^")) {
					licensecoderm = a.substring(13);
				} else if (0 == a.indexOf("licCount:^")) {
					licenselen = a.substring(10);
				} else if (0 == a.indexOf("licString:^")) {
					int nIndex = data.indexOf("licString:^");
					if (nIndex > 0)
						license = data.substring(nIndex + 11);

					nIndex = license.indexOf("endOfLicString");
					if (nIndex > 0)
						license = license.substring(0, nIndex);

					//break;
				}
			}
		}

		Map<String, String> map = new HashMap<>();
		map.put("message", message);
		map.put("messagecode", messagecode);
		map.put("messagestring", messagestring);
		map.put("status", status);
		map.put("licensecoderm", licensecoderm);
		map.put("license", license);
		map.put("licenselen", licenselen);
		map.put("messtimestamp", messtimestamp);
		return map;
	}
}

