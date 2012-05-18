
package org.ejbca.core.protocol.ws.jaxws;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.ejbca.core.protocol.ws.objects.KeyStore;

@XmlRootElement(name = "pkcs12ReqResponse", namespace = "http://ws.protocol.core.ejbca.org/")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "pkcs12ReqResponse", namespace = "http://ws.protocol.core.ejbca.org/")
public class Pkcs12ReqResponse {

    @XmlElement(name = "return", namespace = "")
    private KeyStore _return;

    /**
     * 
     * @return
     *     returns KeyStore
     */
    public KeyStore get_return() {
        return this._return;
    }

    /**
     * 
     * @param _return
     *     the value for the _return property
     */
    public void set_return(KeyStore _return) {
        this._return = _return;
    }

}
