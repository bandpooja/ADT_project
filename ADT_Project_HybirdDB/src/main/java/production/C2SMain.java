/*
 * Copyright (c) 2017.
 *
 * Oliver Crawford <o.crawford@hotmail.co.uk>
 * Lucian Carata <lc525@cam.ac.uk>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package production;

import exceptions.ConversionSQLException;
import exceptions.DQInvalidException;
import intermediate_rep.DecodedQuery;
import org.apache.commons.io.FileUtils;
import org.neo4j.driver.exceptions.ClientException;
import org.neo4j.driver.exceptions.ServiceUnavailableException;

import database.Sql.InsertSchemaSql;
import database.Sql.SQLDatabaseConnection;
import query_translation.sql.conversion_types.*;
import schema_conversion.SchemaConvert;

import java.io.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Main class for starting the application. View README.md for usage.
 */
public class C2SMain {
    // for optimisations based on the return clause of Cypher
    public static final Map<String, String> labelProps = new HashMap<>();
    // for use in deleting relationships attached to nodes
    public static final ArrayList<String> allRelTypes = new ArrayList<>();

    private static final String OS = System.getProperty("os.name").toLowerCase();
    public static int numResultsNeo4j;
    public static int numResultsPostgres;

    // static variable set by the translation unit if the ID needs to be returned from the Postgres database
    // by default it is set to false as this is the same logic as Neo4j
    public static boolean needToPrintID = false;
    // variable set at the command line to turn on/off printing to a file the results of a read query.
    static boolean printBool = false;
    // cache for previously successful queries (saves time and computation doing repetitive work).
    private static Map<String, String> cache = new HashMap<>();

    /**
     * {@literal <}-schema|-translate|-s|-t{@literal >}
     * {@literal <}propertiesFile{@literal >} {@literal <}databaseName{@literal >} {@literal <}-dp|-dn|-r{@literal >}
     * View README for additional guidance.
     *
     * @param args Arguments for the application.
     * @throws SQLException 
     * @throws ClassNotFoundException 
     */
    public static void main(String args[]) throws SQLException, ClassNotFoundException {
        if (args.length == 0) printError(0);

        if ((args[0].equals("-s") || args[0].equals("-schema")) && args.length != 3) printError(1);
        else if ((args[0].equals("-t") || args[0].equals("-translate")) && args.length != 4) printError(2);

        // obtain properties for the program from the properties file.
        C2SProperties props = new C2SProperties(args[1]);

        // find out the database being used, based on the argument from the command line.
        String dbName = args[2];
        System.out.println("DATABASE RUNNING : " + dbName);

        switch (args[0]) {
            case "-schema":
            case "-s":
                // perform the schema translation
                convertGraphSchema(dbName, props);
                break;
            case "-translate":
            case "-t":
                getLabelMapping(props);
                warmUpResetSSL(props);

                // create file objects to store results of the file.
                File f_cypher = new File(props.getNeo4jRes());
                File f_sql = new File(props.getSqlRes());

                // find out the value of the additional command line flag,
                // and choose the appropriate method to run as a result.
                switch (args[3]) {
                    case "-dp":
                        printBool = true;
                        C2SInteractive.run_debug(f_cypher, f_sql, dbName, props);
                        break;
                    case "-dn":
                        printBool = false;
                        C2SInteractive.run_debug(f_cypher, f_sql, dbName, props);
                        break;
                    case "-r":
                        C2SInteractive.run(dbName, props);
                        break;
                    default:
                        printError(3);
                        break;
                }
                break;
            default:
                printError(4);
                break;
        }
    }

    /**
     * This method performs two important tasks for the translator tool:
     * - Firstly, the translator tool checks to see if it can optimise any of the queries by storing a mapping
     * between unique keys, and the label of the node that tke key will only appear under. These mappings are
     * stored in the labelProps object.
     * <p>
     * - Secondly, it reads in all the possible types of relationships
     * (used for example when deleting nodes from the relational schema).
     *
     * @param props C2SProperties object.
     */
    static void getLabelMapping(C2SProperties props) {
        // open file and read in property keys, removing duplicates as they are of no use.
        FileInputStream fis;
        try {
            fis = new FileInputStream(props.getWspace() + "/meta_labelProps.txt");
            BufferedReader br = new BufferedReader(new InputStreamReader(fis));
            String line;
            String currentLabelType = null;

            // ArrayList to keep track of duplicates.
            ArrayList<String> dupKeys = new ArrayList<>();

            while ((line = br.readLine()) != null) {
                if (line.startsWith("*")) {
                    // view meta_labelProps.txt/README for more information.
                    // Lines starting with a '*' defines the label that the following properties
                    // in the file belong to.
                    currentLabelType = line.substring(1, line.length() - 1);
                } else {
                    if (labelProps.containsKey(line)) {
                        dupKeys.add(line);
                    } else {
                        labelProps.put(line, currentLabelType);
                    }
                }
            }

            // remove the duplicate keys.
            for (String s : dupKeys) labelProps.remove(s);

            br.close();
            fis.close();

            // Section for reading in all the different types of relationships contained within the dataset.
            fis = new FileInputStream(props.getWspace() + "/meta_rels.txt");
            br = new BufferedReader(new InputStreamReader(fis));
            while ((line = br.readLine()) != null) allRelTypes.add(line);
            br.close();
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * warm up the Neo4j caches if the server has just been turned on.
     * https://neo4j.com/developer/kb/warm-the-cache-to-improve-performance-from-cold-start/
     * also, if there is an SSL issue when changing databases, attempt to fix it by running
     * some fix up code.
     *
     * @param props C2SProperties object.
     */
    static void warmUpResetSSL(C2SProperties props) {
        try {
            Neo4jDriver.warmUp(props);
        } catch (ClientException ce) {
            if (ce.getMessage().contains("SSLEngine problem")) {
                System.err.println("SSL Engine issue");
            }
        } catch (ServiceUnavailableException unavail) {
            System.err.println("*** Make sure the Neo4j database is up and running correctly. ***");
            unavail.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * If there is an error in the main method of C2SMain.java, then one of these messages will be printed
     * out to the terminal.
     *
     * @param errorCode Error code matching the error that has occurred.
     */
    private static void printError(int errorCode) {
        switch (errorCode) {
            case 0:
                System.err.println("*** Error 0: Must pass either 3 or 4 arguments to the program. ***");
                break;
            case 1:
                System.err.println("*** Error 1: Schema translator requires 3 arguments. ***");
                break;
            case 2:
                System.err.println("*** Error 2: The tool requires 4 arguments to function correctly. ***");
                break;
            case 3:
                System.err.println("*** Error 3: Fourth command line flag is invalid. Options are below. ***");
                break;
            case 4:
                System.err.println("*** Error 4: First command line flag is invalid. Options are below. ***");
                break;
            default:
                System.err.println("*** Error running the application. ***");
                break;
        }

        System.err.println("*** Please see README for further help. ***");
        System.err.println("<-schema|-translate|-s|-t> <propertiesFile> <databaseName> <-dp|-dn|-r>");
        System.exit(1);
    }

    /**
     * Converting a 'dump' from an existing Neo4j graph into a relational schema, before executing
     * on a relational backend.
     *
     * @param databaseName The name of the database to be used for storing the relational representation.
     * @param props        C2SProperties object.
     * @throws SQLException 
     * @throws ClassNotFoundException 
     */
    private static void convertGraphSchema(String databaseName, C2SProperties props) throws SQLException, ClassNotFoundException {
        System.out.println("\n***CONVERTING THE SCHEMA***\n");
        boolean successfulConversion = SchemaConvert.translate(props);
        if (successfulConversion) {
            System.out.println("\n***INSERTING THE SCHEMA TO THE DATABASE***\n");
            InsertSchemaSql.executeSchemaChange(databaseName, props);
            // KeyValueTest.executeSchemaChange();
        } else {
            // error in the schema conversion - exits the application.
            System.err.println("Conversion of the graph schema to a relational form has failed.");
            System.exit(1);
        }
    }

    /**
     * Run the SQL on native Postgres (using psql via a script). The results are then piped back into the console
     * and outputted using the System.out.println command.
     *
     * @param sql    SQL to execute through psql.
     * @param dbName The name of the database for psql to connect to.
     * @return Results from the psql script.
     * @throws IOException Issue with running the script.
     */
    private static String runPostgres(String sql, String dbName) throws IOException {
        String scriptLocation;

        // deduce the correct OS, and from this, the correct .bat/shell file to execute.
        if (isWindows())
            scriptLocation = System.getProperty("user.dir") + "/pgdbPlay.bat";
        else
            scriptLocation = System.getProperty("user.dir") + "/auto_run_pg.sh";

        System.out.println(sql);
        return runPostgres(sql, dbName, scriptLocation);
    }

    /**
     * Executes the query sql on the relational database named dbName. The arguments are themselves passed as
     * arguments currently to a Windows batch file, which performs the query on the Postgres database. The
     * results are then piped back into this tool and outputted.
     *
     * @param sql            Query to be executed on the database.
     * @param dbName         Name of the relational database for the query to be executed on.
     * @param scriptLocation File location of the script to run.
     * @throws IOException Some issue with the Buffered Reader object.
     */
    public static String runPostgres(String sql, String dbName, String scriptLocation) throws IOException {
        // run the script with two arguments - see the code in the scripts for more information.
        ProcessBuilder b = new ProcessBuilder(scriptLocation, dbName, sql);

        b.redirectErrorStream(true);
        Process process = b.start();

        // output the results from the process back into this console.
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        StringBuilder builder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            builder.append(line);
            builder.append(System.getProperty("line.separator"));
        }

        return builder.toString();
    }


    /**
     * Method for translating Cypher to SQL.
     *
     * @param cypherInput The original Cypher input that is to be translated.
     * @param f_cypher    The file object for storing the results from Neo4j (if desired).
     * @param f_pg        The file object for storing the results from Postgres (if desired).
     * @param dbName      The name of the database to execute the newly created SQL on.
     * @param execNeo4j   In the case that the user is just running the tool in its normal operation, there is no need
     *                    to deal with any of the files, or printing of the results to these files. Instead, the
     *                    newly generated SQL is executed via a .bat script on Postgres, with the results piped
     *                    back to this tool.
     * @param props       C2SProperties object.
     * @throws ClassNotFoundException 
     */
    static void translateCypherToSQL(String cypherInput, File f_cypher, File f_pg, String dbName, boolean execNeo4j,
                                     C2SProperties props) throws ConversionSQLException, ClassNotFoundException {
        String sql;

        // either calculate the SQL or retrieve from a cache of valid translations.
        if (cache.containsKey(cypherInput)) sql = cache.get(cypherInput);
        else {
            // generate the DecodedQuery object first, then use it to translate to SQL.
            DecodedQuery dQ = getDQ(cypherInput, props);
            sql = getTranslation(cypherInput, dQ, props);
        }

        if (!execNeo4j) {
            try {
                System.out.println(runPostgres(sql, dbName));
                // cache.put(cypherInput, sql);
            } catch (IOException ioe) {
                ioe.printStackTrace();
            } finally {
                resetExecTimes();
            }
            return;
        }

        boolean sqlExecSuccess;
        if (sql != null && !sql.isEmpty()) {
            sqlExecSuccess = executeSQL(sql, f_pg, (printBool || cypherInput.toLowerCase().contains("count")),
                    dbName, props);
        } else throw new ConversionSQLException("Conversion of SQL failed on input: " + cypherInput);

        // All the Cypher queries other than the extension
        if (!cypherInput.toLowerCase().contains("iterate"))
            Neo4jDriver.run(cypherInput, f_cypher, printBool, props);

        // validate the results
        boolean fileSame = false;

        if (sqlExecSuccess) {
            if ((numResultsNeo4j != numResultsPostgres) && !cypherInput.toLowerCase().contains("iterate")
                    && !cypherInput.toLowerCase().startsWith("create")
                    && !cypherInput.toLowerCase().contains("detach delete")
                    && !cypherInput.toLowerCase().contains("foreach")) {
                translationFail(cypherInput, sql, f_cypher, f_pg);
            } else if (cypherInput.toLowerCase().contains("count") && !cypherInput.toLowerCase().contains("with")) {
                try {
                    fileSame = FileUtils.contentEquals(f_cypher, f_pg);
                    if (!fileSame) {
                        translationFail(cypherInput, sql, f_cypher, f_pg);
                    }
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }

            if ((numResultsNeo4j == numResultsPostgres) || fileSame) cache.put(cypherInput, sql);

            // print the performance of Cypher and SQL on Neo4J and Postgres respectively.
            try {
                printSummary(cypherInput, sql, f_cypher, f_pg);
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }

        resetExecTimes();
    }

    /**
     * Execute the SQL command on the database.
     * If query is a concatenation of multiple queries, then perform then
     * one by one in order that was passed to the method.
     * Type of method call depends on whether or not query begins with CREATE
     * or not.
     *
     * @param sql         SQL to execute.
     * @param pg_results  File to store the results.
     * @param printOutput Write the results to a file for viewing.
     * @param dbName      Name of the database the SQL will be executed on.
     * @param props       C2SProperties object.
     * @return True if the execution on Postgres was successful, false otherwise.
     * @throws ClassNotFoundException 
     */
    private static boolean executeSQL(String sql, File pg_results, boolean printOutput, String dbName,
                                      C2SProperties props) throws ClassNotFoundException {
        try {
            String indivSQL[] = sql.split(";");
            for (String q : indivSQL) {
                if (q.trim().startsWith("CREATE")) {
                	SQLDatabaseConnection.executeCreateView(q);
                } else if (q.trim().startsWith("INSERT")) {
                	SQLDatabaseConnection.insertOrDelete(q + ";");
                } else if (q.trim().startsWith("DELETE")) {
                	SQLDatabaseConnection.insertOrDelete(q + ";");
                } else
                	SQLDatabaseConnection.select(q + ";");
            }
        } catch (SQLException e) {
            System.out.println("FAILED IN executeSQL -- " + sql);
            e.printStackTrace();
            return false;
        }
        return true;
    }
    
    

    /**
     * Print summary of the translation.
     *
     * @param line     Cypher query.
     * @param sql      SQL equivalent.
     * @param f_cypher File containing Neo4J output.
     * @param f_pg     File containing Postgres output.
     * @throws IOException Error comparing the files f_cypher and f_pg.
     */
    private static void printSummary(String line, String sql, File f_cypher, File f_pg) throws IOException {
        System.out.println("**********\nCypher Input : " + line);
        System.out.println("SQL Output: " + sql + "\nExact Result: " +
                FileUtils.contentEquals(f_cypher, f_pg) + "\nNumber of records from Neo4j: " +
                numResultsNeo4j + "\nNumber of results from PostG: " + numResultsPostgres);
        System.out.println("Time on Neo4j: \t\t" + (Neo4jDriver.lastExecTime / 1000000.0) +
                " ms.\nTime on Postgres: \t" + ((SQLDatabaseConnection.lastExecTimeCreate +
                		SQLDatabaseConnection.lastExecTimeInsert)
                / 1000000.0) +
                " ms.");
        System.out.println("**********\n");
    }

    /**
     * If the translation failed, alert the user.
     *
     * @param line     Original Cypher input.
     * @param sql      Converted SQL.
     * @param f_cypher Neo4j results file object.
     * @param f_pg     Postgres results file object.
     */
    private static void translationFail(String line, String sql, File f_cypher, File f_pg) {
        System.err.println("\n**********Statements do not appear to " +
                "be logically correct - please check**********\n"
                + line + "\n" + sql + "\n***********");
        try {
            printSummary(line, sql, f_cypher, f_pg);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        System.exit(1);
    }

    /**
     * Reset measured performance times.
     */
    private static void resetExecTimes() {
        Neo4jDriver.lastExecTime = 0;
       // SQLDatabaseConnection.lastExecTimeCreate = 0;
        SQLDatabaseConnection.lastExecTimeInsert = 0;
    }

    /**
     * Returns true if the user is running a Windows OS.
     *
     * @return True if OS is Windows; false otherwise.
     */
    private static boolean isWindows() {
        return (OS.contains("win"));
    }

    /**
     * Returns true if the user is running some Unix based OS.
     *
     * @return True if the OS is Unix based; false otherwise.
     */
    private static boolean isUnix() {
        return (OS.contains("nix") || OS.contains("nux") || OS.indexOf("aix") > 0);
    }

    /**
     * Generate DecodedQuery object from a Cypher input. Must also provide an initialised C2SProperties object.
     *
     * @param cypherInput Cypher input to translate.
     * @param props       C2SProperties object.
     * @return A new DecodedQuery object (may also contain SQL translation in the object in some instances).
     */
    public static DecodedQuery getDQ(String cypherInput, C2SProperties props) {
        try {
            if (cypherInput.toLowerCase().contains(" foreach ")) {
                ForEach_Cypher fe = new ForEach_Cypher();
                return fe.generateDQ(cypherInput, props);
            } else if (cypherInput.toLowerCase().contains(" with ")) {
                if (cypherInput.toLowerCase().split(" with ").length > 2) {
                    Multiple_With_Cypher mwc = new Multiple_With_Cypher();
                    return mwc.generateDQ(cypherInput, props);
                } else {
                    With_Cypher wc = new With_Cypher();
                    return wc.generateDQ(cypherInput, props);
                }
            } else if (cypherInput.toLowerCase().contains("shortestpath")) {
                SP_Cypher spc = new SP_Cypher();
                return spc.generateDQ(cypherInput, props);
            } else if (cypherInput.toLowerCase().contains("iterate")) {
                Iterate_Cypher ic = new Iterate_Cypher();
                return ic.generateDQ(cypherInput, props);
            } else {
                return AbstractConversion.genDQAndSQL(cypherInput, props);
            }
        } catch (DQInvalidException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    /**
     * Translate a DecodedQuery object to SQL (the original Cypher input and a valid C2SProperties object must
     * also be passed as arguments).
     *
     * @param cypherInput Original Cypher input (must be the same input as the one used to generate dQ).
     * @param dQ          A non-null DecodedQuery object.
     * @param props       C2SProperties object.
     * @return SQL equivalent of Cypher
     */
    public static String getTranslation(String cypherInput, DecodedQuery dQ, C2SProperties props) {
        try {
            if (cypherInput.toLowerCase().contains(" foreach ")) {
                ForEach_Cypher fe = new ForEach_Cypher();
                return fe.convertToSQL(dQ, props);
            } else if (cypherInput.toLowerCase().contains(" with ")) {
                if (cypherInput.toLowerCase().split(" with ").length > 2) {
                    Multiple_With_Cypher mwc = new Multiple_With_Cypher();
                    return mwc.convertToSQL(dQ, props);
                } else {
                    With_Cypher wc = new With_Cypher();
                    return wc.convertQuery(cypherInput, props);
                }
            } else if (cypherInput.toLowerCase().contains("shortestpath")) {
                SP_Cypher spc = new SP_Cypher();
                return spc.convertToSQL(dQ, props);
            } else if (cypherInput.toLowerCase().contains("iterate")) {
                Iterate_Cypher ic = new Iterate_Cypher();
                return ic.convertToSQL(dQ, props);
            } else {
                return dQ.getSqlEquiv();
            }
        } catch (DQInvalidException ex) {
            ex.printStackTrace();
            return null;
        } catch (NullPointerException nulle) {
            System.err.println("No valid DecodedQuery object presented.");
            return null;
        }
    }
}