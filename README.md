# BarleyDB

BarleyDB is a Java ORM library which takes a different approach. Some of the interesting features of BarleyDB are:
* Allowing the programmer to control **per usecase** how much data will be fetched when lazy loading an entity.
* Normal garbage collection rules for entities loaded from the database.
* Transfer over the wire of Entities and their EntityContext.
* Batching of queries to the database (depending if the database supports multiple result-sets).
* Java based schema specification which takes advantage of the compiler to ensure dependencies between tables are met.
* No bytecode manipulation.

### Dynamic and Static Nature
Another key interesting aspect of BarleyDB the fact that **compilation is a completely optional step!**. 
It is completely possible and valid to import an XML specification outlining the complete **database schema**. 
You can then use the meta-model to query and persist data. 

It is also possible to generate Java classes which then allow static and compilation safe interaction with the database. 

#### Benefits to UI development of dynamic nature:
You could use the schema / meta model to create generic CRUD UI screens in the UI technology of your choice which would allow users to view and edit the database data. This would allow products to ship very early. Custom / fancy UI screens could then be created on an as needed basis.

The custom UI screens can then use Java classes generated from the schema to query and persist data ensuring that any custom UI is completely compile safe.

#### Benefits to ETL systems
As someone who has worked extensively with ETL tools. BarleyDB could be used to dynamically define a database schema. The schema could then be loaded and the ETL tool could allow a message to be mapped to the meta-model provided by BarleyDB. Once the data is mapped to the meta model . Then BarleyDB could simply be asked to persist the whole dataset to the database.

## Usual ORM Featutres
BarleyDB also supports the usual ORM features:
* Lazy loading of data.
* Persisting of data to the database.
* Transactional scope.
* Querying of data using a QueryDSL, including joins, subqueries etc.
* Optimistic Locking.
* Auditing of changes.
* Access Control for insert, update and delete operations.1

## Data Structure
BarleyDB has it's own simple data model for holding database data. It consists of:
* Entity - a data record from a table, an entity contains one or more of...
  * ValueNode - A data value. 
  * RefNode - A 1:1 foreign key reference to another entity.
  * ToManyNode - A 1:N reference to many entities.
* EntityContext - A container of entities and scope for transactions.

## Class Generation
As programmers usually want to have their own Classes to program against. BarleyDB can generate the required Classes
for the programmer which are simply proxies to the underlying Entity data structure.

BarleyDB also generates a domain specific query DSL which can be used to query for data. A simple example is as follows:

```java
  //build a simple query
  QUser quser  = new QUser();
  quser.where( quser.name().equal("John") );

  //execute the query and process the results.
  for (User user: ctx.performQuery( quser ).getList()) {
     System.out.println(user.getName() + " - " + user.getAge());
  }
```  
  
## Supported Features:
The following features are supported by BarleyDB

### Querying
* Eager loading via inner joins or outer joins. 
* Flexible lazy loading
  * The queries used to perform fetching can changed at any time to control how much data is fetched.
  * Lazy loading of individual columns.
* SubQueries, Sub-SubQueries...
* And, or, exists, not, equals, greater-than, greater-than-or-equal,less-than,less-than-or-equal, like.
* Batching - executing multiple queries in one go (in a single statement if the database supports multiple result sets). 
* Configuration of the JDBC scroll type, Concurrency and fetch size.

### Persisting
* Inserting, Updating and deleting various entities in a single transaction.
* Optimistic locking (Timestamp based).
* Advanced Freshness checks on the optimistic lock (checking if dependent data is also fresh).
* Batch inserts, updates, deletes.
* Statement reordering to increase batch size.
* Access Control checks - each insert, update or delete operation can be sent to a permission control system to see if it is allowed.
* Auditing - record modifications can be sent to an auditing system.

### Client / Server
BarleyDB supports sending queries and data over the wire allowing for:
* BarleyDB queries to be sent to a server and for the query results to be sent back.
* Persist requests to be sent to a server to persist data to the database.
* Smart serialization ensures that only the relevant data is serialized over the wire. 
  
### Entity Context
The entity context functions as the executer of queries and persist requests. It also holds all of the entites.
In this respect it is similar to the JPA EntityManager. The entity context however supports some extra features
which the EntityManager does not:
* Automatic garbage collection for a more normal expectation of the Java programmer (can be turned off if desired).
* Serialization - the EntityContext can be transferred across the wire.
