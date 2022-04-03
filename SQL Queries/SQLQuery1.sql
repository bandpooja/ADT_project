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



--steven buchanans top 5 selling products
set statistics time on
select TOP 5 a.LastName,a.FirstName,b.ProductName,sum(round(y.UnitPrice * y.Quantity * (1 - y.Discount), 2)) as ProductSales
from [Order Details] y
inner join Orders d on d.OrderID = y.OrderID
inner join Products b on b.ProductID = y.ProductID
inner join Employees a on a.EmployeeID = d.EmployeeID
where a.LastName like'Buchanan' and a.FirstName like 'Steven'
group by  a.LastName,a.FirstName,b.ProductName
order by  ProductSales desc;
set statistics time off
/*result
 SQL Server Execution Times:
   CPU time = 0 ms,  elapsed time = 167 ms.
*/

--reporting structure two level deep
set statistics time on
select a.CategoryName,a.Description
from Products b
inner join Categories a on a.CategoryID=b.CategoryID
where b.ProductName like 'chai'
set statistics time off

/* SQL Server Execution Times:
   CPU time = 0 ms,  elapsed time = 0 ms*/

--ten high pice products
set statistics time on
select distinct ProductName as Ten_Most_Expensive_Products, 
         UnitPrice
from Products as a
where 10 >= (select count(distinct UnitPrice)
                    from Products as b
                    where b.UnitPrice >= a.UnitPrice)
order by UnitPrice desc;
set statistics time off

/*result
  SQL Server Execution Times:
   CPU time = 15 ms,  elapsed time = 76 ms.
*/


--products starting with c prices greater than 100
set statistics time on
select ProductName,UnitPrice from Products
where ProductName like 'C%' and UnitPrice >100;
set statistics time off

/*result
 SQL Server Execution Times:
   CPU time = 0 ms,  elapsed time = 2 ms.
*/

--which employees ordered chocolade
set statistics time on
select  a.LastName,a.FirstName,b.ProductName
from [Order Details] y
inner join Orders d on d.OrderID = y.OrderID
inner join Products b on b.ProductID = y.ProductID
inner join Employees a on a.EmployeeID = d.EmployeeID
where b.ProductName like 'Chocolade'
group by  a.LastName,a.FirstName,b.ProductName
set statistics time off

/*result
  SQL Server Execution Times:
   CPU time = 0 ms,  elapsed time = 93 ms.
*/

