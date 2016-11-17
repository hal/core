package org.jboss.as.console.client.shared.subsys.jca.model;

import com.google.inject.Inject;
import org.jboss.as.console.client.shared.BeanFactory;
import org.jboss.as.console.client.shared.properties.PropertyRecord;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static org.jboss.as.console.client.shared.subsys.jca.model.DataSourceTemplate.Vendor.*;

/**
 * List of well known datasource templates
 */
public class DataSourceTemplates implements Iterable<DataSourceTemplate<? extends DataSource>> {

    private final List<DataSourceTemplate<? extends DataSource>> pool;
    private final BeanFactory beanFactory;

    @Inject
    public DataSourceTemplates(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;

        DataSource dataSource;
        XADataSource xaDataSource;
        JDBCDriver driver;
        List<DataSourceTemplate<? extends DataSource>> setup = new ArrayList<>();


        // ------------------------------------------------------ H2
        // Driver
        driver = beanFactory.jdbcDriver().as();
        driver.setName("h2");
        driver.setDriverModuleName("com.h2database.h2");
        driver.setDriverClass("org.h2.Driver");
        driver.setXaDataSourceClass("org.h2.jdbcx.JdbcDataSource");

        // DS
        dataSource = beanFactory.dataSource().as();
        dataSource.setName("H2DS");
        dataSource.setPoolName("H2DS_Pool");
        dataSource.setJndiName("java:/H2DS");
        dataSource.setDriverName("h2");
        dataSource.setConnectionUrl("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        dataSource.setPassword("sa");
        dataSource.setBackgroundValidation(false);
        setup.add(new DataSourceTemplate<>("h2", H2, dataSource, driver));

        // XA DS
        xaDataSource = beanFactory.xaDataSource().as();
        xaDataSource.setName("H2XADS");
        xaDataSource.setPoolName("H2XADS_Pool");
        xaDataSource.setJndiName("java:/H2XADS");
        xaDataSource.setDriverName("h2");
        xaDataSource.setProperties(properties("URL", "jdbc:h2:mem:test"));
        xaDataSource.setUsername("sa");
        xaDataSource.setPassword("sa");
        xaDataSource.setBackgroundValidation(false);
        setup.add(new DataSourceTemplate<>("h2-xa", H2, xaDataSource, driver));


        // ------------------------------------------------------ PostgreSQL
        // Driver
        driver = beanFactory.jdbcDriver().as();
        driver.setName("postgresql");
        driver.setDriverModuleName("org.postgresql");
        driver.setDriverClass("org.postgresql.Driver");
        driver.setXaDataSourceClass("org.postgresql.xa.PGXADataSource");

        // DS
        dataSource = beanFactory.dataSource().as();
        dataSource.setName("PostgresDS");
        dataSource.setPoolName("PostgresDS_Pool");
        dataSource.setJndiName("java:/PostgresDS");
        dataSource.setDriverName("postgresql");
        dataSource.setConnectionUrl("jdbc:postgresql://localhost:5432/postgresdb");
        dataSource.setUsername("admin");
        dataSource.setPassword("admin");
        dataSource.setBackgroundValidation(true);
        dataSource.setValidConnectionChecker("org.jboss.jca.adapters.jdbc.extensions.postgres.PostgreSQLValidConnectionChecker");
        dataSource.setExceptionSorter("org.jboss.jca.adapters.jdbc.extensions.postgres.PostgreSQLExceptionSorter");
        setup.add(new DataSourceTemplate<>("postgresql", POSTGRE_SQL, dataSource, driver));

        // XA DS
        xaDataSource = beanFactory.xaDataSource().as();
        xaDataSource.setName("PostgresXADS");
        xaDataSource.setPoolName("PostgresXADS_Pool");
        xaDataSource.setJndiName("java:/PostgresXADS");
        xaDataSource.setDriverName("postgresql");
        xaDataSource.setProperties(properties("ServerName", "servername",
                "PortNumber", "5432",
                "DatabaseName", "postgresdb"));
        xaDataSource.setUsername("admin");
        xaDataSource.setPassword("admin");
        xaDataSource.setBackgroundValidation(true);
        xaDataSource.setValidConnectionChecker("org.jboss.jca.adapters.jdbc.extensions.postgres.PostgreSQLValidConnectionChecker");
        xaDataSource.setExceptionSorter("org.jboss.jca.adapters.jdbc.extensions.postgres.PostgreSQLExceptionSorter");
        setup.add(new DataSourceTemplate<>("postgresql-xa", POSTGRE_SQL, xaDataSource, driver));


        // ------------------------------------------------------ MySQL
        // Driver
        driver = beanFactory.jdbcDriver().as();
        driver.setName("mysql");
        driver.setDriverModuleName("com.mysql");
        driver.setDriverClass("com.mysql.jdbc.Driver");
        driver.setXaDataSourceClass("com.mysql.jdbc.jdbc2.optional.MysqlXADataSource");

        // DS
        dataSource = beanFactory.dataSource().as();
        dataSource.setName("MySqlDS");
        dataSource.setPoolName("MySqlDS_Pool");
        dataSource.setJndiName("java:/MySqlDS");
        dataSource.setDriverName("mysql");
        dataSource.setConnectionUrl("jdbc:mysql://localhost:3306/mysqldb");
        dataSource.setUsername("admin");
        dataSource.setPassword("admin");
        dataSource.setBackgroundValidation(true);
        dataSource.setValidConnectionChecker("org.jboss.jca.adapters.jdbc.extensions.mysql.MySQLValidConnectionChecker");
        dataSource.setExceptionSorter("org.jboss.jca.adapters.jdbc.extensions.mysql.MySQLExceptionSorter");
        setup.add(new DataSourceTemplate<>("mysql", MYSQL, dataSource, driver));

        // XA DS
        xaDataSource = beanFactory.xaDataSource().as();
        xaDataSource.setName("MysqlXADS");
        xaDataSource.setPoolName("MysqlXADS_Pool");
        xaDataSource.setJndiName("java:/MysqlXADS");
        xaDataSource.setDriverName("mysql");
        xaDataSource.setProperties(properties("ServerName", "localhost",
                "DatabaseName", "mysqldb"));
        xaDataSource.setUsername("admin");
        xaDataSource.setPassword("admin");
        xaDataSource.setBackgroundValidation(true);
        xaDataSource.setValidConnectionChecker("org.jboss.jca.adapters.jdbc.extensions.mysql.MySQLValidConnectionChecker");
        xaDataSource.setExceptionSorter("org.jboss.jca.adapters.jdbc.extensions.mysql.MySQLExceptionSorter");
        setup.add(new DataSourceTemplate<>("mysql-xa", MYSQL, xaDataSource, driver));

        // ------------------------------------------------------ MariaDB
        // Driver
        driver = beanFactory.jdbcDriver().as();
        driver.setName("mariadb");
        driver.setDriverModuleName("org.mariadb");
        driver.setDriverClass("org.mariadb.jdbc.Driver");
        driver.setXaDataSourceClass("org.mariadb.jdbc.MariaDbDataSource");

        // DS
        dataSource = beanFactory.dataSource().as();
        dataSource.setName("MariaDB");
        dataSource.setPoolName("MariaDB_Pool");
        dataSource.setJndiName("java:/MariaDB");
        dataSource.setDriverName("mariadb");
        dataSource.setConnectionUrl("jdbc:mariadb://localhost:3306/db");
        dataSource.setUsername("admin");
        dataSource.setPassword("admin");
        dataSource.setValidateOnMatch(true);
        dataSource.setBackgroundValidation(false);
        dataSource.setValidConnectionChecker("org.jboss.jca.adapters.jdbc.extensions.mysql.MySQLValidConnectionChecker");
        dataSource.setExceptionSorter("org.jboss.jca.adapters.jdbc.extensions.mysql.MySQLExceptionSorter");
        setup.add(new DataSourceTemplate<>("mariadb", MARIA_DB, dataSource, driver));

        // XA DS
        xaDataSource = beanFactory.xaDataSource().as();
        xaDataSource.setName("MariaDBXADS");
        xaDataSource.setPoolName("MariaDBXADS_Pool");
        xaDataSource.setJndiName("java:/MariaDBXADS");
        xaDataSource.setDriverName("mariadb");
        xaDataSource.setProperties(properties("ServerName", "localhost",
                "DatabaseName", "db"));
        xaDataSource.setUsername("admin");
        xaDataSource.setPassword("admin");
        xaDataSource.setValidateOnMatch(true);
        xaDataSource.setBackgroundValidation(false);
        xaDataSource.setValidConnectionChecker("org.jboss.jca.adapters.jdbc.extensions.mysql.MySQLValidConnectionChecker");
        xaDataSource.setExceptionSorter("org.jboss.jca.adapters.jdbc.extensions.mysql.MySQLExceptionSorter");
        setup.add(new DataSourceTemplate<>("mariadb-xa", MARIA_DB, xaDataSource, driver));


        // ------------------------------------------------------ Oracle
        // Driver
        driver = beanFactory.jdbcDriver().as();
        driver.setName("oracle");
        driver.setDriverModuleName("com.oracle");
        driver.setDriverClass("oracle.jdbc.driver.OracleDriver");
        driver.setXaDataSourceClass("oracle.jdbc.xa.client.OracleXADataSource");

        // DS
        dataSource = beanFactory.dataSource().as();
        dataSource.setName("OracleDS");
        dataSource.setPoolName("OracleDS_Pool");
        dataSource.setJndiName("java:/OracleDS");
        dataSource.setDriverName("oracle");
        dataSource.setConnectionUrl("jdbc:oracle:thin:@localhost:1521:orcalesid");
        dataSource.setUsername("admin");
        dataSource.setPassword("admin");
        dataSource.setBackgroundValidation(true);
        dataSource.setValidConnectionChecker("org.jboss.jca.adapters.jdbc.extensions.oracle.OracleValidConnectionChecker");
        dataSource.setExceptionSorter("org.jboss.jca.adapters.jdbc.extensions.oracle.OracleExceptionSorter");
        dataSource.setStaleConnectionChecker("org.jboss.jca.adapters.jdbc.extensions.oracle.OracleStaleConnectionChecker");
        setup.add(new DataSourceTemplate<>("oracle", ORACLE, dataSource, driver));

        // XA DS
        xaDataSource = beanFactory.xaDataSource().as();
        xaDataSource.setName("XAOracleDS");
        xaDataSource.setPoolName("XAOracleDS_Pool");
        xaDataSource.setJndiName("java:/XAOracleDS");
        xaDataSource.setDriverName("oracle");
        xaDataSource.setProperties(properties("URL", "jdbc:oracle:oci8:@tc"));
        xaDataSource.setUsername("admin");
        xaDataSource.setPassword("admin");
        xaDataSource.setBackgroundValidation(true);
        xaDataSource.setValidConnectionChecker("org.jboss.jca.adapters.jdbc.extensions.oracle.OracleValidConnectionChecker");
        xaDataSource.setExceptionSorter("org.jboss.jca.adapters.jdbc.extensions.oracle.OracleExceptionSorter");
        xaDataSource.setStaleConnectionChecker("org.jboss.jca.adapters.jdbc.extensions.oracle.OracleStaleConnectionChecker");
        xaDataSource.setNoTxSeparatePool(true);
        xaDataSource.setSameRmOverride(false);
        setup.add(new DataSourceTemplate<>("oracle-xa", ORACLE, xaDataSource, driver));


        // ------------------------------------------------------ Microsoft SQL Server
        // Driver
        driver = beanFactory.jdbcDriver().as();
        driver.setName("sqlserver");
        driver.setDriverModuleName("com.microsoft");
        driver.setDriverClass("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        driver.setXaDataSourceClass("com.microsoft.sqlserver.jdbc.SQLServerXADataSource");

        // DS
        dataSource = beanFactory.dataSource().as();
        dataSource.setName("MSSQLDS");
        dataSource.setPoolName("MSSQLDS_Pool");
        dataSource.setJndiName("java:/MSSQLDS");
        dataSource.setDriverName("sqlserver");
        dataSource.setConnectionUrl("jdbc:sqlserver://localhost:1433;DatabaseName=MyDatabase");
        dataSource.setUsername("admin");
        dataSource.setPassword("admin");
        dataSource.setBackgroundValidation(true);
        dataSource.setValidConnectionChecker("org.jboss.jca.adapters.jdbc.extensions.mssql.MSSQLValidConnectionChecker");
        setup.add(new DataSourceTemplate<>("sqlserver", SQL_SERVER, dataSource, driver));

        // XA DS
        xaDataSource = beanFactory.xaDataSource().as();
        xaDataSource.setName("MSSQLXADS");
        xaDataSource.setPoolName("MSSQLXADS_Pool");
        xaDataSource.setJndiName("java:/MSSQLXADS");
        xaDataSource.setDriverName("sqlserver");
        xaDataSource.setProperties(properties("ServerName", "localhost",
                "DatabaseName", "mssqldb",
                "SelectMethod", "cursor"));
        xaDataSource.setUsername("admin");
        xaDataSource.setPassword("admin");
        xaDataSource.setBackgroundValidation(true);
        xaDataSource.setValidConnectionChecker("org.jboss.jca.adapters.jdbc.extensions.mssql.MSSQLValidConnectionChecker");
        xaDataSource.setSameRmOverride(false);
        setup.add(new DataSourceTemplate<>("sqlserver-xa", SQL_SERVER, xaDataSource, driver));


        // ------------------------------------------------------ DB2
        // Driver
        driver = beanFactory.jdbcDriver().as();
        driver.setName("ibmdb2");
        driver.setDriverModuleName("com.ibm");
        driver.setDriverClass("COM.ibm.db2.jdbc.app.DB2Driver");
        driver.setXaDataSourceClass("COM.ibm.db2.jdbc.DB2XADataSource");

        // DS
        dataSource = beanFactory.dataSource().as();
        dataSource.setName("DB2DS");
        dataSource.setPoolName("DB2DS_Pool");
        dataSource.setJndiName("java:/DB2DS");
        dataSource.setDriverName("ibmdb2");
        dataSource.setConnectionUrl("jdbc:db2:yourdatabase");
        dataSource.setUsername("admin");
        dataSource.setPassword("admin");
        dataSource.setBackgroundValidation(true);
        dataSource.setValidConnectionChecker("org.jboss.jca.adapters.jdbc.extensions.db2.DB2ValidConnectionChecker");
        dataSource.setExceptionSorter("org.jboss.jca.adapters.jdbc.extensions.db2.DB2ExceptionSorter");
        dataSource.setStaleConnectionChecker("org.jboss.jca.adapters.jdbc.extensions.db2.DB2StaleConnectionChecker");
        dataSource.setMinPoolSize(0);
        dataSource.setMaxPoolSize(50);
        setup.add(new DataSourceTemplate<>("db2", DB2, dataSource, driver));

        // XA DS
        xaDataSource = beanFactory.xaDataSource().as();
        xaDataSource.setName("DB2XADS");
        xaDataSource.setPoolName("DB2XADS_Pool");
        xaDataSource.setJndiName("java:/DB2XADS");
        xaDataSource.setDriverName("ibmdb2");
        xaDataSource.setProperties(properties("ServerName", "localhost",
                "DatabaseName", "ibmdb2db",
                "PortNumber", "446"));
        xaDataSource.setUsername("admin");
        xaDataSource.setPassword("admin");
        xaDataSource.setBackgroundValidation(true);
        xaDataSource.setValidConnectionChecker("org.jboss.jca.adapters.jdbc.extensions.db2.DB2ValidConnectionChecker");
        xaDataSource.setExceptionSorter("org.jboss.jca.adapters.jdbc.extensions.db2.DB2ExceptionSorter");
        xaDataSource.setStaleConnectionChecker("org.jboss.jca.adapters.jdbc.extensions.db2.DB2StaleConnectionChecker");
        xaDataSource.setRecoveryPluginClassName("org.jboss.jca.core.recovery.ConfigurableRecoveryPlugin");
        // TODO Add missing recovery plugin properties
        xaDataSource.setSameRmOverride(false);
        setup.add(new DataSourceTemplate<>("db2-xa", DB2, xaDataSource, driver));


        // ------------------------------------------------------ Sybase
        // Driver
        driver = beanFactory.jdbcDriver().as();
        driver.setName("sybase");
        driver.setDriverModuleName("com.sybase");
        driver.setDriverClass("com.sybase.jdbc.SybDriver");
        driver.setXaDataSourceClass("com.sybase.jdbc4.jdbc.SybXADataSource");

        // DS
        dataSource = beanFactory.dataSource().as();
        dataSource.setName("SybaseDB");
        dataSource.setPoolName("SybaseDB_Pool");
        dataSource.setJndiName("java:/SybaseDB");
        dataSource.setDriverName("sybase");
        dataSource.setConnectionUrl("jdbc:sybase:Tds:localhost:5000/mydatabase?JCONNECT_VERSION=6");
        dataSource.setUsername("admin");
        dataSource.setPassword("admin");
        dataSource.setBackgroundValidation(true);
        dataSource.setValidConnectionChecker("org.jboss.jca.adapters.jdbc.extensions.sybase.SybaseValidConnectionChecker");
        dataSource.setExceptionSorter("org.jboss.jca.adapters.jdbc.extensions.sybase.SybaseExceptionSorter");
        setup.add(new DataSourceTemplate<>("sybase", SYBASE, dataSource, driver));

        // XA DS
        xaDataSource = beanFactory.xaDataSource().as();
        xaDataSource.setName("SybaseXADS");
        xaDataSource.setPoolName("SybaseXADS_Pool");
        xaDataSource.setJndiName("java:/SybaseXADS");
        xaDataSource.setDriverName("sybase");
        xaDataSource.setProperties(properties("NetworkProtocol", "Tds",
                "ServerName", "localhost",
                "PortNumber", "4100",
                "DatabaseName", "mydatabase"));
        xaDataSource.setUsername("admin");
        xaDataSource.setPassword("admin");
        xaDataSource.setBackgroundValidation(true);
        xaDataSource.setValidConnectionChecker("org.jboss.jca.adapters.jdbc.extensions.sybase.SybaseValidConnectionChecker");
        xaDataSource.setExceptionSorter("org.jboss.jca.adapters.jdbc.extensions.sybase.SybaseExceptionSorter");
        setup.add(new DataSourceTemplate<>("sybase-xa", SYBASE, xaDataSource, driver));

        pool = Collections.unmodifiableList(setup);
    }

    @Override
    public Iterator<DataSourceTemplate<? extends DataSource>> iterator() {
        return pool.iterator();
    }

    @SuppressWarnings("unchecked")
    public <T extends DataSource> DataSourceTemplate<T> getTemplate(String id) {
        for (DataSourceTemplate<? extends DataSource> template : this) {
            if (template.getId().equals(id)) {
                return (DataSourceTemplate<T>) template;
            }
        }
        return null;
    }

    private List<PropertyRecord> properties(String... properties) {
        List<PropertyRecord> records = new ArrayList<>();
        for (int i = 0; i < properties.length; i+=2) {
            PropertyRecord record = beanFactory.property().as();
            record.setKey(properties[i]);
            record.setValue(properties[i + 1]);
            records.add(record);
        }
        return records;
    }
}
