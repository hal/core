package org.useware.kernel.gui.kernel;

import org.useware.kernel.model.Dialog;

/**
 * @author Heiko Braun
 * @date 3/22/13
 */
public interface DialogRepository {
    Dialog getDialog(String name);
}
