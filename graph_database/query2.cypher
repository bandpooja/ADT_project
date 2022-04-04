// Current List of Products

MATCH (p:Product)
WHERE toInteger(p.discontinued) = 0
RETURN p.productID AS productID, p.productName AS productName
ORDER BY p.productName
// CPU time:2 ms and elapsed time:  6 ms



// Alphabetical List of Products

MATCH (c:Category)-[r]-(p:Product)
WHERE toInteger(p.discontinued) = 0
RETURN  c AS Category, p AS Product
//  CPU time:1 ms and elapsed time:  3 ms




//Order Details Extended

MATCH (o:OrderDetails)-[]-()-[]-(p:Product)
RETURN o.orderID AS orderID, p.productID AS productID, 
    p.productName AS productName, 
    o.unitPrice AS unitPrice, 
    o.quantity AS quantity, 
    o.discount AS discount, 
    round(o.unitPrice * o.quantity * (1 - o.discount), 2) as ExtendedPrice
ORDER BY o.orderID;
//  CPU time: 2 ms and elapsed time:  650 ms



//Sales by Category

MATCH r=(od:OrderDetails)-[]-(o:Order)-[]-(p:Product)-[]-(c:Category)
RETURN c.categoryID AS categoryID, c.categoryName AS categoryName, p.productName AS productName, sum(round(od.unitPrice * od.quantity * (1 - od.discount), 2)) as ProductSale
order by categoryName, productName, ProductSale;
//  CPU time: 22 ms and elapsed time:  89 ms


