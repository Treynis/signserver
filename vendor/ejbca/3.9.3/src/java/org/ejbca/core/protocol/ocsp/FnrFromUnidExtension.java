/*************************************************************************
 *                                                                       *
 *  EJBCA: The OpenSource Certificate Authority                          *
 *                                                                       *
 *  This software is free software; you can redistribute it and/or       *
 *  modify it under the terms of the GNU Lesser General Public           *
 *  License as published by the Free Software Foundation; either         *
 *  version 2.1 of the License, or any later version.                    *
 *                                                                       *
 *  See terms of license at gnu.org.                                     *
 *                                                                       *
 *************************************************************************/

package org.ejbca.core.protocol.ocsp;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.DERIA5String;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERObjectIdentifier;

/** The ASN.1 extension with OID 2.16.578.1.16.3.2 used to request an FNR from a UNID 
 * and respond with the FNR. When requesting, the fnr passed in the extension deas not matter, use 1.
 * 
 * id-fnrFromUnid OBJECT IDENTIFIER ::= { 2 16 578 1 16 3 2 }
 * 
 * FnrFromUnid ::= Fnr 
 * 
 * Fnr ::= IA5String
 * 
 * @author tomas
 * @version $Id: FnrFromUnidExtension.java 5585 2008-05-01 20:55:00Z anatom $
 *
 */
public class FnrFromUnidExtension extends ASN1Encodable {

	public static final DERObjectIdentifier FnrFromUnidOid = new DERObjectIdentifier("2.16.578.1.16.3.2");
	
    private String fnr;

    public static FnrFromUnidExtension getInstance(
        Object obj)
    {
        if (obj == null || obj instanceof FnrFromUnidExtension) 
        {
            return (FnrFromUnidExtension)obj;
        }
        
        if (obj instanceof DERIA5String) 
        {
            return new FnrFromUnidExtension((DERIA5String)obj);
        }
        
        throw new IllegalArgumentException("Invalid FnrFromUnidExtension: " + obj.getClass().getName());
    }
    
    public FnrFromUnidExtension(
        String nr)
    {
        this.fnr=nr;
    }

    public FnrFromUnidExtension(
    		DERIA5String  nr)
    {
        this.fnr=nr.getString();

    }

    public String getFnr()
    {
        return fnr;
    }

    public DERObject toASN1Object()
    {
        return new DERIA5String(fnr);
    }

}
