package tests;

import mu.ommlib.OMMDB;
import org.junit.*;
import mu.utils.*;

public class OMMDBTest extends junit.framework.TestCase {

    public void testOkay() {
	    String sql = "select site_id, name from sites ";

        OMMDB db = new OMMDB();
        String siteId = db.getSiteID();

	    assertEquals("", siteId);
    }
    
}
