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

        //==============================================================================================================

        data = "msg:^License disabled|MSG_LICENSE_DISABLED\n"
                + "status:^LICENSE_DISABLED\n"
                + "dt:^20171106\n"
                + "<br>";

        map = StrUtils.parseLicenseReturn(data);

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
    }
}
