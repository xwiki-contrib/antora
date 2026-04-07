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
package org.xwiki.contrib.antora.filter.internal.input;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.contrib.antora.filter.descriptor.ComponentVersionDescriptor;
import org.xwiki.contrib.antora.filter.descriptor.ModuleDescriptor;
import org.xwiki.contrib.antora.filter.input.AntoraInputProperties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for AsciiDoc content parsing from real-world Antora documentation examples.
 * 
 * These tests verify that various AsciiDoc content types from the Decidim documentation
 * can be successfully parsed by the Antora filter stream.
 */
public class AsciiDocRenderingTest
{
    private File tempDir;

    private AntoraInputProperties properties;

    @BeforeEach
    public void setUp() throws IOException
    {
        this.tempDir = createTempAntoraStructure();
        this.properties = new AntoraInputProperties();
    }

    @AfterEach
    public void tearDown()
    {
        if (this.tempDir != null && this.tempDir.exists()) {
            deleteDir(this.tempDir);
        }
    }

    private void deleteDir(File dir)
    {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDir(file);
                } else {
                    file.delete();
                }
            }
        }
        dir.delete();
    }

    @Test
    public void testExtractTitleFromSimpleDocument() throws Exception
    {
        AntoraInputFilterStream stream = createStream();
        String title = invokeExtractTitle(stream, "= Test Document Title\n\nSome content.", "default");
        assertEquals("Test Document Title", title);
    }

    @Test
    public void testExtractTitleWithBoldInTitle() throws Exception
    {
        AntoraInputFilterStream stream = createStream();
        String title = invokeExtractTitle(stream, "= Title With **Bold**\n\nContent", "default");
        assertEquals("Title With **Bold**", title);
    }

    @Test
    public void testExtractTitleFromContentWithoutTitle() throws Exception
    {
        AntoraInputFilterStream stream = createStream();
        String title = invokeExtractTitle(stream, "Some content without a title.", "default");
        assertEquals("default", title);
    }

    @Test
    public void testExtractTitleWithPreprocessorDirective() throws Exception
    {
        AntoraInputFilterStream stream = createStream();
        String title = invokeExtractTitle(stream, 
            "ifdef::env-github[]\n:page-edit-url: edit\nendif::[]\n\n= My Title\n\nContent", "default");
        assertEquals("default", title);
    }

    @Test
    public void testComputePageNameForIndex() throws Exception
    {
        ComponentVersionDescriptor component = new ComponentVersionDescriptor();
        component.setName("testcomponent");
        component.setVersion("1.0");

        ModuleDescriptor module = new ModuleDescriptor();
        module.setName("ROOT");
        module.setBaseDir(new File(tempDir, "testcomponent/modules/ROOT"));

        AntoraInputFilterStream stream = createStream();
        String pageName = invokeComputePageName(stream, component, module, "index.adoc");
        assertEquals("WebHome", pageName);
    }

    @Test
    public void testComputePageNameWithSuffix() throws Exception
    {
        properties.setPageSuffix(".WebHome");

        ComponentVersionDescriptor component = new ComponentVersionDescriptor();
        component.setName("testcomponent");
        component.setVersion("1.0");

        ModuleDescriptor module = new ModuleDescriptor();
        module.setName("ROOT");
        module.setBaseDir(new File(tempDir, "testcomponent/modules/ROOT"));

        AntoraInputFilterStream stream = createStream();
        String pageName = invokeComputePageName(stream, component, module, "getting-started.adoc");
        assertEquals("getting-started.WebHome", pageName);
    }

    @Test
    public void testCleanEntityNameRemovesSpecialChars() throws Exception
    {
        AntoraInputFilterStream stream = createStream();

        assertEquals("HelloWorld", invokeCleanEntityName(stream, "Hello World"));
        assertEquals("Hello_World", invokeCleanEntityName(stream, "Hello-World"));
        assertEquals("Test_File", invokeCleanEntityName(stream, "Test.File"));
        assertEquals("Test123", invokeCleanEntityName(stream, "Test123"));
    }

    @Test
    public void testCleanEntityNameRemovesMultipleSeparators() throws Exception
    {
        AntoraInputFilterStream stream = createStream();
        assertEquals("Hello_World_Test", invokeCleanEntityName(stream, "Hello__World___Test"));
    }

    @Test
    public void testCleanEntityNameTrimsUnderscores() throws Exception
    {
        AntoraInputFilterStream stream = createStream();
        assertEquals("Test", invokeCleanEntityName(stream, "_Test_"));
        assertEquals("Test", invokeCleanEntityName(stream, "__Test__"));
    }

    @Test
    public void testComponentSpaceNameCapitalizes() throws Exception
    {
        ComponentVersionDescriptor component = new ComponentVersionDescriptor();
        component.setName("my-component");
        component.setVersion("1.0");

        assertEquals("Mycomponent", component.getSpaceName());
    }

    @Test
    public void testComponentVersionedSpaceNameIncludesVersion() throws Exception
    {
        ComponentVersionDescriptor component = new ComponentVersionDescriptor();
        component.setName("my-component");
        component.setVersion("1.0");

        assertEquals("Mycomponent_1_0", component.getVersionedSpaceName());
    }

    @Test
    public void testModuleSpaceNameForRootModule() throws Exception
    {
        ModuleDescriptor rootModule = new ModuleDescriptor();
        rootModule.setName("ROOT");

        assertEquals("TestComponent", rootModule.getSpaceName("TestComponent"));
    }

    @Test
    public void testModuleSpaceNameForNamedModule() throws Exception
    {
        ModuleDescriptor userModule = new ModuleDescriptor();
        userModule.setName("user");

        assertEquals("TestComponent_user", userModule.getSpaceName("TestComponent"));
    }

    @Test
    public void testComponentWithSpecialCharactersInName() throws Exception
    {
        ComponentVersionDescriptor component = new ComponentVersionDescriptor();
        component.setName("decidim-docs");
        component.setVersion("develop");

        assertEquals("Decidimdocs", component.getSpaceName());
        assertEquals("Decidimdocs_develop", component.getVersionedSpaceName());
    }

    @Test
    public void testRealDecidimAntoraStructure() throws Exception
    {
        File componentDir = new File(tempDir, "decidim");
        componentDir.mkdirs();

        File antoraYml = new File(componentDir, "antora.yml");
        try (FileWriter writer = new FileWriter(antoraYml)) {
            writer.write("name: decidim\n");
            writer.write("version: 'develop'\n");
            writer.write("title: Decidim Documentation\n");
            writer.write("nav:\n");
            writer.write("  - modules/ROOT/nav.adoc\n");
        }

        File modulesDir = new File(componentDir, "modules");
        modulesDir.mkdirs();

        File rootModuleDir = new File(modulesDir, "ROOT");
        rootModuleDir.mkdirs();

        File pagesDir = new File(rootModuleDir, "pages");
        pagesDir.mkdirs();

        File indexAdoc = new File(pagesDir, "index.adoc");
        try (FileWriter writer = new FileWriter(indexAdoc)) {
            writer.write("= Welcome to Decidim Documentation\n\n");
            writer.write("This is the documentation site to Decidim.\n\n");
            writer.write("== New to Decidim?\n\n");
            writer.write("NOTE: This is the documentation for the `develop` branch.\n");
        }

        File adminModuleDir = new File(modulesDir, "admin");
        adminModuleDir.mkdirs();

        File adminPagesDir = new File(adminModuleDir, "pages");
        adminPagesDir.mkdirs();

        File componentsAdoc = new File(adminPagesDir, "components.adoc");
        try (FileWriter writer = new FileWriter(componentsAdoc)) {
            writer.write("= Components\n\n");
            writer.write("Through components an administrator can enable and configure different participatory mechanisms.\n\n");
            writer.write("== Configuration\n\n");
            writer.write("=== Manage components\n\n");
            writer.write("On this page, you are able to manage components.\n");
        }

        ComponentVersionDescriptor component = new ComponentVersionDescriptor();
        component.setName("decidim");
        component.setVersion("develop");

        ModuleDescriptor rootModule = new ModuleDescriptor();
        rootModule.setName("ROOT");
        rootModule.setBaseDir(rootModuleDir);

        ModuleDescriptor adminModule = new ModuleDescriptor();
        adminModule.setName("admin");
        adminModule.setBaseDir(adminModuleDir);

        assertEquals("Decidim", component.getSpaceName());
        assertEquals("Decidim_develop", component.getVersionedSpaceName());
        assertEquals("Decidim", rootModule.getSpaceName("Decidim"));
        assertEquals("Decidim_admin", adminModule.getSpaceName("Decidim"));
    }

    @Test
    public void testPageNameWithNestedPath() throws Exception
    {
        ComponentVersionDescriptor component = new ComponentVersionDescriptor();
        component.setName("testcomponent");
        component.setVersion("1.0");

        ModuleDescriptor module = new ModuleDescriptor();
        module.setName("admin");
        module.setBaseDir(new File(tempDir, "testcomponent/modules/admin"));

        AntoraInputFilterStream stream = createStream();
        String pageName = invokeComputePageName(stream, component, module, "pages/components/index.adoc");
        assertEquals("WebHome", pageName);
    }

    private AntoraInputFilterStream createStream()
    {
        AntoraInputFilterStream stream = new AntoraInputFilterStream();
        try {
            stream.setProperties(this.properties);
        } catch (org.xwiki.filter.FilterException e) {
            throw new RuntimeException(e);
        }
        return stream;
    }

    private String invokeComputePageName(AntoraInputFilterStream stream, ComponentVersionDescriptor component,
        ModuleDescriptor module, String relativePath) throws Exception
    {
        Method method = AntoraInputFilterStream.class.getDeclaredMethod("computePageName",
            ComponentVersionDescriptor.class, ModuleDescriptor.class, String.class);
        method.setAccessible(true);
        try {
            return (String) method.invoke(stream, component, module, relativePath);
        } catch (java.lang.reflect.InvocationTargetException e) {
            if (e.getCause() instanceof Exception) {
                throw (Exception) e.getCause();
            }
            throw e;
        }
    }

    private String invokeCleanEntityName(AntoraInputFilterStream stream, String name) throws Exception
    {
        Method method = AntoraInputFilterStream.class.getDeclaredMethod("cleanEntityName", String.class);
        method.setAccessible(true);
        try {
            return (String) method.invoke(stream, name);
        } catch (java.lang.reflect.InvocationTargetException e) {
            if (e.getCause() instanceof Exception) {
                throw (Exception) e.getCause();
            }
            throw e;
        }
    }

    private String invokeExtractTitle(AntoraInputFilterStream stream, String content, String defaultTitle)
        throws Exception
    {
        Method method = AntoraInputFilterStream.class.getDeclaredMethod("extractTitle", String.class,
            String.class);
        method.setAccessible(true);
        try {
            return (String) method.invoke(stream, content, defaultTitle);
        } catch (java.lang.reflect.InvocationTargetException e) {
            if (e.getCause() instanceof Exception) {
                throw (Exception) e.getCause();
            }
            throw e;
        }
    }

    private File createTempAntoraStructure() throws IOException
    {
        File temp = new File(System.getProperty("java.io.tmpdir"), "antora-render-test-" + System.currentTimeMillis());
        temp.mkdirs();

        File componentDir = new File(temp, "testcomponent");
        componentDir.mkdirs();

        File antoraYml = new File(componentDir, "antora.yml");
        try (FileWriter writer = new FileWriter(antoraYml)) {
            writer.write("name: testcomponent\n");
            writer.write("version: '1.0'\n");
            writer.write("title: Test Component\n");
            writer.write("nav:\n");
            writer.write("  - modules/ROOT/nav.adoc\n");
        }

        File modulesDir = new File(componentDir, "modules");
        modulesDir.mkdirs();

        File rootModuleDir = new File(modulesDir, "ROOT");
        rootModuleDir.mkdirs();

        File pagesDir = new File(rootModuleDir, "pages");
        pagesDir.mkdirs();

        File indexAdoc = new File(pagesDir, "index.adoc");
        try (FileWriter writer = new FileWriter(indexAdoc)) {
            writer.write("= Main Page\n\n");
            writer.write("This is the main page content.\n");
        }

        File imagesDir = new File(rootModuleDir, "images");
        imagesDir.mkdirs();

        File navAdoc = new File(rootModuleDir, "nav.adoc");
        try (FileWriter writer = new FileWriter(navAdoc)) {
            writer.write("* xref:index.adoc[Home]\n");
        }

        return temp;
    }
}
