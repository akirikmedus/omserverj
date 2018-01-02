package tests;

import org.junit.*;
import mu.utils.*;

public class StrUtilsTest extends junit.framework.TestCase {

    public void testgetHash() {
	    String shash = StrUtils.getHash("The quick brown fox jumps over the lazy dog");
	    assertEquals("2fd4e1c67a2d28fced849ee1bb76e7391b93eb12", shash);
    }
}
