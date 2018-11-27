package org.jboss.as.console.client.tools;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.http.client.URL;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.InlineHTML;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;

import java.util.List;

/**
 * @author Heiko Braun
 * @date 8/2/12
 */
public class PageHeader {

    private VerticalPanel header;
    private BrowserNavigation presenter;

    public void setPresenter(BrowserNavigation presenter) {
        this.presenter = presenter;
    }

    Widget asWidget() {

        header = new VerticalPanel();
        ScrollPanel scroll = new ScrollPanel(header);
        return scroll;
    }

    public void updateDescription(ModelNode address, ModelNode description) {


        header.clear();

        final List<Property> path = address.asPropertyList();
        FlowPanel nav = new FlowPanel();
        nav.addStyleName("node-header");

        if(path.isEmpty())
        {
            SafeHtmlBuilder builder = new SafeHtmlBuilder();
            builder.appendHtmlConstant("<h1 class='node-header'>Management Model</h1>");
            header.add(new InlineHTML(builder.toSafeHtml()));
        }
        else {
            int i=0;
            List<Property> subAddress = presenter.getSubaddress(address);

            final ModelNode parentAddress = new ModelNode().setEmptyList();
            for (final Property p : path) {
                nav.add(new InlineHTML("/"));

                boolean isLinked = subaddressContainsPath(subAddress, p);
                String css =  isLinked ? "node-header-link" : "";
                InlineHTML type = new InlineHTML("<span class='"+css+"'>"+p.getName()+"</span>");

                if(isLinked) {
                    type.addClickHandler(new ClickHandler() {
                        final ModelNode address = parentAddress.clone();
                        final String parentName = p.getName();
                        @Override
                        public void onClick(ClickEvent event) {
                            presenter.onViewChild(address, parentName);
                        }
                    });
                }

                nav.add(type);
                nav.add(new InlineHTML("="));
                nav.add(new InlineHTML(SafeHtmlUtils.fromString(URL.decodePathSegment(p.getValue().asString())).asString()));

                // has to be last step. valid for the next iteration
                parentAddress.add(p.getName(), p.getValue().asString());

                i++;
            }

            header.add(nav);
        }

        SafeHtmlBuilder builder = new SafeHtmlBuilder();
        String desc = description!=null ? description.get("description").asString() : "Please select an addressable resource.";
        builder.appendHtmlConstant("<p class='homepage-info-box-body' style='font-size:13px'>")
                .appendHtmlConstant(desc)
                .appendHtmlConstant("</p>");

        header.add(new InlineHTML(builder.toSafeHtml()));


    }

    private boolean subaddressContainsPath(List<Property> subAddress, Property tuple) {
        boolean match = false;
        for(Property p : subAddress)
        {
            if(p.getName().equals(tuple.getName()) && p.getValue().asString().equals(tuple.getValue().asString()))
            {
                match = true;
                break;
            }
        }
        return match;
    }

    public void updateDescription(ModelNode address) {
        updateDescription(address, null);
    }
}
