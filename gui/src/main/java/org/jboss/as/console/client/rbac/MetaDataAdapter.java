package org.jboss.as.console.client.rbac;

import com.allen_sauer.gwt.log.client.Log;
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
        try {
            BeanMetaData beanMetaData = metaData.getBeanMetaData(type);
            for(PropertyBinding propBinding : beanMetaData.getProperties())
            {
                if(!securityContext.getAttributeWritePriviledge(propBinding.getDetypedName()).isGranted()
                        && !propBinding.isKey()) // HAL-202: exclude keys
                    readonlyJavaNames.add(propBinding.getJavaName());
            }
        } catch (Exception e) {
            Log.warn("No meta data for "+type);
        }
        return readonlyJavaNames;
    }

    public Set<String> getReadOnlyJavaNames(Class<?> type, String resourceAddress, SecurityContext securityContext) {
        final Set<String> readonlyJavaNames = new HashSet<String>();
        try {
            BeanMetaData beanMetaData = metaData.getBeanMetaData(type);
            for(PropertyBinding propBinding : beanMetaData.getProperties())
            {
                if(!securityContext.getAttributeWritePriviledge(resourceAddress, propBinding.getDetypedName()).isGranted()
                        && !propBinding.isKey()) // HAL-202: exclude keys
                    readonlyJavaNames.add(propBinding.getJavaName());
            }
        } catch (Exception e) {
            Log.warn("No meta data for "+type);
        }
        return readonlyJavaNames;
    }
}
