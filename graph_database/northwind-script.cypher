//Creating northwind db
CREATE CONSTRAINT Product_productID IF NOT EXISTS FOR (p:Product) REQUIRE (p.productID) IS UNIQUE;
CREATE CONSTRAINT Category_categoryID IF NOT EXISTS FOR (c:Category) REQUIRE (c.categoryID) IS UNIQUE;
CREATE CONSTRAINT Supplier_supplierID IF NOT EXISTS FOR (s:Supplier) REQUIRE (s.supplierID) IS UNIQUE;
CREATE CONSTRAINT Customer_customerID IF NOT EXISTS FOR (c:Customer) REQUIRE (c.customerID) IS UNIQUE;
CREATE CONSTRAINT Order_orderID IF NOT EXISTS FOR (o:Order) REQUIRE (o.orderID) IS UNIQUE;

LOAD CSV WITH HEADERS FROM "http://data.neo4j.com/northwind/products.csv" AS row
MERGE (n:Product {productID:row.productID})
SET n += row,
n.unitPrice = toFloat(row.unitPrice),
n.unitsInStock = toInteger(row.unitsInStock), n.unitsOnOrder = toInteger(row.unitsOnOrder),
n.reorderLevel = toInteger(row.reorderLevel), n.discontinued = (row.discontinued <> "0");

LOAD CSV WITH HEADERS FROM "http://data.neo4j.com/northwind/categories.csv" AS row
MERGE (n:Category {categoryID:row.categoryID})
SET n += row;

LOAD CSV WITH HEADERS FROM "http://data.neo4j.com/northwind/suppliers.csv" AS row
MERGE (n:Supplier {supplierID:row.supplierID})
SET n += row;

MATCH (p:Product),(c:Category)
WHERE p.categoryID = c.categoryID
MERGE (p)-[:PART_OF]->(c);

MATCH (p:Product),(s:Supplier)
WHERE p.supplierID = s.supplierID
MERGE (s)-[:SUPPLIES]->(p);

LOAD CSV WITH HEADERS FROM "http://data.neo4j.com/northwind/customers.csv" AS row
MERGE (n:Customer {customerID:row.customerID})
SET n += row;

LOAD CSV WITH HEADERS FROM "http://data.neo4j.com/northwind/orders.csv" AS row
MERGE (n:Order {orderID:row.orderID})
SET n += row;

MATCH (c:Customer),(o:Order)
WHERE c.customerID = o.customerID
MERGE (c)-[:PURCHASED]->(o);

LOAD CSV WITH HEADERS FROM "http://data.neo4j.com/northwind/order-details.csv" AS row
MATCH (p:Product), (o:Order)
WHERE p.productID = row.productID AND o.orderID = row.orderID
MERGE (o)-[details:ORDERS]->(p)
SET details = row,
details.quantity = toInteger(row.quantity);

USING PERIODIC COMMIT
LOAD CSV WITH HEADERS FROM "file:///dbo.OrderDetails.csv" AS row
MERGE (n:OrderDetails { orderID:row.orderID})
SET n += row,
n.productID = toInteger(row.productID),
n.unitPrice = toFloat(row.unitPrice),
n.quantity = toInteger(row.quantity),
n.discount = toFloat(row.discount);

USING PERIODIC COMMIT
LOAD CSV WITH HEADERS FROM "file:///dbo.Employees.csv" AS row
MERGE (e: Employee {employeeID: toInteger(row.EmployeeID)})
ON CREATE SET e.firstName = row.firstName, e.lastName = row.lastName, e.title = row.title;

USING PERIODIC COMMIT
LOAD CSV WITH HEADERS FROM "file:///dbo.Employees.csv" AS row
MATCH (a: Employee {employeeID: toInteger (row.EmployeeID)}) 
MATCH (b: Employee {employeeID: toInteger (row.ReportsTo)})
MERGE (a)-[:REPORTS_TO]-> (b);

MATCH (o: `Order`),(od: OrderDetails)
WHERE o.orderID = od.orderID
MERGE (o)-[:CONTAINS]-(od)

MATCH (c:`Order`),(p:Employee)
WHERE p.employeeID =toInteger(c.employeeID)
MERGE (p)-[:SOLD]->(c);

USING PERIODIC COMMIT
LOAD CSV WITH HEADERS FROM "file:///dbo.Employees.csv" AS row
MATCH (a: Employee {employeeID: toInteger (row.EmployeeID)}) 
MATCH (b: Employee {employeeID: toInteger (row.ReportsTo)})
MERGE (a)-[:REPORTS_TO]-> (b);
