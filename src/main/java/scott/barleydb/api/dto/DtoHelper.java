package scott.barleydb.api.dto;

/*-
 * #%L
 * BarleyDB
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2014 - 2017 Scott Sinclair
 *       <scottysinclair@gmail.com>
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import scott.barleydb.api.config.EntityType;
import scott.barleydb.api.config.NodeType;
import scott.barleydb.api.core.Environment;
import scott.barleydb.api.exception.BarleyDBRuntimeException;

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
  public <T> T callPropertyGetter(BaseDto dto, NodeType nodeType) {
    Method method = findMethod(dto, nodeType, true);
    return (T)invoke(method, dto);
  }

  private Object invoke(Method method, BaseDto dto, Object ...args) {
    try {
      return method.invoke(dto, args);
    }
    catch(InvocationTargetException x) {
      throw new BarleyDBRuntimeException("DTO method '" + method.getName()+ "'  threw exception", x.getTargetException());
    }
    catch(IllegalAccessException x) {
      throw new BarleyDBRuntimeException("Error accessing DTO method '" + method.getName()+ "'", x);
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
        if (name.equals("get" + propertyName) || name.equals( "is" + propertyName )) {
           methodCache.put(key, m);
           return m;
        }
      }
      else {
        if (name.equals("set" + propertyName)) {
          methodCache.put(key, m);
          return m;
        }
      }
    }
    throw new BarleyDBRuntimeException("Could not find property for nodeType '" + nodeType.getName() + "'");
  }

  public void setProperty(BaseDto dto, NodeType nodeType, Object value) {
    Method method = findMethod(dto, nodeType, false);
    invoke(method, dto, value);
  }

  public void setCollectionFetched(BaseDto dto, NodeType nodeType, boolean fetched) {
    DtoList<?> list = callPropertyGetter(dto, nodeType);
    list.setFetched(fetched);
  }

  public void clearCollection(BaseDto dto, NodeType nodeType, BaseDto reffedDto) {
    DtoList<BaseDto> list = callPropertyGetter(dto, nodeType);
    list.clear();
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
