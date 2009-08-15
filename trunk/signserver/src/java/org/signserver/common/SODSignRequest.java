/*************************************************************************
 *                                                                       *
 *  SignServer: The OpenSource Automated Signing Server                  *
 *                                                                       *
 *  This software is free software; you can redistribute it and/or       *
 *  modify it under the terms of the GNU Lesser General Public           *
 *  License as published by the Free Software Foundation; either         *
 *  version 2.1 of the License, or any later version.                    *
 *                                                                       *
 *  See terms of license at gnu.org.                                     *
 *                                                                       *
 *************************************************************************/
package org.signserver.common;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * 
 * 
 * $Id:$
 */
public class SODSignRequest extends ProcessRequest implements ISignRequest {

    private static final long serialVersionUID = 1L;
    private int requestID;
    private Map<Integer, byte[]> dataGroupHashes;

    /**
     * Default constructor used during serialization
     */
    public SODSignRequest() {
    }

    /**
     * Main constuctor.
     *
     * @param requestID a unique id of the request
     * @param dataGroups the dataData hashes to sign
     */
    public SODSignRequest(int requestID, Map<Integer, byte[]> dataGroupHashes) {
        this.requestID = requestID;
        this.dataGroupHashes = dataGroupHashes;
    }

    /**
     *
     * @see org.signserver.common.ProcessRequest#getRequestID()
     */
    public int getRequestID() {
        return requestID;
    }

    /**
     * Returns the signed data as an ArrayList of document objects to sign.
     */
    public Object getRequestData() {
        return getDataGroupHashes();
    }

    public Map<Integer, byte[]> getDataGroupHashes() {
        return dataGroupHashes;
    }

    public void parse(DataInput in) throws IOException {
        in.readInt();
        this.requestID = in.readInt();
        int mapSize = in.readInt();
        this.dataGroupHashes = new HashMap<Integer, byte[]>(mapSize);
        for (int i = 0; i < mapSize; i++) {
            int key = in.readInt();
            int valueSize = in.readInt();
            byte[] value = new byte[valueSize];
            in.readFully(value);
            dataGroupHashes.put(key, value);
        }
    }

    public void serialize(DataOutput out) throws IOException {
        out.writeInt(RequestAndResponseManager.REQUESTTYPE_SODSIGNREQUEST);
        out.writeInt(this.requestID);
        out.writeInt(this.dataGroupHashes.size());
        for(Map.Entry<Integer, byte[]> entry : dataGroupHashes.entrySet()) {
            out.writeInt(entry.getKey().intValue());
            out.writeInt(entry.getValue().length);
            out.write(entry.getValue());
        }
    }
}
