package org.jboss.as.console.client.rbac;

import com.google.web.bindery.autobean.shared.AutoBean;
import com.google.web.bindery.autobean.shared.AutoBeanUtils;

import java.util.HashSet;
import java.util.Set;

/**
 * API to read/write RBAC overlays on entities.
 *
 * @author Heiko Braun
 * @date 10/17/11
 */
public class RBACAdapter {

    private static final String FILTERED_TAG = "filtered-attributes";
    private static final String READ_ONLY_TAG = "readonly-attributes";

    public static void setFilteredAttributes(Object bean, Set<String> atts)
    {
        final AutoBean autoBean = asAutoBean(bean);
        autoBean.setTag(FILTERED_TAG, atts);
    }

    public static Set<String> getFilteredAttributes(Object bean)
    {
        final AutoBean autoBean = asAutoBean(bean);

        Set<String> atts = (Set<String>)autoBean.getTag(FILTERED_TAG);
        if(null==atts)
        {
            atts = new HashSet<String>();
            autoBean.setTag(FILTERED_TAG, atts);
        }

        return atts;
    }

    public static Set<String> getReadonlyAttributes(Object bean)
    {
        final AutoBean autoBean = asAutoBean(bean);

        Set<String> atts = (Set<String>)autoBean.getTag(READ_ONLY_TAG);
        if(null==atts)
        {
            atts = new HashSet<String>();
            autoBean.setTag(READ_ONLY_TAG, atts);
        }

        return atts;
    }

    private static AutoBean asAutoBean(Object bean) {
        final AutoBean autoBean = AutoBeanUtils.getAutoBean(bean);
        if(null==autoBean)
            throw new IllegalArgumentException("Not an auto bean: " + bean.getClass());
        return autoBean;
    }


    public static <T> void setReadOnlyAttributes(T entity, Set<String> readonlyJavaNames) {
        final AutoBean autoBean = asAutoBean(entity);
        autoBean.setTag(READ_ONLY_TAG, readonlyJavaNames);
    }
}
