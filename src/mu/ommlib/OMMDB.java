package mu.ommlib;

import mu.utils.FileUtils;
import mu.utils.IniFile;
import mu.utils.Logger;

import java.io.*;
import java.sql.*;
import java.util.Properties;
//import com.sybase.jdbc4.*;

public class OMMDB
{
    static private String testProductKey = null;//"16C1-036E-19CE-03D6";

    private java.lang.Object connection;

    private String userName;

    private String password;

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
            initDBConnection(ipAddress, new Integer(portNumber), user, password, dbName);
    }

    /**
     * Initialize the connection to the database server. It only initialize the
     * needed information without starting the connection. Call getConnection()
     * to actually connect to the database server.
     */
    private void initDBConnection(String ipAddress, int port, String userName,
                                  String password, String dbName) {
        String ipAddress1 = ipAddress.trim();
        String dbName1 = dbName.trim();
        this.userName = userName.trim();
        this.password = password.trim();

        Properties sysProps = System.getProperties();
        String jdbcDriver = "com.sybase.jdbc.SybDriver";
        StringBuilder drivers = new StringBuilder(jdbcDriver);
        String oldDrivers = sysProps.getProperty("jdbc.drivers");

        //System.out.println(">>" + oldDrivers);
        if (oldDrivers == null) {
            sysProps.put("jdbc.drivers", drivers.toString());
        } else if (!oldDrivers.contains(jdbcDriver)) {
            drivers.append(":").append(oldDrivers);
            sysProps.put("jdbc.drivers", drivers.toString());
        }

        //System.out.println(">>" + sysProps.getProperty("jdbc.drivers"));
        url = "jdbc:sybase:Tds" + ":" + ipAddress1 +
                ":" + port +
                "/" + dbName1;
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

            while (rs.next()) {
                String pid = rs.getString("patient_id");
                if(null != pid)
                    Logger.info(pid);
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
    private synchronized Object getConnection() throws Exception {
        int retryCount = 0;
        int maxRetry = 1;

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
                    } catch (InterruptedException ignored) {
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
        //return "55ffb06c-559d-4e08-b5ec-6bd3b9541755";
    }

    public String getSiteID()
    {
        String sql = "SELECT site_id, name, type FROM sites";
        return getOneValue(sql);
    }

    public boolean checkDBtables()
    {
        boolean okay = false;
        try {
            Connection c = (Connection) getConnection();
            Statement stmt = c.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);

            ResultSet rs = stmt.executeQuery("SELECT name FROM tm_prefs");
            okay = null != rs;
            stmt.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!okay) {
            String sql = "create table tm_prefs (name varchar(64) not null, param varchar(64) not null, value varchar(255) null, primary key (name, param) )";
            executeUpdate(sql);
            Logger.error("Failed");
        }

        okay = false;
        try {
            Connection c = (Connection) getConnection();
            Statement stmt = c.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);

            ResultSet rs = stmt.executeQuery("SELECT user_id FROM user_privileges");
            okay = null != rs;
            stmt.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!okay) {
            String sql = "create table user_privileges (privilege varchar(30) not null, availability int not null, group_id varchar(30) null, user_id varchar(30) null, description varchar(128) null )";
            executeUpdate(sql);
            Logger.error("Failed");
        }

        okay = false;
        try {
            Connection c = (Connection) getConnection();
            Statement stmt = c.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);

            ResultSet rs = stmt.executeQuery("SELECT indx FROM user_privileges_lc");
            okay = null != rs;
            stmt.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!okay) {
            String sql = "create table user_privileges_lc (indx int not null, privilege varchar(30) not null, description varchar(128) null, licensed int not null )";
            executeUpdate(sql);
            Logger.error("Failed");
        }

        okay = false;
        try {
            Connection c = (Connection) getConnection();
            Statement stmt = c.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);

            ResultSet rs = stmt.executeQuery("SELECT * FROM groups");
            okay = null != rs;
            stmt.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!okay) {
            String sql = "create table groups (group_name varchar(30) not null, user_id varchar(30) null )";
            executeUpdate(sql);
            sql = "insert into groups (group_name, user_id) values ('everyone', '')";
            executeUpdate(sql);
            Logger.error("Failed");
        }

        CheckPrivilegesRecord(PRIVILEGE_NAME_USM_ADD,				INSERT_LINE_ADMIN, PRIVILEGE_DESCR_USM_ADD,				1);
        CheckPrivilegesRecord(PRIVILEGE_NAME_USM_ADMIN,			INSERT_LINE_ADMIN, PRIVILEGE_DESCR_USM_ADMIN,			2);

        CheckPrivilegesRecord(PRIVILEGE_NAME_OMA_CC,				INSERT_LINE_ADMIN, PRIVILEGE_DESCR_OMA_CC,				3);
        CheckPrivilegesRecord(PRIVILEGE_NAME_OMA_CONS,				INSERT_LINE_ADMIN, PRIVILEGE_DESCR_OMA_CONS,			4);
        CheckPrivilegesRecord(PRIVILEGE_NAME_OMA_DB,				INSERT_LINE_ADMIN, PRIVILEGE_DESCR_OMA_DB,				5);
        CheckPrivilegesRecord(PRIVILEGE_NAME_OMA_DICT,				INSERT_LINE_ADMIN, PRIVILEGE_DESCR_OMA_DICT,			6);
        CheckPrivilegesRecord(PRIVILEGE_NAME_OMA_DISTR,			INSERT_LINE_ADMIN, PRIVILEGE_DESCR_OMA_DISTR,			7);
        CheckPrivilegesRecord(PRIVILEGE_NAME_OMA_ERPR,				INSERT_LINE_ADMIN, PRIVILEGE_DESCR_OMA_ERPR,			8);
        CheckPrivilegesRecord(PRIVILEGE_NAME_OMA_FILES,			INSERT_LINE_ADMIN, PRIVILEGE_DESCR_OMA_FILES,			9);
        CheckPrivilegesRecord(PRIVILEGE_NAME_OMA_FLEX,				INSERT_LINE_ADMIN, PRIVILEGE_DESCR_OMA_FLEX,			10);
        CheckPrivilegesRecord(PRIVILEGE_NAME_OMA_HP,				INSERT_LINE_ADMIN, PRIVILEGE_DESCR_OMA_HP,				11);
        CheckPrivilegesRecord(PRIVILEGE_NAME_OMA_KDS,				INSERT_LINE_ADMIN, PRIVILEGE_DESCR_OMA_KDS,				12);
        CheckPrivilegesRecord(PRIVILEGE_NAME_OMA_PUSH,				INSERT_LINE_ADMIN, PRIVILEGE_DESCR_OMA_PUSH,			13);
        CheckPrivilegesRecord(PRIVILEGE_NAME_OMA_RUN,				INSERT_LINE_ADMIN, PRIVILEGE_DESCR_OMA_RUN,				14);
        CheckPrivilegesRecord(PRIVILEGE_NAME_OMA_UTILS,			INSERT_LINE_ADMIN, PRIVILEGE_DESCR_OMA_UTILS,			15);

        CheckPrivilegesRecord(PRIVILEGE_NAME_DICTIONARY_ADD,		INSERT_LINE_ADMIN, PRIVILEGE_DESCR_DICTIONARY_ADD,		16);
        CheckPrivilegesRecord(PRIVILEGE_NAME_STM_WORK_WITH,		INSERT_LINE_ADMIN, PRIVILEGE_DESCR_STM_WORK_WITH,		17);
        CheckPrivilegesRecord(PRIVILEGE_NAME_STM_RUN,				INSERT_LINE_ADMIN, PRIVILEGE_DESCR_STM_RUN,				18);

        CheckPrivilegesRecord(PRIVILEGE_NAME_USL_MANAGE_PATIENTS,	INSERT_LINE_ADMIN, PRIVILEGE_DESCR_USL_MANAGE_PATIENTS,	19);
        CheckPrivilegesRecord(PRIVILEGE_NAME_USL_MANAGE_STUDIES,	INSERT_LINE_ADMIN, PRIVILEGE_DESCR_USL_MANAGE_STUDIES,	20);
        CheckPrivilegesRecord(PRIVILEGE_NAME_USL_PROFILE,			INSERT_LINE_ADMIN, PRIVILEGE_DESCR_USL_PROFILE,			21);
        CheckPrivilegesRecord(PRIVILEGE_NAME_USL_PUSH_MONITOR,		INSERT_LINE_ADMIN, PRIVILEGE_DESCR_USL_PUSH_MONITOR,	22);
        CheckPrivilegesRecord(PRIVILEGE_NAME_USL_RUN,				INSERT_LINE_EVERYONE, PRIVILEGE_DESCR_USL_RUN,			23);
        CheckPrivilegesRecord(PRIVILEGE_NAME_USL_STUDY_STATUS,		INSERT_LINE_ADMIN, PRIVILEGE_DESCR_USL_STUDY_STATUS,	24);

        CheckPrivilegesRecord(PRIVILEGE_NAME_OMV_ADD_REPORT,		INSERT_LINE_ADMIN, PRIVILEGE_DESCR_OMV_ADD_REPORT,		25);
        CheckPrivilegesRecord(PRIVILEGE_NAME_OMV_EDIT_REPORT,		INSERT_LINE_ADMIN, PRIVILEGE_DESCR_OMV_EDIT_REPORT,		26);
        CheckPrivilegesRecord(PRIVILEGE_NAME_OMV_EMAIL,			INSERT_LINE_ADMIN, PRIVILEGE_DESCR_OMV_EMAIL,			27);
        CheckPrivilegesRecord(PRIVILEGE_NAME_OMV_PUSH,				INSERT_LINE_ADMIN, PRIVILEGE_DESCR_OMV_PUSH,			28);
        CheckPrivilegesRecord(PRIVILEGE_NAME_OMV_SAVE_ANNO,		INSERT_LINE_ADMIN, PRIVILEGE_DESCR_OMV_SAVE_ANNO,		29);
        CheckPrivilegesRecord(PRIVILEGE_NAME_OMV_SEARCH,			INSERT_LINE_EVERYONE, PRIVILEGE_DESCR_OMV_SEARCH,		30);
        CheckPrivilegesRecord(PRIVILEGE_NAME_OMV_SHOW_ANNO,		INSERT_LINE_EVERYONE, PRIVILEGE_DESCR_OMV_SHOW_ANNO,	31);
        CheckPrivilegesRecord(PRIVILEGE_NAME_OMV_VIEW,				INSERT_LINE_EVERYONE, PRIVILEGE_DESCR_OMV_VIEW,			32);
        CheckPrivilegesRecord(PRIVILEGE_NAME_OMV_VITREA,			INSERT_LINE_ADMIN, PRIVILEGE_DESCR_OMV_VITREA,			33);

        CheckPrivilegesRecord(PRIVILEGE_NAME_OMX_MULTY,			INSERT_LINE_EVERYONE, PRIVILEGE_DESCR_OMX_MULTY,		34);
        CheckPrivilegesRecord(PRIVILEGE_NAME_OMX_RUN,				INSERT_LINE_EVERYONE, PRIVILEGE_DESCR_OMX_RUN,			35);

        CheckPrivilegesRecord(PRIVILEGE_NAME_ALLPRO_IMAGES,		INSERT_LINE_EVERYONE, PRIVILEGE_DESCR_ALLPRO_IMAGES,	36);
        CheckPrivilegesRecord(PRIVILEGE_NAME_AUTOUPDATE_RUN,		INSERT_LINE_EVERYONE, PRIVILEGE_DESCR_AUTOUPDATE_RUN,	37);

        //updatePrivBasedOnLicensing();

        return true;
    }


    public boolean CheckPrivilegesRecord(String privName, String insertParam, String description, int index)    {
        try {
            Connection c = (Connection) getConnection();
            Statement stmt = c.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            ResultSet rs = stmt.executeQuery("SELECT privilege FROM user_privileges WHERE privilege = '" + privName + "'");
            if(!rs.first()) {
                String sql = "INSERT INTO user_privileges (privilege, availability, group_id, user_id, description)";
                sql += " values ('" + privName + "'" + insertParam + ", '" + description + "')";
                executeUpdate(sql);
            }
            stmt.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            Connection c = (Connection) getConnection();
            Statement stmt = c.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            ResultSet rs = stmt.executeQuery("SELECT privilege FROM user_privileges_lc WHERE privilege = '" + privName + "'");
            if(!rs.first()) {
                String sql = "INSERT INTO user_privileges_lc (indx, privilege, description, licensed)";
                sql += " values (" + Integer.toString(index) + ", '" + privName + "', '" + description + "', 1)";
                executeUpdate(sql);
            }
            stmt.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return true;
    }

    public String GetLicenseCheckResponse()
    {
        String sql = "SELECT value FROM tm_prefs WHERE name = 'LCS' AND param = 'response'";
        return getOneValue(sql);
    }

    public void reportLicenseCheck(String sToLog, String sToDB)
    {
        if (null != sToLog && !sToLog.isEmpty())
            Logger.info(sToLog);

        if (null != sToDB && !sToDB.isEmpty()) {
            String sql = "UPDATE tm_prefs SET value ='" + sToDB + "' WHERE name = 'LCS' AND param = 'last_check'";
            if (executeUpdate(sql) < 1) {
                sql = "INSERT INTO tm_prefs (name, param, value) values ('LCS', 'last_check', '" + sToDB + "')";
                executeUpdate(sql);
            }
        }
    }

    public void setUserReplyString(String request, String response)
    {
        String sql = "UPDATE tm_prefs SET value ='" + request + "' WHERE name = 'LCS' AND param = 'request'";
        if (executeUpdate(sql) < 1) {
            sql = "INSERT INTO tm_prefs values ('LCS', 'request', '" + request + "')";
            executeUpdate(sql);
        }

        sql = "UPDATE tm_prefs SET value = '" + response + "' WHERE name = 'LCS' AND param = 'response'";
        if (executeUpdate(sql) < 1) {
            sql = "INSERT INTO tm_prefs values ('LCS', 'response', '" + response + "')";
            executeUpdate(sql);
        }
    }

    public void forseUpdatePrivBasedOnLicensing(String licenseFile)
    {
        Logger.info("forseUpdatePrivBasedOnLicensing");

        File file = new File(licenseFile);
        if (!file.exists())
            Logger.error("File path " + licenseFile + " does not exists.");

        String sql = "UPDATE user_privileges_lc set licensed = 0";
        executeUpdate(sql);

        try {
            boolean bInPrivs = false;
            BufferedReader br = new BufferedReader( new FileReader( licenseFile ));
            String line;
            while(( line = br.readLine()) != null ) {
                if(line.equals("[PRIVILEGES]"))
                    bInPrivs = true;

                if (bInPrivs)   {
                    String[] s = line.split("=");
                    if(s.length > 1) {
                        sql = "UPDATE user_privileges_lc set licensed = 1 where privilege = '" + s[0] + "'";
                        executeUpdate(sql);
                    }
                }
            }

            updatePrivBasedOnLicensing();

        } catch (IOException e) {
            //e.printStackTrace();
        }
    }

    public void updatePrivBasedOnLicensing() {

        try {
            Connection c = (Connection) getConnection();

            Statement stmt = c.createStatement();
            String sql = "SELECT distinct user_privileges_lc.privilege, user_privileges_lc.description from user_privileges, user_privileges_lc where availability = 0 and user_privileges.privilege = user_privileges_lc.privilege and licensed = 0 ";
            ResultSet rs = stmt.executeQuery(sql);
            Statement stmt2 = c.createStatement();
            while (rs.next()) {
                String privilege = rs.getString(1);
                String description = rs.getString(2);

                sql = "delete from user_privileges where privilege = '" + privilege + "' and availability = 0";
                stmt2.executeUpdate(sql);
                sql = "insert into user_privileges (privilege, availability, group_id, user_id, description) values ('" + privilege +"', 1, '', '', '" + description + "')";
                stmt2.executeUpdate(sql);
            }
            stmt2.close();
            rs.close();
            stmt.close();

            boolean bEveryoneCheck = false;
            sql = "select distinct user_privileges_lc.privilege, user_privileges_lc.description from user_privileges, user_privileges_lc where availability = 1 and user_privileges.privilege = user_privileges_lc.privilege and licensed = 1 "
            +" and user_privileges_lc.privilege in ('omusl_manage_patients', 'omusl_manage_studies', 'omusl_push_monitor', 'omusl_run', 'omusl_study_status',"
            +" 'omv_add_report', 'omv_edit_report', 'omv_email', 'omv_push', 'omv_save_anno', 'omv_search', 'omv_show_anno', 'omv_view', 'omx_multy', 'omx_run', 'omusl_vcd',"
            +" 'allpro_images', 'omusl_wklst_scu', 'omusl_scanner', 'omusl_attach', 'omusl_non_dicom', 'omusl_lightscribe', 'omusl_cd_import', 'omusl_jpeg_export',"
            +" 'omv_adv_anno', 'omv_https', 'omusl_radviewer')";
            stmt = c.createStatement();
            rs = stmt.executeQuery(sql);
            stmt2 = c.createStatement();
            while (rs.next()) {
                String privilege = rs.getString(1);
                String description = rs.getString(2);

                sql = "delete from user_privileges where privilege = '" + privilege + "' and availability = 1";
                stmt2.executeUpdate(sql);
                sql = "insert into user_privileges (privilege, availability, group_id, user_id, description) values ('" + privilege +"', 0, 'everyone', '', '" + description + "')";
                stmt2.executeUpdate(sql);

                bEveryoneCheck = true;
            }
            if (bEveryoneCheck) {
                sql = "SELECT group_name FROM groups WHERE group_name = 'everyone'";
                ResultSet rs3 = stmt2.executeQuery(sql);
                String s = null;
                if(rs3.first())
                    s = rs3.getString(1);
                if (null == s || s.isEmpty()) {
                    sql = "INSERT INTO groups (group_name) VALUES ('everyone', '')";
                    stmt2.executeUpdate(sql);
                }
            }
            stmt2.close();
            rs.close();
            stmt.close();

            boolean bAdminCheck = false;
            sql = "select distinct user_privileges_lc.privilege, user_privileges_lc.description from user_privileges, user_privileges_lc where availability = 1 and user_privileges.privilege = user_privileges_lc.privilege and licensed = 1 "
            +" and user_privileges_lc.privilege in ('omacm_add_priv', 'omacm_admin', 'omadmin_cc', 'omadmin_console', 'omadmin_db_check', 'omadmin_dict', 'omadmin_distr',"
            +" 'omadmin_erpr', 'omadmin_file_audit', 'omadmin_flex', 'omadmin_hp', 'omadmin_kds', 'omadmin_push', 'omadmin_run', 'omadmin_utils', 'omsdm_power_on',"
            +" 'omstm_run', 'omstm_admin', 'omusl_profile', 'omv_vitrea', 'pacs_hl7_adv', 'omv_push_adv', 'pacs_ipad', 'pacs_android', 'omx_publishing',"
            +" 'omusl_vcd_import', 'omusl_oncall_caching', 'rsvw_dictation', 'omv_print', 'omv_dicom_print', 'omv_multi_monitor', 'autoupdate_run', 'omusl_adv_demo',"
            +" 'omusl_adv_filters', 'pacs_wklst_scp', 'pacs_report_activity', 'pacs_backup', 'pacs_backup_adv', 'pacs_hl7', 'pacs_hl7_adv')";
            stmt = c.createStatement();
            rs = stmt.executeQuery(sql);
            stmt2 = c.createStatement();
            while (rs.next()) {
                String privilege = rs.getString(1);
                String description = rs.getString(2);

                sql = "delete from user_privileges where privilege = '" + privilege + "' and availability = 1";
                stmt2.executeUpdate(sql);
                sql = "insert into user_privileges (privilege, availability, group_id, user_id, description) values ('" + privilege + "', 0, '', 'admin', '" + description + "')";
                stmt2.executeUpdate(sql);

                bAdminCheck = true;
            }
            if (bAdminCheck) {
                sql = "SELECT user_id FROM users WHERE user_id = 'admin'";
                ResultSet rs3 = stmt2.executeQuery(sql);
                String s = null;
                if(rs3.first())
                    s = rs3.getString(1);
                if (null == s || s.isEmpty()) {
                    sql = "INSERT INTO users (user_id, name, last_name, first_name, password) VALUES ('admin', 'PACSimple Admin', 'Admin', 'PACSimple', 'admin!')";
                    stmt2.executeUpdate(sql);
                }

                sql = "select privilege from user_privileges where user_id = 'admin' and privilege = 'omadmin_run'";
                rs3 = stmt2.executeQuery(sql);
                s = null;
                if(rs3.first())
                    s = rs3.getString(1);
                if (null == s || s.isEmpty())
                {
                    stmt2.execute("SELECT description FROM user_privileges WHERE privilege = 'omadmin_run'");
                    rs3 = stmt2.executeQuery(sql);
                    String sDescription = null;
                    if(rs3.first())
                        sDescription = rs3.getString(1);

                    sql = "DELETE FROM user_privileges WHERE privilege = 'omadmin_run' AND availability = 1";
                    stmt2.executeUpdate(sql);

                    sql = "INSERT INTO user_privileges (privilege, availability, group_id, user_id, description) VALUES ('omadmin_run', 0, '', 'admin', '" + sDescription + "')";
                    stmt2.executeUpdate(sql);
                }

                sql = "select privilege from user_privileges where user_id = 'admin' and privilege = 'omacm_admin'";
                rs3 = stmt2.executeQuery(sql);
                s = null;
                if(rs3.first())
                    s = rs3.getString(1);
                if (null == s || s.isEmpty())
                {
                    stmt2.execute("SELECT description FROM user_privileges WHERE privilege = 'omacm_admin'");
                    rs3 = stmt2.executeQuery(sql);
                    String sDescription = null;
                    if(rs3.first())
                        sDescription = rs3.getString(1);

                    sql = "DELETE FROM user_privileges WHERE privilege = 'omacm_admin' AND availability = 1";
                    stmt2.executeUpdate(sql);

                    sql = "INSERT INTO user_privileges (privilege, availability, group_id, user_id, description) VALUES ('omacm_admin', 0, '', 'admin', '" + sDescription + "')";
                    stmt2.executeUpdate(sql);
                }

                sql = "select privilege from user_privileges where user_id = 'admin' and privilege = 'omacm_add_priv'";
                rs3 = stmt2.executeQuery(sql);
                s = null;
                if(rs3.first())
                    s = rs3.getString(1);
                if (null == s || s.isEmpty())
                {
                    stmt2.execute("SELECT description FROM user_privileges WHERE privilege = 'omacm_add_priv'");
                    rs3 = stmt2.executeQuery(sql);
                    String sDescription = null;
                    if(rs3.first())
                        sDescription = rs3.getString(1);

                    sql = "DELETE FROM user_privileges WHERE privilege = 'omacm_add_priv' AND availability = 1";
                    stmt2.executeUpdate(sql);

                    sql = "INSERT INTO user_privileges (privilege, availability, group_id, user_id, description) VALUES ('omacm_add_priv', 0, '', 'admin', '" + sDescription + "')";
                    stmt2.executeUpdate(sql);
                }
            }
            stmt2.close();
            rs.close();
            stmt.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void hideDisabled(boolean disabled)
    {
        Logger.info("hideDisabled");
        String str = disabled ? "1" : "0";
        String filename = "/opt/OMTCmm/lib/omm23.jar";
        try {
            FileWriter fw = new FileWriter(filename);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(str);
        } catch (IOException e) {
            //e.printStackTrace();
            Logger.error("Failed to save file " + filename);
        }
    }

    public void forseUpdateMaxBasedOnLicensing(String licenseFile)
    {
        Logger.info("forseUpdateMaxBasedOnLicensing");

        IniFile config = new IniFile(licenseFile);

        String sss = config.getString("CAPACITY", "PACSMaxImageCount", "");
        String sql = "UPDATE tm_prefs SET value ='" + sss + "' WHERE name = 'GLOBAL' AND param = 'pic'";
        if (executeUpdate(sql) < 1) {
            sql = "INSERT INTO tm_prefs (name, param, value) values ('GLOBAL', 'pic', '" + sss + "')";
            executeUpdate(sql);
        }

        sss = config.getString("CAPACITY", "PACSMaxPushDestinations", "");
        sql = "UPDATE tm_prefs SET value ='" + sss + "' WHERE name = 'GLOBAL' AND param = 'pid'";
        if (executeUpdate(sql) < 1) {
            sql = "INSERT INTO tm_prefs (name, param, value) values ('GLOBAL', 'pid', '" + sss + "')";
            executeUpdate(sql);
        }

        sss = config.getString("CAPACITY", "PACSMaxModalities", "");
        sql = "UPDATE tm_prefs SET value ='" + sss + "' WHERE name = 'GLOBAL' AND param = 'pim'";
        if (executeUpdate(sql) < 1) {
            sql = "INSERT INTO tm_prefs (name, param, value) values ('GLOBAL', 'pim', '" + sss + "')";
            executeUpdate(sql);
        }

        sss = config.getString("CAPACITY", "PACSMaxQandR", "");
        sql = "UPDATE tm_prefs SET value ='" + sss + "' WHERE name = 'GLOBAL' AND param = 'piq'";
        if (executeUpdate(sql) < 1) {
            sql = "INSERT INTO tm_prefs (name, param, value) values ('GLOBAL', 'piq', '" + sss + "')";
            executeUpdate(sql);
        }

        sss = config.getString("CAPACITY", "PACSMaxClients", "");
        sql = "UPDATE tm_prefs SET value ='" + sss + "' WHERE name = 'GLOBAL' AND param = 'pil'";
        if (executeUpdate(sql) < 1) {
            sql = "INSERT INTO tm_prefs (name, param, value) values ('GLOBAL', 'pil', '" + sss + "')";
            executeUpdate(sql);
        }
    }

    private String getOneValue(String sql)
    {
        Logger.info("DB::getOneValue. SQL="+sql);
        String s = "";
        try {
            Connection c = (Connection) getConnection();
            Statement stmt = c.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);

            ResultSet rs = stmt.executeQuery(sql);
            if(rs.first())
                s = rs.getString(1);

            /*Vector sites = new Vector();
            while (rs.next()) {
                sites.addElement(rs.getString("site_id") + ":" + rs.getString("name"));
            }*/

            rs.close();
            stmt.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return s;
    }

    public int executeUpdate(String sql)
    {
        Logger.info("DB::executeUpdate. SQL="+sql);
        int n = -1;
        try {
            Connection c = (Connection) getConnection();
            Statement stmt = c.createStatement();

            n = stmt.executeUpdate(sql);

            stmt.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return n;
    }

    private final String MANAGE_MODALITIES_USER				= "om_mod_man";
    private final String PRIVILEGE_NAME_DICTIONARY_ADD		= "omsdm_power_on";
    private final String PRIVILEGE_DESCR_DICTIONARY_ADD		= "add/delete dictionary entrees";

    private final String PRIVILEGE_NAME_USM_ADMIN			= "omacm_admin";
    private final String PRIVILEGE_DESCR_USM_ADMIN			= "manage users/groups/privileges";
    private final String PRIVILEGE_NAME_USM_ADD				= "omacm_add_priv";
    private final String PRIVILEGE_DESCR_USM_ADD				= "add new privileges to the database";

    private final String PRIVILEGE_NAME_STM_WORK_WITH		= "omstm_admin";
    private final String PRIVILEGE_DESCR_STM_WORK_WITH		= "reconcile(create/delete/update) patients";
    private final String PRIVILEGE_NAME_STM_RUN				= "omstm_run";
    private final String PRIVILEGE_DESCR_STM_RUN				= "run application";

    private final String PRIVILEGE_NAME_OMV_SEARCH			= "omv_search";
    private final String PRIVILEGE_DESCR_OMV_SEARCH			= "do search";
    private final String PRIVILEGE_NAME_OMV_VIEW				= "omv_view";
    private final String PRIVILEGE_DESCR_OMV_VIEW			= "view images";
    private final String PRIVILEGE_NAME_OMV_EMAIL			= "omv_email";
    private final String PRIVILEGE_DESCR_OMV_EMAIL			= "send e-mail referral";
    private final String PRIVILEGE_NAME_OMV_PUSH				= "omv_push";
    private final String PRIVILEGE_DESCR_OMV_PUSH			= "push studies";
    private final String PRIVILEGE_NAME_OMV_SHOW_ANNO		= "omv_show_anno";
    private final String PRIVILEGE_DESCR_OMV_SHOW_ANNO		= "view/create annotations";
    private final String PRIVILEGE_NAME_OMV_SAVE_ANNO		= "omv_save_anno";
    private final String PRIVILEGE_DESCR_OMV_SAVE_ANNO		= "save annotations";
    private final String PRIVILEGE_NAME_OMV_VITREA			= "omv_vitrea";
    private final String PRIVILEGE_DESCR_OMV_VITREA			= "run Vitrea";
    private final String PRIVILEGE_NAME_OMV_ADD_REPORT		= "omv_add_report";
    private final String PRIVILEGE_DESCR_OMV_ADD_REPORT		= "add study reports";
    private final String PRIVILEGE_NAME_OMV_EDIT_REPORT		= "omv_edit_report";
    private final String PRIVILEGE_DESCR_OMV_EDIT_REPORT		= "edit study reports";

    private final String PRIVILEGE_NAME_OMX_MULTY			= "omx_multy";
    private final String PRIVILEGE_DESCR_OMX_MULTY			= "create multiple patients per CD";
    private final String PRIVILEGE_NAME_OMX_RUN				= "omx_run";
    private final String PRIVILEGE_DESCR_OMX_RUN				= "run application";

    private final String PRIVILEGE_NAME_OMA_RUN				= "omadmin_run";
    private final String PRIVILEGE_DESCR_OMA_RUN				= "run application";
    private final String PRIVILEGE_NAME_OMA_CC				= "omadmin_cc";
    private final String PRIVILEGE_DESCR_OMA_CC				= "do consistency check";
    private final String PRIVILEGE_NAME_OMA_UTILS			= "omadmin_utils";
    private final String PRIVILEGE_DESCR_OMA_UTILS			= "work with admin utilities";
    private final String PRIVILEGE_NAME_OMA_DB				= "omadmin_db_check";
    private final String PRIVILEGE_DESCR_OMA_DB				= "check database for consistency";
    private final String PRIVILEGE_NAME_OMA_FILES			= "omadmin_file_audit";
    private final String PRIVILEGE_DESCR_OMA_FILES			= "run file audit";
    private final String PRIVILEGE_NAME_OMA_DICT				= "omadmin_dict";
    private final String PRIVILEGE_DESCR_OMA_DICT			= "work with study type dictionary";
    private final String PRIVILEGE_NAME_OMA_HP				= "omadmin_hp";
    private final String PRIVILEGE_DESCR_OMA_HP				= "work with hanging protocols";
    private final String PRIVILEGE_NAME_OMA_PUSH				= "omadmin_push";
    private final String PRIVILEGE_DESCR_OMA_PUSH			= "work with push destinations";
    private final String PRIVILEGE_NAME_OMA_DISTR			= "omadmin_distr";
    private final String PRIVILEGE_DESCR_OMA_DISTR			= "work with distribution list";
    private final String PRIVILEGE_NAME_OMA_CONS				= "omadmin_console";
    private final String PRIVILEGE_DESCR_OMA_CONS			= "work with dashboard";
    private final String PRIVILEGE_NAME_OMA_FLEX				= "omadmin_flex";
    private final String PRIVILEGE_DESCR_OMA_FLEX			= "work with flexstorage";
    private final String PRIVILEGE_NAME_OMA_ERPR				= "omadmin_erpr";
    private final String PRIVILEGE_DESCR_OMA_ERPR			= "work with erpr";
    private final String PRIVILEGE_NAME_OMA_KDS				= "omadmin_kds";
    private final String PRIVILEGE_DESCR_OMA_KDS				= "work with KDS";

    private final String PRIVILEGE_NAME_USL_RUN				= "omusl_run";
    private final String PRIVILEGE_DESCR_USL_RUN				= "run universal study list";
    private final String PRIVILEGE_NAME_USL_PROFILE			= "omusl_profile";
    private final String PRIVILEGE_DESCR_USL_PROFILE			= "edit and save public user profiles";
    private final String PRIVILEGE_NAME_USL_MANAGE_PATIENTS	= "omusl_manage_patients";
    private final String PRIVILEGE_DESCR_USL_MANAGE_PATIENTS	= "manage patients";
    private final String PRIVILEGE_NAME_USL_MANAGE_STUDIES	= "omusl_manage_studies";
    private final String PRIVILEGE_DESCR_USL_MANAGE_STUDIES	= "manage studies";
    private final String PRIVILEGE_NAME_USL_PUSH_MONITOR		= "omusl_push_monitor";
    private final String PRIVILEGE_DESCR_USL_PUSH_MONITOR	= "push monitor";
    private final String PRIVILEGE_NAME_USL_STUDY_STATUS		= "omusl_study_status";
    private final String PRIVILEGE_DESCR_USL_STUDY_STATUS	= "change study status";
    private final String PRIVILEGE_NAME_USL_ADV_DEMO			= "omusl_adv_demo";
    private final String PRIVILEGE_DESCR_USL_ADV_DEMO		= "work with advanced demographics";
    private final String PRIVILEGE_NAME_USL_ADV_FILTERS		= "omusl_adv_filters";
    private final String PRIVILEGE_DESCR_USL_ADV_FILTERS		= "work with advanced filters";
    private final String PRIVILEGE_NAME_USL_WKLST_SCU		= "omusl_wklst_scu";
    private final String PRIVILEGE_DESCR_USL_WKLST_SCU		= "worklist SCU";
    private final String PRIVILEGE_NAME_USL_SCANNER			= "omusl_scanner";
    private final String PRIVILEGE_DESCR_USL_SCANNER			= "scan documents";
    private final String PRIVILEGE_NAME_USL_ATTACH			= "omusl_attach";
    private final String PRIVILEGE_DESCR_USL_ATTACH			= "attach images";
    private final String PRIVILEGE_NAME_USL_NON_DICOM		= "omusl_non_dicom";
    private final String PRIVILEGE_DESCR_USL_NON_DICOM		= "attach non dicom files";
    private final String PRIVILEGE_NAME_USL_LIGHTSCRIBE		= "omusl_lightscribe";
    private final String PRIVILEGE_DESCR_USL_LIGHTSCRIBE		= "use lightscribe technology";
    private final String PRIVILEGE_NAME_USL_CD_IMPORT		= "omusl_cd_import";
    private final String PRIVILEGE_DESCR_USL_CD_IMPORT		= "cd import";
    private final String PRIVILEGE_NAME_USL_JPEG_EXPORT		= "omusl_jpeg_export";
    private final String PRIVILEGE_DESCR_USL_JPEG_EXPORT		= "jpeg export";
    private final String PRIVILEGE_NAME_USL_VIRTUAL_CD		= "omusl_vcd";
    private final String PRIVILEGE_DESCR_USL_VIRTUAL_CD		= "Virtual CD";
    private final String PRIVILEGE_NAME_USL_VIRTUAL_CD_IMPORT	= "omusl_vcd_import";
    private final String PRIVILEGE_DESCR_USL_VIRTUAL_CD_IMPORT	= "import from Virtual CD";
    private final String PRIVILEGE_NAME_USL_ON_CALL			= "omusl_oncall_caching";
    private final String PRIVILEGE_DESCR_USL_ON_CALL			= "use on-call caching in USL";
    private final String PRIVILEGE_NAME_USL_RADVIEWER		= "omusl_radviewer";
    private final String PRIVILEGE_DESCR_USL_RADVIEWER		= "launch radviewer";


    private final String PRIVILEGE_NAME_ALLPRO_IMAGES		= "allpro_images";
    private final String PRIVILEGE_DESCR_ALLPRO_IMAGES		= "work only with AllPro images";

    private final String PRIVILEGE_NAME_AUTOUPDATE_RUN		= "autoupdate_run";
    private final String PRIVILEGE_DESCR_AUTOUPDATE_RUN		= "run software auto updater";

    private final String INSERT_LINE_ADMIN = ", 0, '', 'admin'";
    private final String INSERT_LINE_EVERYONE = ", 0, 'everyone', ''";
}
