package net.punklan.glorfindeil;

import schemacrawler.schema.Catalog;
import schemacrawler.schema.Column;
import schemacrawler.schema.Schema;
import schemacrawler.schema.Table;
import schemacrawler.schemacrawler.*;
import schemacrawler.utility.SchemaCrawlerUtility;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Created by glorfindeil on 24.02.15.
 */
public class SchemaCrawlerHelper {
    //return connection by input
    public Connection getConnection(String driver, String connectionString, String user, String password) throws ClassNotFoundException, SQLException {
        Connection connection = null;


        //Class.forName("org.postgresql.Driver");
        Class.forName(driver);

        connection = DriverManager.getConnection(
                //"jdbc:postgresql://localhost:5432/activiti_164","glorfindeil", "564978"
                //jdbc:postgresql://localhost:5432/accounts
                connectionString, user, password);

        return connection;
    }

    //Takes the schema by connection
    public Catalog getCatalogForConnection(Connection conn) throws SchemaCrawlerException {
        final SchemaCrawlerOptions options = new SchemaCrawlerOptions();

        options.setSchemaInfoLevel(SchemaInfoLevel.standard());

        Catalog catalog = null;

        catalog = SchemaCrawlerUtility.getCatalog(conn, options);

        return catalog;

    }


}
