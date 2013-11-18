package org.jboss.as.console.client.shared;

import com.google.gwt.user.client.rpc.AsyncCallback;
import org.jboss.as.console.client.tools.modelling.workbench.repository.IOSubsystemExample;
import org.jboss.as.console.client.tools.modelling.workbench.repository.Sample;
import org.jboss.as.console.client.tools.modelling.workbench.repository.ServletContainerExample;
import org.jboss.as.console.client.tools.modelling.workbench.repository.UndertowExample;
import org.jboss.as.console.client.tools.modelling.workbench.repository.UndertowServerExample;
import org.jboss.as.console.mbui.DialogRepository;
import org.useware.kernel.model.Dialog;

import java.util.ArrayList;

/**
 * @author Heiko Braun
 * @date 9/2/13
 */
public class CommonDialogs implements DialogRepository {
    private final ArrayList<Sample> dialogs;

    public CommonDialogs() {
        dialogs = new ArrayList<Sample>();

        dialogs.add(new UndertowExample());
        dialogs.add(new ServletContainerExample());
        dialogs.add(new UndertowServerExample());
        dialogs.add(new IOSubsystemExample());
    }
    @Override
    public void getDialog(String name, AsyncCallback<Dialog> callback) {
        Dialog dialog = null;

        for(Sample sample : dialogs)
        {
            if(sample.getName().equals(name))
            {
                dialog = sample.getDialog();
                break;
            }
        }
        callback.onSuccess(dialog);
    }
}
