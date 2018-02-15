package tests;

import org.junit.*;
import mu.utils.*;

public class FileUtilsTest extends junit.framework.TestCase {

    public void test_getHash() {
	    String sResult = FileUtils.getHash("cf/fileutils.txt");
	    assertEquals("8812FA2CC436AF0CC2B0C372CE43A6763E57A547", sResult);

        sResult = FileUtils.getHash("cf/license.dat");
        assertEquals("D2036C0C1905D1591D7087C6D7DD4989A3F5B8C4", sResult);
    }

    public void test_getMacAddress() {
        String sResult = FileUtils.getMacAddress();
        assertNotNull(sResult);
        assertNotSame("", sResult);
    }
}
