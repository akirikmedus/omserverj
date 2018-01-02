package tests;

import org.junit.*;
import mu.utils.*;

public class StrUtilsTest extends junit.framework.TestCase {

    public void testOkay() {
	String greeting = StrUtils.getGreetings();
	assertEquals(13, greeting.length());
    }
}
