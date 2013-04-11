package org.useware.kernel.gui.behaviour;

import org.useware.kernel.model.structure.QName;

/**
 * @author Heiko Braun
 * @date 3/14/13
 */
public interface NavigationDelegate {

    void onNavigation(QName source, QName dialog);
}
