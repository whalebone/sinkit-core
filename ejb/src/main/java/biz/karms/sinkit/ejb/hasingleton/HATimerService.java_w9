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
import org.jboss.as.server.ServerEnvironment;
import org.jboss.logging.Logger;
import org.jboss.msc.service.*;
import org.jboss.msc.value.Value;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * HATimerService registers various timers
 *
 * Butchered by:
 * @author Michal Karm Babacek
 *
 * Kudos:
 * @author Wolf-Dieter Fink
 * @author Ralf Battenfeld
 */
public class HATimerService implements Service<NodeName> {
    // Timers - JNDI
    public static final String JNDI_PATH_SCHEDULER_DEMO = "global/sinkit-ear/sinkit-ejb/SchedulerDemoBean!biz.karms.sinkit.ejb.hasingleton.SchedulerDemo";
    public static final String JNDI_PATH_VIRUSTOTAL = "global/sinkit-ear/sinkit-ejb/VirusTotalEnricherEJB!biz.karms.sinkit.ejb.virustotal.VirusTotalEnricher";
    public static final String JNDI_PATH_IOCDEACTIVATOR = "global/sinkit-ear/sinkit-ejb/IoCDeactivatorEJB!biz.karms.sinkit.ejb.IoCDeactivator";

    public static final ServiceName DEFAULT_SERVICE_NAME = ServiceName.JBOSS.append("sinkit", "ha", "singleton", "default");
    public static final ServiceName QUORUM_SERVICE_NAME = ServiceName.JBOSS.append("sinkit", "ha", "singleton", "quorum");
    // Arbitrary
    public static final String NODE_1 = "nodeOne";
    public static final String NODE_2 = "nodeTwo";
    private static final Logger LOGGER = Logger.getLogger(HATimerService.class);
    private final Value<ServerEnvironment> env;
    private final AtomicBoolean started = new AtomicBoolean(false);

    public HATimerService(Value<ServerEnvironment> env) {
        this.env = env;
    }

    @Override
    public NodeName getValue() {
        if (!this.started.get()) {
            throw new IllegalStateException();
        }
        return new NodeName(this.env.getValue().getNodeName());
    }

    @Override
    public void start(StartContext context) throws StartException {
        if (!started.compareAndSet(false, true)) {
            throw new StartException("The service is still started.");
        }
        LOGGER.info("Start HASingleton timer service '" + this.getClass().getName() + "'.");
        try {
            InitialContext ic = new InitialContext();
            // Timers - Start
            ((SchedulerDemo) ic.lookup(JNDI_PATH_SCHEDULER_DEMO)).initialize("SCHEDULER_DEMO HASingleton timer @" + this.env.getValue().getNodeName() + " " + new Date());
            ((VirusTotalEnricher) ic.lookup(JNDI_PATH_VIRUSTOTAL)).initialize("VIRUSTOTAL HASingleton timer @" + this.env.getValue().getNodeName() + " " + new Date());
            ((IoCDeactivator) ic.lookup(JNDI_PATH_IOCDEACTIVATOR)).initialize("IOCDEACTIVATOR HASingleton timer @" + this.env.getValue().getNodeName() + " " + new Date());
        } catch (NamingException e) {
            throw new StartException("Could not initialize timer.", e);
        }
    }

    @Override
    public void stop(StopContext context) {
        if (!started.compareAndSet(true, false)) {
            LOGGER.warn("The service '" + this.getClass().getName() + "' is not active.");
        } else {
            LOGGER.info("Stop HASingleton timer service '" + this.getClass().getName() + "'.");
            try {
                InitialContext ic = new InitialContext();
                // Timers - Stop
                ((SchedulerDemo) ic.lookup(JNDI_PATH_SCHEDULER_DEMO)).stop();
                ((VirusTotalEnricher) ic.lookup(JNDI_PATH_VIRUSTOTAL)).stop();
                ((IoCDeactivator) ic.lookup(JNDI_PATH_IOCDEACTIVATOR)).stop();
            } catch (NamingException e) {
                LOGGER.error("Could not stop timer.", e);
            }
        }
    }
}
