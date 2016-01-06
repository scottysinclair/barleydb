# BarleyDB

BarleyDB is a Java ORM library which takes a different approach.

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

  //build a simple query
  QUser quser  = new QUser();
  quser.where( quser.name().equal("John") );

  //execute the query and process the results.
  for (User user: ctx.performQuery( quser ).getList()) {
     System.out.println(user.getName() + " - " + user.getAge());
  }
  
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
* Automatic garbage collection (can be turned off).
* Serialization - the EntityContext can be transferred across the wire.
