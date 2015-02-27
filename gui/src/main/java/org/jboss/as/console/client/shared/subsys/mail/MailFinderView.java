package org.jboss.as.console.client.shared.subsys.mail;

import com.google.gwt.core.client.GWT;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.SplitLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SelectionChangeEvent;
import org.jboss.as.console.client.core.SuspendableViewImpl;
import org.jboss.as.console.client.widgets.nav.v3.ColumnManager;
import org.jboss.as.console.client.widgets.nav.v3.FinderColumn;

import java.util.List;

/**
 * @author Heiko Braun
 * @since 27/02/15
 */
public class MailFinderView extends SuspendableViewImpl implements MailFinder.MyView {

    private LayoutPanel previewCanvas;
    private SplitLayoutPanel layout;
    private FinderColumn<MailSession> mailSessions;
    private MailFinder presenter;
    private ColumnManager columnManager;
    private Widget mailSessCol;

    interface Template extends SafeHtmlTemplates {
        @Template("<div class=\"{0}\"><i class='icon-folder-close-alt' style='display:none'></i>&nbsp;{1}</div>")
        SafeHtml item(String cssClass, String title);
    }

    private static final Template TEMPLATE = GWT.create(Template.class);

    @Override
    public void setPresenter(MailFinder presenter) {

        this.presenter = presenter;
    }

    @Override
    public void updateFrom(List<MailSession> list) {
        mailSessions.updateFrom(list);
    }

    @Override
    public Widget createWidget() {

        previewCanvas = new LayoutPanel();

        layout = new SplitLayoutPanel(2);
        columnManager = new ColumnManager(layout);

        mailSessions = new FinderColumn<MailSession>(
                "Mail Sessions",
                new FinderColumn.Display<MailSession>() {

                    @Override
                    public boolean isFolder(MailSession data) {
                        return true;
                    }

                    @Override
                    public SafeHtml render(String baseCss, MailSession data) {
                        return TEMPLATE.item(baseCss, data.getName());
                    }

                    @Override
                    public String rowCss(MailSession data) {
                        return "";
                    }
                },
                new ProvidesKey<MailSession>() {
                    @Override
                    public Object getKey(MailSession item) {
                        return item.getName();
                    }
                })
        ;

        mailSessions.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                if(mailSessions.hasSelectedItem())
                {
                    MailSession item = mailSessions.getSelectedItem();
                    columnManager.updateActiveSelection(mailSessCol);
                }
            }
        });

        mailSessCol = mailSessions.asWidget();

        columnManager.addWest(mailSessCol);
        columnManager.add(previewCanvas);

        return layout;
    }
}
