/*****************************************************************************
 * Copyright (C) 2003-2010 PicoContainer Committers. All rights reserved.    *
 * ------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the BSD      *
 * style license a copy of which has been included with this distribution in *
 * the LICENSE.txt file.                                                     *
 *                                                                           *
 * Original code by                                                          *
 *****************************************************************************/
package org.picocontainer.gems.monitors.prefuse;

import org.picocontainer.gems.monitors.ComponentDependencyMonitor.Dependency;

/**
 * Interprets dependency-related events.
 * 
 * @author Peter Barry
 * @author Kent R. Spillner
 */
public interface ComponentDependencyListener {
    public void addDependency(Dependency dependency);
}
