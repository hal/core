package org.jboss.as.console.client.shared.subsys.logger;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.layout.MultipleToOneLayout;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.as.console.mbui.widgets.ComplexAttributeForm;
import org.jboss.as.console.mbui.widgets.ModelNodeFormBuilder;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.widgets.forms.FormCallback;
import org.jboss.ballroom.client.widgets.tables.DefaultCellTable;
import org.jboss.ballroom.client.widgets.tools.ToolButton;
import org.jboss.ballroom.client.widgets.tools.ToolStrip;
import org.jboss.ballroom.client.widgets.window.Feedback;
import org.jboss.dmr.client.Property;

import java.util.List;
import java.util.Map;

/**
 * @author Heiko Braun
 */
public class MasterDetailTemplate {


    private final LoggerPresenter presenter;
    private final AddressTemplate address;
    private final String title;
    private final DefaultCellTable table;
    private final ListDataProvider<Property> dataProvider;
    private SingleSelectionModel<Property> selectionModel;
    private ModelNodeFormBuilder.FormAssets fileAssets;

    public MasterDetailTemplate(LoggerPresenter presenter, AddressTemplate address, String title) {
        this.presenter = presenter;
        this.address = address;
        this.title = title;
        ProvidesKey<Property> providesKey = Property::getName;
        this.table = new DefaultCellTable(5, providesKey);
        this.dataProvider = new ListDataProvider<Property>(providesKey);
        this.dataProvider.addDataDisplay(table);
    }

    public Widget asWidget() {
        TextColumn<Property> nameColumn = new TextColumn<Property>() {
            @Override
            public String getValue(Property node) {
                return node.getName();
            }
        };

        table.addColumn(nameColumn, "Name");

        ToolStrip tools = new ToolStrip();
        tools.addToolButtonRight(new ToolButton(Console.CONSTANTS.common_label_add(), new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                if(fileAssets!=null)
                    presenter.onLaunchAddResourceDialogFile(address);
                else
                    presenter.onLaunchAddResourceDialog(address);
            }
        }));
        tools.addToolButtonRight(new ToolButton(Console.CONSTANTS.common_label_delete(), new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                Feedback.confirm(Console.MESSAGES.deleteTitle(title),
                        Console.MESSAGES.deleteConfirm(title+" '" + getCurrentSelection().getName() + "'"),
                        new Feedback.ConfirmationHandler() {
                            @Override
                            public void onConfirmation(boolean isConfirmed) {
                                if (isConfirmed) {
                                    presenter.onRemoveResource(
                                            address, getCurrentSelection().getName()
                                    );
                                }
                            }
                        });
            }
        }));

        SecurityContext securityContext = presenter.getSecurityFramework().getSecurityContext(presenter.getProxy().getNameToken());
        ResourceDescription definition = presenter.getDescriptionRegistry().lookup(address);

        ModelNodeFormBuilder builder = new ModelNodeFormBuilder()
                .setConfigOnly()
                .setResourceDescription(definition)
                .setSecurityContext(securityContext);

        final ModelNodeFormBuilder.FormAssets formAssets = builder.build();

        formAssets.getForm().setToolsCallback(new FormCallback() {
            @Override
            public void onSave(Map changeset) {
                presenter.onSaveNamedResource(address, getCurrentSelection().getName(), changeset);
            }

            @Override
            public void onCancel(Object entity) {
                formAssets.getForm().cancel();
            }
        });

        // ----
        final MultipleToOneLayout layoutBuilder = new MultipleToOneLayout()
                .setPlain(true)
                .setHeadline(title)
                .setDescription(definition.get("description").asString())
                .setMasterTools(tools)
                .setMaster(Console.MESSAGES.available(title), table)
                .addDetail(Console.CONSTANTS.common_label_attributes(), formAssets.asWidget());

        fileAssets = null;

        if(definition.get("attributes").hasDefined("file"))
        {
            // complex attribute 'file'
            ComplexAttributeForm fileAttributeForm = new ComplexAttributeForm("file", securityContext, definition);
            fileAssets = fileAttributeForm.build();

            // order matters
            fileAssets.getForm().setToolsCallback(new FormCallback() {
                @Override
                public void onSave(Map changeset) {

                    // ingore the changeset: complex attributes are written atomically, including all attributes

                    presenter.onSaveFileAttributes(address, getCurrentSelection().getName(),
                            fileAssets.getForm().getUpdatedEntity()
                    );
                }

                @Override
                public void onCancel(Object o) {
                    fileAssets.getForm().cancel();
                }
            });

            layoutBuilder.addDetail("File", fileAssets.asWidget());
        }

        selectionModel = new SingleSelectionModel<Property>();
        selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                Property server = selectionModel.getSelectedObject();
                if(server!=null)
                {
                    formAssets.getForm().edit(server.getValue());

                    if(fileAssets!=null)
                    {
                        fileAssets.getForm().edit(server.getValue().get("file"));
                    }
                }
                else
                {
                    formAssets.getForm().clearValues();
                    if(fileAssets!=null) fileAssets.getForm().clearValues();
                }
            }
        });
        table.setSelectionModel(selectionModel);
        return layoutBuilder.build();
    }

    private Property getCurrentSelection() {
        Property selection = ((SingleSelectionModel<Property>) table.getSelectionModel()).getSelectedObject();
        return selection;
    }

    public void setData(List<Property> data) {
        dataProvider.setList(data);
        table.selectDefaultEntity();
    }
}

