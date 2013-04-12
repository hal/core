package org.useware.kernel.gui.reification.pipeline;

import org.useware.kernel.gui.reification.Context;
import org.useware.kernel.gui.reification.ReificationException;
import org.useware.kernel.model.scopes.ScopeAssignment;
import org.useware.kernel.model.Dialog;

/**
 * @author Heiko Braun
 * @date 3/19/13
 */
public class ScopeAssignmentStep<S extends Enum<S>> extends ReificationStep {


    public ScopeAssignmentStep() {
        super("statement context");

    }

    @Override
    public void execute(Dialog dialog, Context context) throws ReificationException {

        ScopeAssignment scopeAssignment = new ScopeAssignment();
        dialog.getInterfaceModel().accept(scopeAssignment);

        dialog.setScopeModel(scopeAssignment.getShim());

        System.out.println("Discovered "+scopeAssignment.getContextIds().size() +" statement context scopes in dialog "+dialog.getId());
    }




}
