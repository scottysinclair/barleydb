package scott.sort.api.specification;

/*
 * #%L
 * Simple Object Relational Framework
 * %%
 * Copyright (C) 2014 Scott Sinclair <scottysinclair@gmail.com>
 * %%
 * All rights reserved.
 * #L%
 */

/**
 * The possible ways to suppress parts of a Spec
 * @author scott
 *
 */
public enum SuppressionSpec {
    /**
     *  The Spec part is suppressed from the generate code
     *  but not from the entity configuration or DDL or DML statements.
     *
     *  So any nodes can still be loaded or persisted along with the entity
     *  but the programmer has no access to them. Useful for
     *  optimistic lock values for example.
     *
     */
    GENERATED_CODE,


    /**
     * The setter is suppressed in the generated code this is useful
     * for auto generated primary keys, where the key should not be programmatically
     * set by the application code.
     *
     */
    GENERATED_CODE_SETTER,

    /**
     *  The Spec part is suppressed from the generate coded
     *  AND the entity configuration but not from DDL or DML statements.
     *
     *  So the column is created in the database but is ignored by the application.
     *
     */
    ENTITY_CONFIGURATION
}

