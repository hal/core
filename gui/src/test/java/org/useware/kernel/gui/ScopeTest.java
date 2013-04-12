package org.useware.kernel.gui;

import org.jboss.as.console.mbui.model.mapping.DMRMapping;
import org.junit.Before;
import org.junit.Test;
import org.useware.kernel.model.Dialog;
import org.useware.kernel.model.mapping.Mapping;
import org.useware.kernel.model.structure.Container;
import org.useware.kernel.model.structure.InteractionUnit;
import org.useware.kernel.model.structure.QName;
import org.useware.kernel.model.structure.builder.Builder;

import static org.jboss.as.console.mbui.model.StereoTypes.Form;
import static org.junit.Assert.assertTrue;
import static org.useware.kernel.model.structure.TemporalOperator.Choice;

/**
 * @author Heiko Braun
 * @date 4/12/13
 */
public class ScopeTest {

    private Dialog dialog;

    @Before
    public void setUp()
    {

        String ns = "org.jboss.transactions";

        // entities
        Mapping global = new DMRMapping()
                .setAddress("/{selected.profile}/subsystem=transactions");

        Mapping basicAttributesMapping = new DMRMapping()
                .addAttributes(
                        "enable-statistics", "enable-tsm-status", "jts", "default-timeout",
                        "node-identifier", "use-hornetq-store");

        Mapping processMapping = new DMRMapping()
                .addAttributes("process-id-uuid", "process-id-socket-binding");

        Mapping recoveryMapping = new DMRMapping()
                .addAttributes("recovery-listener", "socket-binding");

        Container overview = new Container(ns, "transactionManager", "TransactionManager");

        Container basicAttributes = new Container(ns, "transactionManager#basicAttributes", "Attributes",Form);

        Container details = new Container(ns, "configGroups", "Details", Choice);

        Container processAttributes = new Container(ns, "transactionManager#processAttributes", "Process ID",Form);

        Container recoveryAttributes = new Container(ns, "transactionManager#recoveryAttributes", "Recovery",Form);

        // structure & mapping
        InteractionUnit root = new Builder()
                .start(overview)
                .mappedBy(global)
                .add(basicAttributes).mappedBy(basicAttributesMapping)
                .start(details)
                .add(processAttributes).mappedBy(processMapping)
                .add(recoveryAttributes).mappedBy(recoveryMapping)
                .end()
                .end()
                .build();

        this.dialog = new Dialog(QName.valueOf("org.jboss.as:transaction-subsystem"), root);

    }

    @Test
    public void testScopeAssignment()
    {

    }

}
