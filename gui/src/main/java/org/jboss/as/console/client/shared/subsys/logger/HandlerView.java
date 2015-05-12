package org.jboss.as.console.client.shared.subsys.logger;

import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.as.console.client.widgets.pages.PagedView;
import org.jboss.dmr.client.Property;

import java.util.List;

/**
 * @author Heiko Braun
 */
public class HandlerView {

    private LoggerPresenter presenter;

    private PagedView panel;

    private static final AddressTemplate CONSOLE =
            AddressTemplate.of("{selected.profile}/subsystem=logging/console-handler=*");


    private static final AddressTemplate FILE =
            AddressTemplate.of("{selected.profile}/subsystem=logging/file-handler=*");


    private static final AddressTemplate ASYNC =
            AddressTemplate.of("{selected.profile}/subsystem=logging/async-handler=*");


    private static final AddressTemplate CUSTOM =
            AddressTemplate.of("{selected.profile}/subsystem=logging/custom-handler=*");


    private static final AddressTemplate PERIODIC_FILE =
            AddressTemplate.of("{selected.profile}/subsystem=logging/periodic-rotating-file-handler=*");


    private static final AddressTemplate PERIODIC_SIZE =
            AddressTemplate.of("{selected.profile}/subsystem=logging/periodic-size-rotating-file-handler=*");

    private static final AddressTemplate SIZE =
            AddressTemplate.of("{selected.profile}/subsystem=logging/size-rotating-file-handler=*");

    private static final AddressTemplate SYSYLOG =
            AddressTemplate.of("{selected.profile}/subsystem=logging/syslog-handler=*");

    private MasterDetailTemplate consoleHandler;
    private MasterDetailTemplate fileHandler;
    private MasterDetailTemplate periodicHandler;
    private MasterDetailTemplate sizeHandler;
    private MasterDetailTemplate asyncHandler;
    private MasterDetailTemplate customHandler;
    private MasterDetailTemplate syslogHandler;
    private MasterDetailTemplate periodicSizeHandler;

    public HandlerView(LoggerPresenter presenter) {
        this.presenter = presenter;
    }

    public Widget asWidget() {

        panel = new PagedView(true);

        consoleHandler = new MasterDetailTemplate(presenter, CONSOLE, "Console Handler");
        fileHandler = new MasterDetailTemplate(presenter, FILE, "File Handler");
        periodicHandler = new MasterDetailTemplate(presenter, PERIODIC_FILE, "Periodic Handler");
        periodicSizeHandler = new MasterDetailTemplate(presenter, PERIODIC_SIZE, "Periodic Size Handler");
        sizeHandler = new MasterDetailTemplate(presenter, SIZE, "Size Handler");
        asyncHandler = new MasterDetailTemplate(presenter, ASYNC, "Async Handler");
        customHandler = new MasterDetailTemplate(presenter, CUSTOM, "Custom Handler");
        syslogHandler = new MasterDetailTemplate(presenter, SYSYLOG, "Syslog Handler");

        panel.addPage("Console", consoleHandler.asWidget());
        panel.addPage("File", fileHandler.asWidget());
        panel.addPage("Periodic", periodicHandler.asWidget());
        panel.addPage("Periodic Size", periodicSizeHandler.asWidget());
        panel.addPage("Size", sizeHandler.asWidget());
        panel.addPage("Async", asyncHandler.asWidget());
        panel.addPage("Custom", customHandler.asWidget());
        panel.addPage("Syslog", syslogHandler.asWidget());

        // default page
        panel.showPage(0);

        return panel.asWidget();
    }



    public void updateConsoleHandler(List<Property> items) {
        consoleHandler.setData(items);
    }

    public void updateFileHandler(List<Property> items) {
        fileHandler.setData(items);
    }
    public void updatePeriodicHandler(List<Property> items) {
        periodicHandler.setData(items);
    }
    public void updatePeriodicSizeHandler(List<Property> items) {
           periodicSizeHandler.setData(items);
       }
    public void updateSizeHandlerHandler(List<Property> items) {
        sizeHandler.setData(items);
    }
    public void updateAsyncHandler(List<Property> items) {
        asyncHandler.setData(items);
    }
    public void updateCustomHandler(List<Property> items) {
        customHandler.setData(items);
    }
    public void updateSyslogHandler(List<Property> items) {
        syslogHandler.setData(items);
    }

}
