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
package org.xwiki.contrib.antora.filter.input;

import java.util.Locale;

import org.xwiki.filter.DefaultFilterStreamProperties;
import org.xwiki.filter.input.InputSource;
import org.xwiki.filter.type.FilterStreamType;
import org.xwiki.filter.type.SystemType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.properties.annotation.PropertyDescription;
import org.xwiki.properties.annotation.PropertyMandatory;
import org.xwiki.properties.annotation.PropertyName;

/**
 * Properties for configuring Antora input filter stream.
 *
 * @version $Id$
 */
public class AntoraInputProperties extends DefaultFilterStreamProperties
{
    /**
     * The Antora filter format as a single String.
     */
    public static final String FILTER_STREAM_TYPE_STRING = "antora+docs";

    /**
     * Create a new System Type for Antora.
     */
    private static final SystemType ANTORA = new SystemType("antora");

    /**
     * The Data Format for FilterStreamType.
     */
    private static final String DATA_DOCS = "docs";

    /**
     * The Filter Stream Type for Antora documentation.
     */
    public static final FilterStreamType FILTER_STREAM_TYPE = new FilterStreamType(ANTORA, DATA_DOCS);

    private InputSource source;

    private String targetSyntax = "xwiki/2.1";

    private Locale defaultLocale;

    private EntityReference root;

    private boolean createNavigationPage = true;

    private boolean importImages = true;

    private boolean importAttachments = true;

    private String pageSuffix = "";

    private boolean cleanPageNames = true;

    private String componentFilter;

    private String versionFilter;

    private String moduleFilter;

    /**
     * @return the source directory or archive containing Antora documentation
     */
    @PropertyName("Source")
    @PropertyDescription("The source directory or archive containing Antora documentation")
    @PropertyMandatory
    public InputSource getSource()
    {
        return this.source;
    }

    /**
     * @param source the source directory or archive
     */
    public void setSource(InputSource source)
    {
        this.source = source;
    }

    /**
     * @return the XWiki syntax to convert the content to
     */
    @PropertyName("Target Syntax")
    @PropertyDescription("The XWiki syntax to convert the content to (e.g., xwiki/2.1, markdown/1.2)")
    public String getTargetSyntax()
    {
        return this.targetSyntax;
    }

    /**
     * @param targetSyntax the XWiki syntax
     */
    public void setTargetSyntax(String targetSyntax)
    {
        this.targetSyntax = targetSyntax;
    }

    /**
     * @return the locale of the documents
     */
    @PropertyName("Default Locale")
    @PropertyDescription("The locale of the documents")
    public Locale getDefaultLocale()
    {
        return this.defaultLocale;
    }

    /**
     * @param defaultLocale the locale of the documents
     */
    public void setDefaultLocale(Locale defaultLocale)
    {
        this.defaultLocale = defaultLocale;
    }

    /**
     * @return the wiki or space in which pages will be imported
     */
    @PropertyName("Root")
    @PropertyDescription("The wiki or space in which pages will be imported. "
        + "Examples: wiki:sub, space:sub:RootInSubWiki, MyRoot")
    public EntityReference getRoot()
    {
        return this.root;
    }

    /**
     * @param root the root entity reference
     */
    public void setRoot(EntityReference root)
    {
        this.root = root;
    }

    /**
     * @return whether to create a navigation page
     */
    @PropertyName("Create Navigation Page")
    @PropertyDescription("Create a page with navigation links based on the nav.adoc file")
    public boolean isCreateNavigationPage()
    {
        return this.createNavigationPage;
    }

    /**
     * @param createNavigationPage whether to create a navigation page
     */
    public void setCreateNavigationPage(boolean createNavigationPage)
    {
        this.createNavigationPage = createNavigationPage;
    }

    /**
     * @return whether to import images
     */
    @PropertyName("Import Images")
    @PropertyDescription("Import images from the modules")
    public boolean isImportImages()
    {
        return this.importImages;
    }

    /**
     * @param importImages whether to import images
     */
    public void setImportImages(boolean importImages)
    {
        this.importImages = importImages;
    }

    /**
     * @return whether to import attachments
     */
    @PropertyName("Import Attachments")
    @PropertyDescription("Import attachments from the modules")
    public boolean isImportAttachments()
    {
        return this.importAttachments;
    }

    /**
     * @param importAttachments whether to import attachments
     */
    public void setImportAttachments(boolean importAttachments)
    {
        this.importAttachments = importAttachments;
    }

    /**
     * @return the suffix to add to all page names
     */
    @PropertyName("Page Suffix")
    @PropertyDescription("Suffix to add to all page names (e.g., .WebHome)")
    public String getPageSuffix()
    {
        return this.pageSuffix;
    }

    /**
     * @param pageSuffix the suffix for page names
     */
    public void setPageSuffix(String pageSuffix)
    {
        this.pageSuffix = pageSuffix;
    }

    /**
     * @return whether to clean page names
     */
    @PropertyName("Clean Page Names")
    @PropertyDescription("Clean page names to be valid XWiki entity names")
    public boolean isCleanPageNames()
    {
        return this.cleanPageNames;
    }

    /**
     * @param cleanPageNames whether to clean page names
     */
    public void setCleanPageNames(boolean cleanPageNames)
    {
        this.cleanPageNames = cleanPageNames;
    }

    /**
     * @return the filter for components to import
     */
    @PropertyName("Component Filter")
    @PropertyDescription("Filter to import only specific components by name (comma-separated)")
    public String getComponentFilter()
    {
        return this.componentFilter;
    }

    /**
     * @param componentFilter the component filter
     */
    public void setComponentFilter(String componentFilter)
    {
        this.componentFilter = componentFilter;
    }

    /**
     * @return the filter for versions to import
     */
    @PropertyName("Version Filter")
    @PropertyDescription("Filter to import only specific versions (comma-separated)")
    public String getVersionFilter()
    {
        return this.versionFilter;
    }

    /**
     * @param versionFilter the version filter
     */
    public void setVersionFilter(String versionFilter)
    {
        this.versionFilter = versionFilter;
    }

    /**
     * @return the filter for modules to import
     */
    @PropertyName("Module Filter")
    @PropertyDescription("Filter to import only specific modules (comma-separated)")
    public String getModuleFilter()
    {
        return this.moduleFilter;
    }

    /**
     * @param moduleFilter the module filter
     */
    public void setModuleFilter(String moduleFilter)
    {
        this.moduleFilter = moduleFilter;
    }
}
