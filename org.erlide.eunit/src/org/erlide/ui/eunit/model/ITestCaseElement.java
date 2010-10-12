/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.erlide.ui.eunit.model;


/**
 * Represents a test case element.
 * <p>
 * This interface is not intended to be implemented by clients.
 * </p>
 * 
 * @since 3.3
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface ITestCaseElement extends ITestElement {

	/**
	 * The name of the eunit test function (always arity 0)
	 * 
	 * @return the function name
	 */
	public String getTestFunctionName();

	/**
	 * Returns the qualified type name of the module the test is contained in.
	 * 
	 * @return the qualified type name of the module the test is contained in.
	 */
	public String getTestModuleName();

}
