package org.jboss.as.console.client.tools.modelling.workbench.repository;

import com.google.gwt.core.client.GWT;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.visualization.client.DataTable;
import com.google.gwt.visualization.client.visualizations.OrgChart;
import org.useware.kernel.model.scopes.BranchActivation;
import org.useware.kernel.model.Dialog;
import org.useware.kernel.model.mapping.Node;
import org.useware.kernel.model.scopes.Scope;
import org.useware.kernel.model.structure.Container;
import org.useware.kernel.model.structure.InteractionUnit;
import org.useware.kernel.model.structure.QName;
import org.useware.kernel.model.structure.TemporalOperator;
import org.useware.kernel.model.structure.builder.InteractionUnitVisitor;

import java.util.Map;
import java.util.Stack;

import static com.google.gwt.visualization.client.AbstractDataTable.ColumnType.STRING;

/**
 * @author Harald Pehl
 * @date 03/07/2013
 */
public class DialogVisualization
{
    static final NameTemplate NAME_TEMPLATE = GWT.create(NameTemplate.class);
    private static final String ACTIVE_STYLE = "active-default";
    private static final String INACTIVE_STYLE = "inactive-default";
    private final OrgChart chart;
    private final Dialog dialog;

    String[] colors = new String[] {
            "#D8D8D8",
            "#C4BD97",
            "#8DB3E2",
            "#B8CCE4",
            "#E5B9B7",
            "#D7E3BC",
            "#CCC1D9",
            "#B7DDE8",
            "#FBD5B5",
            "#9BBB59",
            "#4F81BD",
            "#4BACC6",
            "#F79646",
            "#C0504D",
    };

    public DialogVisualization(final Dialog dialog)
    {
        this.dialog = dialog;
        this.chart = new OrgChart(createData(dialog), createOptions(dialog));
    }

    protected DataTable createData(final Dialog dialog)
    {
        DataTable dataTable = DataTable.create();
        dataTable.addColumn(STRING, "id");
        dataTable.addColumn(STRING, "parent");
        dataTable.addColumn(STRING, "tooltip");

        OrgChartVisitor visitor = new OrgChartVisitor(dataTable);
        dialog.getInterfaceModel().accept(visitor);

        return dataTable;
    }

    protected OrgChart.Options createOptions(final Dialog dialog)
    {
        OrgChart.Options options = OrgChart.Options.create();
        options.setAllowHtml(true);
        options.setAllowCollapse(true);
        return options;
    }

    public OrgChart getChart()
    {
        return chart;
    }


    class OrgChartVisitor implements InteractionUnitVisitor
    {
        final DataTable dataTable;
        int row;
        Stack<Container> container;

        OrgChartVisitor(final DataTable dataTable)
        {
            this.dataTable = dataTable;
            this.row = 0;
            this.container = new Stack<Container>();
        }

        @Override
        public void startVisit(final Container container)
        {
            addInteractionUnitRow(container);
            this.container.push(container);
        }

        @Override
        public void visit(final InteractionUnit interactionUnit)
        {
            addInteractionUnitRow(interactionUnit);
        }

        @Override
        public void endVisit(final Container container)
        {
            this.container.pop();
        }

        void addInteractionUnitRow(InteractionUnit interactionUnit)
        {
            String id = interactionUnit.getId().toString();
            String name = interactionUnit.getLabel() == null ? interactionUnit.getId().getLocalPart() : interactionUnit.getLabel();

            Container container = this.container.isEmpty() ? null : this.container.peek();
            String parentId = container != null ? container.getId().toString() : null;

            // default activation
            BranchActivation activation = new BranchActivation();
            dialog.getInterfaceModel().accept(activation);
            Map<Integer,QName> activeItems = activation.getActiveItems();

            String style = activeItems.values().contains(interactionUnit.getId()) ? ACTIVE_STYLE : INACTIVE_STYLE;

            // statement context shadow visualisation
            Node<Scope> self = dialog.getScopeModel().findNode(interactionUnit.getScopeId());
            Integer scopeId = self.getData().getId();
            String color = scopeId>colors.length-1 ? "#ffffff" : colors[scopeId];

            if (interactionUnit instanceof Container)
            {
                TemporalOperator operator = ((Container) interactionUnit).getTemporalOperator();
                if (operator != null)
                {
                    name = NAME_TEMPLATE.name(style, name, operator.name(), color).asString();
                }
            }
            else
            {
                String classname = interactionUnit.getClass().getName();

                classname = classname.substring(classname.lastIndexOf('.') + 1);
                name = NAME_TEMPLATE.name(style, name, classname, color).asString();
            }

            StringBuilder tooltip = new StringBuilder();
            tooltip.append("[unit]\n").append(id).append("\n\n");
            if (interactionUnit.doesConsume())
                tooltip.append("[input]\n").append(interactionUnit.getInputs()).append("\n\n");
            if (interactionUnit.doesProduce())
                tooltip.append("[output]\n").append(interactionUnit.getOutputs()).append("\n\n");

            tooltip.append("[scope]\n").append("\n");
            tooltip.append("id:").append(scopeId).append("\n");
            int parentScope = self.getParent()!=null ? self.getParent().getData().getId() : -1;
            tooltip.append("parent:").append(parentScope).append("\n");
            //tooltip.append("demarcation: ").append(self.getData().isDemarcationType()).append("\n");


            dataTable.addRow();

            dataTable.setCell(row, 0, id, name, null);
            dataTable.setValue(row, 1, parentId);
            dataTable.setValue(row, 2, tooltip.toString());

            row++;
        }
    }


    interface NameTemplate extends SafeHtmlTemplates
    {
        @Template("<div class='{0}' style='background-color:{3}'>{1}<br/><span style=\"color:#666;\">&laquo;{2}&raquo;</span></div>")
        SafeHtml name(String css, String name, String stereotype, String color);
    }
}
