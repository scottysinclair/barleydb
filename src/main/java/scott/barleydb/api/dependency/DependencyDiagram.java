package scott.barleydb.api.dependency;

/*-
 * #%L
 * BarleyDB
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2014 - 2017 Scott Sinclair
 *       <scottysinclair@gmail.com>
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 *
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


public class DependencyDiagram {

    private String baseUrl = "https://yuml.me/diagram/scruffy/class/";

    private Map<String, Node> nodes = new HashMap<>();

    public Node getOrCreate(String name) {
        name = name.replace('[', ' ').replace(']', ' ');
        Objects.requireNonNull(name);
        Node n = nodes.get(name);
        if (n == null) {
            nodes.put(name, n = new Node(name));
        }
        return n;
    }

    public Link link(String from, String to, String name, LinkType type) {
        Node nf = getOrCreate(from);
        Node nt = getOrCreate(to);
        Link l = new Link(name, type, nf, nt);
        nf.addLinkFrom(l);
        nt.addLinkTo(l);
        return l;
    }

    public void generate(File file, Link firstLink) throws IOException {
        try ( InputStream response = requestYuml( buildYumlMessage() ); ) {
            writeToFile(response, file);
        }
    }

    private String buildYumlMessage() {
        StringBuilder yuml = new StringBuilder();
        for (Node node: nodes.values()) {
            for (Link link: node.getLinksFrom()) {
                renderNode(node, yuml);
                renderLink(link, yuml);
                renderNode(link.getTo(), yuml);
                yuml.append(", ");
            }
        }
        return yuml.toString();
    }

    private void renderLink(Link link, StringBuilder sb) {
        if (link.getName() != null) {
            sb.append(link.getName());
        }
        sb.append('-');
        if (link.getType() == LinkType.DEPENDENCY_DASHED) {
            sb.append(".-");
        }
        sb.append('>');
    }

    private void renderNode(Node node, StringBuilder sb) {
        sb.append('[');
        sb.append(node.getName());
        sb.append(']');
    }

    private InputStream requestYuml(Object buildYumlMessage) throws IOException {
        URL url = new URL(baseUrl + buildYumlMessage());
        return url.openStream();
    }

    private void writeToFile(InputStream response, File file) throws IOException {
        try (OutputStream out = new BufferedOutputStream(new FileOutputStream(file));) {
            int len;
            byte buf[] = new byte[1024 * 4];
            while((len = response.read(buf)) >= 0) {
                out.write(buf, 0, len);
                out.flush();
            }
        }
    }

}
