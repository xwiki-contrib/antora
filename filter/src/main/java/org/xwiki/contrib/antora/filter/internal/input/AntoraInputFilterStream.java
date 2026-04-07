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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.yaml.snakeyaml.Yaml;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.contrib.antora.filter.descriptor.ComponentVersionDescriptor;
import org.xwiki.contrib.antora.filter.descriptor.ModuleDescriptor;
import org.xwiki.contrib.antora.filter.input.AntoraInputProperties;
import org.xwiki.contrib.antora.filter.internal.AntoraFilter;
import org.xwiki.filter.FilterEventParameters;
import org.xwiki.filter.FilterException;
import org.xwiki.filter.event.model.WikiDocumentFilter;
import org.xwiki.filter.input.AbstractBeanInputFilterStream;
import org.xwiki.filter.input.DefaultFileInputSource;
import org.xwiki.filter.input.FileInputSource;
import org.xwiki.job.event.status.JobProgressManager;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.rendering.syntax.Syntax;

/**
 * Main Antora input filter stream implementation.
 *
 * @version $Id$
 */
@Component
@Named(AntoraInputFilterStreamFactory.ROLEHINT)
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class AntoraInputFilterStream
    extends AbstractBeanInputFilterStream<AntoraInputProperties, AntoraFilter>
{
    private static final String WEB_HOME = "WebHome";

    private static final String MODULES_DIR = "modules";

    private static final String PAGES_DIR = "pages";

    private static final String IMAGES_DIR = "images";

    private static final String ATTACHMENTS_DIR = "attachments";

    private static final String PARTIALS_DIR = "partials";

    private static final String EXAMPLES_DIR = "examples";

    private static final String ROOT_MODULE = "ROOT";

    private static final String ADOC_EXT = ".adoc";

    private static final String REVISION_ID = "1.1";

    private static final String COMMA = ",";

    private static final String UNDERSCORE = "_";

    private static final String MIME_OCTET = "application/octet-stream";

    private static final String MIME_JPEG = "image/jpeg";

    private static final String NEWLINE = "\n";

    private static final String DOUBLE_NEWLINE = "\n\n";

    private static final String INDEX_PAGE = "index";

    private static final String DOT = ".";

    private static final String SLASH = "/";

    private static final String INCLUDE_DOC = "{{includeDocument document=\"XWiki.DocumentTree\" /}}";

    private static final String NAVIGATION_TITLE = "Navigation";

    private static final String NAV_ITEM_DOC = "{\"doc\":\"";

    private static final String NAV_ITEM_TITLE = "\", \"title\":\"";

    private static final String NAV_ITEM_END = "\"},";

    private static final String NAV_XREF_PREFIX = "* xref:";

    private static final String VELOCITY_START = "{{velocity}}";

    private static final String VELOCITY_END = "{{/velocity}}";

    private static final String NAV_SET_START = "#set ($navItems = [";

    private static final String NAV_SET_END = "])";

    private static final String IMAGE_OPEN = "[[";

    private static final String IMAGE_CLOSE = "]]";

    private static final String IMAGE_LINK = ">>";

    private static final String NAV_EQUALS = "= Navigation: ";

    private static final String MSG_FAILED_TO_READ_ANTORA = "Failed to read Antora documentation";

    private static final Map<String, String> MIME_TYPES;

    static {
        MIME_TYPES = new HashMap<>();
        MIME_TYPES.put("png", "image/png");
        MIME_TYPES.put("jpg", MIME_JPEG);
        MIME_TYPES.put("jpeg", MIME_JPEG);
        MIME_TYPES.put("gif", "image/gif");
        MIME_TYPES.put("svg", "image/svg+xml");
        MIME_TYPES.put("webp", "image/webp");
    }

    private static final Pattern VALID_NAME_PATTERN = Pattern.compile("[^a-zA-Z0-9]");

    private static final Pattern MULTIPLE_UNDERSCORES = Pattern.compile("__+");

    private static final Pattern LEADING_TRAILING_UNDERSCORE = Pattern.compile("^_|_$");

    @Inject
    private Logger logger;

    @Inject
    private JobProgressManager progress;

    @Inject
    private AsciiDocRenderer asciidocRenderer;

    private final Map<String, ComponentVersionDescriptor> components = new LinkedHashMap<>();

    private String rootWiki;

    private List<String> rootSpaces = new ArrayList<>();

    @Override
    public void close() throws IOException
    {
        this.properties.getSource().close();
    }

    @Override
    protected void read(Object filter, AntoraFilter proxyFilter) throws FilterException
    {
        File sourceDir = getSourceDirectory();
        if (sourceDir == null || !sourceDir.exists()) {
            this.logger.error("Source directory does not exist or is not a directory: {}",
                sourceDir != null ? sourceDir.getPath() : "null");
            throw new FilterException("Source directory does not exist or is not a directory: " + sourceDir);
        }

        this.logger.info("Starting Antora documentation import from [{}]", sourceDir.getPath());

        try {
            parseRootSpaces();

            this.progress.pushLevelProgress(getTotalSteps(), this);
            try {
                beginWiki(proxyFilter);

                loadComponents(sourceDir);
                sendComponents(proxyFilter);

                endWiki(proxyFilter);

                this.logger.info("Antora documentation import completed successfully");
            } finally {
                this.progress.popLevelProgress(this);
            }
        } catch (Exception e) {
            this.logger.error(MSG_FAILED_TO_READ_ANTORA, e);
            throw new FilterException(MSG_FAILED_TO_READ_ANTORA, e);
        }
    }

    private int getTotalSteps()
    {
        return 10 + this.components.size() * 5;
    }

    private File getSourceDirectory() throws FilterException
    {
        Object source = this.properties.getSource();
        if (source instanceof File) {
            return (File) source;
        } else if (source instanceof String) {
            return new File((String) source);
        } else if (source instanceof FileInputSource) {
            return ((FileInputSource) source).getFile();
        } else if (source instanceof java.io.InputStream) {
            this.logger.warn("InputStream source not fully supported yet, using working directory");
            return null;
        }
        throw new FilterException("Unsupported source type: " + source.getClass().getName());
    }

    private void parseRootSpaces()
    {
        EntityReference root = this.properties.getRoot();
        if (root != null) {
            EntityReference current = root;
            while (current != null) {
                if (current.getType() == EntityType.WIKI) {
                    this.rootWiki = current.getName();
                } else if (current.getType() == EntityType.SPACE) {
                    this.rootSpaces.add(0, current.getName());
                }
                current = current.getParent();
            }
        }
    }

    private void loadComponents(File sourceDir) throws IOException
    {
        File[] subdirs = sourceDir.listFiles(File::isDirectory);
        if (subdirs == null) {
            this.logger.debug("No subdirectories found in [{}]", sourceDir.getPath());
            return;
        }

        this.logger.debug("Found [{}] subdirectories in [{}], scanning for components",
            subdirs.length, sourceDir.getPath());

        for (File subdir : subdirs) {
            File antoraYml = new File(subdir, "antora.yml");
            if (antoraYml.exists()) {
                this.logger.debug("Found antora.yml in [{}], parsing component descriptor", subdir.getName());
                ComponentVersionDescriptor component = parseComponentDescriptor(antoraYml, subdir);
                if (component != null && shouldIncludeComponent(component)) {
                    if (shouldIncludeVersion(component.getVersion())) {
                        loadModules(component, subdir);
                        this.components.put(component.getName() + SLASH + component.getVersion(), component);
                        this.logger.debug("Loaded component [{}] version [{}]",
                            component.getName(), component.getVersion());
                    } else {
                        this.logger.debug("Skipping component [{}] version [{}] - version filtered out",
                            component.getName(), component.getVersion());
                    }
                } else if (component != null) {
                    this.logger.debug("Skipping component [{}] - component filtered out", component.getName());
                }
            }
        }
    }

    private ComponentVersionDescriptor parseComponentDescriptor(File antoraYml, File baseDir) throws IOException
    {
        Yaml yaml = new Yaml();
        try (Reader reader = new InputStreamReader(new FileInputStream(antoraYml), StandardCharsets.UTF_8)) {
            Map<String, Object> data = yaml.load(reader);
            if (data == null) {
                return null;
            }

            ComponentVersionDescriptor descriptor = new ComponentVersionDescriptor();
            descriptor.setName(getString(data, "name"));
            descriptor.setVersion(getString(data, "version"));
            descriptor.setTitle(getString(data, "title"));
            descriptor.setDisplayVersion(getString(data, "display_version"));
            descriptor.setPrerelease(getBoolean(data, "prerelease", false));
            descriptor.setStartPage(getString(data, "start_page"));

            Object navObj = data.get("nav");
            if (navObj instanceof List) {
                descriptor.setNav((List<String>) navObj);
            }

            descriptor.setBaseDir(baseDir);

            return descriptor;
        }
    }

    private void loadModules(ComponentVersionDescriptor component, File baseDir) throws IOException
    {
        File modulesDir = new File(baseDir, MODULES_DIR);
        if (!modulesDir.exists()) {
            this.logger.debug("Modules directory does not exist for component [{}]", component.getName());
            return;
        }

        Map<String, ModuleDescriptor> modules = new LinkedHashMap<>();
        File[] moduleDirs = modulesDir.listFiles(File::isDirectory);
        if (moduleDirs == null) {
            return;
        }

        this.logger.debug("Found [{}] modules in component [{}]", moduleDirs.length, component.getName());

        for (File moduleDir : moduleDirs) {
            String moduleName = moduleDir.getName();
            if (shouldIncludeModule(moduleName)) {
                ModuleDescriptor module = parseModuleDescriptor(moduleDir, moduleName);
                if (module != null) {
                    modules.put(moduleName, module);
                    this.logger.debug("Loaded module [{}] with [{}] pages",
                        moduleName, module.getPages() != null ? module.getPages().size() : 0);
                }
            } else {
                this.logger.debug("Skipping module [{}] - module filtered out", moduleName);
            }
        }

        component.setModules(modules);
    }

    private ModuleDescriptor parseModuleDescriptor(File moduleDir, String moduleName) throws IOException
    {
        ModuleDescriptor module = new ModuleDescriptor();
        module.setName(moduleName);
        module.setBaseDir(moduleDir);

        File pagesDir = new File(moduleDir, PAGES_DIR);
        if (pagesDir.exists() && pagesDir.isDirectory()) {
            module.setPagesDir(pagesDir);
            module.setPages(collectAdocFiles(pagesDir));
        }

        File imagesDir = new File(moduleDir, IMAGES_DIR);
        if (imagesDir.exists()) {
            module.setImagesDir(imagesDir);
        }

        File attachmentsDir = new File(moduleDir, ATTACHMENTS_DIR);
        if (attachmentsDir.exists()) {
            module.setAttachmentsDir(attachmentsDir);
        }

        File partialsDir = new File(moduleDir, PARTIALS_DIR);
        if (partialsDir.exists()) {
            module.setPartialsDir(partialsDir);
        }

        File examplesDir = new File(moduleDir, EXAMPLES_DIR);
        if (examplesDir.exists()) {
            module.setExamplesDir(examplesDir);
        }

        return module;
    }

    private List<File> collectAdocFiles(File dir)
    {
        List<File> files = new ArrayList<>();
        File[] children = dir.listFiles();
        if (children == null) {
            return files;
        }

        Arrays.sort(children, (f1, f2) -> {
            if (f1.isDirectory() != f2.isDirectory()) {
                return f1.isDirectory() ? -1 : 1;
            }
            return f1.getName().compareToIgnoreCase(f2.getName());
        });

        for (File child : children) {
            if (child.isDirectory()) {
                files.addAll(collectAdocFiles(child));
            } else if (child.getName().endsWith(ADOC_EXT)) {
                files.add(child);
            }
        }

        return files;
    }

    private boolean shouldIncludeComponent(ComponentVersionDescriptor component)
    {
        String filter = this.properties.getComponentFilter();
        if (StringUtils.isEmpty(filter)) {
            return true;
        }

        Set<String> allowed = new HashSet<>(Arrays.asList(filter.split(COMMA)));
        return allowed.contains(component.getName());
    }

    private boolean shouldIncludeVersion(String version)
    {
        String filter = this.properties.getVersionFilter();
        if (StringUtils.isEmpty(filter)) {
            return true;
        }

        Set<String> allowed = new HashSet<>(Arrays.asList(filter.split(COMMA)));
        return version != null && allowed.contains(version);
    }

    private boolean shouldIncludeModule(String moduleName)
    {
        String filter = this.properties.getModuleFilter();
        if (StringUtils.isEmpty(filter)) {
            return true;
        }

        Set<String> allowed = new HashSet<>(Arrays.asList(filter.split(COMMA)));
        return allowed.contains(moduleName);
    }

    private void sendComponents(AntoraFilter proxyFilter) throws FilterException
    {
        for (Map.Entry<String, ComponentVersionDescriptor> entry : this.components.entrySet()) {
            this.progress.startStep(this);
            sendComponent(entry.getValue(), proxyFilter);
            this.progress.endStep(this);
        }
    }

    private void sendComponent(ComponentVersionDescriptor component, AntoraFilter proxyFilter) throws FilterException
    {
        if (this.properties.isVerbose()) {
            this.logger.info("Importing Antora component [{}] version [{}]", component.getName(),
                component.getVersion());
        }

        for (String spaceName : this.rootSpaces) {
            proxyFilter.beginWikiSpace(spaceName, FilterEventParameters.EMPTY);
        }

        String componentSpaceName = component.getVersionedSpaceName();
        proxyFilter.beginWikiSpace(componentSpaceName, FilterEventParameters.EMPTY);

        try {
            sendModules(component, proxyFilter);

            if (this.properties.isCreateNavigationPage()) {
                sendNavigationPage(component, proxyFilter);
            }
        } finally {
            proxyFilter.endWikiSpace(componentSpaceName, FilterEventParameters.EMPTY);
            for (int i = this.rootSpaces.size() - 1; i >= 0; i--) {
                proxyFilter.endWikiSpace(this.rootSpaces.get(i), FilterEventParameters.EMPTY);
            }
        }
    }

    private void sendModules(ComponentVersionDescriptor component, AntoraFilter proxyFilter) throws FilterException
    {
        int moduleCount = component.getModules().size();
        this.logger.debug("Sending [{}] modules for component [{}]", moduleCount, component.getName());

        for (ModuleDescriptor module : component.getModules().values()) {
            this.progress.startStep(this);
            sendModule(component, module, proxyFilter);
            this.progress.endStep(this);
        }
    }

    private void sendModule(ComponentVersionDescriptor component, ModuleDescriptor module, AntoraFilter proxyFilter)
        throws FilterException
    {
        String moduleSpaceName;
        if (ROOT_MODULE.equals(module.getName())) {
            moduleSpaceName = component.getSpaceName();
        } else {
            moduleSpaceName = module.getSpaceName(component.getName());
        }

        boolean isRootModule = ROOT_MODULE.equals(module.getName());
        if (!isRootModule) {
            proxyFilter.beginWikiSpace(moduleSpaceName, FilterEventParameters.EMPTY);
        }

        try {
            this.logger.debug("Importing module [{}] in space [{}]",
                module.getName(), moduleSpaceName);
            sendPages(component, module, proxyFilter);
        } finally {
            if (!isRootModule) {
                proxyFilter.endWikiSpace(moduleSpaceName, FilterEventParameters.EMPTY);
            }
        }
    }

    private void sendPages(ComponentVersionDescriptor component, ModuleDescriptor module, AntoraFilter proxyFilter)
        throws FilterException
    {
        int pageCount = module.getPages() != null ? module.getPages().size() : 0;
        this.logger.debug("Sending [{}] pages for module [{}]", pageCount, module.getName());

        for (File page : module.getPages()) {
            this.progress.startStep(this);
            sendPage(component, module, page, proxyFilter);
            this.progress.endStep(this);
        }
    }

    private void sendPage(ComponentVersionDescriptor component, ModuleDescriptor module, File pageFile,
        AntoraFilter proxyFilter) throws FilterException
    {
        String relativePath = getRelativePath(module.getPagesDir(), pageFile);
        String pageName = computePageName(component, module, relativePath);

        if (this.properties.isVerbose()) {
            this.logger.info("Importing page [{}] from [{}]", pageName, relativePath);
        }

        this.logger.debug("Processing page file [{}]", pageFile.getPath());

        FilterEventParameters documentParams = createDocumentParameters();

        proxyFilter.beginWikiDocument(pageName, documentParams);
        try {
            sendDocumentContent(component, module, pageFile, proxyFilter);
            this.logger.debug("Successfully processed page [{}]", pageName);
        } finally {
            proxyFilter.endWikiDocument(pageName, documentParams);
        }
    }

    private FilterEventParameters createDocumentParameters()
    {
        FilterEventParameters params = new FilterEventParameters();
        Locale locale = this.properties.getDefaultLocale();
        if (locale != null) {
            params.put(WikiDocumentFilter.PARAMETER_LOCALE, locale);
        }
        return params;
    }

    private void sendDocumentContent(ComponentVersionDescriptor component, ModuleDescriptor module, File pageFile,
        AntoraFilter proxyFilter) throws FilterException
    {
        proxyFilter.beginWikiDocumentLocale(Locale.ROOT, FilterEventParameters.EMPTY);
        try {
            String content = readPageContent(pageFile);
            String title = extractTitle(content, pageFile.getName());
            String xwikiContent = convertAsciidoc(content);

            FilterEventParameters revisionParams = new FilterEventParameters();
            revisionParams.put(WikiDocumentFilter.PARAMETER_TITLE, title);
            revisionParams.put(WikiDocumentFilter.PARAMETER_SYNTAX, Syntax.XWIKI_2_1);
            revisionParams.put(WikiDocumentFilter.PARAMETER_CONTENT, xwikiContent);

            sendDocumentRevision(proxyFilter, revisionParams);
            sendImages(module, pageFile, proxyFilter);
        } catch (FilterException e) {
            throw e;
        } catch (Exception e) {
            this.logger.error("Failed to process page [{}]", pageFile.getPath(), e);
            throw new FilterException("Failed to process page", e);
        } finally {
            proxyFilter.endWikiDocumentLocale(Locale.ROOT, FilterEventParameters.EMPTY);
        }
    }

    private void sendDocumentRevision(AntoraFilter proxyFilter, FilterEventParameters revisionParams)
        throws FilterException
    {
        proxyFilter.beginWikiDocumentRevision(REVISION_ID, revisionParams);
        proxyFilter.endWikiDocumentRevision(REVISION_ID, revisionParams);
    }

    private String getRelativePath(File baseDir, File file)
    {
        String basePath = baseDir.getAbsolutePath();
        String filePath = file.getAbsolutePath();
        String relative = filePath.substring(basePath.length());
        return relative.startsWith(File.separator) ? relative.substring(1) : relative;
    }

    private String computePageName(ComponentVersionDescriptor component, ModuleDescriptor module, String relativePath)
    {
        String pageName = relativePath;
        pageName = pageName.replace(ADOC_EXT, "");
        pageName = pageName.replace(File.separator, DOT);
        pageName = pageName.replace(SLASH, DOT);

        boolean isIndexPage = pageName.endsWith(DOT + INDEX_PAGE) || pageName.equals(INDEX_PAGE);
        if (isIndexPage) {
            pageName = WEB_HOME;
        } else {
            String suffix = this.properties.getPageSuffix();
            boolean shouldClean = this.properties.isCleanPageNames() && StringUtils.isEmpty(suffix);

            if (shouldClean) {
                pageName = cleanEntityName(pageName);
            }

            if (StringUtils.isNotEmpty(suffix) && !pageName.endsWith(suffix)) {
                pageName = pageName + suffix;
            }
        }

        return pageName;
    }

    private String cleanEntityName(String name)
    {
        if (name == null || name.isEmpty()) {
            return name;
        }

        String cleaned = name;
        cleaned = cleaned.replace(" ", "");
        cleaned = VALID_NAME_PATTERN.matcher(cleaned).replaceAll(UNDERSCORE);

        cleaned = MULTIPLE_UNDERSCORES.matcher(cleaned).replaceAll(UNDERSCORE);

        cleaned = LEADING_TRAILING_UNDERSCORE.matcher(cleaned).replaceAll("");

        return cleaned;
    }

    private String readPageContent(File pageFile) throws FilterException
    {
        try {
            return FileUtils.readFileToString(pageFile, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new FilterException("Failed to read page file: " + pageFile, e);
        }
    }

    private String convertAsciidoc(String content)
    {
        return this.asciidocRenderer.render(content);
    }

    private String extractTitle(String content, String defaultTitle)
    {
        if (content == null) {
            return defaultTitle;
        }

        String[] lines = content.split(NEWLINE);
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("= ")) {
                return trimmed.substring(2).trim();
            }
            if (isNonTitleLine(trimmed)) {
                break;
            }
        }

        return defaultTitle;
    }

    private boolean isNonTitleLine(String trimmed)
    {
        if (trimmed.isEmpty()) {
            return true;
        }
        if (trimmed.startsWith("//")) {
            return true;
        }
        if (isPreprocessorDirective(trimmed)) {
            return true;
        }
        return false;
    }

    private boolean isPreprocessorDirective(String trimmed)
    {
        return trimmed.startsWith("ifdef:") || trimmed.startsWith("ifndef:") || trimmed.startsWith("endif:");
    }

    private void sendImages(ModuleDescriptor module, File pageFile, AntoraFilter proxyFilter) throws FilterException
    {
        if (!this.properties.isImportImages()) {
            this.logger.debug("Image import is disabled, skipping images for page [{}]", pageFile.getPath());
            return;
        }

        if (module.getImagesDir() == null) {
            this.logger.debug("No images directory configured for module [{}]", module.getName());
            return;
        }

        String content = readPageContent(pageFile);

        if (content == null || !content.contains("image")) {
            this.logger.debug("Page [{}] does not contain image references", pageFile.getPath());
            return;
        }

        File[] images = module.getImagesDir().listFiles();
        if (images == null) {
            this.logger.warn("Images directory exists but cannot be listed: [{}]",
                module.getImagesDir().getPath());
            return;
        }

        int imageCount = 0;
        for (File image : images) {
            if (image.isFile() && isImageFile(image.getName())) {
                sendAttachment(image, proxyFilter);
                imageCount++;
            }
        }
        this.logger.debug("Imported [{}] images for page [{}]", imageCount, pageFile.getPath());
    }

    private void sendAttachment(File file, AntoraFilter proxyFilter) throws FilterException
    {
        try {
            String filename = file.getName();

            FilterEventParameters params = new FilterEventParameters();
            params.put("filename", filename);
            params.put("mimetype", getMimeType(file));
            params.put("size", file.length());

            DefaultFileInputSource source = new DefaultFileInputSource(file);
            long size = file.length();

            proxyFilter.beginWikiDocumentAttachment(filename, source, size, params);
            proxyFilter.endWikiDocumentAttachment(filename, source, size, params);

            this.logger.debug("Successfully imported attachment [{}] ({} bytes)", filename, size);
        } catch (Exception e) {
            this.logger.warn("Failed to import attachment [{}]: {}", file.getName(), e.getMessage());
        }
    }

    private String getMimeType(File file)
    {
        String ext = FilenameUtils.getExtension(file.getName()).toLowerCase(Locale.ROOT);
        return MIME_TYPES.getOrDefault(ext, MIME_OCTET);
    }

    private boolean isImageFile(String filename)
    {
        String ext = FilenameUtils.getExtension(filename).toLowerCase(Locale.ROOT);
        return isImageExtension(ext);
    }

    private boolean isImageExtension(String ext)
    {
        return MIME_TYPES.containsKey(ext);
    }

    private void sendNavigationPage(ComponentVersionDescriptor component, AntoraFilter proxyFilter)
        throws FilterException
    {
        List<String> navFiles = component.getNav();
        if (navFiles.isEmpty()) {
            this.logger.debug("No navigation files defined for component [{}]", component.getName());
            return;
        }

        this.logger.debug("Building navigation page for component [{}] with [{}] nav files",
            component.getName(), navFiles.size());

        String navPageName = WEB_HOME;
        String suffix = this.properties.getPageSuffix();
        if (StringUtils.isNotEmpty(suffix)) {
            navPageName = navPageName + suffix;
        }

        String navContent = buildNavigationContent(component, navFiles);

        FilterEventParameters documentParams = createDocumentParameters();

        proxyFilter.beginWikiDocument(navPageName, documentParams);
        try {
            sendDocumentContentWithTitle(component, proxyFilter, navContent);
            this.logger.debug("Navigation page [{}] created successfully", navPageName);
        } finally {
            proxyFilter.endWikiDocument(navPageName, documentParams);
        }
    }

    private String buildNavigationContent(ComponentVersionDescriptor component, List<String> navFiles)
    {
        StringBuilder navContent = new StringBuilder();
        navContent.append(INCLUDE_DOC).append(DOUBLE_NEWLINE);
        navContent.append(NAV_EQUALS).append(component.getTitle()).append(DOUBLE_NEWLINE);

        for (String navFile : navFiles) {
            File nav = new File(component.getBaseDir(), MODULES_DIR + SLASH + navFile);
            if (nav.exists()) {
                try {
                    String navText = FileUtils.readFileToString(nav, StandardCharsets.UTF_8);
                    navContent.append(VELOCITY_START).append(NEWLINE);
                    navContent.append(NAV_SET_START).append(NEWLINE);
                    navContent.append(parseNavToVelocity(navText));
                    navContent.append(NAV_SET_END).append(NEWLINE);
                    navContent.append(VELOCITY_END).append(NEWLINE);
                } catch (IOException e) {
                    this.logger.warn("Failed to read nav file [{}]: {}", nav.getPath(), e.getMessage());
                }
            } else {
                this.logger.warn("Navigation file not found: [{}]", nav.getPath());
            }
        }

        return navContent.toString();
    }

    private void sendDocumentContentWithTitle(ComponentVersionDescriptor component, AntoraFilter proxyFilter,
        String content) throws FilterException
    {
        proxyFilter.beginWikiDocumentLocale(Locale.ROOT, FilterEventParameters.EMPTY);
        try {
            FilterEventParameters revisionParams = new FilterEventParameters();
            revisionParams.put(WikiDocumentFilter.PARAMETER_TITLE, NAVIGATION_TITLE);
            revisionParams.put(WikiDocumentFilter.PARAMETER_SYNTAX, Syntax.XWIKI_2_1);
            revisionParams.put(WikiDocumentFilter.PARAMETER_CONTENT, content);

            sendDocumentRevision(proxyFilter, revisionParams);
        } finally {
            proxyFilter.endWikiDocumentLocale(Locale.ROOT, FilterEventParameters.EMPTY);
        }
    }

    private String parseNavToVelocity(String navText)
    {
        StringBuilder velocity = new StringBuilder();
        String[] lines = navText.split(NEWLINE);

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith(NAV_XREF_PREFIX)) {
                String navItem = parseNavItem(trimmed);
                if (!navItem.isEmpty()) {
                    velocity.append(navItem);
                }
            }
        }

        return velocity.toString();
    }

    private String parseNavItem(String line)
    {
        int start = line.indexOf("xref:") + 5;
        int bracket = line.indexOf("[");
        int end = bracket > 0 ? bracket : line.length();
        String target = line.substring(start, end).trim();

        StringBuilder item = new StringBuilder();
        item.append(NAV_ITEM_DOC);
        item.append(target.replace(ADOC_EXT, "").replace(SLASH, DOT));
        item.append(NAV_ITEM_TITLE);
        if (bracket > 0) {
            item.append(line.substring(bracket + 1, line.indexOf("]")));
        } else {
            item.append(target.replace(ADOC_EXT, "").replace(SLASH, " / "));
        }
        item.append(NAV_ITEM_END);
        item.append('\n');

        return item.toString();
    }

    private void beginWiki(AntoraFilter proxyFilter) throws FilterException
    {
    }

    private void endWiki(AntoraFilter proxyFilter) throws FilterException
    {
    }

    private String getString(Map<String, Object> data, String key)
    {
        Object value = data.get(key);
        return value != null ? value.toString() : null;
    }

    private boolean getBoolean(Map<String, Object> data, String key, boolean defaultValue)
    {
        Object value = data.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return defaultValue;
    }
}
