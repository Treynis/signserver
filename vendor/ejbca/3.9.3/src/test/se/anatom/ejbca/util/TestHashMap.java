package se.anatom.ejbca.util;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;

import junit.framework.TestCase;

import org.apache.commons.lang.StringUtils;
import org.ejbca.util.Base64GetHashMap;
import org.ejbca.util.Base64PutHashMap;

/** Tests Base64 HashMap XML encoding and decoding
 * 
 * @author tomasg
 * @version $Id: TestHashMap.java 5585 2008-05-01 20:55:00Z anatom $
 */
public class TestHashMap extends TestCase {
    //private static final Logger log = Logger.getLogger(TestHashMap.class);

    public TestHashMap(String name) {
        super(name);
    }

    protected void setUp() throws Exception {
    }

    protected void tearDown() throws Exception {
    }

	public void test01HashMapNormal() throws Exception {
        HashMap a = new HashMap();
        a.put("foo0", Boolean.valueOf(false));
        a.put("foo1", "fooString");
        a.put("foo2", new Integer(2));
        a.put("foo3", Boolean.valueOf(true));
        
        // Write to XML
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        java.beans.XMLEncoder encoder = new java.beans.XMLEncoder(baos);
        encoder.writeObject(a);
        encoder.close();
        String data = baos.toString("UTF8");
        //log.error(data);
        
        java.beans.XMLDecoder decoder = new  java.beans.XMLDecoder(new java.io.ByteArrayInputStream(data.getBytes("UTF8")));
        HashMap b = (HashMap) decoder.readObject();
        decoder.close();
        assertEquals(((Boolean)b.get("foo0")).booleanValue(),false);
        assertEquals(((Boolean)b.get("foo3")).booleanValue(),true);
        assertEquals(((String)b.get("foo1")),"fooString");
        assertEquals(((Integer)b.get("foo2")).intValue(),2);

	}
	
    public void test01HashMapStrangeChars() throws Exception {
        HashMap a = new HashMap();
        a.put("foo0", Boolean.valueOf(false));
        a.put("foo1", "\0001\0002fooString");
        a.put("foo2", new Integer(2));
        a.put("foo3", Boolean.valueOf(true));
        
        // Write to XML
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        java.beans.XMLEncoder encoder = new java.beans.XMLEncoder(baos);
        encoder.writeObject(a);
        encoder.close();
        String data = baos.toString("UTF8");
        //log.error(data);

        try {
            java.beans.XMLDecoder decoder = new  java.beans.XMLDecoder(new java.io.ByteArrayInputStream(data.getBytes("UTF8")));
            HashMap b = (HashMap) decoder.readObject();
            decoder.close();         
            assertEquals(((Boolean)b.get("foo0")).booleanValue(),false);
        // We can get two different errors, I don't know if it is different java versions or what...
        // The important thing is that we do expect an error to occur here
        } catch (ClassCastException e) {
            return;
        } catch (ArrayIndexOutOfBoundsException e) {
            return;
        }
        String javaver = System.getProperty("java.version");
        //log.error(javaver);
        if (StringUtils.contains(javaver, "1.6")) {
        	// In java 1.6 the above does work because it encodes the special characters
        	//   <string><char code="#0"/>1<char code="#0"/>2fooString</string> 
            assertTrue(true);        	
        } else {
        	// In java 1.5 the above does not work, because it will insert bad xml-characters 
        	// so the test will fail if we got here.
            assertTrue(false);        	        	
        }
    }
    public void test01HashMapStrangeCharsSafe() throws Exception {
        HashMap h = new HashMap();
        h.put("foo0", Boolean.valueOf(false));
        h.put("foo1", "\0001\0002fooString");
        h.put("foo2", new Integer(2));
        h.put("foo3", Boolean.valueOf(true));
        h.put("foo4", "");
        HashMap a = new Base64PutHashMap();
        a.putAll(h);
        
        // Write to XML
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        java.beans.XMLEncoder encoder = new java.beans.XMLEncoder(baos);
        encoder.writeObject(a);
        encoder.close();
        String data = baos.toString("UTF8");
        //log.error(data);

        try {
            java.beans.XMLDecoder decoder = new  java.beans.XMLDecoder(new java.io.ByteArrayInputStream(data.getBytes("UTF8")));
            HashMap b = (HashMap) decoder.readObject();
            decoder.close();    
            HashMap c = new Base64GetHashMap(b);
            assertEquals(((Boolean)c.get("foo0")).booleanValue(),false);
            assertEquals(((Boolean)c.get("foo3")).booleanValue(),true);
            assertEquals(((String)c.get("foo1")),"\0001\0002fooString");
            assertEquals(((String)c.get("foo4")),"");
            assertEquals(((Integer)c.get("foo2")).intValue(),2);
            
        } catch (ClassCastException e) {
            assertTrue(false);
        }
    }
    public void test01HashMapNormalCharsSafe() throws Exception {
        HashMap h = new HashMap();
        h.put("foo0", Boolean.valueOf(false));
        h.put("foo1", "fooString");
        h.put("foo2", new Integer(2));
        h.put("foo3", Boolean.valueOf(true));
        h.put("foo4", "");
        HashMap a = new Base64PutHashMap();
        a.putAll(h);
        
        // Write to XML
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        java.beans.XMLEncoder encoder = new java.beans.XMLEncoder(baos);
        encoder.writeObject(a);
        encoder.close();
        String data = baos.toString("UTF8");
        //log.error(data);

        try {
            java.beans.XMLDecoder decoder = new  java.beans.XMLDecoder(new java.io.ByteArrayInputStream(data.getBytes("UTF8")));
            HashMap b = (HashMap) decoder.readObject();
            decoder.close();    
            HashMap c = new Base64GetHashMap(b);
            assertEquals(((Boolean)c.get("foo0")).booleanValue(),false);
            assertEquals(((Boolean)c.get("foo3")).booleanValue(),true);
            assertEquals(((String)c.get("foo4")),"");
            assertEquals(((String)c.get("foo1")),"fooString");
            assertEquals(((Integer)c.get("foo2")).intValue(),2);
            
        } catch (ClassCastException e) {
            assertTrue(false);
        }
    }
}
