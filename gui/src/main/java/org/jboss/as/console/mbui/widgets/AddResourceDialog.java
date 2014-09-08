package org.jboss.as.console.mbui.widgets;

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.widgets.ContentDescription;
import org.jboss.as.console.mbui.dmr.ResourceAddress;
import org.jboss.as.console.mbui.dmr.ResourceDefinition;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.widgets.forms.FormValidation;
import org.jboss.ballroom.client.widgets.window.DialogueOptions;
import org.jboss.ballroom.client.widgets.window.TrappedFocusPanel;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;
import org.useware.kernel.gui.behaviour.StatementContext;

import java.util.List;

/**
 * @author Heiko Braun
 * @since 29/08/14
 */
public class AddResourceDialog extends ModelDrivenWidget {

    private Callback callback;
    private SecurityContext securityContext;
    private ModelNodeForm form;

    public interface Callback {
        public void onAddResource(ResourceAddress address, ModelNode payload);
        public void closeDialogue();
    }

    public AddResourceDialog(String address, StatementContext statementContext, SecurityContext securityContext, Callback callback) {
        super(address, statementContext);
        this.securityContext = securityContext;
        this.callback = callback;
    }

    public AddResourceDialog(String address, SecurityContext securityContext, Callback callback) {
        this(address, Console.MODULES.getCoreGUIContext(), securityContext, callback);
    }

    @Override
    public Widget buildWidget(final ResourceAddress address, ResourceDefinition definition) {

        List<Property> tuples = address.asPropertyList();
        String type = "";
        if(tuples.size()>0)
        {
            type = tuples.get(tuples.size()-1).getName();
        }

        ModelNodeFormBuilder builder = new ModelNodeFormBuilder()
                .setCreateMode(true)
                .setResourceDescription(definition)
                .setRequiredOnly(true)
                .setSecurityContext(securityContext);

        ModelNodeFormBuilder.FormAssets assets = builder.build();

        form = assets.getForm();
        form.setEnabled(true);

        if(form.hasWritableAttributes()) {

            DialogueOptions options = new DialogueOptions(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    // save
                    FormValidation validation = form.validate();
                    if(!validation.hasErrors())
                    {
                        callback.onAddResource(address, form.getUpdatedEntity());
                    }
                }
            }, new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    // cancel
                    callback.closeDialogue();
                }
            });

            VerticalPanel layout = new VerticalPanel();
            layout.setStyleName("fill-layout-width");
            Widget formWidget = form.asWidget();
            formWidget.getElement().getStyle().setBackgroundColor("#ffffff");
            ModelNode opDescription = definition.get("operations").get("add").get("description");
            ContentDescription text = new ContentDescription(opDescription.asString());
            layout.add(text);
            layout.add(formWidget);

            ScrollPanel scroll = new ScrollPanel(layout);

            LayoutPanel content = new LayoutPanel();
            content.addStyleName("fill-layout");
            //content.addStyleName("window-content"); clashes with TrappedFocusPanel
            content.add(scroll);
            content.add(options);

            content.getElement().setAttribute("style", "margin-bottom:10px");
            content.setWidgetTopHeight(scroll, 0, Style.Unit.PX, 92, Style.Unit.PCT);
            content.setWidgetBottomHeight(options, 0, Style.Unit.PX, 35, Style.Unit.PX);

            return new TrappedFocusPanel(content);

        }
        else
        {
            // no writable attributes
            return new HTML("There are no configurable attributes on resources " + address);
        }
    }

    public void clearValues() {
        if (form != null) {
            form.clearValues();
        }
    }
}
