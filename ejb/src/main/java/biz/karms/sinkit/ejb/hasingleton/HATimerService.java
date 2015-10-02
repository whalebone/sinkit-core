/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013, Red Hat, Inc. and/or its affiliates, and individual
 * contributors by the @authors tag. See the copyright.txt in the
 * distribution for a full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package biz.karms.sinkit.ejb.hasingleton;

import biz.karms.sinkit.ejb.IoCDeactivator;
import biz.karms.sinkit.ejb.virustotal.VirusTotalEnricher;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * HATimerService registers various timers
 * <p>
 * Butchered by:
 *
 * @author Michal Karm Babacek
 *         <p>
 *         Kudos:
 * @author Wolf-Dieter Fink
 * @author Ralf Battenfeld
 */
public class HATimerService implements Service<String> {
    private static final java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger(HATimerService.class.toString());
    public static final ServiceName SINGLETON_SERVICE_NAME = ServiceName.JBOSS.append("sinkit", "ha", "singleton", "timer");
    // Timers - JNDI
    public static final String JNDI_PATH_SCHEDULER_DEMO = "global/sinkit-ear/sinkit-ejb/SchedulerDemoBean!biz.karms.sinkit.ejb.hasingleton.SchedulerDemo";
    public static final String JNDI_PATH_VIRUSTOTAL = "global/sinkit-ear/sinkit-ejb/VirusTotalEnricherEJB!biz.karms.sinkit.ejb.virustotal.VirusTotalEnricher";
    public static final String JNDI_PATH_IOCDEACTIVATOR = "global/sinkit-ear/sinkit-ejb/IoCDeactivatorEJB!biz.karms.sinkit.ejb.IoCDeactivator";

    /**
     * A flag whether the service is started.
     */
    private final AtomicBoolean started = new AtomicBoolean(false);

    /**
     * @return the name of the server node
     */
    public String getValue() throws IllegalStateException, IllegalArgumentException {
        LOGGER.info(String.format("%s is %s at %s", HATimerService.class.getSimpleName(), (started.get() ? "started" : "not started"), System.getProperty("jboss.node.name")));
        return "";
    }

    public void start(StartContext arg0) throws StartException {
        if (!started.compareAndSet(false, true)) {
            throw new StartException("The service is still started!");
        }
        LOGGER.info("Start HASingleton timer service '" + this.getClass().getName() + "'");

        final String node = System.getProperty("jboss.node.name");
        try {
            InitialContext ic = new InitialContext();
            // Timers - Start
            ((SchedulerDemo) ic.lookup(JNDI_PATH_SCHEDULER_DEMO)).initialize("SCHEDULER_DEMO HASingleton timer @" + node + " " + new Date());
            ((VirusTotalEnricher) ic.lookup(JNDI_PATH_VIRUSTOTAL)).initialize("VIRUSTOTAL HASingleton timer @" + node + " " + new Date());
            ((IoCDeactivator) ic.lookup(JNDI_PATH_IOCDEACTIVATOR)).initialize("IOCDEACTIVATOR HASingleton timer @" + node + " " + new Date());
        } catch (NamingException e) {
            throw new StartException("Could not initialize timer", e);
        }
    }

    public void stop(StopContext arg0) {
        if (!started.compareAndSet(true, false)) {
            LOGGER.warning("The service '" + this.getClass().getName() + "' is not active!");
        } else {
            LOGGER.info("Stop HASingleton timer service '" + this.getClass().getName() + "'");
            try {
                InitialContext ic = new InitialContext();
                // Timers - Stop
                ((SchedulerDemo) ic.lookup(JNDI_PATH_SCHEDULER_DEMO)).stop();
                ((VirusTotalEnricher) ic.lookup(JNDI_PATH_VIRUSTOTAL)).stop();
                ((IoCDeactivator) ic.lookup(JNDI_PATH_IOCDEACTIVATOR)).stop();
            } catch (NamingException e) {
                LOGGER.info("Could not stop timer:" + e.getMessage());
            }
        }
    }
}
