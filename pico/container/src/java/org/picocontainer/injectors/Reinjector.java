/*****************************************************************************
 * Copyright (C) 2003-2010 PicoContainer Committers. All rights reserved.    *
 * ------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the BSD      *
 * style license a copy of which has been included with this distribution in *
 * the LICENSE.txt file.                                                     *
 *                                                                           *
 *****************************************************************************/
package org.picocontainer.injectors;

import org.picocontainer.*;
import org.picocontainer.lifecycle.NullLifecycleStrategy;
import org.picocontainer.monitors.NullComponentMonitor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Properties;

/**
 * A Reinjector allows methods on pre-instantiated classes to be invoked,
 * with appropriately scoped parameters.
 */
public class Reinjector {
    
    private final PicoContainer parent;
    private final ComponentMonitor monitor;
    private static NullLifecycleStrategy NO_LIFECYCLE = new NullLifecycleStrategy();
    private static Properties NO_PROPERTIES = new Properties();

    /**
     * Make a reinjector with a parent container from which to pull components to be reinjected to.
     * With this constructor, a NullComponentMonitor is used.
     * @param parentContainer the parent container
     */
    public Reinjector(PicoContainer parentContainer) {
        this(parentContainer, parentContainer instanceof ComponentMonitorStrategy
                ? ((ComponentMonitorStrategy) parentContainer).currentMonitor()
                : new NullComponentMonitor());
    }

    /**
     * Make a reinjector with a parent container from which to pull components to be reinjected to
     * @param parentContainer the parent container
     * @param monitor the monitor to use for 'instantiating' events
     */
    public Reinjector(PicoContainer parentContainer, ComponentMonitor monitor) {
        this.parent = parentContainer;
        this.monitor = monitor;
    }

    /**
     * Reinjecting into a method.
     * @param key the component-key from the parent set of components to inject into
     * @param reinjectionMethod the reflection method to use for injection.
     * @return the result of the reinjection-method invocation.
     */
    public Object reinject(Class<?> key, Method reinjectionMethod) {
        return reinject(key, key, parent.getComponent(key), NO_PROPERTIES, new MethodInjection(reinjectionMethod));
    }

    /**
     * Reinjecting into a method.
     * @param key the component-key from the parent set of components to inject into
     * @param reinjectionMethodEnum the enum for the reflection method to use for injection.
     * @return the result of the reinjection-method invocation.
     */
    public Object reinject(Class<?> key, Enum reinjectionMethodEnum) {
        return reinject(key, key, parent.getComponent(key), NO_PROPERTIES, new MethodInjection(toMethod(reinjectionMethodEnum)));
    }

    private Method toMethod(final Enum reinjectionMethodEnum) {
        Object methodOrException = AccessController.doPrivileged(new PrivilegedAction<Object>() {
            public Object run() {
                try {
                    return reinjectionMethodEnum.getClass().getMethod("toMethod").invoke(reinjectionMethodEnum);
                } catch (IllegalAccessException e) {
                    return new PicoCompositionException("Illegal access to " + reinjectionMethodEnum.name());
                } catch (InvocationTargetException e) {
                    return new PicoCompositionException("Invocation Target Exception " + reinjectionMethodEnum.name(), e.getCause());
                } catch (NoSuchMethodException e) {
                    return new PicoCompositionException("Expected generated method toMethod() on enum");
                }
            }
        });
        if (methodOrException instanceof Method) {
            return (Method) methodOrException;
        } else {
            throw (PicoCompositionException) methodOrException;
        }
    }

    /**
     * Reinjecting into a method.
     * @param key the component-key from the parent set of components to inject into (key and impl are the same)
     * @param reinjectionFactory the InjectionFactory to use for reinjection.
     * @return the result of the reinjection-method invocation.
     */
    public Object reinject(Class<?> key, InjectionFactory reinjectionFactory) {
        Object o = reinject(key, key, parent.getComponent(key), NO_PROPERTIES, reinjectionFactory);
        return o;
    }

    /**
     * Reinjecting into a method.
     * @param key the component-key from the parent set of components to inject into
     * @param impl the implementation of the component that is going to result.
     * @param reinjectionFactory the InjectionFactory to use for reinjection.
     * @return
     */
    public Object reinject(Class<?> key, Class<?> impl, InjectionFactory reinjectionFactory) {
        return reinject(key, impl, parent.getComponent(key), NO_PROPERTIES, reinjectionFactory);
    }

    /**
     * Reinjecting into a method.
     * @param key the component-key from the parent set of components to inject into
     * @param implementation the implementation of the component that is going to result.
     * @param instance the object that has the provider method to be invoked
     * @param reinjectionFactory the InjectionFactory to use for reinjection.
     * @return the result of the reinjection-method invocation.
     */
    public Object reinject(Class<?> key, Class implementation, Object instance, InjectionFactory reinjectionFactory) {
        return reinject(key, implementation, instance, NO_PROPERTIES, reinjectionFactory);
    }

    /**
     * Reinjecting into a method.
     * @param key the component-key from the parent set of components to inject into
     * @param implementation the implementation of the component that is going to result.
     * @param instance the object that has the provider method to be invoked
     * @param properties for reinjection
     * @param reinjectionFactory the InjectionFactory to use for reinjection.
     * @return the result of the reinjection-method invocation.
     */
    public Object reinject(Class<?> key, Class implementation, Object instance, Properties properties,
                           InjectionFactory reinjectionFactory) {
        Reinjection reinjection = new Reinjection(reinjectionFactory, parent);
        org.picocontainer.Injector injector = (org.picocontainer.Injector) reinjection.createComponentAdapter(
                monitor, NO_LIFECYCLE, properties, key, implementation, null);
        return injector.decorateComponentInstance(parent, null, instance);
    }

}
