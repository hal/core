package org.useware.kernel.gui.behaviour;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HasHandlers;
import org.useware.kernel.model.structure.QName;

/**
 * A navigation event is the created by a {@link ModelDrivenCommand}.
 * It's an instruction to navigate to a different interaction unit.
 *
 * @author Heiko Braun
 * @date 11/15/12
 */
public class NavigationEvent extends GwtEvent<NavigationEvent.NavigationHandler> {

    public static final Type TYPE = new Type<NavigationHandler>();
    private Relation relation;

    private QName id;

    private QName target;

    public static enum Relation {
        None, Prev, Next;

        public static Relation fromString(String s) {
            Relation rel = None;
            if(s.equals("next"))
                rel = Next;
            else if(s.equals("prev"))
                rel = Prev;

            return rel;
        }
    }

    public NavigationEvent(QName id, QName target) {
        super();
        this.id = id;
        this.target = target;
        this.relation = Relation.None;
    }

    public NavigationEvent(QName id, QName target, Relation relation) {
        super();
        this.id = id;
        this.target = target;
        this.relation = relation;
    }

    public Relation getRelation() {
        return relation;
    }

    public QName getId() {
        return id;
    }

    public QName getTarget() {
        return target;
    }

    @Override
    public Type<NavigationHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(NavigationHandler listener) {
        if(listener.accepts(this))
            listener.onNavigationEvent(this);
    }

    public interface NavigationHandler extends EventHandler {
        boolean accepts(NavigationEvent event);
        void onNavigationEvent(NavigationEvent event);
    }

    public static void fire(HasHandlers source, NavigationEvent eventInstance) {
        source.fireEvent(eventInstance);
    }

    @Override
    public String toString() {
        return "NavigationEvent{" +
                "id=" + id +
                ", target=" + target +
                '}';
    }
}
