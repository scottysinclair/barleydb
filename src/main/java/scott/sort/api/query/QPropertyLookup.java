package scott.sort.api.query;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import scott.sort.api.exception.model.QPropertyInvalidException;
import scott.sort.api.exception.model.QPropertyMissingException;

/**
 * Allows QProperties to be looked up by name
 * @author scott
 *
 */
class QPropertyLookup implements Serializable {

    private static final long serialVersionUID = 1L;

    private final QueryObject<?> queryObject;
    /**
     * The cache state is not serialized as it contains methods
     * rather the cache is lazily built and so is just built again on first
     * use after deserialization.
     */
    private transient boolean loadedMap;
    private transient Map<String,Method> map;

    public QPropertyLookup(QueryObject<?> queryObject) throws QPropertyInvalidException {
        this.queryObject = queryObject;
        this.map = new HashMap<String, Method>();
        this.loadedMap = false;
    }

    private void loadMap() throws QPropertyInvalidException {
        if (loadedMap) {
            return;
        }
        /*
         * Even if the rest of this code block fails we do not attempt to reload
         * so we set loadedMap to true here.
         */
        loadedMap = true;
        for (Method method: queryObject.getClass().getMethods()) {
            if (method.getReturnType().equals(QProperty.class) && method.getParameterTypes().length == 0) {
                /*
                 *We test the method out to see if it returns a QProperty as expected
                 */
                QProperty<?> property = newQProperty(method);
                /*
                 * If so store this method under the QProperty name
                 */
                map.put(property.getName(), method);
            }
        }
    }

    public QProperty<?> getProperty(String propertyName) throws QPropertyMissingException, QPropertyInvalidException {
        loadMap();
        Method method = map.get(propertyName);
        if (method == null) {
            throw new QPropertyMissingException(queryObject, propertyName);
        }
        return newQProperty(method);
    }

    private QProperty<?> newQProperty(Method method) throws QPropertyInvalidException {
        try {
            QProperty<?> property = (QProperty<?>) method.invoke(queryObject);
            if (property == null) {
                throw new QPropertyInvalidException("Did not get query propery when invoking method: " + method.getName());
            }
            return property;
        }
        catch (IllegalAccessException | IllegalArgumentException x) {
            throw new QPropertyInvalidException("Error calling method '"  + method.getName() + "'", x);
        }
        catch (InvocationTargetException x) {
            throw new QPropertyInvalidException("Method '"  + method.getName() + "' threw exception", x.getTargetException());
        }
    }

    private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        loadedMap = false;
        map = new HashMap<String, Method>();
    }

}
