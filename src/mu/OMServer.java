package mu;

import mu.ommlib.HttpClient;
import mu.utils.*;
import mu.ommlib.*;

import java.util.Map;

public class OMServer {
	public OMServer() {
	}

	public void doLicense()	{

		OMMDB db = new OMMDB();

		String strTime = "";// = strftime("%Y%m%d%H%M",gmtime());
		String strTimeStamp = "";// = strftime("%Y%m%d%H%M%S",gmtime());

		if(!OMMDB.checkDBtables(true)) {
			//logger.error("Database check failed. Cannot continue.");
			return;
		}

		String licenseFile = "/opt/OMTCmm/cf/license.dat";
		String medusHomeDir = "/opt/OMTCmm/";

		boolean bLicense = true;//os.path.isfile(licenseFile);
		boolean bDemoLicense = !bLicense;

		String macaddress = OMMDB.getMachineID();
		//String macaddress = OMMDB.getMachineID();
		//logger.info('mac address: '+macaddress);

		String siteid = OMMDB.getSiteID();
		//logger.info('site id: '+siteid);

		String productKey = OMMDB.getProductKey();
		//logger.info('product key: '+productKey);

		String hash = "";
		if (bLicense) {
			hash = FileUtils.getHash(licenseFile);
			//logger.info('license file hash = '+hash)
		}

		String request = "VERIFY";
		String response = OMMDB.GetLicenseCheckResponse(); //TRANSFER or HARDWARE_CHANGE
		if(null != response)
			request = response;
		else
			if (bDemoLicense)
				request = "GET_LICENSE";
			else
				request = "VERIFY";

		String data = HttpClient.getLicenseInfo(productKey,macaddress,request,hash,strTimeStamp);
		//logger.info('FROM POST:'+data);

		if(null == data) {
			String str = strTime + "|FAILED_IN_POST|FAILED_IN_POST_MSG|";
			OMMDB.reportLicenseCheck("Failed in POST", str);
			return;
		}

		Map<String, String> map = StrUtils.parseLicenseReturn(data);
		//logger.info('message:'+message+'; status:'+status);

		if(null == map) {
			String str = strTime + "|FAILED_IN_POST|FAILED_IN_POST_MSG|Corrupted return from server";
			OMMDB.reportLicenseCheck("Corrupted return from server", str);
			return;
		}

		String message = "", messagecode = "", messagestring = "", status = "", licensecoderm = "",
				license = "", licenselen = "", messtimestamp = "";
		message = (null == map.get("message") ? "" : map.get("message"));
		messagecode = (null == map.get("messagecode") ? "" : map.get("messagecode"));
		//messagestring = (null == map.get("messagestring") ? "" : map.get("messagestring"));
		status = (null == map.get("status") ? "" : map.get("status"));
		licensecoderm = (null == map.get("licensecoderm") ? "" : map.get("licensecoderm"));
		license = (null == map.get("license") ? "" : map.get("license"));
		//licenselen = (null == map.get("licenselen") ? "" : map.get("licenselen"));
		//messtimestamp = (null == map.get("messtimestamp") ? "" : map.get("messtimestamp"));

		String str = strTime + '|' + status + '|' + messagecode + '|' + message;
		OMMDB.reportLicenseCheck("",str);

		if("PRODUCT_KEY_NOT_REGISTERED" == status || "'NO_LICENSE" == status)
			onNoLicense(bDemoLicense, licenseFile, strTime, status, message, messagecode);
		else if ("LICENSE_ISSUED" == status)
			onNewLicense(licenseFile, license, licensecoderm);
		else if ("OK" == status)
			onOk();
		else if ("POSSIBLE_TRANSFER" == status)
			onPossibleTransfer(strTime, status, message, messagecode);
		else if ("FAILED" == status)
			onFailed(productKey, strTime, status, message, messagecode);
		else if ("TRANSFER_FAILED" == status)
			onTransferFailed(strTime, status, message, messagecode);
		else if ("TRANSFER_DENIED" == status)
			onTransferDenied(strTime, status, message, messagecode);
		else if ("TRANSFER_COMPLETE" == status)
			onTransferComplete(licenseFile, license, licensecoderm);
		else if ("LICENSE_DISABLED" == status)
			onLicenseDisabled(strTime, status, message, messagecode, licenseFile);
		else if ("CORRUPTED" == status)
			onCorrupted(licenseFile, strTime, status, message, messagecode, productKey, macaddress, strTimeStamp);
		else
			;//logger.error('something is wrong, should not be here');
	}

	public static void deleteLicenseFile(String fileName) {
		//logger.info('deleting file: ' + fileName);
		//os.remove(fileName);
		//logger.error('Failed to delete file ' + fileName, exc_info = False);
	}

	public static void saveLicenseFile(String fileName, String license) {
		//logger.info('saving license file: ' + fileName)
		//f = open(fileName, 'w')
		//f.write(license)
		//f.close()
		//logger.error('Failed to save file ' + fileName, exc_info = False)
	}

	public static void onNoLicense(boolean bDemoLicense, String licenseFile, String strTime, String status, String message, String messagecode) {
		//logger.info('onNoLicense')

		if ("" == messagecode)
			OMMDB.setUserReplyString(strTime + '|' + status + '|' + message, "");
		else
			OMMDB.setUserReplyString(strTime + '|' + status + '|' + messagecode, "");

		if (!bDemoLicense) {
			deleteLicenseFile(licenseFile);
			OMMDB.forseUpdatePrivBasedOnLicensing();
		}
	}

	public static void onNewLicense(String licenseFile, String license, String licensecoderm) {
		//logger.info('onNewLicense')

		OMMDB.hideDisabled(false);
		//licenseCodeLcNew = su.getHash(license)

		saveLicenseFile(licenseFile, license);
		String licenseCodeLcNewFile = FileUtils.getHash(licenseFile);
		if (licensecoderm != licenseCodeLcNewFile)
			;//logger.error("Hash code don't match. Received: " + licensecoderm + " from file: " + licenseCodeLcNewFile)

		OMMDB.setUserReplyString("", "");

		OMMDB.forseUpdateMaxBasedOnLicensing(licenseFile);
		OMMDB.forseUpdatePrivBasedOnLicensing(licenseFile);
		//fu.updateWatcherBasedOnLicensing() - no need
	}

	public static void onTransferComplete(String licenseFile, String license, String licensecoderm) {
		//logger.info('onTransferComplete')

		//licenseCodeLcNew = su.getHash(license)

		OMMDB.hideDisabled(false);

		saveLicenseFile(licenseFile, license);
		String licenseCodeLcNewFile = FileUtils.getHash(licenseFile);
		if (licensecoderm != licenseCodeLcNewFile)
			;//logger.error("Hash code don't match. Received: " + licensecoderm + " from file: " + licenseCodeLcNewFile)

		OMMDB.forseUpdatePrivBasedOnLicensing();

		OMMDB.setUserReplyString("", "");
	}

	public static void onOk() {
		//logger.info('onOk')
		OMMDB.setUserReplyString("", "");
		//done here
	}

	public static void onPossibleTransfer(String strTime, String status, String message, String messagecode) {
		//logger.info('onPossibleTransfer')

		if ("" == messagecode)
			OMMDB.setUserReplyString(strTime + '|' + status + '|' + message, "");
		else
			OMMDB.setUserReplyString(strTime + '|' + status + '|' + messagecode, "");
	}

	public static void onFailed(String productKey, String strTime, String status, String message, String messagecode) {
		//logger.info('onFailed')

		if(null == productKey) // no produce key means DEMO:
			;//do nothing
    	else	{
    		if ("" == messagecode)
				OMMDB.setUserReplyString(strTime + '|' + status + '|' + message, "");
			else
				OMMDB.setUserReplyString(strTime + '|' + status + '|' + messagecode, "");
    	}
	}

	public static void onTransferFailed(String strTime, String status, String message, String messagecode) {
		//logger.info('onTransferFailed')

		if ("" == messagecode)
			OMMDB.setUserReplyString(strTime + '|' + status + '|' + message, "");
		else
			OMMDB.setUserReplyString(strTime + '|' + status + '|' + messagecode, "");
	}

	public static void onTransferDenied(String strTime, String status, String message, String messagecode) {
		//logger.info('onTransferDenied')

		if ("" == messagecode)
			OMMDB.setUserReplyString(strTime + '|' + status + '|' + message, "");
		else
			OMMDB.setUserReplyString(strTime + '|' + status + '|' + messagecode, "");
	}

	public static void onLicenseDisabled(String strTime, String status, String message, String messagecode, String licenseFile) {
		//logger.info('onLicenseDisabled')

		if ("" == messagecode)
			OMMDB.setUserReplyString(strTime + '|' + status + '|' + message, "");
		else
			OMMDB.setUserReplyString(strTime + '|' + status + '|' + messagecode, "");

		deleteLicenseFile(licenseFile);
		OMMDB.hideDisabled(true);
	}

	public static void onCorrupted(String licenseFile, String strTime, String status, String message, String messagecode, String productKey, String macaddress, String strTimeStamp) {
		//logger.info('onCorrupted')

		String hash = "";
		String request = "GET_LICENSE";
		String data = HttpClient.getLicenseInfo(productKey, macaddress, request, hash, strTimeStamp);
		//logger.info('FROM POST:' + data)

		if(null == data) {
			String str = strTime + "|FAILED_IN_POST|FAILED_IN_POST_MSG|";
			OMMDB.reportLicenseCheck("Failed in POST", str);
			return;
		}

		Map<String, String> map = StrUtils.parseLicenseReturn(data);
		//logger.info('message:' + message + '; status:' + status)

		if (null == map)	{
			String str = strTime + "|FAILED_IN_POST|FAILED_IN_POST_MSG|Corrupted return from server";
			OMMDB.reportLicenseCheck("Corrupted return from server", str);
			return;
		}

		message = "";
		messagecode = "";
		status = "";
		String messagestring = "", licensecoderm = "", license = "", licenselen = "", messtimestamp = "";
		message = (null == map.get("message") ? "" : map.get("message"));
		messagecode = (null == map.get("messagecode") ? "" : map.get("messagecode"));
		messagestring = (null == map.get("messagestring") ? "" : map.get("messagestring"));
		status = (null == map.get("status") ? "" : map.get("status"));
		licensecoderm = (null == map.get("licensecoderm") ? "" : map.get("licensecoderm"));
		license = (null == map.get("license") ? "" : map.get("license"));
		licenselen = (null == map.get("licenselen") ? "" : map.get("licenselen"));
		messtimestamp = (null == map.get("messtimestamp") ? "" : map.get("messtimestamp"));

		String str = strTime + '|' + status + '|' + messagecode + '|' + message;
		OMMDB.reportLicenseCheck("", str);

		if ("LICENSE_ISSUED" == status)	{
			onNewLicense(licenseFile, license, licensecoderm);
		}
	}

	public static void main(String args[])
	{
		OMServer omserver = new OMServer();
		omserver.doLicense();
		System.out.println(StrUtils.getGreetings());
	}
}
