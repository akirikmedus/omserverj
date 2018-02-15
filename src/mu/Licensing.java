package mu;

import mu.ommlib.HttpClient;
import mu.ommlib.OMMDB;
import mu.utils.FileUtils;
import mu.utils.Logger;
import mu.utils.StrUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

public class Licensing extends Thread
{
    private OMMDB db = new OMMDB();
    public boolean m_bRun = true;

    @Override
    public void run()
    {
        do {
            doLicense();

            try {
                sleep(30 * 60 * 1000);// 30 minutes
            } catch (InterruptedException ignored) {}

        } while(m_bRun);
    }

    public void doLicense() {

        Date now = new Date();
        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyyMMddHHmm");
        String strTime = sdfDate.format(now);
        sdfDate = new SimpleDateFormat("yyyyMMddHHmmss");
        String strTimeStamp = sdfDate.format(now);

        String licenseFile = "/opt/OMTCmm/cf/license.dat";
        //String medusHomeDir = "/opt/OMTCmm/";

        File file = new File(licenseFile);
        boolean bLicense = file.exists();
        boolean bDemoLicense = !bLicense;
        Logger.info("License file " + licenseFile + (bLicense ? " exist" : " does not exist"));

        String macaddress = db.getMachineID();
        //String macaddress = db.getMachineID();
        Logger.info("mac address: " + macaddress);

        String siteid = db.getSiteID();
        Logger.info("site id: " + siteid);

        String productKey = db.getProductKey();
        Logger.info("product key: " + productKey);

        String hash = "";
        if (bLicense) {
            hash = FileUtils.getHash(licenseFile);
            Logger.info("license file hash = " + hash);
        }

        String request;
        String response = db.GetLicenseCheckResponse(); //TRANSFER or HARDWARE_CHANGE
        if(null != response && response.length() > 1)
            request = response;
        else
        if (bDemoLicense)
            request = "GET_LICENSE";
        else
            request = "VERIFY";

        Logger.info("Request:" + request);
        String data = HttpClient.getLicenseInfo(productKey,macaddress,request,hash,strTimeStamp);
        Logger.info("FROM POST:" + data);

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

        Logger.info("message:" + message + "'; status:" + status);

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
            Logger.error("something is wrong, should not be here");
    }

    private void deleteLicenseFile(String fileName) {
        Logger.info("deleting file: " + fileName);
        File f = new File(fileName);
        if(!f.delete())
            Logger.error("Failed to delete file " + fileName);
    }

    private void saveLicenseFile(String fileName, String license) {
        Logger.info("saving license file: " + fileName);
        try {
            FileWriter fw = new FileWriter(fileName);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(license);
            bw.flush();
            bw.close();
        } catch (IOException e) {
            //e.printStackTrace();
            Logger.error("Failed to save file " + fileName);
        }
    }

    private void onNoLicense(boolean bDemoLicense, String licenseFile, String strTime, String status, String message, String messagecode) {
        Logger.info("onNoLicense");

        db.setUserReplyString(strTime + '|' + status + '|' + (messagecode.isEmpty() ? message : messagecode), "");

        if (!bDemoLicense) {
            deleteLicenseFile(licenseFile);
            db.forseUpdatePrivBasedOnLicensing(licenseFile);
        }
    }

    private void onNewLicense(String licenseFile, String license, String licensecoderm) {
        Logger.info("onNewLicense");

        db.hideDisabled(false);
        //licenseCodeLcNew = su.getHash(license)

        saveLicenseFile(licenseFile, license);
        String licenseCodeLcNewFile = FileUtils.getHash(licenseFile);
        if (!licensecoderm.equalsIgnoreCase(licenseCodeLcNewFile))
            Logger.error("Hash code don't match. Received: " + licensecoderm + " from file: " + licenseCodeLcNewFile);

        db.setUserReplyString("", "");

        db.forseUpdateMaxBasedOnLicensing(licenseFile);
        db.forseUpdatePrivBasedOnLicensing(licenseFile);
        //fu.updateWatcherBasedOnLicensing() - no need
    }

    private void onTransferComplete(String licenseFile, String license, String licensecoderm) {
        Logger.info("onTransferComplete");

        //licenseCodeLcNew = su.getHash(license)

        db.hideDisabled(false);

        saveLicenseFile(licenseFile, license);
        String licenseCodeLcNewFile = FileUtils.getHash(licenseFile);
        if (!licensecoderm.equals(licenseCodeLcNewFile))
            Logger.error("Hash code don't match. Received: " + licensecoderm + " from file: " + licenseCodeLcNewFile);

        db.forseUpdatePrivBasedOnLicensing(licenseFile);
        db.setUserReplyString("", "");
    }

    private void onOk() {
        Logger.info("onOk");
        db.setUserReplyString("", "");
        //done here
    }

    private void onPossibleTransfer(String strTime, String status, String message, String messagecode) {
        Logger.info("onPossibleTransfer");
        db.setUserReplyString(strTime + '|' + status + '|' + (messagecode.isEmpty() ? message : messagecode), "");
    }

    private void onFailed(String productKey, String strTime, String status, String message, String messagecode) {
        Logger.info("onFailed");

        if(null != productKey && !productKey.isEmpty()) // no produce key means DEMO:
            db.setUserReplyString(strTime + '|' + status + '|' + (messagecode.isEmpty() ? message : messagecode), "");
    }

    private void onTransferFailed(String strTime, String status, String message, String messagecode) {
        Logger.info("onTransferFailed");
        db.setUserReplyString(strTime + '|' + status + '|' + (messagecode.isEmpty() ? message : messagecode), "");
    }

    private void onTransferDenied(String strTime, String status, String message, String messagecode) {
        Logger.info("onTransferDenied");
        db.setUserReplyString(strTime + '|' + status + '|' + (messagecode.isEmpty() ? message : messagecode), "");
    }

    private void onLicenseDisabled(String strTime, String status, String message, String messagecode, String licenseFile) {
        Logger.info("onLicenseDisabled");
        db.setUserReplyString(strTime + '|' + status + '|' + (messagecode.isEmpty() ? message : messagecode), "");

        deleteLicenseFile(licenseFile);
        db.hideDisabled(true);
    }

    private void onCorrupted(String licenseFile, String strTime, String productKey, String macaddress, String strTimeStamp) {
        Logger.info("onCorrupted");

        String hash = "";
        String request = "GET_LICENSE";
        String data = HttpClient.getLicenseInfo(productKey, macaddress, request, hash, strTimeStamp);
        Logger.info("FROM POST:" + data);

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

        Logger.info("message:" + message + "; status:" + status);

        String str = strTime + '|' + status + '|' + messagecode + '|' + message;
        db.reportLicenseCheck("", str);

        if (status.equals("LICENSE_ISSUED"))	{
            onNewLicense(licenseFile, license, licensecoderm);
        }
    }

}
