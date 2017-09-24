package scott.barleydb.api.dto;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import scott.barleydb.api.config.EntityType;
import scott.barleydb.api.config.NodeType;
import scott.barleydb.api.core.Environment;
import scott.barleydb.api.exception.SortRuntimeException;

/**
 * Helper methods for DTO reflection
 * @author scott
 *
 */
public class DtoHelper {
  private final Environment env;
  private final String namespace;
  private final Map<ProperyKey,Method> methodCache = new HashMap<>();


  public DtoHelper(Environment env, String namespace) {
    this.env = env;
    this.namespace = namespace;
  }

  public Map<String,Object> getProperties(BaseDto dto) {
    Map<String,Object> result = new HashMap<String, Object>();
    EntityType entityType = env.getDefinitions(namespace).getEntityTypeForDtoClass(dto.getClass(), true);
    for (NodeType nodeType: entityType.getNodeTypes()) {
      Object value = callPropertyGetter(dto, nodeType);
      result.put(nodeType.getName(), value);
    }
    return result;
  }

  @SuppressWarnings("unchecked")
  private <T> T callPropertyGetter(BaseDto dto, NodeType nodeType) {
    Method method = findMethod(dto, nodeType, true);
    return (T)invoke(method, dto);
  }

  private Object invoke(Method method, BaseDto dto, Object ...args) {
    try {
      return method.invoke(dto, args);
    }
    catch(InvocationTargetException x) {
      throw new SortRuntimeException("DTO method '" + method.getName()+ "'  threw exception", x.getTargetException());
    }
    catch(IllegalAccessException x) {
      throw new SortRuntimeException("Error accessing DTO method '" + method.getName()+ "'", x);
    }
  }

  private Method findMethod(BaseDto dto, NodeType nodeType, boolean getter) {
    ProperyKey key = new ProperyKey(dto.getClass().getName(), nodeType.getName(), getter);
    Method method = methodCache.get(key);
    if (method != null) {
      return method;
    }
    final String propertyName = nodeType.getName().toLowerCase();
    for (Method m: dto.getClass().getMethods()) {
      final String name = m.getName().toLowerCase();
      if (getter) {
        if ((name.startsWith("get") || name.startsWith("is")) && name.contains( propertyName )) {
           methodCache.put(key, m);
           return m;
        }
      }
      else {
        if (name.startsWith("set") && name.contains(propertyName)) {
          methodCache.put(key, m);
          return m;
        }
      }
    }
    throw new SortRuntimeException("Could not find property for nodeType '" + nodeType.getName() + "'");
  }

  public void setProperty(BaseDto dto, NodeType nodeType, Object value) {
    Method method = findMethod(dto, nodeType, false);
    invoke(method, dto, value);
  }

  public void setCollectionFetched(BaseDto dto, NodeType nodeType, boolean fetched) {
    DtoList<?> list = callPropertyGetter(dto, nodeType);
    list.setFetched(fetched);
  }

  public void addToCollection(BaseDto dto, NodeType nodeType, BaseDto reffedDto) {
    DtoList<BaseDto> list = callPropertyGetter(dto, nodeType);
    list.add(reffedDto);
  }

  private static class ProperyKey {
    private final String className;
    private final String propertyName;
    private final boolean getter;
    public ProperyKey(String className, String propertyName, boolean getter) {
      this.className = className;
      this.propertyName = propertyName;
      this.getter = getter;
    }
    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((className == null) ? 0 : className.hashCode());
      result = prime * result + (getter ? 1231 : 1237);
      result = prime * result + ((propertyName == null) ? 0 : propertyName.hashCode());
      return result;
    }
    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      ProperyKey other = (ProperyKey) obj;
      if (className == null) {
        if (other.className != null)
          return false;
      } else if (!className.equals(other.className))
        return false;
      if (getter != other.getter)
        return false;
      if (propertyName == null) {
        if (other.propertyName != null)
          return false;
      } else if (!propertyName.equals(other.propertyName))
        return false;
      return true;
    }
  }

}
