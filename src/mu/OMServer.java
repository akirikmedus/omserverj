package mu;

import mu.ommlib.HttpClient;
import mu.utils.*;
import mu.ommlib.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

public class OMServer {

	private OMMDB db = new OMMDB();

	private Logger logger = new Logger();

	public OMServer() 	{
	}

	private void doLicense()	{

		Date now = new Date();
		SimpleDateFormat sdfDate = new SimpleDateFormat("yyyyMMddHHmm");
		String strTime = sdfDate.format(now);
		sdfDate = new SimpleDateFormat("yyyyMMddHHmmss");
		String strTimeStamp = sdfDate.format(now);

		if(!db.checkDBtables(true)) {
			logger.error("Database check failed. Cannot continue.");
			return;
		}

		String licenseFile = "/opt/OMTCmm/cf/license.dat";
		//String medusHomeDir = "/opt/OMTCmm/";

		File file = new File(licenseFile);
		boolean bLicense = file.exists();
		boolean bDemoLicense = !bLicense;

		String macaddress = db.getMachineID();
		//String macaddress = db.getMachineID();
		logger.info("mac address: " + macaddress);

		String siteid = db.getSiteID();
		logger.info("site id: " + siteid);

		String productKey = db.getProductKey();
		logger.info("product key: " + productKey);

		String hash = "";
		if (bLicense) {
			hash = FileUtils.getHash(licenseFile);
			logger.info("license file hash = " + hash);
		}

		String request;
		String response = db.GetLicenseCheckResponse(); //TRANSFER or HARDWARE_CHANGE
		if(null != response)
			request = response;
		else
			if (bDemoLicense)
				request = "GET_LICENSE";
			else
				request = "VERIFY";

		String data = HttpClient.getLicenseInfo(productKey,macaddress,request,hash,strTimeStamp);
		logger.info("FROM POST:" + data);

		if(null == data) {
			String str = strTime + "|FAILED_IN_POST|FAILED_IN_POST_MSG|";
			db.reportLicenseCheck("Failed in POST", str);
			return;
		}

		Map<String, String> map = StrUtils.parseLicenseReturn(data);

		if(null == map) {
			String str = strTime + "|FAILED_IN_POST|FAILED_IN_POST_MSG|Corrupted return from server";
			db.reportLicenseCheck("Corrupted return from server", str);
			return;
		}

		String message, messagecode, license, status, licensecoderm;
		//String messagestring, licenselen, messtimestamp;
		message = (null == map.get("message") ? "" : map.get("message"));
		messagecode = (null == map.get("messagecode") ? "" : map.get("messagecode"));
		//messagestring = (null == map.get("messagestring") ? "" : map.get("messagestring"));
		status = (null == map.get("status") ? "" : map.get("status"));
		licensecoderm = (null == map.get("licensecoderm") ? "" : map.get("licensecoderm"));
		license = (null == map.get("license") ? "" : map.get("license"));
		//licenselen = (null == map.get("licenselen") ? "" : map.get("licenselen"));
		//messtimestamp = (null == map.get("messtimestamp") ? "" : map.get("messtimestamp"));

		logger.info("message:" + message + "'; status:" + status);

		String str = strTime + '|' + status + '|' + messagecode + '|' + message;
		db.reportLicenseCheck("",str);

		if(status.equals("PRODUCT_KEY_NOT_REGISTERED") || status.equals("NO_LICENSE"))
			onNoLicense(bDemoLicense, licenseFile, strTime, status, message, messagecode);
		else if (status.equals("LICENSE_ISSUED"))
			onNewLicense(licenseFile, license, licensecoderm);
		else if (status.equals("OK"))
			onOk();
		else if (status.equals("POSSIBLE_TRANSFER"))
			onPossibleTransfer(strTime, status, message, messagecode);
		else if (status.equals("FAILED"))
			onFailed(productKey, strTime, status, message, messagecode);
		else if (status.equals("TRANSFER_FAILED"))
			onTransferFailed(strTime, status, message, messagecode);
		else if (status.equals("TRANSFER_DENIED"))
			onTransferDenied(strTime, status, message, messagecode);
		else if (status.equals("TRANSFER_COMPLETE"))
			onTransferComplete(licenseFile, license, licensecoderm);
		else if (status.equals("LICENSE_DISABLED"))
			onLicenseDisabled(strTime, status, message, messagecode, licenseFile);
		else if (status.equals("CORRUPTED"))
			onCorrupted(licenseFile, strTime, productKey, macaddress, strTimeStamp);
		else
			logger.error("something is wrong, should not be here");
	}

	private void deleteLicenseFile(String fileName) {
		logger.info("deleting file: " + fileName);
		File f = new File(fileName);
		if(!f.delete())
			logger.error("Failed to delete file " + fileName);
	}

	private void saveLicenseFile(String fileName, String license) {
		logger.info("saving license file: " + fileName);
		try {
			FileWriter  fw = new FileWriter(fileName);
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(license);
		} catch (IOException e) {
			//e.printStackTrace();
			logger.error("Failed to save file " + fileName);
		}
	}

	private void onNoLicense(boolean bDemoLicense, String licenseFile, String strTime, String status, String message, String messagecode) {
		logger.info("onNoLicense");

		db.setUserReplyString(strTime + '|' + status + '|' + (messagecode.isEmpty() ? message : messagecode), "");

		if (!bDemoLicense) {
			deleteLicenseFile(licenseFile);
			db.forseUpdatePrivBasedOnLicensing(licenseFile);
		}
	}

	private void onNewLicense(String licenseFile, String license, String licensecoderm) {
		logger.info("onNewLicense");

		db.hideDisabled(false);
		//licenseCodeLcNew = su.getHash(license)

		saveLicenseFile(licenseFile, license);
		String licenseCodeLcNewFile = FileUtils.getHash(licenseFile);
		if (!licensecoderm.equals(licenseCodeLcNewFile))
			logger.error("Hash code don't match. Received: " + licensecoderm + " from file: " + licenseCodeLcNewFile);

		db.setUserReplyString("", "");

		db.forseUpdateMaxBasedOnLicensing(licenseFile);
		db.forseUpdatePrivBasedOnLicensing(licenseFile);
		//fu.updateWatcherBasedOnLicensing() - no need
	}

	private void onTransferComplete(String licenseFile, String license, String licensecoderm) {
		logger.info("onTransferComplete");

		//licenseCodeLcNew = su.getHash(license)

		db.hideDisabled(false);

		saveLicenseFile(licenseFile, license);
		String licenseCodeLcNewFile = FileUtils.getHash(licenseFile);
		if (!licensecoderm.equals(licenseCodeLcNewFile))
			logger.error("Hash code don't match. Received: " + licensecoderm + " from file: " + licenseCodeLcNewFile);

		db.forseUpdatePrivBasedOnLicensing(licenseFile);
		db.setUserReplyString("", "");
	}

	private void onOk() {
		logger.info("onOk");
		db.setUserReplyString("", "");
		//done here
	}

	private void onPossibleTransfer(String strTime, String status, String message, String messagecode) {
		logger.info("onPossibleTransfer");
		db.setUserReplyString(strTime + '|' + status + '|' + (messagecode.isEmpty() ? message : messagecode), "");
	}

	private void onFailed(String productKey, String strTime, String status, String message, String messagecode) {
		logger.info("onFailed");

		if(null != productKey && !productKey.isEmpty()) // no produce key means DEMO:
			db.setUserReplyString(strTime + '|' + status + '|' + (messagecode.isEmpty() ? message : messagecode), "");
	}

	private void onTransferFailed(String strTime, String status, String message, String messagecode) {
		logger.info("onTransferFailed");
		db.setUserReplyString(strTime + '|' + status + '|' + (messagecode.isEmpty() ? message : messagecode), "");
	}

	private void onTransferDenied(String strTime, String status, String message, String messagecode) {
		logger.info("onTransferDenied");
		db.setUserReplyString(strTime + '|' + status + '|' + (messagecode.isEmpty() ? message : messagecode), "");
	}

	private void onLicenseDisabled(String strTime, String status, String message, String messagecode, String licenseFile) {
		logger.info("onLicenseDisabled");
		db.setUserReplyString(strTime + '|' + status + '|' + (messagecode.isEmpty() ? message : messagecode), "");

		deleteLicenseFile(licenseFile);
		db.hideDisabled(true);
	}

	private void onCorrupted(String licenseFile, String strTime, String productKey, String macaddress, String strTimeStamp) {
		logger.info("onCorrupted");

		String hash = "";
		String request = "GET_LICENSE";
		String data = HttpClient.getLicenseInfo(productKey, macaddress, request, hash, strTimeStamp);
		logger.info("FROM POST:" + data);

		if(null == data) {
			String str = strTime + "|FAILED_IN_POST|FAILED_IN_POST_MSG|";
			db.reportLicenseCheck("Failed in POST", str);
			return;
		}

		Map<String, String> map = StrUtils.parseLicenseReturn(data);

		if (null == map)	{
			String str = strTime + "|FAILED_IN_POST|FAILED_IN_POST_MSG|Corrupted return from server";
			db.reportLicenseCheck("Corrupted return from server", str);
			return;
		}

		String licensecoderm, license, status, message, messagecode;
		//String messagestring = "", licenselen = "", messtimestamp = "";
		message = (null == map.get("message") ? "" : map.get("message"));
		messagecode = (null == map.get("messagecode") ? "" : map.get("messagecode"));
		//messagestring = (null == map.get("messagestring") ? "" : map.get("messagestring"));
		status = (null == map.get("status") ? "" : map.get("status"));
		licensecoderm = (null == map.get("licensecoderm") ? "" : map.get("licensecoderm"));
		license = (null == map.get("license") ? "" : map.get("license"));
		//licenselen = (null == map.get("licenselen") ? "" : map.get("licenselen"));
		//messtimestamp = (null == map.get("messtimestamp") ? "" : map.get("messtimestamp"));

		logger.info("message:" + message + "; status:" + status);

		String str = strTime + '|' + status + '|' + messagecode + '|' + message;
		db.reportLicenseCheck("", str);

		if (status.equals("LICENSE_ISSUED"))	{
			onNewLicense(licenseFile, license, licensecoderm);
		}
	}

	public static void main(String args[])
	{
		OMServer omserver = new OMServer();
		omserver.doLicense();
		System.out.println("DONE");
	}
}
