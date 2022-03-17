//Queries


//top orders of steven
MATCH p=(e:Employee) 
WHERE e.firstName = 'Steven' 
OPTIONAL MATCH p1=(o: `Order`)-[r:CONTAINS]-(od: OrderDetails) 
RETURN od.OrderID, ((toInteger(od.Quantity)*toInteger(od.UnitPrice)) * (1-toInteger(od.Discount) )) AS Price
ORDER BY Price DESC
LIMIT 5
// CPU time: 1ms, elapsed time: 20ms


//reporting structure 2 levels deep
MATCH r=(c)-[:PURCHASED]-(o:`Order`)-[:ORDERS]-(p:Product)
RETURN r
LIMIT 30
// CPU time: 1ms, elapsed time: 1ms


//top 10 most expensive products
MATCH (p:Product)
WHERE p.unitPrice IS NOT NULL
RETURN p.productName, p.unitPrice
ORDER BY toFloat(p.unitPrice) DESC
LIMIT 10
// CPU time: 1ms, elapsed time: 8ms


//products starting with c and price > 100
MATCH (p:Product)
WHERE p.productName STARTS WITH "C" AND toFloat(p.unitPrice) > 100
RETURN p AS Product;
// CPU time: 4ms, elapsed time: 5ms



//which employees sold chocolade
MATCH (e: Employee)-[:SOLD]-(o: `Order`)-[:ORDERS]-(p:Product)
WHERE p.productName = "Chocolade"
RETURN e AS Employees
// CPU time: 1ms, elapsed time: 3ms



