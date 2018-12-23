package scott.barleydb.api.query;

import java.io.Serializable;

import scott.barleydb.api.core.types.JavaType;

/**
 * @author scott
 *
 * @param <VAL>
 */
public class QParameter<VAL> implements Serializable {
	private final String name;
	private final JavaType type;
	private VAL value;
	
	public QParameter(String name) {
		this(name, null);
	}

	public QParameter(String name, JavaType type) {
		this.name = name;
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public VAL getValue() {
		return value;
	}

	public void setValue(VAL value) {
		this.value = value;
	}

	public JavaType getType() {
		return type;
	}
	
}
