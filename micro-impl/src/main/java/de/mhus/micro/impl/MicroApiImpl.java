package de.mhus.micro.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.EventAdmin;

import de.mhus.lib.core.IProperties;
import de.mhus.lib.core.MLog;
import de.mhus.lib.core.config.IConfig;
import de.mhus.lib.core.operation.OperationDescription;
import de.mhus.micro.api.MicroApi;
import de.mhus.micro.api.client.MicroDiscoverer;
import de.mhus.micro.api.client.MicroExecutor;
import de.mhus.micro.api.client.MicroFilter;
import de.mhus.micro.api.client.MicroOperation;
import de.mhus.micro.api.client.MicroResult;
import de.mhus.micro.api.server.MicroPusher;
import de.mhus.osgi.api.MOsgi;
import de.mhus.osgi.api.util.MServiceTracker;

@Component
public class MicroApiImpl extends MLog implements MicroApi {

    private MServiceTracker<MicroExecutor> executorTracker = null;
    private List<MicroExecutor> executors = Collections.synchronizedList(new LinkedList<MicroExecutor>());
    
    private MServiceTracker<MicroDiscoverer> discoverTracker = null;
    private List<MicroDiscoverer> discoverers = Collections.synchronizedList(new LinkedList<MicroDiscoverer>());
    
    @Reference
    private EventAdmin eventAdmin;
    
    @Activate
    public void doActivate(BundleContext ctx) {
        executorTracker = new MServiceTracker<MicroExecutor>(ctx, MicroExecutor.class) {

            @Override
            protected void removeService(ServiceReference<MicroExecutor> reference, MicroExecutor service) {
                executors.remove(service);
            }

            @Override
            protected void addService(ServiceReference<MicroExecutor> reference, MicroExecutor service) {
                executors.add(service);
            }
        };
        executorTracker.start();
        
        discoverTracker = new MServiceTracker<MicroDiscoverer>(ctx, MicroDiscoverer.class) {

            @Override
            protected void removeService(ServiceReference<MicroDiscoverer> reference, MicroDiscoverer service) {
                discoverers.remove(service);
            }

            @Override
            protected void addService(ServiceReference<MicroDiscoverer> reference, MicroDiscoverer service) {
                discoverers.add(service);
            }
        };
        discoverTracker.start();
        
    }
    
    @Deactivate
    public void doDeactivate() {
        if (executorTracker != null)
            executorTracker.stop();
        executorTracker = null;
        if (discoverTracker != null)
            discoverTracker.stop();
        discoverTracker = null;
    }

    @Override
    public List<MicroResult> execute(MicroFilter filter, IConfig arguments, IProperties properties) {
        ArrayList<MicroOperation> list = new ArrayList<>();
        operations(filter, list);
        List<MicroResult> out = new ArrayList<>();
        for (MicroOperation oper : list) {
            IConfig res = oper.execute(arguments);
            if (res != null)
                out.add(new MicroResult(oper.getDescription(), res));
        }
        return out;
    }

    @Override
    public void operations(MicroFilter filter, List<MicroOperation> results) {
        executors.forEach(p -> p.list(filter, results) );
    }

    @Override
    public void discover(MicroFilter filter, List<OperationDescription> results) {
        discoverers.forEach(d -> d.list(filter, results));
    }
    
    @Override
    public List<MicroPusher> getPushers() {
        return MOsgi.getServices(MicroPusher.class, null);
    }

}