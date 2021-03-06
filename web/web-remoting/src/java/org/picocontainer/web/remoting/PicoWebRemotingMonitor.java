/*******************************************************************************
 * Copyright (C) 2003-2010 PicoContainer Committers. All rights reserved.
 * ---------------------------------------------------------------------------
 * The software in this package is published under the terms of the BSD style
 * license a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 ******************************************************************************/
package org.picocontainer.web.remoting;

import org.picocontainer.PicoCompositionException;

public interface PicoWebRemotingMonitor {

    Object picoCompositionExceptionForMethodInvocation(PicoCompositionException e);

    Object runtimeExceptionForMethodInvocation(RuntimeException e);

    Object nullParameterForMethodInvocation(String parameterName);
}
