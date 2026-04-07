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

import java.io.StringReader;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;

/**
 * Handles AsciiDoc to XWiki 2.1 conversion using the XWiki Rendering framework.
 *
 * @version $Id$
 */
@Component(roles = AsciiDocRenderer.class)
@Singleton
public class AsciiDocRenderer
{
    @Inject
    @Named("asciidoc/1.0")
    private Parser asciidocParser;

    @Inject
    @Named("xwiki/2.1")
    private BlockRenderer xwiki21Renderer;

    /**
     * Converts AsciiDoc content to XWiki 2.1 syntax.
     *
     * @param content the AsciiDoc content to convert
     * @return the converted XWiki 2.1 syntax content
     */
    public String render(String content)
    {
        if (content == null) {
            return "";
        }

        try {
            XDOM xdom = this.asciidocParser.parse(new StringReader(content));

            WikiPrinter printer = new DefaultWikiPrinter();
            this.xwiki21Renderer.render(xdom, printer);

            return printer.toString();
        } catch (ParseException e) {
            throw new RuntimeException("Failed to parse AsciiDoc content", e);
        }
    }
}
