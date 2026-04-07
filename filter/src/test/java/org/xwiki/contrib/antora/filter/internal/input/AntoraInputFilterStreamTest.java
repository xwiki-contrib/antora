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
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.contrib.antora.filter.descriptor.ComponentVersionDescriptor;
import org.xwiki.contrib.antora.filter.descriptor.ModuleDescriptor;
import org.xwiki.contrib.antora.filter.input.AntoraInputProperties;
import org.xwiki.filter.FilterException;

import static org.junit.jupiter.api.Assertions.*;

public class AntoraInputFilterStreamTest
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

    private File createTempAntoraStructure() throws IOException
    {
        File temp = new File(System.getProperty("java.io.tmpdir"), "antora-test-" + System.currentTimeMillis());
        temp.mkdirs();

        File componentDir = new File(temp, "mycomponent");
        componentDir.mkdirs();

        File antoraYml = new File(componentDir, "antora.yml");
        try (FileWriter writer = new FileWriter(antoraYml)) {
            writer.write("name: mycomponent\n");
            writer.write("version: '1.0'\n");
            writer.write("title: My Component\n");
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
            writer.write("This is the main page content.\n\n");
            writer.write("== Section One\n\n");
            writer.write("Content of section one.\n\n");
            writer.write("* bullet item\n");
            writer.write("** nested item\n");
        }

        File pageAdoc = new File(pagesDir, "getting-started.adoc");
        try (FileWriter writer = new FileWriter(pageAdoc)) {
            writer.write("= Getting Started\n\n");
            writer.write("Welcome to the getting started guide.\n\n");
            writer.write("----\n");
            writer.write("code example\n");
            writer.write("----\n");
        }

        File imagesDir = new File(rootModuleDir, "images");
        imagesDir.mkdirs();

        File navAdoc = new File(rootModuleDir, "nav.adoc");
        try (FileWriter writer = new FileWriter(navAdoc)) {
            writer.write("* xref:index.adoc[Home]\n");
            writer.write("* xref:getting-started.adoc[Getting Started]\n");
        }

        File userModuleDir = new File(modulesDir, "user");
        userModuleDir.mkdirs();

        File userPagesDir = new File(userModuleDir, "pages");
        userPagesDir.mkdirs();

        File userGuideAdoc = new File(userPagesDir, "user-guide.adoc");
        try (FileWriter writer = new FileWriter(userGuideAdoc)) {
            writer.write("= User Guide\n\n");
            writer.write("This is the user guide.\n");
        }

        return temp;
    }

    @Test
    public void testComputePageName() throws Exception
    {
        ComponentVersionDescriptor component = new ComponentVersionDescriptor();
        component.setName("mycomponent");
        component.setVersion("1.0");

        ModuleDescriptor module = new ModuleDescriptor();
        module.setName("ROOT");
        module.setBaseDir(new File(tempDir, "mycomponent/modules/ROOT"));

        AntoraInputFilterStream stream = createStream();
        String pageName = invokeComputePageName(stream, component, module, "index.adoc");

        assertEquals("WebHome", pageName);
    }

    @Test
    public void testComputePageNameWithSuffix() throws Exception
    {
        properties.setPageSuffix(".WebHome");

        ComponentVersionDescriptor component = new ComponentVersionDescriptor();
        component.setName("mycomponent");
        component.setVersion("1.0");

        ModuleDescriptor module = new ModuleDescriptor();
        module.setName("ROOT");

        AntoraInputFilterStream stream = createStream();
        String pageName = invokeComputePageName(stream, component, module, "getting-started.adoc");

        assertEquals("getting-started.WebHome", pageName);
    }

    @Test
    public void testCleanEntityName() throws Exception
    {
        AntoraInputFilterStream stream = createStream();

        assertEquals("HelloWorld", invokeCleanEntityName(stream, "Hello World"));
        assertEquals("Hello_World", invokeCleanEntityName(stream, "Hello-World"));
        assertEquals("Hello_World_Test", invokeCleanEntityName(stream, "Hello__World___Test"));
        assertEquals("Test123", invokeCleanEntityName(stream, "Test123"));
        assertEquals("Test_File", invokeCleanEntityName(stream, "Test.File"));
    }

    @Test
    public void testExtractTitle() throws Exception
    {
        AntoraInputFilterStream stream = createStream();

        assertEquals("Main Title", invokeExtractTitle(stream, "= Main Title\n\nSome content", "default"));

        assertEquals("default", invokeExtractTitle(stream, "Some content without title", "default"));

        assertEquals("Title With *Bold*", invokeExtractTitle(stream, "= Title With *Bold*\n\nContent", "default"));
    }

    @Test
    public void testComponentSpaceName()
    {
        ComponentVersionDescriptor component = new ComponentVersionDescriptor();
        component.setName("my-component");
        component.setVersion("1.0");

        assertEquals("Mycomponent", component.getSpaceName());
        assertEquals("Mycomponent_1_0", component.getVersionedSpaceName());
    }

    @Test
    public void testModuleSpaceName()
    {
        ModuleDescriptor rootModule = new ModuleDescriptor();
        rootModule.setName("ROOT");

        assertEquals("MyComponent", rootModule.getSpaceName("MyComponent"));

        ModuleDescriptor userModule = new ModuleDescriptor();
        userModule.setName("user");

        assertEquals("MyComponent_user", userModule.getSpaceName("MyComponent"));
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
}
