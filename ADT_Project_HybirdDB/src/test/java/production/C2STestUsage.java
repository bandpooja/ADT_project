package production;

import intermediate_rep.DecodedQuery;
import production.C2SMain;
import production.C2SProperties;
import schema_conversion.SchemaConvert;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

import database.Sql.InsertSchemaSql;
import database.Sql.SQLDatabaseConnection;

public class C2STestUsage {
    public static void main(String args[]) throws SQLException, ClassNotFoundException {
        // these need to be set before anything can occur.
        String propsFile = "c2s_props.properties";
        C2SProperties props = new C2SProperties(propsFile);

        // name of the blank database to either:
        //     - convert the schema too
        //     - execute the translated Cypher on
        String dbName = "testa";

        String thingToDo = "translate";     // alternative is "convert"
        String executeOn = "toFile";        // alternative is "postgres"

        switch (thingToDo) {
            case "convert":
                // the properties file needs to be edited first with all the correct values.
                // a dump from Neo4j is also needed.
                boolean successConvert = SchemaConvert.translate(props);
                if (successConvert) InsertSchemaSql.executeSchemaChange(dbName, props);
                break;
            case "translate":
                // location of the script to allow results from Postgres to be piped back to
                // this class. View and adapt the scripts if necessary.
                String scriptLoc = "pgdbPlay.bat";

                // Cypher query to translate and then execute.
                String cypher = "MATCH (a)-[e]->(b:Process) WHERE e.state > 5 WITH b MATCH (c) WHERE (exists(c.pid) AND c.pid < b.pid) WITH c MATCH (c)<--(d:Local) WHERE any(n in d.name WHERE n = '4') RETURN count(d) AS cool_thing;"
                		;

                try {
                    // obtain the intermediate representation
                    DecodedQuery dQ = C2SMain.getDQ(cypher, props);

                    // convert the intermediate representation to SQL
                    String sql = C2SMain.getTranslation(cypher, dQ, props);
                    System.out.println(sql);

                    switch (executeOn) {
                        case "postgres":
                            // execute directly on Postgres if desired (the script will pipe
                            // the results back into this tool).
                            String postgresOutput = C2SMain.runPostgres(sql, dbName, scriptLoc);
                            System.out.println(postgresOutput);
                            break;
                        case "toFile":
                            // alternatively, print the results to a local file.
                            File f = new File(props.getSqlRes());
                            //SQLDatabaseConnection.select(sql);
                            
                            break;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            default:
                System.err.println("Not a valid option...");
                System.exit(1);
        }
    }
}