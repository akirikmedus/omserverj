package tests;

import org.junit.*;
import mu.ommlib.*;

public class HttpClientTest extends junit.framework.TestCase {

    public void test_getLicenseInfo() {
        String productkey = "", mc = "", regtype = "", lichashcode = "", ts = "";

        String license = HttpClient.getLicenseInfo(productkey, mc, regtype, lichashcode, ts);
	    assertNotNull(license);
    }
   
} 
