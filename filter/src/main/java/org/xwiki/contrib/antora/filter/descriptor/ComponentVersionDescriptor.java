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
import java.util.Map;

/**
 * Descriptor for an Antora component version.
 *
 * @version $Id$
 */
public class ComponentVersionDescriptor
{
    private String name;

    private String version;

    private String title;

    private String displayVersion;

    private boolean prerelease;

    private String startPage;

    private List<String> nav;

    private File baseDir;

    private Map<String, ModuleDescriptor> modules;

    /**
     * @return the component name
     */
    public String getName()
    {
        return this.name;
    }

    /**
     * @param name the component name
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * @return the version string
     */
    public String getVersion()
    {
        return this.version;
    }

    /**
     * @param version the version string
     */
    public void setVersion(String version)
    {
        this.version = version;
    }

    /**
     * @return the component title
     */
    public String getTitle()
    {
        return this.title;
    }

    /**
     * @param title the component title
     */
    public void setTitle(String title)
    {
        this.title = title;
    }

    /**
     * @return the display version
     */
    public String getDisplayVersion()
    {
        return this.displayVersion;
    }

    /**
     * @param displayVersion the display version
     */
    public void setDisplayVersion(String displayVersion)
    {
        this.displayVersion = displayVersion;
    }

    /**
     * @return true if this is a prerelease version
     */
    public boolean isPrerelease()
    {
        return this.prerelease;
    }

    /**
     * @param prerelease true if this is a prerelease version
     */
    public void setPrerelease(boolean prerelease)
    {
        this.prerelease = prerelease;
    }

    /**
     * @return the start page path
     */
    public String getStartPage()
    {
        return this.startPage;
    }

    /**
     * @param startPage the start page path
     */
    public void setStartPage(String startPage)
    {
        this.startPage = startPage;
    }

    /**
     * @return the navigation file paths
     */
    public List<String> getNav()
    {
        return this.nav != null ? this.nav : Collections.emptyList();
    }

    /**
     * @param nav the navigation file paths
     */
    public void setNav(List<String> nav)
    {
        this.nav = nav;
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
     * @return the module descriptors
     */
    public Map<String, ModuleDescriptor> getModules()
    {
        return this.modules != null ? this.modules : Collections.emptyMap();
    }

    /**
     * @param modules the module descriptors
     */
    public void setModules(Map<String, ModuleDescriptor> modules)
    {
        this.modules = modules;
    }

    /**
     * @return the XWiki space name for this component
     */
    public String getSpaceName()
    {
        return capitalizeName(cleanName(this.name));
    }

    /**
     * @return the XWiki space name including the version
     */
    public String getVersionedSpaceName()
    {
        String spaceName = getSpaceName();
        if (this.version != null && !this.version.isEmpty()) {
            spaceName = spaceName + '_' + this.version.replace('.', '_');
        }
        return spaceName;
    }

    private String cleanName(String name)
    {
        if (name == null) {
            return "";
        }
        return name.replaceAll("[^a-zA-Z0-9]", "");
    }

    private String capitalizeName(String name)
    {
        if (name == null || name.isEmpty()) {
            return name;
        }
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }
}
