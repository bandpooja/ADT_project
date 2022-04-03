package production;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;

import static org.neo4j.driver.Values.parameters;

import java.io.File;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Stream;

public class Neo4jDriver implements AutoCloseable
{
    public static double lastExecTime1;
	public static int lastExecTime;
	private final Driver driver;

    public Neo4jDriver( String uri, String user, String password )
    {
        driver = GraphDatabase.driver( uri, AuthTokens.basic( user, password ) );
    }

    @Override
    public void close() throws Exception
    {
        driver.close();
    }

    public void executematch( final String query )
    {
        try ( Session session = driver.session() )
        {
        	Stream<Map<String, Object>>  r = session.writeTransaction( tx ->
                                                        {
                                                            Result result = tx.run( query);
                                                            
                                                            
                                                           
                                                            return result.list(r1 -> r1.asMap()).stream();
                                                            
                                                        } );
        	r.forEach(System.out::println);
        	session.close();
        }
    }

    
    public void create( final String query )
    {
    	Session session = driver.session();
    	
    	session.run(query);
    	
    	session.close();
    }
    
    public static void main( String... args ) throws Exception
    {
        try ( Neo4jDriver greeter = new Neo4jDriver( "bolt://localhost:7687", "neo4j", "Pooja@123" ) )
        {
            greeter.executematch( "MATCH (p:Product)"
            	+ "RETURN p" );
        	
        	//greeter.create("LOAD CSV WITH HEADERS FROM 'http://data.neo4j.com/northwind/employees.csv' AS row MERGE (e: Employee {employeeID: toInteger(row.employeeID)}) ON CREATE SET e.firstName = row.firstName, e.lastName = row.lastName, e.title = row.title;");
        }
    }

	public static void warmUp(C2SProperties props) {
		// TODO Auto-generated method stub
		
	}

	public static void run(String cypherInput, File f_cypher, boolean printBool, C2SProperties props) {
		// TODO Auto-generated method stub
		
	}
}