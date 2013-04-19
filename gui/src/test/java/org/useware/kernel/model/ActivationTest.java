package org.useware.kernel.model;

import org.jboss.as.console.client.tools.modelling.workbench.repository.SecurityDomainsSample;
import org.junit.Before;
import org.junit.Test;
import org.useware.kernel.gui.behaviour.DialogState;
import org.useware.kernel.model.scopes.DefaultActivation;
import org.useware.kernel.model.structure.QName;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Heiko Braun
 * @date 4/12/13
 */
public class ActivationTest {

    private Dialog dialog;

    @Before
    public void setUp()
    {
        this.dialog = new SecurityDomainsSample().getDialog();
    }

    @Test
    public void testDefaultActivation() {

        DefaultActivation activation = new DefaultActivation();
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

    @Test
    public void testActivationCapability() {

        DialogState dialogState = new DialogState(dialog, new NoopContext());

        QName availableDomains = QName.valueOf("org.jboss.security.domain:availableDomains");
        QName addButton = QName.valueOf("org.jboss.security.domain.authentication:add");
        QName moduleOptions = QName.valueOf("org.jboss.security.domain.authentication:moduleOptions");

        assertTrue("Should be capable of activation", dialogState.canBeActivated(availableDomains));
        assertFalse("Should not be capable of activation", dialogState.canBeActivated(addButton));
        assertTrue("Should be capable of activation", dialogState.canBeActivated(moduleOptions));
    }
}
