package org.useware.kernel.model;

import org.jboss.as.console.client.tools.modelling.workbench.repository.SecurityDomainsSample;
import org.junit.Before;
import org.junit.Test;
import org.useware.kernel.gui.reification.ActivationVisitor;
import org.useware.kernel.model.scopes.ScopeAssignment;
import org.useware.kernel.model.structure.QName;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * @author Heiko Braun
 * @date 4/12/13
 */
public class ActivationTest {

    private Dialog dialog;
    private ScopeAssignment scopeAssignment;

    @Before
    public void setUp()
    {
        this.dialog = new SecurityDomainsSample().getDialog();

        // assign scopes
        scopeAssignment = new ScopeAssignment();
        dialog.getInterfaceModel().accept(scopeAssignment);
        dialog.setScopeModel(scopeAssignment.getShim());
    }

    @Test
    public void testDefaultActivation() {

        ActivationVisitor activation = new ActivationVisitor();
        dialog.getInterfaceModel().accept(activation);
        Map<Integer,QName> activeItems = activation.getActiveItems();
        assertFalse(activeItems.isEmpty());

        for(Integer level : activeItems.keySet())
        {
            QName activeChild = activeItems.get(level);
            System.out.println(level + " > "+activeChild);
        }

        assertEquals("Wrong number of active items", activeItems.size(), 3);
        assertEquals(activeItems.get(3), QName.valueOf("org.jboss.security.domain:details#attributes"));
    }
}
