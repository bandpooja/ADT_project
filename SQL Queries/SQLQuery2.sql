select * from Employees;

select * from Categories;
select * from Customers;
select * from EmployeeTerritories;
select * from Region;
select * from Shippers;
select * from Suppliers;
select * from Territories;

select * from Products;

select * from [Order Details];

select * from Orders;


--Current List of Products
set statistics time on
SELECT ProductID, ProductName
FROM Products
WHERE Discontinued = 0
ORDER BY ProductName;
set statistics time off

/*result
SQL Server Execution Times:
CPU time = 0 ms,  elapsed time = 0 ms.
*/



--Alphabetical List of Products
set statistics time on
select distinct b.*, a.CategoryName
from Categories a 
inner join Products b on a.CategoryID = b.CategoryID
where b.Discontinued = 0
order by b.ProductName;
set statistics time off

/*result
SQL Server Execution Times:
   CPU time = 0 ms,  elapsed time = 1 ms.
*/


--Employee Sales by Country
set statistics time on
select distinct b.*, a.CategoryName
from Categories a 
inner join Products b on a.CategoryID = b.CategoryID
where b.Discontinued = 0
order by b.ProductName;
set statistics time off

/*result
 SQL Server Execution Times:
   CPU time = 0 ms,  elapsed time = 0 ms.
*/


--Order Details Extended
set statistics time on
select distinct y.OrderID, 
    y.ProductID, 
    x.ProductName, 
    y.UnitPrice, 
    y.Quantity, 
    y.Discount, 
    round(y.UnitPrice * y.Quantity * (1 - y.Discount), 2) as ExtendedPrice
from Products x
inner join [Order Details] y on x.ProductID = y.ProductID
order by y.OrderID;
set statistics time off

/*result
 SQL Server Execution Times:
   CPU time = 0 ms,  elapsed time = 406 ms.
*/


--Sales by Category

set statistics time on
select distinct a.CategoryID, 
    a.CategoryName,  
    b.ProductName, 
    sum(round(y.UnitPrice * y.Quantity * (1 - y.Discount), 2)) as ProductSales
from [Order Details] y
inner join Orders d on d.OrderID = y.OrderID
inner join Products b on b.ProductID = y.ProductID
inner join Categories a on a.CategoryID = b.CategoryID
where d.OrderDate between '1997/1/1' and '1997/12/31'
group by a.CategoryID, a.CategoryName, b.ProductName
order by a.CategoryName, b.ProductName, ProductSales;
set statistics time off

/*result
SQL Server Execution Times:
   CPU time = 15 ms,  elapsed time = 8 ms.
*/
 

 --Customers and Suppliers by City
set statistics time on
 select City, CompanyName, ContactName, 'Customers' as Relationship 
from Customers
union
select City, CompanyName, ContactName, 'Suppliers'
from Suppliers
order by City, CompanyName;
set statistics time off

/*result
 SQL Server Execution Times:
   CPU time = 0 ms,  elapsed time = 3 ms.
*/







