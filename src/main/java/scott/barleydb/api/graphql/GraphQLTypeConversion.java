package scott.barleydb.api.graphql;

import scott.barleydb.api.config.NodeType;
import scott.barleydb.api.core.types.JavaType;

public class GraphQLTypeConversion {

	public static Object convertValue(NodeType nodeType, Object value) {
		return convertValue(value, nodeType.getJavaType());
	}

	public static Object convertValue(Object value, JavaType javaType) {
		switch (javaType) {
			case LONG:
				return convertToLong(value);
			default: return value;
		}
	}
	
	public static Long convertToLong(Object value) {
		if (value instanceof Integer) {
			return ((Integer)value).longValue();
		}
		throw new IllegalStateException(String.format("Cannot convert %s to Long", value));
	}

}
