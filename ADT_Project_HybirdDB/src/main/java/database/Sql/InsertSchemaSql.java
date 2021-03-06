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

package database.Sql;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import database.SchemaConstants;
import production.C2SProperties;
import schema_conversion.SchemaConvert;

import java.io.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Store the results of the schema conversion from Neo4j to Postgres.
 */
public class InsertSchemaSql {
    private static List<String> fieldsForMetaFile = new ArrayList<>();

    /**
     * Executing the various schema parts one by one to Sql.
     *
     * @param database Name of the Sql database to store the new schema on.
     * @param props    C2SProperties object (should already be initialised).
     * @throws SQLException 
     * @throws ClassNotFoundException 
     */
    public static void executeSchemaChange(String database, C2SProperties props) throws SQLException, ClassNotFoundException {
    	Connection connection=SQLDatabaseConnection.createConnection();

        String createAdditionalNodeTables = insertEachLabel(props);
        String createAdditionalEdgesTables = insertEachRelType();

        String sqlInsertNodes = insertNodes();
        String sqlInsertEdges = insertEdges();

        try {
        	SQLDatabaseConnection.createInsert(createAdditionalNodeTables);
        	SQLDatabaseConnection.createInsert(createAdditionalEdgesTables);
        	SQLDatabaseConnection.createInsert(sqlInsertNodes);
        	SQLDatabaseConnection.createInsert(sqlInsertEdges);
        	SQLDatabaseConnection.createInsert(SqlConstants.ADJLIST_FROM);
        	SQLDatabaseConnection.createInsert(SqlConstants.ADJLIST_TO);
        	SQLDatabaseConnection.createInsert(SqlConstants.FOR_EACH_FUNC);
        	SQLDatabaseConnection.createInsert(SqlConstants.CYPHER_ITERATE);
        	SQLDatabaseConnection.createInsert(SqlConstants.UNIQUE_ARR_FUNC);
        	SQLDatabaseConnection.createInsert(SqlConstants.AUTO_SEQ_QUERY);

            addFieldsToMetaFile(props);

            // potentially risky insert, presumes no duplicated edges
            SQLDatabaseConnection.createInsert(SqlConstants.EDGES_INDEX);
        } catch (SQLException e) {
            if (e.getMessage().contains("not create unique index")) {
                System.err.println("Could not create a unique index in the edges relation " +
                        "as there is a duplicated relationship present.");
            } else e.printStackTrace();
        } finally {
        	SQLDatabaseConnection.closeConnection(connection);
        }
    }

    /**
     * All of the fields and relationships gathered during the schema conversion are stored in
     * meta files (to be used when outputting the results of the queries from both Postgres and
     * Neo4j).
     *
     * @param props C2SProperties object (should already be initialised).
     */
    private static void addFieldsToMetaFile(C2SProperties props) {
        FileOutputStream fos;
        try {
            fos = new FileOutputStream(props.getWspace() + "/meta_nodeProps.txt");

            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
            for (String s : fieldsForMetaFile) {
                bw.write(s);
                bw.newLine();
            }
            bw.close();
            fos.close();

            fos = new FileOutputStream(props.getWspace() + "/meta_rels.txt");

            bw = new BufferedWriter(new OutputStreamWriter(fos));
            for (String s : SchemaConvert.relTypes) {
                bw.write(s);
                bw.newLine();
            }
            bw.close();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * If there is a label which is applied to a node only ever on its own in isolation, then store this as a
     * relation to remove unnecessary NULLs which slow execution of SQL down.
     *
     * @param props C2SProperties object (should already be initialised).
     * @return SQL to execute.
     */
    private static String insertEachLabel(C2SProperties props) {
        StringBuilder sb = new StringBuilder();
        FileOutputStream fos_labelProps;
        FileOutputStream fos_labelNames;

        try {
            fos_labelProps = new FileOutputStream(props.getWspace() + "/meta_labelProps.txt");
            fos_labelNames = new FileOutputStream(props.getWspace() + "/meta_labelNames.txt");

            BufferedWriter bw_labelProps = new BufferedWriter(new OutputStreamWriter(fos_labelProps));
            BufferedWriter bw_labelNames = new BufferedWriter(new OutputStreamWriter(fos_labelNames));

            for (String label : SchemaConvert.labelMappings.keySet()) {
                String tableLabel = label.replace(", ", "_");
                if (!SchemaConstants.RESERVED_KW.contains(tableLabel)) {
                    sb.append("CREATE TABLE ").append(tableLabel).append("(");
                    sb.append(SchemaConvert.labelMappings.get(label));
                    sb.append("); ");

                    bw_labelProps.write("*" + tableLabel + "*");
                    bw_labelProps.newLine();

                    for (String y : SchemaConvert.labelMappings.get(label).replace(" TEXT[]", "")
                            .replace(" BIGINT", "")
                            .replace(" INT", "")
                            .replace(" TEXT", "")
                            .replace(" REAL", "")
                            .replace(" BOOLEAN", "")
                            .split(", ")) {
                        bw_labelProps.write(y);
                        bw_labelProps.newLine();
                    }

                    bw_labelNames.write(tableLabel);
                    bw_labelNames.newLine();
                }
            }

            bw_labelProps.close();
            bw_labelNames.close();
            fos_labelProps.close();
            fos_labelNames.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        return sb.toString();
    }

    /**
     * Create each relationship table in the relational schema. The tables in the relational schema
     * that correspond to each edge type of Neo4j will be prefixed with the two-letter pair of 'e$'.
     *
     * @return SQL string for insertion of each relationship type.
     */
    private static String insertEachRelType() {
        StringBuilder sb = new StringBuilder();

        for (String rel : SchemaConvert.relTypes) {
            // specific relationship types will be stored in the following
            // format, with the name of the relation being e${type of relationship}
            String relTableName = "e$" + rel;

            sb.append("CREATE TABLE ").append(relTableName).append("(");

            for (String x : SchemaConvert.edgesRelLabels) {
                sb.append(x).append(", ");
            }
            sb.setLength(sb.length() - 2);
            sb.append("); ");
        }
        return sb.toString();
    }

    /**
     * For each 'label' relation, insert the appropriate values.
     *
     * @param sb    Original SQL that will be appended to.
     * @param label Name of relation.
     * @param o     JSON object containing data to store.
     * @return New SQL statement with correct INSERT INTO statement.
     */
    private static StringBuilder insertDataForLabels(StringBuilder sb, String label, JsonObject o) {
        String tableLabel = label.replace(", ", "_");
        if (!SchemaConstants.RESERVED_KW.contains(tableLabel)) {
            sb.append("INSERT INTO ").append(tableLabel).append("(");

            for (String prop : SchemaConvert.labelMappings.get(label).split(", ")) {
                String modified_prop = prop;
                for (String data_type : SchemaConstants.DATATYPES) {
                    modified_prop = modified_prop.replace(data_type, "");
                }
                sb.append(modified_prop).append(", ");
            }

            sb.setLength(sb.length() - 2);
            sb.append(") VALUES(");

            for (String z : SchemaConvert.labelMappings.get(label).split(", ")) {
                sb.append(getInsertString(z, o));
            }

            sb.setLength(sb.length() - 2);
            sb.append("); ");
        }
        return sb;
    }

    /**
     * Insert all nodes into relational database.
     *
     * @return SQL to execute.
     */
    private static String insertNodes() {
        StringBuilder sb = new StringBuilder();

        sb.append("CREATE TABLE nodes(");
        for (String x : SchemaConvert.nodeRelLabels) {
            if (x.startsWith("mono_time")) x = "mono_time BIGINT";
            sb.append(x).append(", ");
        }
        sb.setLength(sb.length() - 2);
        sb.append("); ");

        sb = insertTableDataNodes(sb);
        return sb.toString();
    }

    /**
     * Data/properties of the nodes to store.
     *
     * @param sb Original SQL to append data to.
     * @return New SQL
     */
    private static StringBuilder insertTableDataNodes(StringBuilder sb) {
        StringBuilder sbLabels = new StringBuilder();
        sb.append("INSERT INTO nodes (");

        for (String y : SchemaConvert.nodeRelLabels) {
            if (y.startsWith("mono_time")) y = "mono_time BIGINT";
            sb.append(y.split(" ")[0]).append(", ");
            fieldsForMetaFile.add(y.split(" ")[0]);
        }
        sb.setLength(sb.length() - 2);
        sb.append(")");

        sb.append(" VALUES ");

        try {
            FileInputStream fis = new FileInputStream(SchemaConvert.nodesFile);
            BufferedReader br = new BufferedReader(new InputStreamReader(fis));
            String line;
            JsonParser parser = new JsonParser();

            while ((line = br.readLine()) != null) {
                JsonObject o = (JsonObject) parser.parse(line);
                String label = o.get("label").getAsString();

                sbLabels = insertDataForLabels(sbLabels, label, o);
                sb.append("(");
                for (String z : SchemaConvert.nodeRelLabels) {
                    sb.append(getInsertString(z, o));
                }
                sb.setLength(sb.length() - 2);
                sb.append("), ");
            }
            br.close();
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            File f = new File(SchemaConvert.nodesFile);
            f.delete();
        }

        sb.setLength(sb.length() - 2);
        sb.append("; ");
        sb.append(sbLabels.toString()).append(";");

        return sb;
    }

    /**
     * Insert relationships into SQL.
     *
     * @return SQL to execute.
     */
    private static String insertEdges() {
        StringBuilder sb = new StringBuilder();

        sb.append("CREATE TABLE edges(");
        for (String x : SchemaConvert.edgesRelLabels) {
            sb.append(x).append(", ");
        }
        sb.setLength(sb.length() - 2);
        sb.append("); ");

        sb = insertTableDataEdges(sb);
        return sb.toString();
    }


    /**
     * Insert properties of the relationships to SQL.
     *
     * @param sb Original SQL to append data to.
     * @return New SQL with data inserted into it.
     */
    private static StringBuilder insertTableDataEdges(StringBuilder sb) {
        sb.append("INSERT INTO edges (");
        StringBuilder sbTypes = new StringBuilder();

        StringBuilder columns = new StringBuilder();

        for (String y : SchemaConvert.edgesRelLabels) {
            columns.append(y.split(" ")[0]).append(", ");
        }
        columns = new StringBuilder(columns.substring(0, columns.length() - 2));
        columns.append(")");
        sb.append(columns);

        sb.append(" VALUES ");

        try {
            FileInputStream fis = new FileInputStream(SchemaConvert.edgesFile);
            BufferedReader br = new BufferedReader(new InputStreamReader(fis));
            String line;
            JsonParser parser = new JsonParser();

            while ((line = br.readLine()) != null) {
                JsonObject o = (JsonObject) parser.parse(line);
                sb.append("(");
                StringBuilder values = new StringBuilder();
                for (String z : SchemaConvert.edgesRelLabels) {
                    String v = getInsertString(z, o);
                    values.append(v);
                    sb.append(v);
                }
                values = new StringBuilder(values.substring(0, values.length() - 2));
                sbTypes = addType(sbTypes, columns.toString(), o, values.toString());
                sb.setLength(sb.length() - 2);
                sb.append("), ");
            }
            br.close();
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            File f = new File(SchemaConvert.edgesFile);
            f.delete();
        }

        sb.setLength(sb.length() - 2);
        sb.append(";").append(" ").append(sbTypes.toString());

        return sb;
    }

    /**
     * For each type of relationship in the original Neo4j graph, add a relation into Postgres.
     *
     * @param sbTypes The StringBuilder object containing all of the SQL to be executed.
     * @param columns The columns of the relation to be committed to the database.
     * @param o       The JSONObject containing the value of the key 'type'. The value will be the name of the
     *                relation (for example, if the value is 'OWNS', the relation stored will be 'e$OWNS'.
     * @param values  String of the values to store into the database.
     * @return Updated StringBuilder object with additional SQL to execute on the database.
     */
    private static StringBuilder addType(StringBuilder sbTypes, String columns, JsonObject o, String values) {
        sbTypes.append("INSERT INTO ");
        sbTypes.append("e$").append(o.get("type").getAsString()).append("(").append(columns);
        sbTypes.append(" VALUES (");
        sbTypes.append(values);
        sbTypes.append(");");
        return sbTypes;
    }

    /**
     * Correctly format the string to insert into the database.
     *
     * @param inputField The key for which the value is being retrieved for.
     * @param obj        The JSONObject from a node/relationship.
     * @return Correctly formatted String to insert into the SQL statement.
     */
    private static String getInsertString(String inputField, JsonObject obj) {
        String temp;

        //OPUS hack
        if (inputField.startsWith("mono_time")) inputField = "mono_time BIGINT";

        try {
            if (inputField.endsWith("BIGINT")) {
                long value = obj.get(inputField.split(" ")[0]).getAsLong();
                temp = value + ", ";
            } else if (inputField.endsWith("INT") && !inputField.contains("BIGINT")) {
                int value = obj.get(inputField.split(" ")[0]).getAsInt();
                temp = value + ", ";
            } else if (inputField.endsWith("REAL") && !inputField.contains("BIGINT")) {
                float value = obj.get(inputField.split(" ")[0]).getAsFloat();
                temp = value + ", ";
            } else if (inputField.endsWith("BOOLEAN") && !inputField.contains("BIGINT")) {
                boolean value = obj.get(inputField.split(" ")[0]).getAsBoolean();
                temp = value + ", ";
            } else if (inputField.endsWith("[]")) {
                // is text with list property
                JsonArray value = obj.get(inputField.split(" ")[0]).getAsJsonArray();
                temp = "ARRAY" + value.toString().replace("\"", "'") + ", ";
            } else {
                // is just text
                String value = obj.get(inputField.split(" ")[0]).getAsString();
                temp = "'" + value + "', ";
            }
        } catch (NumberFormatException | NullPointerException nfe) {
            temp = "null, ";
        }
        return temp;
    }
}
