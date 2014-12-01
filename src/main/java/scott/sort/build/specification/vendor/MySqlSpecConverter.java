package scott.sort.build.specification.vendor;

/*
 * #%L
 * Simple Object Relational Framework
 * %%
 * Copyright (C) 2014 Scott Sinclair <scottysinclair@gmail.com>
 * %%
 * All rights reserved.
 * #L%
 */

import scott.sort.api.core.types.JavaType;
import scott.sort.api.core.types.JdbcType;
import scott.sort.api.specification.DefinitionsSpec;
import scott.sort.api.specification.EntitySpec;
import scott.sort.api.specification.NodeSpec;
import scott.sort.server.jdbc.converter.LongToStringTimestampConverter;

public class MySqlSpecConverter {

	/**
	 * Converts the given spec to work with MySql DB.
	 * @param definitionsSpec
	 * @return
	 */
	public static DefinitionsSpec convertSpec(DefinitionsSpec definitionsSpec) {
		for (EntitySpec entitySpec: definitionsSpec.getEntitySpecs()) {
			for (NodeSpec nodeSpec: entitySpec.getNodeSpecs()) {
				if (nodeSpec.getJavaType() != JavaType.LONG) {
					continue;
				}
				if (nodeSpec.getJdbcType() != JdbcType.TIMESTAMP) {
					continue;
				}
				nodeSpec.setJdbcType(JdbcType.VARCHAR);
				nodeSpec.setLength(50);
				nodeSpec.setTypeConverter(LongToStringTimestampConverter.class.getName());
			}
		}
		return definitionsSpec;
	}
}
