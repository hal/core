package org.jboss.as.console.client.tools.modelling.workbench;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HasHandlers;
import com.google.web.bindery.event.shared.HandlerRegistration;

public class InstrumentEvent extends GwtEvent<InstrumentEvent.InstrumentHandler> {

    public enum SIGNALS {DISABLE_CACHE, ENABLE_CACHE}

    private SIGNALS singal = null;

    public InstrumentEvent(SIGNALS signal) {
        this.singal = signal;
    }

    public SIGNALS getSingal() {
        return singal;
    }

    public static void fire(HasHandlers source, org.jboss.as.console.client.tools.modelling.workbench.repository.Sample sample) {
        ActivateEvent eventInstance = new ActivateEvent(sample);
        source.fireEvent(eventInstance);
    }

    public static void fire(HasHandlers source, InstrumentEvent eventInstance) {
        source.fireEvent(eventInstance);
    }

    public interface HasActivateHandlers extends HasHandlers {
        HandlerRegistration addActivateHandler(InstrumentHandler handler);
    }

    public interface InstrumentHandler extends EventHandler {
        public void onInstrument(InstrumentEvent event);
    }

    private static final Type<InstrumentHandler> TYPE = new Type<InstrumentHandler>();

    public static Type<InstrumentHandler> getType() {
        return TYPE;
    }

    @Override
    public Type<InstrumentHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(InstrumentHandler handler) {
        handler.onInstrument(this);
    }

}
