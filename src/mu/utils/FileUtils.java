package mu.utils;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;

public class FileUtils
{
	public static String getHash(String fileName) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			FileInputStream fis = new FileInputStream(fileName);

			byte[] dataBytes = new byte[1024];

			int nread = 0;
			while ((nread = fis.read(dataBytes)) != -1) {
				md.update(dataBytes, 0, nread);
			};
			byte[] mdbytes = md.digest();

			//convert the byte to hex format method 1
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < mdbytes.length; i++) {
				sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
			}

			return sb.toString().toUpperCase();

			//String fileAsString = new String(Files.readAllBytes(Paths.get(fileName)));

			/*InputStream is = new FileInputStream(fileName);
			BufferedReader buf = new BufferedReader(new InputStreamReader(is));
			String line = buf.readLine();
			StringBuilder sb = new StringBuilder();
			while(line != null){
				sb.append(line);
				line = buf.readLine();
				if(line != null) sb.append("\r\n");
			}
			String fileAsString = sb.toString();*/

			//return StrUtils.getHash(fileAsString);
		} catch (IOException e) {
			//e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			//e.printStackTrace();
		}
		return "";
	}

	public static String getMacAddress()
	{
		return GetAddress("mac");
	}

	public static String GetAddress(String addressType)
	{
		String address = "";
		InetAddress lanIp = null;
		try {

			String ipAddress;
			Enumeration<NetworkInterface> net;
			net = NetworkInterface.getNetworkInterfaces();

			while (net.hasMoreElements())
			{
				NetworkInterface element = net.nextElement();
				Enumeration<InetAddress> addresses = element.getInetAddresses();

				while (addresses.hasMoreElements()
						&& element.getHardwareAddress() != null
						&& element.getHardwareAddress().length > 0
						&& !isVMMac(element.getHardwareAddress()))
				{
					InetAddress ip = addresses.nextElement();
					if (ip instanceof Inet4Address)
					{

						if (ip.isSiteLocalAddress())
						{
							ipAddress = ip.getHostAddress();
							lanIp = InetAddress.getByName(ipAddress);
						}
					}
				}
			}

			if (lanIp == null)
				return null;

			if (addressType.equals("ip")) {
				address = lanIp.toString().replaceAll("^/+", "");

			} else if (addressType.equals("mac")) {
				address = getMacAddress(lanIp);

			} else {
				throw new Exception("Specify \"ip\" or \"mac\"");
			}

		} catch (UnknownHostException ex) {
			ex.printStackTrace();
		} catch (SocketException ex) {
			ex.printStackTrace();
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return address;
	}

	private static String getMacAddress(InetAddress ip) {
		String address = null;
		try {

			NetworkInterface network = NetworkInterface.getByInetAddress(ip);
			byte[] mac = network.getHardwareAddress();

			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < mac.length; i++) {
				sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));
			}
			address = sb.toString();

		} catch (SocketException ex) {

			ex.printStackTrace();

		}

		return address;
	}

	private static boolean isVMMac(byte[] mac) {
		if(null == mac) return false;
		byte invalidMacs[][] = {
				{0x00, 0x05, 0x69},             //VMWare
				{0x00, 0x1C, 0x14},             //VMWare
				{0x00, 0x0C, 0x29},             //VMWare
				{0x00, 0x50, 0x56},             //VMWare
				{0x08, 0x00, 0x27},             //Virtualbox
				{0x0A, 0x00, 0x27},             //Virtualbox
				{0x00, 0x03, (byte)0xFF},       //Virtual-PC
				{0x00, 0x15, 0x5D}              //Hyper-V
		};

		for (byte[] invalid: invalidMacs){
			if (invalid[0] == mac[0] && invalid[1] == mac[1] && invalid[2] == mac[2]) return true;
		}

		return false;
	}

}

