/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */

package org.jboss.as.console.client.shared.subsys.logging;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.core.SuspendableViewImpl;
import org.jboss.as.console.client.rbac.SecurityFramework;
import org.jboss.as.console.client.shared.subsys.logging.LoggingLevelProducer.LogLevelConsumer;
import org.jboss.as.console.client.widgets.forms.ApplicationMetaData;
import org.jboss.as.console.client.widgets.pages.PagedView;
import org.jboss.as.console.client.widgets.tabs.DefaultTabLayoutPanel;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.dmr.client.dispatch.DispatchAsync;

import javax.inject.Inject;
import java.util.*;

/**
 * Main view class for the Logging subsystem.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2011 Red Hat Inc.
 */
public class LoggingView extends SuspendableViewImpl implements LoggingPresenter.MyView {

    private DispatchAsync dispatcher;
    private RootLoggerSubview rootLoggerSubview;
    private LoggerSubview loggerSubview;

    private final Map<String, AbstractHandlerSubview<?>> supportedHandlers;


    @Inject
    public LoggingView(ApplicationMetaData applicationMetaData, DispatchAsync dispatcher,
                       HandlerListManager handlerListManager, SecurityFramework securityFramework) {
        this.dispatcher = dispatcher;

        rootLoggerSubview = new RootLoggerSubview(applicationMetaData, dispatcher);
        loggerSubview = new LoggerSubview(applicationMetaData, dispatcher);

        ConsoleHandlerSubview consoleHandlerSubview =
                new ConsoleHandlerSubview(applicationMetaData, dispatcher, handlerListManager);
        FileHandlerSubview fileHandlerSubview =
                new FileHandlerSubview(applicationMetaData, dispatcher, handlerListManager);
        PeriodicRotatingFileHandlerSubview periodicRotatingFileHandlerSubview =
                new PeriodicRotatingFileHandlerSubview(applicationMetaData, dispatcher, handlerListManager);
        PeriodicSizeRotatingFileHandlerSubview periodicSizeRotatingFileHandlerSubview =
                new PeriodicSizeRotatingFileHandlerSubview(applicationMetaData, dispatcher, handlerListManager);
        SizeRotatingFileHandlerSubview sizeRotatingFileHandlerSubview =
                new SizeRotatingFileHandlerSubview(applicationMetaData, dispatcher, handlerListManager);
        AsyncHandlerSubview asyncHandlerSubview =
                new AsyncHandlerSubview(applicationMetaData, dispatcher, handlerListManager);
        CustomHandlerSubview customHandlerSubview =
                new CustomHandlerSubview(applicationMetaData, dispatcher, handlerListManager);
        SyslogHandlerView syslogHandlerView =
                new SyslogHandlerView(applicationMetaData, dispatcher, handlerListManager);

        handlerListManager.addHandlerConsumers(rootLoggerSubview, loggerSubview, asyncHandlerSubview);

        // Setup list of supported consumer and handler
        supportedHandlers = new LinkedHashMap<>();
        supportedHandlers.put(Console.CONSTANTS.subsys_logging_console(), consoleHandlerSubview);
        supportedHandlers.put(Console.CONSTANTS.subsys_logging_file(), fileHandlerSubview);
        supportedHandlers.put(Console.CONSTANTS.subsys_logging_periodic(), periodicRotatingFileHandlerSubview);
        supportedHandlers.put(Console.CONSTANTS.subsys_logging_periodicSize(), periodicSizeRotatingFileHandlerSubview);
        supportedHandlers.put(Console.CONSTANTS.subsys_logging_size(), sizeRotatingFileHandlerSubview);
        supportedHandlers.put(Console.CONSTANTS.subsys_logging_async(), asyncHandlerSubview);
        supportedHandlers.put(Console.CONSTANTS.subsys_logging_custom(), customHandlerSubview);
        supportedHandlers.put("Syslog Handler", syslogHandlerView);

        // filter
        // TODO (hpehl) Right now the security context is 'misused' to get the list of supported handlers. This needs
        // to be replaced once a more MBUI related approach is in place.
        SecurityContext securityContext = securityFramework.getSecurityContext(NameTokens.Logger);
        if (securityContext != null) {
            for (Iterator<Map.Entry<String, AbstractHandlerSubview<?>>> iterator = supportedHandlers.entrySet().iterator(); iterator.hasNext(); ) {
                Map.Entry<String, AbstractHandlerSubview<?>> entry = iterator.next();
                HandlerProducer handlerProducer = entry.getValue();
                String check = "{selected.profile}/subsystem=logging/" + handlerProducer.getManagementModelType() + "=*";
                try {
                    if (!securityContext.getReadPrivilege(check).isGranted()) {
                        iterator.remove();
                    }
                } catch (RuntimeException e) {
                    // The address is not part of the security context -> remove it
                    iterator.remove();
                }
            }
        }

        // add
        handlerListManager.addHandlerProducers(supportedHandlers.values().toArray(new HandlerProducer[supportedHandlers.size()]));
    }

    @Override
    public Widget createWidget() {

        DefaultTabLayoutPanel loggersTabs = new DefaultTabLayoutPanel(40, Style.Unit.PX);
        loggersTabs.addStyleName("default-tabpanel");

        loggersTabs.add(rootLoggerSubview.asWidget(), rootLoggerSubview.getEntityDisplayName(), true);
        loggersTabs.add(loggerSubview.asWidget(), loggerSubview.getEntityDisplayName(), true);

        // log handler
        PagedView handlerPages = new PagedView(true);
        for (Map.Entry<String, AbstractHandlerSubview<?>> entry : supportedHandlers.entrySet()) {
            handlerPages.addPage(entry.getKey(), entry.getValue().asWidget());
        }
        loggersTabs.add(handlerPages.asWidget(), Console.CONSTANTS.subsys_logging_handler(), true);

        loggersTabs.selectTab(0);
        handlerPages.showPage(0);

        List<LogLevelConsumer> logLevelConsumer = new ArrayList<>();
        logLevelConsumer.add(rootLoggerSubview);
        logLevelConsumer.add(loggerSubview);
        logLevelConsumer.addAll(supportedHandlers.values());
        LoggingLevelProducer.setLogLevels(dispatcher,
                logLevelConsumer.toArray(new LogLevelConsumer[logLevelConsumer.size()]));
        return loggersTabs;
    }

    public void initialLoad() {
        rootLoggerSubview.initialLoad();
        loggerSubview.initialLoad();

        for (AbstractHandlerSubview view : supportedHandlers.values()) {
            view.initialLoad();
        }
    }
}
