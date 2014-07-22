/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.console.client.shared.subsys.logging;

import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.shared.subsys.logging.LoggingLevelProducer.LogLevelConsumer;
import org.jboss.as.console.client.shared.subsys.logging.model.PeriodicSizeRotatingFileHandler;
import org.jboss.as.console.client.shared.viewframework.FrameworkView;
import org.jboss.as.console.client.widgets.forms.ApplicationMetaData;
import org.jboss.as.console.client.widgets.forms.FormMetaData;
import org.jboss.as.console.client.widgets.forms.PropertyBinding;
import org.jboss.ballroom.client.widgets.forms.Form;
import org.jboss.ballroom.client.widgets.forms.FormAdapter;
import org.jboss.ballroom.client.widgets.forms.FormItem;
import org.jboss.ballroom.client.widgets.forms.TextBoxItem;
import org.jboss.dmr.client.dispatch.DispatchAsync;

/**
 * Subview for PeriodicSizeRotatingFileHandler.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class PeriodicSizeRotatingFileHandlerSubview extends AbstractFileHandlerSubview<PeriodicSizeRotatingFileHandler> implements FrameworkView, LogLevelConsumer, HandlerProducer {

    TextBoxItem suffixAdd = new SuffixValidatingTextItem();
    TextBoxItem suffixEdit = new SuffixValidatingTextItem();

    public PeriodicSizeRotatingFileHandlerSubview(ApplicationMetaData applicationMetaData,
                                                  DispatchAsync dispatcher,
                                                  HandlerListManager handlerListManager) {
        super(PeriodicSizeRotatingFileHandler.class, applicationMetaData, dispatcher, handlerListManager);
    }

    @Override
    protected String provideDescription() {
        return Console.CONSTANTS.subsys_logging_periodicSizeRotatingFileHandlers_desc();
    }

    @Override
    protected String getEntityDisplayName() {
        return Console.CONSTANTS.subsys_logging_periodicSizeRotatingFileHandlers();
    }

    @Override
    protected FormAdapter<PeriodicSizeRotatingFileHandler> makeAddEntityForm() {
        final Form<PeriodicSizeRotatingFileHandler> form = new Form<PeriodicSizeRotatingFileHandler>(type);
        form.setNumColumns(1);
        form.setFields(formMetaData.findAttribute("name").getFormItemForAdd(),
                levelItemForAdd,
                formMetaData.findAttribute("filePath").getFormItemForAdd(),
                formMetaData.findAttribute("fileRelativeTo").getFormItemForAdd(),
                formMetaData.findAttribute("maxBackupIndex").getFormItemForAdd(),
                new FormItem[] {suffixAdd});
        return form;
    }

    @Override
    protected FormAdapter<PeriodicSizeRotatingFileHandler> makeEditEntityDetailsForm() {
        final Form<PeriodicSizeRotatingFileHandler> form = new Form<PeriodicSizeRotatingFileHandler>(type);
        form.setNumColumns(2);
        final FormMetaData attributes = getFormMetaData();

        // add base items to form
        final FormItem[][] items = new FormItem[attributes.getBaseAttributes().size()][];
        int i = 0;
        for (PropertyBinding attribute : attributes.getBaseAttributes()) {
            if ("suffix".equals(attribute.getDetypedName())) {
                items[i++] = new FormItem[] {suffixEdit};
                continue;
            }
            items[i++] = attribute.getFormItemForEdit(this);
        }
        form.setFields(items);

        // add grouped items to form
        for (String subgroup : attributes.getGroupNames()) {
            FormItem[][] groupItems = new FormItem[attributes.getGroupedAttribtes(subgroup).size()][];
            int j = 0;
            for (PropertyBinding attribute : attributes.getGroupedAttribtes(subgroup)) {
                groupItems[j++] = attribute.getFormItemForEdit(this);
            }
            form.setFieldsInGroup(subgroup, groupItems);
        }

        return form;
    }
}
