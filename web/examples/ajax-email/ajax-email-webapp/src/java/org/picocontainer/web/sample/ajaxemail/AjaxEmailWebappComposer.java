package org.picocontainer.web.sample.ajaxemail;

import static org.picocontainer.Characteristics.USE_NAMES;

import javax.servlet.ServletContext;

import org.picocontainer.MutablePicoContainer;
import org.picocontainer.web.WebappComposer;
//import org.picocontainer.web.caching.FallbackCacheProvider;
import org.picocontainer.web.remoting.PicoWebRemotingMonitor;
import org.picocontainer.web.sample.ajaxemail.persistence.InMemoryPersister;
import org.picocontainer.web.sample.ajaxemail.persistence.JdoPersister;
import org.picocontainer.web.sample.ajaxemail.persistence.Persister;

public class AjaxEmailWebappComposer implements WebappComposer {

    public void composeApplication(MutablePicoContainer container, ServletContext context) {
        container.addComponent(PicoWebRemotingMonitor.class, AjaxEmailWebRemotingMonitor.class);
        container.addComponent(UserStore.class);
        container.addComponent(Persister.class, getPersisterClass());
        container.addComponent(QueryStore.class);
//        container.addAdapter(new FallbackCacheProvider());
    }

    public void composeSession(MutablePicoContainer container) {
        // stateless
    }

    public void composeRequest(MutablePicoContainer container) {

        container.addAdapter(new CookieUserProviderAdapter());
        container.as(USE_NAMES).addComponent(Auth.class);
        container.as(USE_NAMES).addComponent(Inbox.class);
        container.as(USE_NAMES).addComponent(Sent.class);
        container.addComponent(SampleData.class);

    }

    /**
     * Some ugliness to determine if the user is deploying the app via "maven jetty:run-war"
     * @return
     */
    private Class<? extends Persister> getPersisterClass() {
        boolean isMaven = false;
        try {
            throw new RuntimeException();
        } catch (RuntimeException re) {
            for (StackTraceElement ste : re.getStackTrace()) {
                if (ste.getClassName().contains("maven")) {
                    isMaven = true;
                }
            }
        }
        if (isMaven) {
            return InMemoryPersister.class;
        } else {
            return JdoPersister.class;
        }

    }

}
