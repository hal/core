package org.jboss.as.console.client.tools;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ToggleButton;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.shared.util.LRUCache;
import org.jboss.as.console.client.tools.mapping.DescriptionMapper;
import org.jboss.as.console.client.tools.mapping.RequestParameter;
import org.jboss.as.console.client.tools.mapping.ResponseParameter;
import org.jboss.dmr.client.ModelNode;

import java.util.List;

/**
 * @author Heiko Braun
 * @date 6/19/12
 */
public class DescriptionView {

    private HTML attributes;
    private HTML operations;
    //private HTML children;
    //private HTML header;

    private LRUCache<String, SafeHtml[]> widgetCache = new LRUCache<String, SafeHtml[]>(10);

    Widget asWidget() {
        VerticalPanel layout = new VerticalPanel();
        layout.setStyleName("fill-layout-width");

        attributes = new HTML();
        attributes.setStyleName("fill-layout");
        attributes.getElement().setAttribute("style", "padding:10px");

        operations = new HTML();
        operations.setStyleName("fill-layout");
        operations.getElement().setAttribute("style", "padding:10px");

        final ToggleButton toggleAttributes = new ToggleButton("Attributes", "Attributes");
        final ToggleButton toggleOperations= new ToggleButton("Operations", "Operations");

        toggleAttributes.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
            @Override
            public void onValueChange(ValueChangeEvent<Boolean> event) {
                Boolean isDown = event.getValue();
                toggleOperations.setDown(!isDown);
                attributes.setVisible(isDown);
                operations.setVisible(!isDown);
            }
        });

        toggleOperations.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
            @Override
            public void onValueChange(ValueChangeEvent<Boolean> event) {
                Boolean isDown = event.getValue();
                toggleAttributes.setDown(!isDown);
                operations.setVisible(isDown);
                attributes.setVisible(!isDown);
            }
        });

        toggleAttributes.setDown(true);
        toggleOperations.setDown(false);
        operations.setVisible(false);

        HorizontalPanel tools = new HorizontalPanel();
        tools.add(toggleAttributes);
        tools.add(toggleOperations);
        layout.add(tools);
        tools.getElement().setAttribute("align", "center");

        layout.add(attributes);
        layout.add(operations);

        return layout;
    }

    public void updateDescription(ModelNode address, ModelNode description)
    {

        String cacheKey = AddressUtils.asKey(address, true);
        if(widgetCache.containsKey(cacheKey))
        {
            SafeHtml[] panels = widgetCache.get(cacheKey);
            attributes.setHTML(panels[0]);
            operations.setHTML(panels[1]);
        }
        else
        {
            createDescriptionPanel(address, description);
        }

    }

    private void createDescriptionPanel(final ModelNode address, ModelNode description) {
        DescriptionMapper mapper = new DescriptionMapper(address, description);


        mapper.map(new DescriptionMapper.Mapping() {

            int numOps = 0;
            int numAttributes = 0;

            SafeHtmlBuilder attributeBuilder = new SafeHtmlBuilder();
            SafeHtmlBuilder operationsBuilder = new SafeHtmlBuilder();
            SafeHtmlBuilder childrenBuilder = new SafeHtmlBuilder();

            @Override
            public void onAttribute(
                    String name, String description, String type,
                    boolean required, boolean expressions,
                    boolean runtime, boolean readOnly, String deprecationReason) {

                attributeBuilder.appendHtmlConstant("<tr valign=top>");
                attributeBuilder.appendHtmlConstant("<td class='doc-attribute'>");

                if (deprecationReason != null)
                    attributeBuilder.appendHtmlConstant("<div style='text-decoration:line-through;font-weight: normal;'>").appendEscaped(name).appendHtmlConstant("</div>");
                else
                    attributeBuilder.appendEscaped(name);

                String requiredSuffix = (required && !readOnly) ? " <span style='color:#B8B8B8;font-size:10px'>(required)</span>" : "";
                attributeBuilder.appendHtmlConstant(requiredSuffix);
                attributeBuilder.appendHtmlConstant("</td>");

                attributeBuilder.appendHtmlConstant("<td>");
                attributeBuilder.appendEscaped(type);
                attributeBuilder.appendHtmlConstant("</td>");
                attributeBuilder.appendHtmlConstant("</tr>");

                attributeBuilder.appendHtmlConstant("<tr class='doc-table-description' valign=top>");
                attributeBuilder.appendHtmlConstant("<td width=60%>");

                if (deprecationReason != null) {
                    attributeBuilder.appendHtmlConstant("<b>Deprecated: </b>");
                    attributeBuilder.appendEscaped(deprecationReason);
                } else {
                    attributeBuilder.appendEscaped(description);
                }
                attributeBuilder.appendHtmlConstant("</td>");
                attributeBuilder.appendHtmlConstant("<td width=40% style='color:#B8B8B8'>");

                String expressionSuffix = expressions ? " expression<br/>" : "";
                attributeBuilder.appendHtmlConstant(expressionSuffix);

                String runtimeSuffix = runtime ? " runtime<br/>" : "";
                attributeBuilder.appendHtmlConstant(runtimeSuffix);

                String readOnlySuffix = readOnly ? " read-only<br/>" : "";
                attributeBuilder.appendHtmlConstant(readOnlySuffix);

                attributeBuilder.appendHtmlConstant("</td>");
                attributeBuilder.appendHtmlConstant("</tr>");

                numAttributes++;

            }

            @Override
            public void onOperation(
                    String name, String description,
                    List<RequestParameter> parameter, ResponseParameter response,
                    boolean isDefault) {

                String css = isDefault ? "doc-table-description muted" : "doc-table-description";
                operationsBuilder.appendHtmlConstant("<tr valign=top class='" + css + "'>");
                operationsBuilder.appendHtmlConstant("<td width=60%>");
                operationsBuilder.appendHtmlConstant("<span class='doc-attribute' style='margin-bottom:10px'>");
                operationsBuilder.appendEscaped(name).appendHtmlConstant("<br/>");
                operationsBuilder.appendHtmlConstant("</span>");
                operationsBuilder.appendEscaped(description);
                operationsBuilder.appendHtmlConstant("</td>");
                operationsBuilder.appendHtmlConstant("<td width=40%>");

                // -- inner

                if (parameter.size() > 0)
                    operationsBuilder.appendHtmlConstant("<b>Input</b>:<br/>");

                // parameters
                for (RequestParameter param : parameter) {
                    boolean required = param.isRequired();
                    operationsBuilder.appendEscaped(param.getParamName()).appendEscaped(": ");
                    operationsBuilder.appendEscaped(param.getParamType());
                    String requiredSuffix = (required) ? " <span style='color:#B8B8B8;font-size:10px'>(required)</span>" : "";
                    operationsBuilder.appendHtmlConstant(requiredSuffix);
                    operationsBuilder.appendHtmlConstant("<br/>");
                }

                String responseTitle = !"".equals(response.getReplyType()) ? "<br/><b>Output:</b><br/>" : "";
                operationsBuilder.appendHtmlConstant(responseTitle);

                operationsBuilder.appendEscaped(response.getReplyType());


                // -- end inner

                operationsBuilder.appendHtmlConstant("</td>");
                operationsBuilder.appendHtmlConstant("</tr>");

                numOps++;

            }

            @Override
            public void onChild(String name, String description) {

                childrenBuilder.appendHtmlConstant("<tr valign=top>");
                childrenBuilder.appendHtmlConstant("<td class='doc-child'>")
                        .appendEscaped(name)
                        .appendHtmlConstant("</td>");
                childrenBuilder.appendHtmlConstant("</tr>");

                childrenBuilder.appendHtmlConstant("<tr class='doc-table-description'>");
                childrenBuilder.appendHtmlConstant("<td colspan=2>")
                        .appendEscaped(description)
                        .appendHtmlConstant("</td>");
                childrenBuilder.appendHtmlConstant("</tr>");

            }

            @Override
            public void onBegin(int numAttributes, int numOperations) {
                attributeBuilder.appendHtmlConstant("<h2 class='homepage-secondary-header' id='attributes.header'>Attributes (" + numAttributes + ")</h2>");
                attributeBuilder.appendHtmlConstant("<table class='doc-table' cellpadding=5>");

                operationsBuilder.appendHtmlConstant("<h2 class='homepage-secondary-header' id='attributes.header'>Operations (" + numOperations + ")</h2>");
                operationsBuilder.appendHtmlConstant("<table class='doc-table' cellpadding=5>");

                childrenBuilder.appendHtmlConstant("<table class='doc-table' cellpadding=5>");
            }

            @Override
            public void onFinish() {


                if (0 == numOps) {
                    operationsBuilder.appendHtmlConstant("<tr valign=top class='doc-table-description'>");
                    operationsBuilder.appendHtmlConstant("<td colspan=2 width=100% style='vertical-align:center'>");
                    operationsBuilder.appendEscaped("No operations found.").appendHtmlConstant("<br/>");
                    operationsBuilder.appendHtmlConstant("</td>");
                    operationsBuilder.appendHtmlConstant("</tr>");
                }

                if (0 == numAttributes) {
                    attributeBuilder.appendHtmlConstant("<tr valign=top class='doc-table-description'>");
                    attributeBuilder.appendHtmlConstant("<td colspan=2 width=100% style='vertical-align:center'>");
                    attributeBuilder.appendEscaped("No attributes found.").appendHtmlConstant("<br/>");
                    attributeBuilder.appendHtmlConstant("</td>");
                    attributeBuilder.appendHtmlConstant("</tr>");
                }

                SafeHtml attPanel = attributeBuilder.toSafeHtml();
                SafeHtml opPanel = operationsBuilder.toSafeHtml();

                // caching
                String cacheKey = AddressUtils.asKey(address, true);
                widgetCache.put(cacheKey, new SafeHtml[] {attPanel, opPanel});

                attributeBuilder.appendHtmlConstant("</table>");
                attributes.setHTML(attPanel);

                operationsBuilder.appendHtmlConstant("</table>");
                operations.setHTML(opPanel);

                childrenBuilder.appendHtmlConstant("</table>");


            }
        });
    }

    public DescriptionView() {
        super();    //To change body of overridden methods use File | Settings | File Templates.
    }

    public void clearDisplay() {
        attributes.setHTML("");
        operations.setHTML("");
    }
}
