package org.jboss.as.console.client.shared.subsys.logger;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.core.SuspendableViewImpl;
import org.jboss.as.console.client.widgets.tabs.DefaultTabLayoutPanel;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;

import java.util.List;

/**
 * @author Heiko Braun
 * @since 11/05/15
 */
public class LoggerView extends SuspendableViewImpl implements LoggerPresenter.MyView{

    private LoggerPresenter presenter;
    private RootLoggerView rootLoggerView;
    private CategoryView categoryView;
    private HandlerView handlerView;
    private FormatterView formatterView;

    @Override
    public void setPresenter(LoggerPresenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public void updateRootLogger(ModelNode payload) {
        rootLoggerView.setData(payload);
    }

    @Override
    public void updateLogger(List<Property> items) {
        categoryView.setData(items);
    }

    @Override
    public void updateHandler(List<Property> items) {

    }

    @Override
    public void updatePeriodicSizeHandler(List<Property> items) {
        handlerView.updatePeriodicSizeHandler(items);
    }

    @Override
    public void updateConsoleHandler(List<Property> items) {
        handlerView.updateConsoleHandler(items);
    }

    @Override
    public void updateFileHandler(List<Property> items) {
        handlerView.updateFileHandler(items);
    }

    @Override
    public void updatePeriodicHandler(List<Property> items) {
        handlerView.updatePeriodicHandler(items);
    }

    @Override
    public void updateSizeHandlerHandler(List<Property> items) {
        handlerView.updateSizeHandlerHandler(items);
    }

    @Override
    public void updateAsyncHandler(List<Property> items) {
        handlerView.updateAsyncHandler(items);
    }

    @Override
    public void updateCustomHandler(List<Property> items) {
        handlerView.updateCustomHandler(items);
    }

    @Override
    public void updateSyslogHandler(List<Property> items) {
        handlerView.updateSyslogHandler(items);
    }

    @Override
    public void updatePatternFormatter(List<Property> items) {
        formatterView.updatePatternFormatter(items);
    }

    @Override
    public void updateCustomFormatter(List<Property> items) {
        formatterView.updateCustomFormatter(items);
    }

    @Override
    public Widget createWidget() {

        rootLoggerView = new RootLoggerView(presenter);
        categoryView = new CategoryView(presenter);
        handlerView = new HandlerView(presenter);
        formatterView = new FormatterView(presenter);

        DefaultTabLayoutPanel tabLayoutpanel = new DefaultTabLayoutPanel(40, Style.Unit.PX);
        tabLayoutpanel.addStyleName("default-tabpanel");

        tabLayoutpanel.add(rootLoggerView.asWidget(), "Root Logger", true);
        tabLayoutpanel.add(categoryView.asWidget(), "Log Categories", true);
        tabLayoutpanel.add(handlerView.asWidget(), "Log Handlers", true);
        tabLayoutpanel.add(formatterView.asWidget(), "Log Formatters", true);

        tabLayoutpanel.selectTab(0);

        return tabLayoutpanel;
    }
}
