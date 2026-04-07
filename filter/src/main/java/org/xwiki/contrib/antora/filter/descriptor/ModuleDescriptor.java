/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
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
package org.xwiki.contrib.antora.filter.descriptor;

import java.io.File;
import java.util.Collections;
import java.util.List;

/**
 * Descriptor for an Antora module.
 *
 * @version $Id$
 */
public class ModuleDescriptor
{
    private String name;

    private File baseDir;

    private File pagesDir;

    private File imagesDir;

    private File attachmentsDir;

    private File partialsDir;

    private File examplesDir;

    private List<File> pages;

    /**
     * @return the module name
     */
    public String getName()
    {
        return this.name;
    }

    /**
     * @param name the module name
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * @return the base directory
     */
    public File getBaseDir()
    {
        return this.baseDir;
    }

    /**
     * @param baseDir the base directory
     */
    public void setBaseDir(File baseDir)
    {
        this.baseDir = baseDir;
    }

    /**
     * @return the pages directory
     */
    public File getPagesDir()
    {
        return this.pagesDir;
    }

    /**
     * @param pagesDir the pages directory
     */
    public void setPagesDir(File pagesDir)
    {
        this.pagesDir = pagesDir;
    }

    /**
     * @return the images directory
     */
    public File getImagesDir()
    {
        return this.imagesDir;
    }

    /**
     * @param imagesDir the images directory
     */
    public void setImagesDir(File imagesDir)
    {
        this.imagesDir = imagesDir;
    }

    /**
     * @return the attachments directory
     */
    public File getAttachmentsDir()
    {
        return this.attachmentsDir;
    }

    /**
     * @param attachmentsDir the attachments directory
     */
    public void setAttachmentsDir(File attachmentsDir)
    {
        this.attachmentsDir = attachmentsDir;
    }

    /**
     * @return the partials directory
     */
    public File getPartialsDir()
    {
        return this.partialsDir;
    }

    /**
     * @param partialsDir the partials directory
     */
    public void setPartialsDir(File partialsDir)
    {
        this.partialsDir = partialsDir;
    }

    /**
     * @return the examples directory
     */
    public File getExamplesDir()
    {
        return this.examplesDir;
    }

    /**
     * @param examplesDir the examples directory
     */
    public void setExamplesDir(File examplesDir)
    {
        this.examplesDir = examplesDir;
    }

    /**
     * @return the list of page files
     */
    public List<File> getPages()
    {
        return this.pages != null ? this.pages : Collections.emptyList();
    }

    /**
     * @param pages the list of page files
     */
    public void setPages(List<File> pages)
    {
        this.pages = pages;
    }

    /**
     * @param componentName the component name
     * @return the XWiki space name for this module
     */
    public String getSpaceName(String componentName)
    {
        if ("ROOT".equals(this.name)) {
            return cleanName(componentName);
        }
        return cleanName(componentName) + '_' + cleanName(this.name);
    }

    private String cleanName(String name)
    {
        if (name == null) {
            return "";
        }
        return name.replaceAll("[^a-zA-Z0-9]", "");
    }
}
