package org.jboss.as.console.client.rbac;

import com.google.inject.Inject;
import org.jboss.as.console.client.widgets.forms.ApplicationMetaData;
import org.jboss.as.console.client.widgets.forms.BeanMetaData;
import org.jboss.as.console.client.widgets.forms.PropertyBinding;
import org.jboss.ballroom.client.rbac.SecurityContext;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Heiko Braun
 * @date 8/12/13
 */
public class MetaDataAdapter {

    private ApplicationMetaData metaData;

    @Inject
    public MetaDataAdapter(ApplicationMetaData metaData) {
        this.metaData = metaData;
    }

    public Set<String> getReadOnlyJavaNames(Class<?> type, SecurityContext securityContext) {
        final Set<String> readonlyJavaNames = new HashSet<String>();
        BeanMetaData beanMetaData = metaData.getBeanMetaData(type);
        for(PropertyBinding propBinding : beanMetaData.getProperties())
        {
            if(!securityContext.getAttributeWritePriviledge(propBinding.getDetypedName()).isGranted())
                readonlyJavaNames.add(propBinding.getJavaName());
        }
        return readonlyJavaNames;
    }
}
