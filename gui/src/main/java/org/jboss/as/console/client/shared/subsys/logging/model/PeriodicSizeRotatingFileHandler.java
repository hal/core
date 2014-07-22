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
package org.jboss.as.console.client.shared.subsys.logging.model;

import org.jboss.as.console.client.shared.viewframework.NamedEntity;
import org.jboss.as.console.client.widgets.forms.Address;
import org.jboss.as.console.client.widgets.forms.Binding;
import org.jboss.as.console.client.widgets.forms.FormItem;

/**
 * Periodic Size Rotating File Handler
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@Address("/subsystem=logging/periodic-size-rotating-file-handler={0}")
public interface PeriodicSizeRotatingFileHandler extends NamedEntity, HasLevel {

    @Override
    @Binding(detypedName = "name", key = true)
    @FormItem(defaultValue = "",
            localLabel = "common_label_name",
            required = true,
            formItemTypeForEdit = "TEXT",
            formItemTypeForAdd = "TEXT_BOX")
    public String getName();

    @Override
    public void setName(String name);

    @Override
    @Binding(detypedName = "level")
    @FormItem(defaultValue = "INFO",
            localLabel = "subsys_logging_logLevel",
            required = true,
            formItemTypeForEdit = "COMBO_BOX",
            formItemTypeForAdd = "COMBO_BOX")
    public String getLevel();

    @Override
    public void setLevel(String logLevel);

    @Binding(detypedName = "encoding")
    @FormItem(defaultValue = "UTF-8",
            localLabel = "subsys_logging_encoding",
            required = false,
            formItemTypeForEdit = "TEXT_BOX",
            formItemTypeForAdd = "TEXT_BOX")
    public String getEncoding();

    public void setEncoding(String encoding);

    /* Filters not implemented yet
    public String getFilter();
    public void setFilter(String filter);
    */

    @Binding(detypedName = "formatter")
    @FormItem(defaultValue = "%d{HH:mm:ss,SSS} %-5p [%c] (%t) %s%E%n",
            localLabel = "subsys_logging_formatter",
            required = true,
            formItemTypeForEdit = "FREE_FORM_TEXT_BOX",
            formItemTypeForAdd = "FREE_FORM_TEXT_BOX")
    public String getFormatter();

    public void setFormatter(String formatter);

    @Binding(detypedName = "file/relative-to")
    @FormItem(defaultValue = FormItem.NULL,
            localLabel = "subsys_logging_fileRelativeTo",
            required = false,
            formItemTypeForEdit = "TEXT_BOX",
            formItemTypeForAdd = "TEXT_BOX")
    public String getFileRelativeTo();

    public void setFileRelativeTo(String file);

    @Binding(detypedName = "file/path")
    @FormItem(defaultValue = "",
            localLabel = "subsys_logging_filePath",
            required = true,
            formItemTypeForEdit = "TEXT_BOX",
            formItemTypeForAdd = "TEXT_BOX")
    public String getFilePath();

    public void setFilePath(String file);

    @Binding(detypedName = "rotate-size")
    @FormItem(defaultValue = "2m",
            localLabel = "subsys_logging_rotateSize",
            required = true,
            formItemTypeForEdit = "BYTE_UNIT",
            formItemTypeForAdd = "BYTE_UNIT")
    public String getRotateSize();

    public void setRotateSize(String rotateSize);

    @Binding(detypedName = "max-backup-index")
    @FormItem(defaultValue = "1",
            localLabel = "subsys_logging_maxBackupIndex",
            required = true,
            formItemTypeForEdit = "NUMBER_BOX",
            formItemTypeForAdd = "NUMBER_BOX")
    public Integer getMaxBackupIndex();

    public void setMaxBackupIndex(Integer maxBackupIndex);

    @Binding(detypedName = "suffix")
    @FormItem(defaultValue = "",
            localLabel = "subsys_logging_suffix",
            required = true,
            formItemTypeForEdit = "TEXT_BOX",
            formItemTypeForAdd = "TEXT_BOX")
    public String getSuffix();

    public void setSuffix(String suffix);

    @Binding(detypedName = "append")
    @FormItem(defaultValue = "true",
            localLabel = "subsys_logging_append",
            required = false,
            formItemTypeForEdit = "CHECK_BOX",
            formItemTypeForAdd = "CHECK_BOX")
    public boolean isAppend();

    public void setAppend(boolean append);

    @Binding(detypedName = "autoflush")
    @FormItem(defaultValue = "true",
            localLabel = "subsys_logging_autoFlush",
            required = false,
            formItemTypeForEdit = "CHECK_BOX",
            formItemTypeForAdd = "CHECK_BOX")
    public boolean isAutoFlush();

    public void setAutoFlush(boolean autoFlush);
}
