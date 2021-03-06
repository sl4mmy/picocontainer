/*****************************************************************************
 * Copyright (C) 2003-2010 PicoContainer Committers. All rights reserved.    *
 * ------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the BSD      *
 * style license a copy of which has been included with this distribution in *
 * the LICENSE.txt file.                                                     *
 *                                                                           *
 * Original code by Mike Rimov                                               *
 *****************************************************************************/
package org.picocontainer.gems.containers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.picocontainer.ComponentAdapter;
import org.picocontainer.ComponentFactory;
import org.picocontainer.ComponentMonitor;
import org.picocontainer.DefaultPicoContainer;
import org.picocontainer.LifecycleStrategy;
import org.picocontainer.MutablePicoContainer;
import org.picocontainer.Parameter;
import org.picocontainer.PicoCompositionException;
import org.picocontainer.PicoContainer;
import org.picocontainer.adapters.InstanceAdapter;
import org.picocontainer.behaviors.Stored;

/**
 * Normal PicoContainers are meant to be created, started, stopped, disposed and
 * garbage collected. The goal of this container is to reduce the number of
 * registration calls (and therefore objects created) in areas where performance
 * is key (for example, this might be used in NanoContainer request containers).
 * <p>
 * It accomplishes its goal in two ways: <br/> (1) Once a container is disposed
 * of, start() may be called again, allowing for recycling of the container. (This is default
 * behavior with a picocontainer)
 * </p>
 * <p>
 * (2) All instance registrations will be unregistered when stop is called. (For example,
 * HttpServletRequest would be removed), and all component adapter instance values
 * are flushed.
 * </p>
 * <h4>Container Storage</h4>
 * <p>It is still up to the builder of this container to decide where to store its reference.  Since
 * it is reusable, it needs to be stored someplace that doesn't easily expire.  Probably the most
 * common storage location would be ThreadLocal storage.  HttpSession might be another valid
 * storage location.</p>
 * @author Michael Rimov
 */
@SuppressWarnings("serial")
public class ReusablePicoContainer extends DefaultPicoContainer {

	private final List<ComponentAdapter<?>> instanceRegistrations = new ArrayList<ComponentAdapter<?>>();

	private final Map<ComponentAdapter<?>, Stored<?>> storedReferences = new HashMap<ComponentAdapter<?>, Stored<?>>();
	
	public ReusablePicoContainer() {
		super();
	}

	public ReusablePicoContainer(final ComponentFactory componentFactory,
			final LifecycleStrategy lifecycleStrategy, final PicoContainer parent,
			final ComponentMonitor componentMonitor) {
		super(componentFactory, lifecycleStrategy, parent, componentMonitor);
	}

	public ReusablePicoContainer(final ComponentFactory componentFactory,
			final LifecycleStrategy lifecycleStrategy, final PicoContainer parent) {
		super(componentFactory, lifecycleStrategy, parent);
	}

	public ReusablePicoContainer(final ComponentFactory componentFactory,
			final PicoContainer parent) {
		super(componentFactory, parent);
	}

	public ReusablePicoContainer(final ComponentFactory componentFactory) {
		super(componentFactory);
	}

	public ReusablePicoContainer(final ComponentMonitor monitor,
			final LifecycleStrategy lifecycleStrategy, final PicoContainer parent) {
		super(monitor, lifecycleStrategy, parent);
	}

	public ReusablePicoContainer(final ComponentMonitor monitor, final PicoContainer parent) {
		super(monitor, parent);
	}

	public ReusablePicoContainer(final ComponentMonitor monitor) {
		super(monitor);
	}

	public ReusablePicoContainer(final LifecycleStrategy lifecycleStrategy,
			final PicoContainer parent) {
		super(lifecycleStrategy, parent);
	}

	public ReusablePicoContainer(final PicoContainer parent) {
		super(parent);
	}

	@Override
	public MutablePicoContainer addComponent(final Object componentKey,
			final Object componentImplementationOrInstance,
			final Parameter... parameters) throws PicoCompositionException {
		
		if (componentKey == null) {
			throw new NullPointerException("componentKey");
		}
		
		if (componentImplementationOrInstance == null) {
			throw new NullPointerException("componentImplementationOrInstance");			
		}
		
		super.addComponent(componentKey, 
				componentImplementationOrInstance, 
				parameters);
		
		if (! (componentImplementationOrInstance instanceof Class)) {
			instanceRegistrations.add(super.getComponentAdapter(componentKey));
		} else {
			addStoredReference(componentKey);			
		}
		
		return this;
	}

	@Override
	public  MutablePicoContainer addComponent(final Object implOrInstance)
			throws PicoCompositionException {
		if ((implOrInstance instanceof Class)) {
			super.addComponent(implOrInstance);
			addStoredReference(implOrInstance);
			return this;
		} else {
			return this.addComponent(implOrInstance.getClass(), implOrInstance);
		}
	}

	/**
	 * Precalculates all references to Stored behaviors.
	 * @param key the object key.
	 */
	private void addStoredReference(final Object key) {
		ComponentAdapter<?> ca = this.getComponentAdapter(key);
		Stored<?> stored =  ca.findAdapterOfType(Stored.class);
	    if (stored != null) {
	    	storedReferences.put(ca, stored);
	    }
    }

	@Override
	public synchronized void stop() {
		super.stop();
		
		//Remove all instance registrations.
		for (ComponentAdapter<?> eachAdapter : this.instanceRegistrations) {
			this.removeComponent(eachAdapter.getComponentKey());
		}
		
		instanceRegistrations.clear();
		
		//Flush all remaining objects.
		for (Stored<?> eachStoredBehavior : this.storedReferences.values()) {
			eachStoredBehavior.flush();			
		}
	}

	@Override
    public MutablePicoContainer addAdapter(final ComponentAdapter<?> componentAdapter, final Properties properties) {
		super.addAdapter(componentAdapter, properties);
		if (componentAdapter.findAdapterOfType(InstanceAdapter.class) != null) {
			this.instanceRegistrations.add(componentAdapter);
		} else {
			this.addStoredReference(componentAdapter.getComponentKey());
		}
		
		return this;
    }

	@Override
    public MutablePicoContainer addAdapter(final ComponentAdapter<?> componentAdapter) {
		super.addAdapter(componentAdapter);
		if (componentAdapter.findAdapterOfType(InstanceAdapter.class) != null) {
			this.instanceRegistrations.add(componentAdapter);
		} else {
			this.addStoredReference(componentAdapter.getComponentKey());
		}
		
		return this;
    }
	
	private void removeLocalReferences(final ComponentAdapter<?> ca) {
		this.storedReferences.remove(ca);
	}

	@Override
    public <T> ComponentAdapter<T> removeComponent(final Object componentKey) {
		ComponentAdapter<T> result =  super.removeComponent(componentKey);
		if (result != null) {
			removeLocalReferences(result);
		}
		
		return result;
    }

	@Override
    public <T> ComponentAdapter<T> removeComponentByInstance(final T componentInstance) {
		ComponentAdapter<T> result =  super.removeComponentByInstance(componentInstance);
		if (result != null) {
			removeLocalReferences(result);
		}
		
		return result;
    }

    @Override
	public MutablePicoContainer makeChildContainer() {
        ReusablePicoContainer pc = new ReusablePicoContainer(componentFactory, lifecycleStrategy, this);
        addChildContainer(pc);
        return pc;
    }

}
