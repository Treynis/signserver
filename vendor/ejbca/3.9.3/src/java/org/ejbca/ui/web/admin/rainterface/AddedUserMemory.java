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
 
/*
 * AddedUserMemory.java
 *
 * Created on den 27 juli 2002, 22:01
 */
package org.ejbca.ui.web.admin.rainterface;

import java.io.Serializable;
import java.util.Vector;


/**
 * A class used to remember a RA Admins last added users. It's use is in the adduser.jsp to list
 * previously added users give the RA admins a better overlook of the his work.
 *
 * @author TomSelleck
 * @version $Id: AddedUserMemory.java 5585 2008-05-01 20:55:00Z anatom $
 */
public class AddedUserMemory implements Serializable {
    // Public Constants
    public static final int MEMORY_SIZE = 100; // Remember the 100 last users. 

    // Public Methods

    /**
     * Creates a new instance of AddedUserMemory
     */
    public AddedUserMemory() {
        memory = new Vector();
    }

    /**
     * Used to add a user tho the memory
     *
     * @param user the UserView representation of the user to add.
     */
    public void addUser(UserView user) {
        memory.add(user);

        while (memory.size() > MEMORY_SIZE) {
            memory.remove(0);
        }
    }

    /**
     * Used to retrieve a number of previously added users.
     *
     * @param size the size of the array of users to return
     *
     * @return the 'size' or available users in memory.
     */
    public UserView[] getUsers(int size) {
        int endindex = memory.size() - size;
        int tempsize = size;
        UserView[] returnval;

        if (endindex < 0) {
            endindex = 0;
        }

        if (size > memory.size()) {
            tempsize = memory.size();
        }

        returnval = new UserView[tempsize];

        int j = 0;

        for (int i = memory.size() - 1; i >= endindex; i--) {
            returnval[j] = (UserView) memory.elementAt(i);
            j++;
        }

        return returnval;
    }

    /**
     * Used to update the data of a user.
     *
     * @param user the stringarray representation of the user to change.
     */
    public void changeUser(UserView user) {
        int i;

        // Find user in memory.
        for (i = 0; i < memory.size(); i++) {
            if (((UserView) memory.elementAt(i)).getUsername().equals(user.getUsername())) {
                memory.set(i, user);

                break;
            }
        }
    }

    // Private fields
    private Vector memory = null;
}
