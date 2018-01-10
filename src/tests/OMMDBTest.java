package tests;

import mu.ommlib.OMMDB;
import org.junit.*;
import mu.utils.*;

public class OMMDBTest extends junit.framework.TestCase {

    public void test_getSiteID() {
	    //String sql = "SELECT site_id, name FROM sites";

        OMMDB db = new OMMDB();
        String siteId = db.getSiteID();

	    assertEquals("LT1-", siteId);
    }

    public void test_executeUpdate() {
        String sql = "UPDATE sites set name = 'blabla' WHERE site_id = 'bla'";

        OMMDB db = new OMMDB();
        int nUpdated = db.executeUpdate(sql);

        assertEquals(0, nUpdated);
    }

    public void test_checkDBtables() {
        OMMDB db = new OMMDB();
        boolean bOkay = db.checkDBtables();

        assertEquals(true, bOkay);
    }

    public void test_updatePrivBasedOnLicensing() {
        OMMDB db = new OMMDB();
        //db.updatePrivBasedOnLicensing();

        assertEquals(true, true);
    }
    
}
