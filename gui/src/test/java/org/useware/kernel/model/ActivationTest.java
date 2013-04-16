package org.useware.kernel.model;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.useware.kernel.gui.reification.ActivationVisitor;
import org.useware.kernel.model.scopes.ScopeAssignment;
import org.useware.kernel.model.structure.Container;
import org.useware.kernel.model.structure.InteractionUnit;
import org.useware.kernel.model.structure.Output;
import org.useware.kernel.model.structure.QName;
import org.useware.kernel.model.structure.Select;
import org.useware.kernel.model.structure.builder.Builder;

import static org.jboss.as.console.mbui.model.StereoTypes.Form;
import static org.useware.kernel.model.structure.TemporalOperator.Choice;

/**
 * @author Heiko Braun
 * @date 4/12/13
 */
public class ActivationTest {

    private Dialog dialog;
    private static String ns = "org.jboss.transactions";
    private ScopeAssignment scopeAssignment;

    static final QName basicAttributes = new QName(ns, "transactionManager#basicAttributes");
    static final QName processAttributes = new QName(ns, "transactionManager#processAttributes");
    static final QName recoveryAttributes = new QName(ns, "transactionManager#recoveryAttributes");

    @Before
    public void setUp()
    {
        Container overview = new Container(ns, "transactionManager", "TransactionManager");

        Container basicAttributes = new Container(ns, "transactionManager#basicAttributes", "Attributes",Form);

        Container details = new Container(ns, "configGroups", "Details", Choice);

        Container processAttributes = new Container(ns, "transactionManager#processAttributes", "Process ID",Form);

        Container recoveryAttributes = new Container(ns, "transactionManager#recoveryAttributes", "Recovery",Form);

        // structure & mapping
        InteractionUnit root = new Builder()
                .start(overview)
                    .start(basicAttributes)
                        .add(new Select(ns, "selection", "A List"))
                        .add(new Output(ns, "output", "Some output"))
                    .end()
                    .start(details)
                        .add(processAttributes)
                        .add(recoveryAttributes)
                    .end()
                .end()
        .build();

        this.dialog = new Dialog(QName.valueOf("org.jboss.as:transaction-subsystem"), root);

        // assign scopes
        scopeAssignment = new ScopeAssignment();
        dialog.getInterfaceModel().accept(scopeAssignment);
        dialog.setScopeModel(scopeAssignment.getShim());
    }

    @Test
    public void testDefaultActivation() {

        ActivationVisitor activation = new ActivationVisitor();
        dialog.getInterfaceModel().accept(activation);
        assertNotNull(activation.getCandidate());
        assertEquals("transactionManager#processAttributes", activation.getCandidate().getId().getLocalPart());

    }
}
