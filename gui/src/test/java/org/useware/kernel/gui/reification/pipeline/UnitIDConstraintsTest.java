package org.useware.kernel.gui.reification.pipeline;

import static org.useware.kernel.model.structure.TemporalOperator.Choice;
import static org.useware.kernel.model.structure.TemporalOperator.Concurrency;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.useware.kernel.gui.reification.Context;
import org.useware.kernel.gui.reification.ReificationException;
import org.useware.kernel.model.Dialog;
import org.useware.kernel.model.structure.Container;
import org.useware.kernel.model.structure.InteractionUnit;
import org.useware.kernel.model.structure.QName;
import org.useware.kernel.model.structure.Select;
import org.useware.kernel.model.structure.builder.Builder;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Harald Pehl
 * @date 03/14/2013
 */
public class UnitIDConstraintsTest
{
    ReificationPipeline pipeline;

    @Before
    public void setUp()
    {
        pipeline = new ReificationPipeline(new UniqueIdCheckStep());
    }

    @Test
    public void uniqueIds()
    {
        String namespace = "org.jboss.sample";
        InteractionUnit root = new Builder()
                .start(new Container(namespace, "sample", "Sample", Choice))
                    .start(new Container(namespace, "tab1", "Fooo", Concurrency))
                        .add(new Select(namespace, "list", "List"))
                    .end()
                    .start(new Container(namespace, "tab2", "Bar", Concurrency))
                    .end()
                .end()
                .build();
        Dialog dialog = new Dialog(QName.valueOf(namespace + ":sample"), root);
        pipeline.execute(dialog, new Context());
    }

    @Test
    public void noneUniqueIds()
    {
        String namespace = "org.jboss.sample";
        InteractionUnit root = new Builder()
                .start(new Container(namespace, "sample", "Sample", Choice))
                    .start(new Container(namespace, "tab1", "Fooo", Concurrency))
                        .add(new Select(namespace, "list", "List1"))
                    .end()
                    .start(new Container(namespace, "tab2", "Bar", Concurrency))
                        .add(new Select(namespace, "list", "List2"))
                    .end()
                .end()
                .build();
        Dialog dialog = new Dialog(QName.valueOf(namespace + ":sample"), root);

        try
        {
            pipeline.execute(dialog, new Context());
            fail("ReificationException expected");
        }
        catch (ReificationException e)
        {
            String message = e.getMessage();
            assertTrue(message.contains("list: 2"));
        }
    }
}
