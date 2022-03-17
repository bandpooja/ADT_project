# ADT_project

In this project we want to compare and analyze the performances of Relational, Graph and Hybrid databases in context of recommendation system.

For this we have followed the below steps:

### Database Selection

#### Northwind Database
The Northwind sample database is based on a fictitious company called Northwind Traders, which imports and exports specialty foods from around the world. It is a commonly-used SQL datasets which are graphic enough.

#### Specifications-
size - 1.01 MB,
Relations - 13,
Tuples - 3308

###Relational Database
We have loaded the nothwind database into the sql server and performed some queries on it.

The following images depict the table from the executed queries
![query1](https://user-images.githubusercontent.com/43738136/158866218-bb832dc0-5cba-45a6-8681-2126d8d4c623.png)
![query2](https://user-images.githubusercontent.com/43738136/158867003-dc51985a-c551-4f5d-abec-603e59745207.png)
![query3](https://user-images.githubusercontent.com/43738136/158867029-8e61335b-6f97-4aed-b539-8ba66ffa4a12.png)
![query4](https://user-images.githubusercontent.com/43738136/158867051-5be88ebd-9f89-4daa-8b48-d5a7e7a2ebee.png)
![query5](https://user-images.githubusercontent.com/43738136/158867070-7928d35f-139b-45c2-be2b-ef4ea118460a.png)




### Graph Database
We loaded the csv files into Neo4j. We defined the nodes and relationships to create the graph database using Cypher queries. 

The following images depict the graph database:


The following graph depicts products and category relationship
<img src="graph_1.png" width="500" height="500">


The following graph depicts employees and their managers
<img src="graph_2.png" width="500" height="500">




