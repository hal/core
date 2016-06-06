package org.jboss.as.console.client.shared.subsys.jca;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Collections2;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.shared.expr.ExpressionAdapter;
import org.jboss.as.console.client.shared.help.FormHelpPanel;
import org.jboss.as.console.client.shared.subsys.Baseadress;
import org.jboss.as.console.client.shared.subsys.jca.model.CapacityPolicy;
import org.jboss.as.console.client.shared.subsys.jca.model.PoolConfig;
import org.jboss.as.console.client.widgets.forms.FormToolStrip;
import org.jboss.ballroom.client.widgets.forms.CheckBoxItem;
import org.jboss.ballroom.client.widgets.forms.ComboBoxItem;
import org.jboss.ballroom.client.widgets.forms.Form;
import org.jboss.ballroom.client.widgets.forms.NumberBoxItem;
import org.jboss.ballroom.client.widgets.forms.PropertyListItem;
import org.jboss.ballroom.client.widgets.tools.ToolButtonDropdown;
import org.jboss.dmr.client.ModelNode;

/**
 * @author Heiko Braun
 * @date 9/16/11
 */
public class PoolConfigurationView {

    private Form<PoolConfig> form;
    private String editedName = null;
    private PoolManagement management;
    private boolean xaDisplay = false;

    public PoolConfigurationView(PoolManagement management) {
        this.management = management;
    }

    Widget asWidget() {

        NumberBoxItem minCon = new NumberBoxItem("minPoolSize", "Min Pool Size");
        minCon.setRequired(false);

        NumberBoxItem initialCon = new NumberBoxItem("initialPoolSize", "Initial Pool Size");
        initialCon.setRequired(false);

        NumberBoxItem maxCon = new NumberBoxItem("maxPoolSize", "Max Pool Size");
        maxCon.setRequired(false);

        CheckBoxItem strictMin = new CheckBoxItem("poolStrictMin", "Strict Minimum");

        CheckBoxItem prefill = new CheckBoxItem("poolPrefill", "Prefill");

        ComboBoxItem flushStrategy = new ComboBoxItem("flushStrategy", "Flush Strategy");
        flushStrategy.setValueMap(new String[]{"FailingConnectionOnly",
                "InvalidIdleConnections",
                "IdleConnections",
                "Gracefully",
                "EntirePool",
                "AllInvalidIdleConnections",
                "AllIdleConnections",
                "AllGracefully",
                "AllConnections"});

        ComboBoxItem trackStmt = new ComboBoxItem("trackStatements", "Track Statements");
        trackStmt.setValueMap(new String[]{"true", "false", "nowarn"});

        CheckBoxItem useFastFail = new CheckBoxItem("useFastFail", "Use Fast Fail");

        // decrementer
        Set<CapacityPolicy> decs = Sets.filter(EnumSet.allOf(CapacityPolicy.class),
                (capacityPolicy) -> !capacityPolicy.isIncrement());
        Collection<String> decNames = Collections2.transform(decs, CapacityPolicy::className);
        PropertyListItem decrementerProperties = new PropertyListItem("capacityDecrementerProperties",
                "Decrementer Properties");
        decrementerProperties.setRequired(false);
        ComboBoxItem decrementerClass = new ComboBoxItem("capacityDecrementerClass", "Decrementer Class", true);
        decrementerClass.setRequired(false);
        decrementerClass.setValueMap(Ordering.natural().immutableSortedCopy(decNames));

        // incrementer
        Set<CapacityPolicy> incs = Sets.filter(EnumSet.allOf(CapacityPolicy.class), CapacityPolicy::isIncrement);
        Collection<String> incNames = Collections2.transform(incs, CapacityPolicy::className);
        PropertyListItem incrementerProperties = new PropertyListItem("capacityIncrementerProperties",
                "Incrementer Properties");
        incrementerProperties.setRequired(false);
        ComboBoxItem incrementerClass = new ComboBoxItem("capacityIncrementerClass", "Incrementer Class", true);
        incrementerClass.setRequired(false);
        incrementerClass.setValueMap(Ordering.natural().immutableSortedCopy(incNames));

        VerticalPanel panel = new VerticalPanel();
        panel.setStyleName("fill-layout");

        form = new Form<>(PoolConfig.class);
        form.setNumColumns(2);
        form.setEnabled(false);
        form.setFields(minCon, initialCon, maxCon, prefill, flushStrategy, strictMin, useFastFail,
                decrementerClass, decrementerProperties, incrementerClass, incrementerProperties);

        form.addFormValidator((formItems, outcome) -> {
            PoolConfig updatedEntity = form.getUpdatedEntity();
            // only works on real values
            if (ExpressionAdapter.getExpressions(updatedEntity).isEmpty()) {
                int minPoolSize = updatedEntity.getMinPoolSize();
                int maxPoolSize = updatedEntity.getMaxPoolSize();
                if (minPoolSize > maxPoolSize) {
                    outcome.addError("maxPoolSize");
                    maxCon.setErroneous(true);
                    maxCon.setErrMessage("Max Pool Size must be greater than Min Pool Size");
                }
            }
        });

        FormToolStrip<PoolConfig> toolStrip = new FormToolStrip<>(form,
                new FormToolStrip.FormCallback<PoolConfig>() {
                    @Override
                    public void onSave(Map<String, Object> changeset) {
                        management.onSavePoolConfig(editedName, changeset);
                    }

                    @Override
                    public void onDelete(PoolConfig entity) {}
                }
        );

        // TODO: https://issues.jboss.org/browse/AS7-3254
        if (Console.getBootstrapContext().isStandalone()) {
            final ToolButtonDropdown flushDropdown = new ToolButtonDropdown("Flush Gracefully",
                    event -> management.onDoFlush(editedName, "flush-gracefully-connection-in-pool"));
            flushDropdown.addItem("Flush Idle", () -> management.onDoFlush(editedName, "flush-idle-connection-in-pool"));
            flushDropdown.addItem("Flush Invalid",
                    () -> management.onDoFlush(editedName, "flush-invalid-connection-in-pool"));
            flushDropdown.addItem("Flush All", () -> management.onDoFlush(editedName, "flush-all-connection-in-pool"));
            toolStrip.addToolButtonRight(flushDropdown);
        }

        FormHelpPanel helpPanel = new FormHelpPanel(() -> {
            ModelNode address = Baseadress.get();
            address.add("subsystem", "datasources");
            if (xaDisplay) { address.add("xa-data-source", "*"); } else { address.add("data-source", "*"); }
            return address;
        }, form);

        panel.add(toolStrip.asWidget());
        panel.add(helpPanel.asWidget());
        panel.add(form.asWidget());
        return panel;
    }

    public Form<PoolConfig> getForm() {
        return form;
    }

    public void updateFrom(String name, PoolConfig poolConfig) {
        this.editedName = name;
        form.edit(poolConfig);
    }
}
