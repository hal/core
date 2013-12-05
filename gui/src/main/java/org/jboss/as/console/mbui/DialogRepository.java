package org.jboss.as.console.mbui;

import com.google.gwt.user.client.rpc.AsyncCallback;
import org.useware.kernel.model.Dialog;

/**
 * @author Heiko Braun
 * @date 3/22/13
 */
public interface DialogRepository {
    void getDialog(String name, AsyncCallback<Dialog> callback);
}
