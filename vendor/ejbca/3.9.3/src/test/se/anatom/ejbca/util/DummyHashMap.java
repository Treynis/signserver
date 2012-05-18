package se.anatom.ejbca.util;

import java.util.HashMap;

/**
 * @author tomasg
 * @version $Id: DummyHashMap.java 5585 2008-05-01 20:55:00Z anatom $
 */
public class DummyHashMap extends HashMap {
    public Object get(Object key) {
        return "dummy";
    }
}
