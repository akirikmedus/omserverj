package tests;

import org.junit.*;
import mu.utils.*;

public class FileUtilsTest extends junit.framework.TestCase {

    public void test_getHash() {
	    String sResult = FileUtils.getHash("cf/fileutils.txt");
	    assertEquals("8812fa2cc436af0cc2b0c372ce43a6763e57a547", sResult);
    }

    public void test_getMacAddress() {
        String sResult = FileUtils.getMacAddress();
        assertNotNull(sResult);
        assertNotSame("", sResult);
    }
}
