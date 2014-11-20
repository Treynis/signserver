package xades4j.xml.marshalling.algorithms;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.xml.security.utils.Constants;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import xades4j.algorithms.XPath2FilterTransform;
import xades4j.algorithms.XPath2FilterTransform.XPath2Filter;
import xades4j.utils.SignatureServicesTestBase;

/**
 *
 * @author Luís
 */
public class XPath2FilterTransformParamsMarshallerTest {

    private Document doc;
    private XPath2FilterTransformParamsMarshaller sut;

    @Before
    public void setUp() throws Exception {
        doc = SignatureServicesTestBase.getNewDocument();
        sut = new XPath2FilterTransformParamsMarshaller();
    }

    @Test
    public void testMarshalXPathParametersWithNamespacePrefixes() throws Exception {
        XPath2FilterTransform xpath = XPath2Filter
                .intersect("foo:elem1")
                .union("bar:elem2")
                .withNamespace("foo", "http://test.xades4j/ns1")
                .withNamespace("bar", "http://test.xades4j/ns2");

        List<Node> params = sut.marshalParameters(xpath, doc);
        assertEquals(2, params.size());

        Set<Map.Entry<String, String>> namespaces = xpath.getNamespaces().entrySet();

        for (Node paramNode : params) 
        {
            for (Map.Entry<String, String> entry : namespaces) 
            {
                String ns = ((Element)paramNode).getAttributeNS(Constants.NamespaceSpecNS, entry.getKey());
                assertNotNull(ns);
                assertFalse(ns.isEmpty());
                assertEquals(entry.getValue(), ns);
            }
        }
    }
}
