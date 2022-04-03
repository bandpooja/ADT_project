package database.Sql;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import production.C2SProperties;

public class SQLDatabaseConnection {
    // Connect to your database.
    // Replace server name, username, and password with your credentials

	 public static long lastExecTimeCreate = 0;
	 public static long lastExecTimeInsert = 0;
    static Connection createConnection() throws SQLException, ClassNotFoundException {
        
    	
    	//Loading the required JDBC Driver class
    			Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");	
    			
    			//Creating a connection to the database

    			Connection connection = DriverManager.getConnection("jdbc:sqlserver://localhost:1434; database =master;encrypt=true;trustServerCertificate=true; integratedSecurity=true;");
            
            	return connection;
           
    }
    
    static void closeConnection(Connection connection) {
        try {
        	connection.close();
                   } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public static void executeCreateView(String query) throws SQLException, ClassNotFoundException {
    	Connection connection=SQLDatabaseConnection.createConnection();
        Statement stmt = connection.createStatement();

        // timing unit for creating statements.
        long startNanoCreate = System.nanoTime();
        stmt.executeUpdate(query);
        long endNanoCreate = System.nanoTime();
        lastExecTimeCreate += (endNanoCreate - startNanoCreate);

        stmt.close();
    }
    
    public static void select(String query) throws SQLException, ClassNotFoundException {
    	
        // Create and execute a SELECT SQL statement.
        
        ResultSet resultSet = null;

        try (Connection connection=SQLDatabaseConnection.createConnection();
                Statement statement = connection.createStatement();) {

            // Create and execute a SELECT SQL statement.
            String selectSql = query;
            resultSet = statement.executeQuery(selectSql);
            ResultSetMetaData rsm = resultSet.getMetaData();

            // Print results from select statement
            while (resultSet.next()) {
            	for(int i=1;i<=rsm.getColumnCount();i++){

            		System.out.println(resultSet.getString(i));
            		
            }
            	System.out.println("---------------------------------------------------------------------------------------------------------------------------------------------------------------");
            }
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    static void createInsert(String query) throws SQLException, ClassNotFoundException {
    	Connection connection=SQLDatabaseConnection.createConnection();
        Statement statement = connection.createStatement();
       
        long startNanoInsert = System.nanoTime();
        statement.executeUpdate(query);
        long endNanoInsert = System.nanoTime();

        System.out.println("TIME OF QUERY : " + query.substring(0, Math.min(query.length(), 50)) + " -- " +
                ((endNanoInsert - startNanoInsert) / 1000000.0) + " ms.");

        statement.close();
    }
    
    public static void insertOrDelete(String query) throws SQLException, ClassNotFoundException {
        Connection connection= createConnection();
        Statement stmt = connection.createStatement();

        // timing unit for creating statements.
        long startNanoInsert = System.nanoTime();
        stmt.executeUpdate(query);
        long endNanoInsert = System.nanoTime();
        lastExecTimeInsert += (endNanoInsert - startNanoInsert);

        stmt.close();
    }
    
   public static void main(String args[]) throws ClassNotFoundException, SQLException
   {
	   select("select * from dbo.Employees ;");
   }
}