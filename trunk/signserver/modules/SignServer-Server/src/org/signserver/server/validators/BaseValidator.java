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
package org.signserver.server.validators;

import org.signserver.server.BaseProcessable;

/**
 * Base class that all (document) validators can extend to cover basic in common
 * functionality.
 * 
 * @author Markus Kilås
 * @version $Id$
 */
public abstract class BaseValidator extends BaseProcessable implements IValidator {

}
