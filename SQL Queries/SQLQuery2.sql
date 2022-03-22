SELECT ProductName, QuantityPerUnit 
FROM Products;

--Current List of Products
SELECT *
FROM Products

SELECT ProductID, ProductName
FROM Products
WHERE Discontinued = 0
ORDER BY ProductName;


SELECT CompanyName, Phone, null as "ContactName"
FROM Shippers
    UNION
SELECT CompanyName, Phone, ContactName
FROM Customers
    UNION
SELECT CompanyName, Phone, ContactName
FROM Suppliers
GROUP BY CompanyName, Phone, ContactName


--Alphabetical List of Products
select * from Categories

select * from Products

select distinct b.*, a.CategoryName
from Categories a 
inner join Products b on a.CategoryID = b.CategoryID
where b.Discontinued = 0
order by b.ProductName;

--Employee Sales by Country

select distinct b.*, a.CategoryName
from Categories a 
inner join Products b on a.CategoryID = b.CategoryID
where b.Discontinued = 0
order by b.ProductName;

--Order Details Extended

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



--Sales by Category


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

--Ten Most Expensive Products

select distinct ProductName as Ten_Most_Expensive_Products, 
         UnitPrice
from Products as a
where 10 >= (select count(distinct UnitPrice)
                    from Products as b
                    where b.UnitPrice >= a.UnitPrice)
order by UnitPrice desc;
 

 --Customers and Suppliers by City

 select City, CompanyName, ContactName, 'Customers' as Relationship 
from Customers
union
select City, CompanyName, ContactName, 'Suppliers'
from Suppliers
order by City, CompanyName;
