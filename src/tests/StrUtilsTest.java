package tests;

import org.junit.*;
import mu.utils.*;

import java.util.Map;

public class StrUtilsTest extends junit.framework.TestCase {

    public void test_getHash() {
	    String shash = StrUtils.getHash("The quick brown fox jumps over the lazy dog");
	    assertEquals("2fd4e1c67a2d28fced849ee1bb76e7391b93eb12", shash);
    }

    public void test_parseLicenseReturn()   {

        {//==============================================================================================================
            String message = "", messagecode = "", messagestring = "", status = "", licensecoderm = "", license = "", licenselen = "", messtimestamp = "";
            String data = null;

            Map<String, String> map = StrUtils.parseLicenseReturn(data);

            message = (null == map.get("message") ? "" : map.get("message"));
            messagecode = (null == map.get("messagecode") ? "" : map.get("messagecode"));
            messagestring = (null == map.get("messagestring") ? "" : map.get("messagestring"));
            status = (null == map.get("status") ? "" : map.get("status"));
            licensecoderm = (null == map.get("licensecoderm") ? "" : map.get("licensecoderm"));
            license = (null == map.get("license") ? "" : map.get("license"));
            licenselen = (null == map.get("licenselen") ? "" : map.get("licenselen"));
            messtimestamp = (null == map.get("messtimestamp") ? "" : map.get("messtimestamp"));

            assertEquals("", message);
            assertEquals("", messagecode);
            assertEquals("", messagestring);
            assertEquals("", status);
            assertEquals("", licensecoderm);
            assertEquals("", license);
            assertEquals("", licenselen);
            assertEquals("", messtimestamp);

        }//==============================================================================================================

        {//==============================================================================================================

            String message = "", messagecode = "", messagestring = "", status = "", licensecoderm = "", license = "", licenselen = "", messtimestamp = "";
            String data = "";

            Map<String, String> map = StrUtils.parseLicenseReturn(data);

            message = (null == map.get("message") ? "" : map.get("message"));
            messagecode = (null == map.get("messagecode") ? "" : map.get("messagecode"));
            messagestring = (null == map.get("messagestring") ? "" : map.get("messagestring"));
            status = (null == map.get("status") ? "" : map.get("status"));
            licensecoderm = (null == map.get("licensecoderm") ? "" : map.get("licensecoderm"));
            license = (null == map.get("license") ? "" : map.get("license"));
            licenselen = (null == map.get("licenselen") ? "" : map.get("licenselen"));
            messtimestamp = (null == map.get("messtimestamp") ? "" : map.get("messtimestamp"));

            assertEquals("", message);
            assertEquals("", messagecode);
            assertEquals("", messagestring);
            assertEquals("", status);
            assertEquals("", licensecoderm);
            assertEquals("", license);
            assertEquals("", licenselen);
            assertEquals("", messtimestamp);

        }//==============================================================================================================

        {//==============================================================================================================

            String message = "", messagecode = "", messagestring = "", status = "", licensecoderm = "", license = "", licenselen = "", messtimestamp = "";
            String data = "The quick brown fox jumps over the lazy dog";

            Map<String, String> map = StrUtils.parseLicenseReturn(data);

            message = (null == map.get("message") ? "" : map.get("message"));
            messagecode = (null == map.get("messagecode") ? "" : map.get("messagecode"));
            messagestring = (null == map.get("messagestring") ? "" : map.get("messagestring"));
            status = (null == map.get("status") ? "" : map.get("status"));
            licensecoderm = (null == map.get("licensecoderm") ? "" : map.get("licensecoderm"));
            license = (null == map.get("license") ? "" : map.get("license"));
            licenselen = (null == map.get("licenselen") ? "" : map.get("licenselen"));
            messtimestamp = (null == map.get("messtimestamp") ? "" : map.get("messtimestamp"));

            assertEquals("", message);
            assertEquals("", messagecode);
            assertEquals("", messagestring);
            assertEquals("", status);
            assertEquals("", licensecoderm);
            assertEquals("", license);
            assertEquals("", licenselen);
            assertEquals("", messtimestamp);

        }//==============================================================================================================

        {//==============================================================================================================

            String message = "", messagecode = "", messagestring = "", status = "", licensecoderm = "", license = "", licenselen = "", messtimestamp = "";
            String data = "msg:^License disabled|MSG_LICENSE_DISABLED\n"
                    + "status:^LICENSE_DISABLED\n"
                    + "dt:^20171106\n"
                    + "<br>";

            Map<String, String> map = StrUtils.parseLicenseReturn(data);

            message = (null == map.get("message") ? "" : map.get("message"));
            messagecode = (null == map.get("messagecode") ? "" : map.get("messagecode"));
            messagestring = (null == map.get("messagestring") ? "" : map.get("messagestring"));
            status = (null == map.get("status") ? "" : map.get("status"));
            licensecoderm = (null == map.get("licensecoderm") ? "" : map.get("licensecoderm"));
            license = (null == map.get("license") ? "" : map.get("license"));
            licenselen = (null == map.get("licenselen") ? "" : map.get("licenselen"));
            messtimestamp = (null == map.get("messtimestamp") ? "" : map.get("messtimestamp"));

            assertEquals("License disabled|MSG_LICENSE_DISABLED", message);
            assertEquals("License disabled", messagestring);
            assertEquals("MSG_LICENSE_DISABLED", messagecode);
            assertEquals("LICENSE_DISABLED", status);
            assertEquals("", licensecoderm);
            assertEquals("", license);
            assertEquals("", licenselen);
            assertEquals("20171106", messtimestamp);

        }//==============================================================================================================

        {//==============================================================================================================

            String message = "", messagecode = "", messagestring = "", status = "", licensecoderm = "", license = "", licenselen = "", messtimestamp = "";
            String data = "msg:^License issued|MSG_LICENSE_ISSUED\n"
                    + "status:^LICENSE_ISSUED\n"
                    + "licHashCode:^D2036C0C1905D1591D7087C6D7DD4989A3F5B8C4\n"
                    + "licCount:^5494\n"
                    + "licString:^#\n"
                    + "# !!! PLEASE DO NOT EDIT THE CONTENT OF THIS FILE !!!\n"
                    + "#\n"
                    + "\n"
                    + "[LICENSE]\n"
                    + "Version=2\n"
                    + "GenerationDate=2017-10-19\n"
                    + "GeneratedBy=Bernard Maury\n"
                    + "DistributorName=MedUS, LLC\n"
                    + "ExpirationDate=*\n"
                    + "TransactionID=226500030451\n"
                    + "LicenseType=New License\n"
                    + "FeatureSet=0\n"
                    + "AutoUpdateTrigger=MEDUS\n"
                    + "SupportType=3\n"
                    + "MaintanenceExpirationDate=*\n"
                    + "Connected=1\n"
                    + "Transferable=0\n"
                    + "\n"
                    + "[CUSTOMER]\n"
                    + "InstitutionName=MedUS, LLC\n"
                    + "SiteID=B0CX\n"
                    + "SiteName=BM Laptop 1\n"
                    + "SiteKey=16C1-036E\n"
                    + "ProductKey=16C1-036E-19CE-03D5\n"
                    + "\n"
                    + "[CAPACITY]\n"
                    + "PACSMaxImageCount=-1\n"
                    + "PACSMaxPrinters=-1\n"
                    + "PACSMaxPushDestinations=-1\n"
                    + "PACSMaxModalities=-1\n"
                    + "PACSMaxQandR=-1\n"
                    + "PACSMaxClients=-1\n"
                    + "\n"
                    + "[OPTIONS]\n"
                    + "POMaps=.1.19.17.1.1..33.1.2.1.33.1.1.20.1.1..8.8.1.1.1.33.33.33..1.1.1.1.1..26.26..1.1.22.12.23.24.24.27.25.1.35.14.1.18.3.5.6.7.11.15.16.28.29.30.31.34.36.37.38.4.43\n"
                    + "SpecialConditions=no\n"
                    + "\n"
                    + "[PRIVILEGES]\n"
                    + "omacm_admin=8E597305B950CEE989ACD3189E176822699CBA8F\n"
                    + "omusl_radviewer=1AAAB0413A817535B0D408A3BEBC6097477C967BendOfLicString\n"
                    + "t:^20171030";

            Map<String, String> map = StrUtils.parseLicenseReturn(data);

            message = (null == map.get("message") ? "" : map.get("message"));
            messagecode = (null == map.get("messagecode") ? "" : map.get("messagecode"));
            messagestring = (null == map.get("messagestring") ? "" : map.get("messagestring"));
            status = (null == map.get("status") ? "" : map.get("status"));
            licensecoderm = (null == map.get("licensecoderm") ? "" : map.get("licensecoderm"));
            license = (null == map.get("license") ? "" : map.get("license"));
            licenselen = (null == map.get("licenselen") ? "" : map.get("licenselen"));
            messtimestamp = (null == map.get("messtimestamp") ? "" : map.get("messtimestamp"));

            assertEquals("License issued|MSG_LICENSE_ISSUED", message);
            assertEquals("License issued", messagestring);
            assertEquals("MSG_LICENSE_ISSUED", messagecode);
            assertEquals("LICENSE_ISSUED", status);
            assertEquals("D2036C0C1905D1591D7087C6D7DD4989A3F5B8C4", licensecoderm);
            assertNotSame(0, license.length());
            assertEquals("5494", licenselen);
            assertEquals("20171030", messtimestamp);

        }//==============================================================================================================
    }
}
