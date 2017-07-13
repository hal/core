package org.jboss.as.console.mbui.widgets;

import org.jboss.dmr.client.ModelNode;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Anup Dey (andey@redhat.com)
 */
public class AddressUtilsTest {

    @Test
    public void testToString() {
        ModelNode address = new ModelNode();
        address.add("subsystem", "test");
        address.add("resource", "name");

        String s = AddressUtils.toString(address, true);
        Assert.assertEquals("subsystem=test/resource=name", s);
    }

    @Test
    public void testToStringValueContainsSlash() {
        ModelNode address = new ModelNode();
        address.add("subsystem", "test");
        address.add("resource", "java:/global/a");

        String s = AddressUtils.toString(address, true);
        Assert.assertEquals("subsystem=test/resource=java\\:\\/global\\/a", s);
    }

    @Test
    public void testAsKey() {
        ModelNode address = new ModelNode();
        address.add("subsystem", "test");
        address.add("resource", "name");

        String s = AddressUtils.asKey(address, true);
        Assert.assertEquals("subsystem=test/resource=name", s);
    }

    @Test
    public void testAsKeyValueContainsSlash() {
        ModelNode address = new ModelNode();
        address.add("subsystem", "test");
        address.add("resource", "java:/global/a");

        String s = AddressUtils.asKey(address, true);
        Assert.assertEquals("subsystem=test/resource=java\\:\\/global\\/a", s);
    }

}
