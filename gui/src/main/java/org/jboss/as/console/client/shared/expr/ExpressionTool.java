package org.jboss.as.console.client.shared.expr;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.VerticalPanel;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.UIConstants;
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.as.console.client.tools.Tool;
import org.jboss.as.console.client.widgets.ContentDescription;
import org.jboss.ballroom.client.widgets.ContentHeaderLabel;
import org.jboss.ballroom.client.widgets.forms.Form;
import org.jboss.ballroom.client.widgets.forms.TextAreaItem;
import org.jboss.ballroom.client.widgets.forms.TextBoxItem;
import org.jboss.ballroom.client.widgets.window.DefaultWindow;
import org.jboss.ballroom.client.widgets.window.DialogueOptions;
import org.jboss.ballroom.client.widgets.window.WindowContentBuilder;

import java.util.Map;

/**
 * @author Heiko Braun
 * @date 8/3/12
 */
public class ExpressionTool implements Tool {

    private DefaultWindow window;
    private ExpressionResolver resolver;
    private TextAreaItem output;
    private TextBoxItem input;

    public ExpressionTool(ExpressionResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public void launch() {
        if(null==window)
            window = asWidget();

        window.center();
    }

    DefaultWindow asWidget() {
        final DefaultWindow window = new DefaultWindow("Expressions");
        window.setWidth(480);
        window.setHeight(360);


        VerticalPanel panel = new VerticalPanel();
        panel.setStyleName("window-content");

        panel.add(new ContentHeaderLabel(((UIConstants) GWT.create(UIConstants.class)).resolveExpressionValues()));

        Form<Expression> form = new Form<Expression>(Expression.class);
        input = new TextBoxItem("input", "Expression");
        output = new TextAreaItem("output", ((UIConstants) GWT.create(UIConstants.class)).resolvedValue()) {
            @Override
            public String getErrMessage() {
                return ((UIConstants) GWT.create(UIConstants.class)).unableToResolve();
            }
        };

        form.setFields(input, output);

        panel.add(new ContentDescription(((UIConstants) GWT.create(UIConstants.class)).expressionsRunningServer()));
        panel.add(form.asWidget());


        ClickHandler submitHandler = new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                resolve(input.getValue());
            }
        };

        ClickHandler cancelHandler = new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                // Close tool
                window.hide();
            }
        };

        DialogueOptions options = new DialogueOptions(
                ((UIConstants) GWT.create(UIConstants.class)).resolve(),submitHandler, Console.CONSTANTS.common_label_done(),cancelHandler);


        window.trapWidget(new WindowContentBuilder(panel, options).build());

        window.setGlassEnabled(true);

        window.addCloseHandler(closeEvent -> Console.getPlaceManager().navigateBack());

        return window;
    }

    @Override
    public void dispose() {
        if(window!=null)
            window.hide();
    }

    public void resolve(String expr) {

        output.setErroneous(false);
        output.clearValue();


        final Expression exprModel = Expression.fromString(expr);
        resolver.resolveValue(exprModel, new SimpleCallback<Map<String,String>>() {
            @Override
            public void onSuccess(Map<String,String> serverValues) {

                input.setValue(exprModel.toString());

                output.setErroneous(serverValues.isEmpty());

                StringBuilder sb = new StringBuilder();
                for(String server : serverValues.keySet())
                {
                    sb.append(server).append("=").append(serverValues.get(server));
                    sb.append("\n");
                }

                output.setValue(sb.toString());
            }
        });
    }
}
