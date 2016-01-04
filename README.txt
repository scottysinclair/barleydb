Overview of project

An EntityContext contains Entities, RefNodes and ToManyNodes. These objects combine together to provide a managed data environment. 

The programmer can generate his or her own model and query classes which can then be used to query the database via the EntityContext
and interact with the data via his model classes. The model classes are simple proxies around the managed data. No byte code generation is required. 

The generated query and model classes are simple convenience APIs for the programmer to use. It is possible to fully use
the framework to query and persist data without any compilation _at all_. It is perfectly possible to setup a configuration dynamically at runtime
and then start to query and get data from it.

This opens up various interesting possibilities:
* Save previous schema versions as XML and to support importing data from older datasources. The old configuration can be loaded
on an as needed basis and then thrown away once the data is imported.



The EntityContext can be used to:
* Query for data using a Query API 
* Define fetch queries for lazy loading
* Persist data to the database.

Entity
RefNode
ToManyNode

1:1 => Entity.RefNode -> Entity
1:N => Entity.ToManyNode -> [Entity,...]
N:M => Entity.ToManyNode -> [Entity.RefNode -> Entity,...]

The EntityContext can run in autocommit mode or transactional mode.
It is possible (but not implemented yet) for the data in the entity context to also be rolled back to the same state as the start of the transaction.
Transactional mode is only supported for JdbcEntityContextServices which have direct connections to a database. 