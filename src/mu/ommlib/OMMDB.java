package mu.ommlib;

import mu.utils.FileUtils;
import mu.utils.IniFile;
import mu.utils.Logger;

import java.io.*;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;
import java.util.regex.Matcher;

public class OMMDB
{
    static private String testProductKey = "16C1-036E-19CE-03D6";

    protected java.lang.Object connection;

    private String dbName;

    private String ipAddress;

    private int port;

    private String userName;

    private String password;

    private final String jdbcDriver = "com.sybase.jdbc.SybDriver";

    private String url;

    public OMMDB() {
        IniFile props = new IniFile("cf/tm_prefs");

        String ipAddress = props.getString("MedUS Manager", "localIpAddress", null);
        String portNumber = props.getString("MedUS Manager", "localPortNumber", null);
        String user = props.getString("MedUS Manager", "localUser", null);
        String password = props.getString("MedUS Manager", "localPasswd", null);
        String dbName = props.getString("MedUS Manager", "localDBName", null);
        if(null == ipAddress || ipAddress.isEmpty()) {
            ipAddress = props.getString("OpenMed Manager", "localIpAddress", null);
            portNumber = props.getString("OpenMed Manager", "localPortNumber", null);
            user = props.getString("OpenMed Manager", "localUser", null);
            password = props.getString("OpenMed Manager", "localPasswd", null);
            dbName = props.getString("OpenMed Manager", "localDBName", null);
        }

        if(!ipAddress.isEmpty() && !portNumber.isEmpty() && !user.isEmpty() && !password.isEmpty() && !dbName.isEmpty())
            initDBConnection(ipAddress, (new Integer(portNumber)).intValue(), user, password, dbName);
    }

    /**
     * Initialize the connection to the database server. It only initialize the
     * needed information without starting the connection. Call getConnection()
     * to actually connect to the database server.
     */
    private void initDBConnection(String ipAddress, int port, String userName,
                                  String password, String dbName) {
        this.ipAddress = ipAddress.trim();
        this.port = port;
        this.dbName = dbName.trim();
        this.userName = userName.trim();
        this.password = password.trim();

        Properties sysProps = System.getProperties();
        StringBuffer drivers = new StringBuffer(jdbcDriver);
        String oldDrivers = sysProps.getProperty("jdbc.drivers");

        //System.out.println(">>" + oldDrivers);
        if (oldDrivers == null) {
            sysProps.put("jdbc.drivers", drivers.toString());
        } else if (oldDrivers.indexOf(jdbcDriver) < 0) {
            drivers.append(":" + oldDrivers);
            sysProps.put("jdbc.drivers", drivers.toString());
        }

        //System.out.println(">>" + sysProps.getProperty("jdbc.drivers"));
        StringBuffer urlBuffer = new StringBuffer("jdbc:sybase:Tds");
        urlBuffer.append(":" + this.ipAddress);
        urlBuffer.append(":" + this.port);
        urlBuffer.append("/" + this.dbName);
        url = urlBuffer.toString();
    }

    private boolean connectionIsValid() {
        try {
            if (connection == null) {
                return false;
            }

            if (((Connection) connection).isClosed()) {
                return false;
            }

            Statement stmt = ((Connection) connection).createStatement();
            ResultSet rs = stmt.executeQuery("select patient_id from patients where patient_id = '0000000000'");
            String pid = null;

            while (rs.next()) {
                pid = rs.getString("patient_id");
            }

            stmt.close();

            return true;
        } catch (SQLException e) {
            e.printStackTrace();

            try {
                ((Connection) connection).close();
            } catch (SQLException se) {
                se.printStackTrace();
            }

            return false;
        }
    }

    /** Initiate the connection to the database server of this site. */
    public synchronized Object getConnection() throws Exception {
        int retryCount = 0;
        int maxRetry = 10;

        while (true) {
            try {
                if (!connectionIsValid()) {
                    //System.out.println("URL : " + url);
                    //System.out.println("URL : " + userName);
                    //System.out.println("URL : " + password);
                    connection = DriverManager.getConnection(url, userName, password);
                }

                break;
            } catch (SQLException e) {
                if (++retryCount > maxRetry) {
                    String s = "gave up after " + retryCount + " on connection to " + url + ": " + e.getMessage();
                    Logger.error(s);
                    throw new Exception(s);
                } else {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ex) {
                    }
                }
            }
        }

        return connection;
    }


    public String getProductKey()
    {
        if(null != testProductKey) return testProductKey;

        String sql = "SELECT value FROM tm_prefs WHERE name = 'GLOBAL' AND param = 'DeviceID'";
        return getOneValue(sql);
    }

    public String getMachineID()
    {
        // sudo dmidecode | grep -i uuid
        return FileUtils.getMacAddress();
    }

    public String getSiteID()
    {
        String sql = "SELECT site_id, name, type FROM sites";
        return getOneValue(sql);
    }

    public boolean checkDBtables(boolean b)
    {
        boolean okay = false;
        /*data = pkgutil.get_data(__package__, 'database.dat')
        values = re.split("\W+", data)
        try:
        db = Sybase.connect(values[0], values[1], values[2], values[3])
        c = db.cursor()
        c.execute("select name from tm_prefs")
        data = c.fetchall()
        # name = data[0][0]  # first row, first column
            okay = True
        except (SystemExit, KeyboardInterrupt):
        raise
        except Exception:
        okay = False

        if not okay:
        sql = "create table tm_prefs (name varchar(64) not null, param varchar(64) not null, value varchar(255) null, primary key (name, param) )"
        try:
        c.execute(sql)
        c.close()
        db.close()
        except (SystemExit, KeyboardInterrupt):
        raise
        except Exception:
        logger.error('Failed', exc_info=True)*/

        return true;

    }

    public String GetLicenseCheckResponse()
    {
        String sql = "select value from tm_prefs where name = 'LCS' and param = 'response'";
        return getOneValue(sql);
    }

    public int reportLicenseCheck(String sToLog, String sToDB)
    {
        if (null != sToLog && !sToLog.isEmpty())
            ;//logger.info(sToLog);

        int updated = 0;
        if (null != sToDB && !sToDB.isEmpty()) {
            String sql = "UPDATE tm_prefs SET value ='" + sToDB + "' WHERE name = 'LCS' AND param = 'last_check'";
            updated = executeSql(sql);
            if (executeSql(sql) < 1) {
                sql = "INSERT INTO tm_prefs (name, param, value) values ('LCS', 'last_check', '" + sToDB + "')";
                updated = executeSql(sql);
            }
        }
        return updated;
    }

    public void setUserReplyString(String request, String response)
    {
        String sql = "UPDATE tm_prefs SET value ='" + request + "' WHERE name = 'LCS' AND param = 'request'";
        if (executeSql(sql) < 1) {
            sql = "INSERT INTO tm_prefs values ('LCS', 'request', '" + request + "')";
            executeSql(sql);
        }

        sql = "UPDATE tm_prefs SET value ='" + response + "' WHERE name = 'LCS' AND param = 'response'";
        if (executeSql(sql) < 1) {
            sql = "INSERT INTO tm_prefs values ('LCS', 'response', '" + response + "')";
            executeSql(sql);
        }
    }

    public void forseUpdatePrivBasedOnLicensing(String licenseFile)
    {
        //logger.info("forseUpdatePrivBasedOnLicensing")

        File file = new File(licenseFile);
        if (!file.exists())
            ;//logger.error("File path {} does not exists.".format(licenseFile));

        String sql = "update user_privileges_lc set licensed = 0";
        executeSql(sql);

        try {
            boolean bInPrivs = false;
            BufferedReader br = new BufferedReader( new FileReader( licenseFile ));
            String line;
            while(( line = br.readLine()) != null ) {
                if(line.equals("[PRIVILEGES]"))
                    bInPrivs = true;

                if (!bInPrivs)
                    continue;
                else {
                    String[] s = line.split("=");
                    sql = "update user_privileges_lc set licensed = 1 where privilege = '" + s[0] + "'";
                    executeSql(sql);
                }
            }

            //updatePrivBasedOnLicensing();

        } catch (FileNotFoundException e) {
            //e.printStackTrace();
        } catch (IOException e) {
            //e.printStackTrace();
        }
    }

    /*def updatePrivBasedOnLicensing():

    data = pkgutil.get_data(__package__, 'database.dat')
    values = re.split("\W+", data)
        try:
    db = Sybase.connect(values[0], values[1], values[2], values[3])
    c = db.cursor()

    sql = "select distinct user_privileges_lc.privilege, user_privileges_lc.description from user_privileges, user_privileges_lc where availability = 0 and user_privileges.privilege = user_privileges_lc.privilege and licensed = 0 "
        c.execute(sql)
    data = c.fetchall()

        for row in data:
    privilege = row[0]
    description = row[1]

    sql = "delete from user_privileges where privilege = '" + privilege + "' and availability = 0"
        c.execute(sql)
    sql = "insert into user_privileges (privilege, availability, group_id, user_id, description) values ('" + privilege +"', 1, '', '', '" + description + "')"
        c.execute(sql)

    bEveryoneCheck = False
        sql = "select distinct user_privileges_lc.privilege, user_privileges_lc.description from user_privileges, user_privileges_lc where availability = 1 and user_privileges.privilege = user_privileges_lc.privilege and licensed = 1 "
        " and user_privileges_lc.privilege in ('omusl_manage_patients', 'omusl_manage_studies', 'omusl_push_monitor', 'omusl_run', 'omusl_study_status',"
                " 'omv_add_report', 'omv_edit_report', 'omv_email', 'omv_push', 'omv_save_anno', 'omv_search', 'omv_show_anno', 'omv_view', 'omx_multy', 'omx_run', 'omusl_vcd',"
                " 'allpro_images', 'omusl_wklst_scu', 'omusl_scanner', 'omusl_attach', 'omusl_non_dicom', 'omusl_lightscribe', 'omusl_cd_import', 'omusl_jpeg_export',"
                " 'omv_adv_anno', 'omv_https', 'omusl_radviewer')"
                c.execute(sql)
    data = c.fetchall()

        for row in data:
    privilege = row[0]
    description = row[1]

    sql = "delete from user_privileges where privilege = '" + privilege + "' and availability = 1"
        c.execute(sql)
    sql = "insert into user_privileges (privilege, availability, group_id, user_id, description) values ('" + privilege +"', 0, 'everyone', '', '" + description + "')"
        c.execute(sql)
    bEveryoneCheck = True

        if (bEveryoneCheck):
    sql = "SELECT group_name FROM groups WHERE group_name = 'everyone'"
        c.execute(sql)
    data = c.fetchall()
        if (data[0][0] != ""):
    sql = "insert into groups (group_name) values ('everyone', '')"
        c.execute(sql)

    bAdminCheck = False
        sql = "select distinct user_privileges_lc.privilege, user_privileges_lc.description from user_privileges, user_privileges_lc where availability = 1 and user_privileges.privilege = user_privileges_lc.privilege and licensed = 1 "
        " and user_privileges_lc.privilege in ('omacm_add_priv', 'omacm_admin', 'omadmin_cc', 'omadmin_console', 'omadmin_db_check', 'omadmin_dict', 'omadmin_distr',"
                " 'omadmin_erpr', 'omadmin_file_audit', 'omadmin_flex', 'omadmin_hp', 'omadmin_kds', 'omadmin_push', 'omadmin_run', 'omadmin_utils', 'omsdm_power_on',"
                " 'omstm_run', 'omstm_admin', 'omusl_profile', 'omv_vitrea', 'pacs_hl7_adv', 'omv_push_adv', 'pacs_ipad', 'pacs_android', 'omx_publishing',"
                " 'omusl_vcd_import', 'omusl_oncall_caching', 'rsvw_dictation', 'omv_print', 'omv_dicom_print', 'omv_multi_monitor', 'autoupdate_run', 'omusl_adv_demo',"
                " 'omusl_adv_filters', 'pacs_wklst_scp', 'pacs_report_activity', 'pacs_backup', 'pacs_backup_adv', 'pacs_hl7', 'pacs_hl7_adv')"

                for row in data:
    privilege = row[0]
    description = row[1]

    sql = "delete from user_privileges where privilege = '" + privilege + "' and availability = 1"
        c.execute(sql)
    sql = "insert into user_privileges (privilege, availability, group_id, user_id, description) values ('" + privilege + "', 0, '', 'admin', '" + description + "')"
        c.execute(sql)
    bAdminCheck = True

        if (bAdminCheck):
    sql = "select user_id from users where user_id = 'admin'"
        c.execute(sql)
    data = c.fetchall()
        if (data[0][0] != ""):
    sql = "insert into users (user_id, name, last_name, first_name, password) values ('admin', 'PACSimple Admin', 'Admin', 'PACSimple', 'admin!')"
        c.execute(sql)

    sql = "select privilege from user_privileges where user_id = 'admin' and privilege = 'omadmin_run'"
        c.execute(sql)
    data = c.fetchall()
        if (data[0][0] != ""):
        c.execute("select description from user_privileges where privilege = 'omadmin_run'")
    sql = "delete from user_privileges where privilege = 'omadmin_run' and availability = 1"
        c.execute(sql)
    data = c.fetchall()
    sDescription = data[0][0]
    sql = "insert into user_privileges (privilege, availability, group_id, user_id, description) values ('omadmin_run', 0, '', 'admin', '" + sDescription + "')"
        c.execute(sql)

    sql = "select privilege from user_privileges where user_id = 'admin' and privilege = 'omacm_admin'"
        c.execute(sql)
    data = c.fetchall()
        if (data[0][0] != ""):
        c.execute("select description from user_privileges where privilege = 'omacm_admin'")
    sql = "delete from user_privileges where privilege = 'omacm_admin' and availability = 1"
        c.execute(sql)
    data = c.fetchall()
    sDescription = data[0][0]
    sql = "insert into user_privileges (privilege, availability, group_id, user_id, description) values ('omacm_admin', 0, '', 'admin', '" + sDescription + "')"
        c.execute(sql)

    sql = "select privilege from user_privileges where user_id = 'admin' and privilege = 'omacm_add_priv'"
        c.execute(sql)
    data = c.fetchall()
        if (data[0][0] != ""):
        c.execute("select description from user_privileges where privilege = 'omacm_add_priv'")
    sql = "delete from user_privileges where privilege = 'omacm_add_priv' and availability = 1"
        c.execute(sql)
    data = c.fetchall()
    sDescription = data[0][0]
    sql = "insert into user_privileges (privilege, availability, group_id, user_id, description) values ('omacm_add_priv', 0, '', 'admin', '" + sDescription + "')"
        c.execute(sql)

        c.close()
        db.close()
    except (SystemExit, KeyboardInterrupt):
    raise
    except Exception:
        logger.error('Failed', exc_info=False)
    */

    public void hideDisabled(boolean disabled)
    {
        //logger.info("hideDisabled")
        String str = disabled ? "1" : "0";
        String filename = "/opt/OMTCmm/lib/omm23.jar";
        try {
            FileWriter fw = new FileWriter(filename);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(str);
        } catch (IOException e) {
            //e.printStackTrace();
            //logger.error("Failed to save file " + fileName);
        }
    }

    public void forseUpdateMaxBasedOnLicensing(String licenseFile)
    {
        //logger.info("forseUpdateMaxBasedOnLicensing")

        IniFile config = new IniFile();
        config.loadFromString(licenseFile);

        String sss = config.getString("CAPACITY", "PACSMaxImageCount", "");
        String sql = "UPDATE tm_prefs SET value ='" + sss + "' WHERE name = 'GLOBAL' AND param = 'pic'";
        if (executeSql(sql) < 1) {
            sql = "INSERT INTO tm_prefs (name, param, value) values ('GLOBAL', 'pic', '" + sss + "')";
            executeSql(sql);
        }

        sss = config.getString("CAPACITY", "PACSMaxPushDestinations", "");
        sql = "UPDATE tm_prefs SET value ='" + sss + "' WHERE name = 'GLOBAL' AND param = 'pid'";
        if (executeSql(sql) < 1) {
            sql = "INSERT INTO tm_prefs (name, param, value) values ('GLOBAL', 'pid', '" + sss + "')";
            executeSql(sql);
        }

        sss = config.getString("CAPACITY", "PACSMaxModalities", "");
        sql = "UPDATE tm_prefs SET value ='" + sss + "' WHERE name = 'GLOBAL' AND param = 'pim'";
        if (executeSql(sql) < 1) {
            sql = "INSERT INTO tm_prefs (name, param, value) values ('GLOBAL', 'pim', '" + sss + "')";
            executeSql(sql);
        }

        sss = config.getString("CAPACITY", "PACSMaxQandR", "");
        sql = "UPDATE tm_prefs SET value ='" + sss + "' WHERE name = 'GLOBAL' AND param = 'piq'";
        if (executeSql(sql) < 1) {
            sql = "INSERT INTO tm_prefs (name, param, value) values ('GLOBAL', 'piq', '" + sss + "')";
            executeSql(sql);
        }

        sss = config.getString("CAPACITY", "PACSMaxClients", "");
        sql = "UPDATE tm_prefs SET value ='" + sss + "' WHERE name = 'GLOBAL' AND param = 'pil'";
        if (executeSql(sql) < 1) {
            sql = "INSERT INTO tm_prefs (name, param, value) values ('GLOBAL', 'pil', '" + sss + "')";
            executeSql(sql);
        }
    }

    private String getOneValue(String sql)
    {
        try {
            Connection c = (Connection) getConnection();
            Statement stmt;
            stmt = c.createStatement();
            Vector sites = new Vector();

            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next()) {
                sites.addElement(rs.getString("site_id") + ":" + rs.getString("name"));
            }

            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "";
    }

    private int executeSql(String sql)
    {
        return -1;
    }
}
