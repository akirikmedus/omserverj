package tests;

import org.junit.*;
import mu.utils.*;

public class FileUtilsTest extends junit.framework.TestCase {

    public void testOkay() {
	int nResult = FileUtils.abs(1);
	assertEquals(1, nResult);
    }
    
    public void testWillFail() {
	int nResult = FileUtils.abs(-1);
	assertEquals(1, nResult);
    }
    
}
