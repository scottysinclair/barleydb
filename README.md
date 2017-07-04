# BarleyDB
BarleyDB is a Java ORM library which makes it as easy as possible to load and save application domain models to and from the database. It is also extremely powerfull with a lot of features.

### Generated domain query models
Query DSL classes are auto-generated and allow the programmer to easily build queries to load their application data.
```java
  QUser quser = new QUser();
  quser.joinToAddress();
  QDepartment qdepartment = quser.joinToDepartment();
  qdepartment.joinToCountry();
  
  List<User> users = ctx.performQuery( quser ).getList();
```
### Complex where clauses 
The Query DSL support the usual operators logical and arithmetic operators as well as subqueries for example.
```java
  QUser quser = new QUser();
  QDepartment qdepartment = quser.existsDepartment();
  quser.where( quser.name().equal("fred") )
       .andExists( qdepartment.where( qdepartment.name().like("computing") ) );
```

### Batching multiple queries
The queries will be combined into a composite query returning multiple result-sets (depending on database vendor support).
```java
QueryBatcher batch = new QueryBatcher();
batch.addQuery(new QUser());
batch.addQuery(new QDepartment());
batch.addQiery(new QCountry());

ctx.performQueries( batch );
```

### Fetch plan definition whenever you want
Fetch plans for lazy loading can be registered at any time by specifying query models with the desired joins. 
```java
//create and register the fetch plan to be used the next time a deparment must be fetched.
QDepartment qdepartment = new QDepartment();
qdepartment.joinToCountry();
ctx.registerQuery( qdepartment );

//call user.getDepartment() which will cause a fetch.
Department dep = user.getDepartment();
//country was fetch along with the deparment as per the fetch plam
Country country = dep.getCountry();
```

### Persisting changes to the database
Persist requests are used to bundle together domain models to be persisted.
```java
PersistRequest pr = new PersistRequest();
pr.save( userA, userB, userC );
pr.delete( userX, userY );

ctx.persist( pr  );
```

### Auditing
Auditing is very straightforward as a change report is generated every time domain models are persisted.
```
audit AUDIT SS_XML_MAPPING                 ID                             null                           3                             
audit AUDIT SS_XML_MAPPING                 SYNTAX_MODEL_ID                null                           1                             
audit AUDIT SS_XML_MAPPING                 XPATH                          null                           /root3                        
audit AUDIT SS_XML_MAPPING                 TARGET_FIELD_NAME              null                           target3                       
audit AUDIT SS_XML_MAPPING                 ID                             null                           4                             
audit AUDIT SS_XML_MAPPING                 SYNTAX_MODEL_ID                null                           2                             
audit AUDIT SS_XML_MAPPING                 XPATH                          null                           sub1                          
audit AUDIT SS_XML_MAPPING                 TARGET_FIELD_NAME              null                           subtarget1                    
audit AUDIT SS_XML_MAPPING                 ID                             null                           5                             
audit AUDIT SS_XML_MAPPING                 SYNTAX_MODEL_ID                null                           2                             
audit AUDIT SS_XML_MAPPING                 XPATH                          null                           sub2                          
audit AUDIT SS_XML_MAPPING                 TARGET_FIELD_NAME              null                           subtarget2     
```

### Relationship management
Ownership vs simple referral relationships impact how data is persisted across relations.
* Ownership causes inserts, updates and orphan removal to be performed.
* Simple refferal causes inserts to be performed when the referred to entity is new.

### Freshness checking (optimistic locking)
BarleyDB will verify any optimistic locks defined on entities. 
* Relationship management works in combination with optimistic locking, so that owned data can cause the owner's optimistic lock to be updated.
* The dependsOn relationship requires that the entity depended on must also be fresh, even if it is not being saved.

### Transaction management
```java
ctx.setAutocommit(false);
..
..
ctx.commit();
```

### Large data-set support / streaming
Domain models can be streamed from the database.
```java
QUser quser = new QUser();
quser.joinToAddress();

try ( ObjectInputStream<User> in = ctx.streamObjectQuery( quser ); ) {
  User user;
  while( (user = in.read()) != null ) {
    ...
  }
}
```
A stream can also be opened on any 1:N relation on any domain model.
```java
try ( ObjectInputStream<Address> in = user.streamAddresses(); ) {
  Address address;
  while( (address = in.read()) != null ) {
    ...
  }
}
```

### Garbage collection of unreferenced entities
BarleyDB supports garabage collection so that entities which are no longer referred to are removed. 
This works very well in combination with large data-set streaming as memory will be reclaimed automatically as the program proceeds through the data stream.

### 3 tier architecture support
BarleyDB can be used on the client tier in a 3 tier architecture. A client can create a remote context to the application server to perform queries and persist domain models as normal. 

### Easy domain schema definition
Both Java and XML Schema definition is supported though Java is preferred as the compiler can catch any inconsistencies.
```java
public class Applicationpec extends PlatformSpec {

    public PlaySpec() {
        super("com.mycomp.application");
    }

    @Enumeration(JdbcType.INT)
    public static class EmployeeType {
        public static final int ADMIN = 1;
        public static final int ENGINEER = 2;
        public static final int MANAGER = 3;
        public static final int HR = 4;
        public static final int CHIEF = 5;
    }

    @Enumeration(JdbcType.INT)
    public static class Language {
        public static final int ENGLISH = 1;
        public static final int FRENCH = 2;
        public static final int SPANISH = 3;
        public static final int GERMAN = 4;
        public static final int SWISS = 5;
    }


    @Entity("GEN_EMPLOYEE")
    public static class Employee {

        public static final NodeSpec id = longPrimaryKey();

        public static final NodeSpec employeeType = mandatoryEnum(EmployeeType.class);

        public static final NodeSpec name = name();

        public static final NodeSpec department = mandatoryRefersTo(Department.class);

        public static final NodeSpec countryOfOrigin = mandatoryRefersTo(Country.class);

        public static final NodeSpec motherLang = mandatoryEnum(Language.class);

    }
    
    @Entity("GEN_LOB")
    public static class LineOfBusiness{

        public static final NodeSpec id = longPrimaryKey();

        public static final NodeSpec name = name();

        public static final NodeSpec parent = optionallyRefersTo(LineOfBusiness.class, "PARENT_ID");

        public static final NodeSpec children = ownsMany(LineOfBusiness.class, LineOfBusiness.parent);

    }    
    ...
```

### Modular definition of Schemas and importing of schemas
Each schema definition has it's own namespace and can import other schemas to build highly modular applications. 

### Easy bootstrapping with schema creation
To get up and running simply specify the datasource and the schema definitions like so.
As can be see below the schema can also be dropped and recreated.
```java
Environment env = EnvironmentDef.build()
        .withDataSource()
            .withDriver("org.postgresql.xa.PGXADataSource")
            .withUser("test_user")
            .withPassword("password")
            .withUrl("jdbc:postgresql://localhost:5432/test_db")
            .end()
         .withSpecs(ApplicationSpec.class)
         .withDroppingSchema(true)
         .withSchemaCreation(true)
         .create();

ApplicationCtx ctx = new ApplicationCtx( env );
ctx.performQuery(new QUser());
```

## Database script generation
Create scripts, drop scripts and clean scripts can be automatically generated.


-----

BarleyDB is a Java ORM library which takes a different approach. Some of the interesting features of BarleyDB are:
* Allowing the programmer to control **per usecase** how much data will be fetched when lazy loading an entity.
* Normal garbage collection behaviour for entities loaded from the database.
* Transfer over the wire of Entities and their EntityContext.
* Batching of queries to the database (depending if the database supports multiple result-sets).
* Java based schema specification which takes advantage of the compiler to ensure dependencies between tables are met.
* No bytecode manipulation.

### Dynamic and Static Nature
Another key interesting aspect of BarleyDB the fact that **compilation is a completely optional step!**.
It is completely possible and valid to import an XML specification outlining the complete **database schema**.
You can then use the meta-model to query and persist data. This is a very unusual and powerfull feature which no other the Java ORM solution offers and it allows BarleyDB to be used in interesting ways.

It is of course also possible to generate Java classes which then allow static and compilation safe interaction with the database.

#### Benefits to UI development of dynamic nature
You could use the schema / meta model to create generic CRUD UI screens in the UI technology of your choice which would allow users to view and edit the database data. This would allow products to ship very early. Custom / fancy UI screens could then be created on an as needed basis.

The custom UI screens can then use Java classes generated from the schema to query and persist data ensuring that any custom UI is completely compile safe.

#### Benefits to ETL systems of dynamic nature
As someone who has worked extensively with ETL tools. BarleyDB could be used to dynamically define a database schema. The schema could then be loaded and the ETL tool could allow a message to be mapped to the meta-model provided by BarleyDB. Once the data is mapped to the meta model . Then BarleyDB could simply be asked to persist the whole dataset to the database.

#### Loading data from an older schema version on the fly
As no compilation is required to load and save data from a database schema. It is possible to import BarleyDB XML schema definitions of an older schema into a running system on the fly and then use those definitions to pull data out of the older database.

#### Sophisticated version management (future)
BarleyDB can generate XML schema definition files. Each schema definition can be reduced to a SHA-1 hash. If migation logic was introduced, then it would be possible to define how to migrate data from one schema definition to another.
This would allow for automatic forward porting and backporting of data.

Such a system would allow connecting to a database with an older schema version, then loading in data and upgrading it to match the current schema and then inserting it into the current database. If backporting was supported, the reverse could also be accomplished.

## Usual ORM Featutres
BarleyDB also supports the usual ORM features:
* Generation of DDL database scripts.
* Generation of Java Classes.
* Entity Inheritence with proper class inheritence in the generated classes.
* Lazy loading of data.
* Persisting of data to the database.
* Transactional scope.
* Querying of data using a query DSL, including joins, subqueries etc.
* Optimistic Locking.
* Auditing of changes.
* Access Control for insert, update and delete operations.

## Data Structure
BarleyDB has it's own simple data model for holding database data. It consists of:
* Entity - a data record from a table, an entity contains one or more of...
  * ValueNode - A data value.
  * RefNode - A 1:1 foreign key reference to another entity.
  * ToManyNode - A 1:N reference to many entities.
* EntityContext - A container of entities and scope for transactions.

## Class Generation
As programmers usually want to have their own classes to program against. BarleyDB can generate the required Classes
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

## A more detailed look...
The following features are supported by BarleyDB

### Querying
A more complex query eample is as follows:
```java
  //find users with name 'John' who have a primary address with postcode 'KW14' or a seconary address with
  //postcode 'OSA'
  QUser quser = new QUser();
  QAddress primAddr = quser.existsPrimaryAddress(); //sub-query for primary address
  QAddress secAddr = quser.existsSecondaryAddress(); //sub-query for secondard address

  //join to the user's department and the department's country so the data is pulled
  //in as part of the same query.
  quser.joinToDepartment().joinToCountry();

  quser.where( quser.name().equal("John") )
       .andExists( primAddr.where( primAddr.postCode().like("KW14") ) )
       .orExists ( secAddr.where( primAddr.postCode().like("OSA") ) );

  //execute the query and process the results.
  for (User user: ctx.performQuery( quser ).getList()) {
     System.out.println(user.getName() + " - " + user.getDepartment().getCountry().getName());
  }
```
The feature set is as follows:
* Eager loading via inner joins or outer joins.
* Flexible lazy loading
  * The queries used to perform fetching can changed at any time to control how much data is fetched.
  * Lazy loading of individual columns.
* SubQueries, Sub-SubQueries...
* And, or, exists, not, equals, greater-than, greater-than-or-equal,less-than,less-than-or-equal, like.
* Batching - executing multiple queries in one go (in a single statement if the database supports multiple result sets).
* Configuration of the JDBC scroll type, Concurrency and fetch size.

### Persisting
An example of persisting data is as follows:
```java
//create a new user
User user = ctx.newModel(User.class);
user.setName("John");

//create a new department
Department dept = ctx.newModel(Department.class);
dept.setName("Computer Science");

//assign the department to the user.
user.setDepartment( dept );

//Save the user. The user has a FK reference to the department so the department is saved too.
PeristRequest req = new PeristRequest();
req.save( user );
ctx.persist( req );
```
The feature set is as follows:
* Inserting, Updating and deleting various entities in a single transaction.
* Uses **owning** and **depends-on** and **refers-to** relationships to correctly manage dependencies when saving and performing freshness checks.
* Plugin sequence generator.
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
* Transactions are not maintained between across client calls, therefore optimistic locking should be used as provided by BarleyDB.

### Entity Context
The entity context functions as the executer of queries and persist-requests. It also holds all of the entites.
In this respect it is similar to the JPA EntityManager. The entity context however supports some extra features
which the EntityManager does not:
* Automatic garbage collection for a more normal expectation of the Java programmer (can be turned off if desired).
* Serialization - the EntityContext can be transferred across the wire.

### Specification
BarleyDB allows the schema to be specified in an XML file or via Java classes. Java classes have the advantage of using  compilation safety to ensure foreign key references between tables.

# Getting Started
The best way to see how BarleyDB works is to look at the test cases.
* The [scott.barleydb.test.TestQuery](https://github.com/scottysinclair/barleydb/blob/master/src/test/java/scott/barleydb/test/TestQuery.java) class executes queries using the query DSL against an in memory HSQLDB instance.
* The [scott.barleydb.test.TestPersistence](https://github.com/scottysinclair/barleydb/blob/master/src/test/java/scott/barleydb/test/TestPersistence.java) class executes persist requests saving data into an in memory HSQLDB instance.
* The [org.example.etl.EtlSpec](https://github.com/scottysinclair/barleydb/blob/master/src/test/java/org/example/etl/EtlSpec.java) defines a schema for an ETL tool which itself references elements from the [org.example.acl.AclSpec]()
* The [scott.barleydb.test.TestGenerator](https://github.com/scottysinclair/barleydb/blob/master/src/test/java/scott/barleydb/test/TestGenerator.java) class generates schema DDL files, query DSL classes and pojo classes for a given schema specification.


#Future Features
* Autogeneration of UUIDs
* Streaming of large result-sets.
