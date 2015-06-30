package org.jboss.as.console.client.shared.subsys.jca.wizard;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.shared.subsys.jca.DataSourceFinder;
import org.jboss.as.console.client.shared.subsys.jca.model.DataSource;
import org.jboss.as.console.client.shared.subsys.jca.model.DataSourceTemplate;
import org.jboss.as.console.client.shared.subsys.jca.model.DataSourceTemplates;
import org.jboss.ballroom.client.widgets.window.DialogueOptions;
import org.jboss.ballroom.client.widgets.window.WindowContentBuilder;

public class ChooseTemplateStep<T extends DataSource> implements IsWidget, ClickHandler {

    private final DataSourceFinder presenter;
    private final DataSourceTemplates templates;
    private final boolean xa;
    private final Command onNext;
    private DataSourceTemplate<T> selectedTemplate;

    public ChooseTemplateStep(DataSourceFinder presenter, DataSourceTemplates templates, boolean xa, Command onNext) {
        this.presenter = presenter;
        this.templates = templates;
        this.xa = xa;
        this.onNext = onNext;
    }

    @Override
    public Widget asWidget() {

        VerticalPanel layout = new VerticalPanel();
        layout.setStyleName("window-content");
        layout.add(new HTML("<h3>" + Console.CONSTANTS.subsys_jca_dataSource_choose_template() + "</h3>"));

        RadioButton customButton = new RadioButton("template", Console.CONSTANTS.subsys_jca_dataSource_custom_template());
        customButton.getElement().setId("custom");
        customButton.setStyleName("choose_template");
        customButton.setValue(true);
        customButton.addClickHandler(this);
        customButton.setFocus(true);
        layout.add(customButton);

        for (DataSourceTemplate<? extends DataSource> template : templates) {
            if (template.isXA() != xa) {
                continue;
            }
            RadioButton radioButton = new RadioButton("template", template.toString());
            radioButton.getElement().setId(template.getId());
            radioButton.setStyleName("choose_template");
            radioButton.addClickHandler(this);
            layout.add(radioButton);
        }


        ClickHandler submitHandler = new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                onNext.execute();
            }
        };
        ClickHandler cancelHandler = new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                presenter.closeDialogue();
            }
        };
        DialogueOptions options = new DialogueOptions(
                Console.CONSTANTS.common_label_next(), submitHandler,
                Console.CONSTANTS.common_label_cancel(), cancelHandler
        );

        return new WindowContentBuilder(layout, options).build();
    }

    @Override
    public void onClick(ClickEvent event) {
        RadioButton button = (RadioButton) event.getSource();
        selectedTemplate = templates.getTemplate(button.getElement().getId());
    }

    public DataSourceTemplate<T> getSelectedTemplate() {
        return selectedTemplate;
    }
}
