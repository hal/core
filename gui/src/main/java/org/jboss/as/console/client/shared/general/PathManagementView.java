package org.jboss.as.console.client.shared.general;

import java.util.List;
import java.util.Map;

import com.google.gwt.dom.client.Style;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SingleSelectionModel;
import com.google.inject.Inject;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.SuspendableViewImpl;
import org.jboss.as.console.client.layout.MultipleToOneLayout;
import org.jboss.as.console.client.rbac.SecurityFramework;
import org.jboss.as.console.client.shared.general.model.Path;
import org.jboss.as.console.client.v3.ResourceDescriptionRegistry;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.as.console.mbui.widgets.ModelNodeFormBuilder;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.widgets.forms.FormCallback;
import org.jboss.ballroom.client.widgets.tables.DefaultCellTable;
import org.jboss.ballroom.client.widgets.tools.ToolButton;
import org.jboss.ballroom.client.widgets.tools.ToolStrip;
import org.jboss.ballroom.client.widgets.window.Feedback;

/**
 * @author Heiko Braun
 * @date 10/15/12
 */
public class PathManagementView extends SuspendableViewImpl implements PathManagementPresenter.MyView {

    private PathManagementPresenter presenter;
    private DefaultCellTable<Path> table;
    private ListDataProvider<Path> dataProvider;
    private SecurityFramework securityFramework;
    private ResourceDescription resourceDescription;
    private SingleSelectionModel<Path> selectionModel;
    private ModelNodeFormBuilder.FormAssets modelForm;

    @Inject
    PathManagementView(SecurityFramework securityFramework,
            ResourceDescriptionRegistry descriptionRegistry) {
        this.securityFramework = securityFramework;
        resourceDescription = descriptionRegistry.lookup(AddressTemplate.of("/path=*"));
    }

    @Override
    public Widget createWidget() {
        ProvidesKey<Path> providesKey = Path::getName;
        selectionModel = new SingleSelectionModel<>(providesKey);
        SecurityContext securityContext = securityFramework.getSecurityContext(presenter.getProxy().getNameToken());
        ToolStrip toolstrip = new ToolStrip();

        ToolButton addBtn = new ToolButton(Console.CONSTANTS.common_label_add(),
                event -> presenter.launchNewPathDialogue());

        toolstrip.addToolButtonRight(addBtn);

        ToolButton removeBtn = new ToolButton(Console.CONSTANTS.common_label_delete(), event -> {
            Path selectedObject = selectionModel.getSelectedObject();
            if (selectedObject != null) {
                final String name = selectedObject.getName();
                Feedback.confirm(
                        Console.MESSAGES.deleteTitle("Path"),
                        Console.MESSAGES.deleteConfirm("Path " + name),
                        isConfirmed -> {
                            if (isConfirmed) {
                                presenter.onDeletePath(name);
                                selectionModel.clear();
                            }
                        });
            }
        });
        removeBtn.setEnabled(false);

        toolstrip.addToolButtonRight(removeBtn);

        table = new DefaultCellTable<>(6, Path::getName);
        dataProvider = new ListDataProvider<>();
        dataProvider.addDataDisplay(table);

        TextColumn<Path> nameCol = new TextColumn<Path>() {
            @Override
            public String getValue(Path record) {
                return record.getName();
            }
        };
        TextColumn<Path> valueCol = new TextColumn<Path>() {
            @Override
            public String getValue(Path record) {
                return record.getPath();
            }
        };
        table.addColumn(nameCol, "Name");
        table.addColumn(valueCol, "Path");
        table.setColumnWidth(nameCol, 20, Style.Unit.PCT);
        table.setColumnWidth(valueCol, 80, Style.Unit.PCT);

        modelForm = new ModelNodeFormBuilder()
                .setResourceDescription(resourceDescription)
                .setSecurityContext(securityContext).build();

        modelForm.getForm().setToolsCallback(new FormCallback() {
            @Override
            @SuppressWarnings("unchecked")
            public void onSave(final Map changeset) {
                String name = selectionModel.getSelectedObject().getName();
                presenter.onSavePath(name, changeset);
            }

            @Override
            public void onCancel(final Object entity) {
                modelForm.getForm().cancel();
            }
        });

        VerticalPanel formPanel = new VerticalPanel();
        formPanel.setStyleName("fill-layout-width");
        formPanel.add(modelForm.getHelp().asWidget());
        formPanel.add(modelForm.getForm().asWidget());

        MultipleToOneLayout layout = new MultipleToOneLayout()
                .setTitle("Paths")
                .setHeadline("Path References")
                .setDescription(SafeHtmlUtils.fromString(Console.MESSAGES.path_description()))
                .setMaster(Console.MESSAGES.available("Paths"), table)
                .setMasterTools(toolstrip)
                .addDetail(Console.CONSTANTS.common_label_attributes(), formPanel);

        selectionModel.addSelectionChangeHandler(event -> {
            Path path = selectionModel.getSelectedObject();
            if (path != null) {
                modelForm.getForm().edit(presenter.getEntityAdapter().fromEntity(path));
                if (path.isReadOnly())
                    removeBtn.setEnabled(false);
                else
                    removeBtn.setEnabled(true);

            } else {
                removeBtn.setEnabled(false);
                modelForm.getForm().clearValues();
            }
        });
        table.setSelectionModel(selectionModel);

        return layout.build();
    }

    @Override
    public void setPresenter(PathManagementPresenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public void setPaths(List<Path> paths) {
        List<Path> list = dataProvider.getList();
        list.clear(); // cannot call setList() as that breaks the sort handler
        list.addAll(paths);
        dataProvider.flush();
        table.selectDefaultEntity();
        selectionModel.clear();
    }
}
