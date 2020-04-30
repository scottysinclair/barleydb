package scott.barleydb.server.jdbc.vendor;

/*-
 * #%L
 * BarleyDB
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2014 - 2020 Scott Sinclair
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

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class SemVer implements Comparable<SemVer> {

    public static int compare(String a, String b) {
        SemVer sa = new SemVer(a);
        SemVer sb = new SemVer(b);
        return sa.compareTo(sb);
    }

    List<SemVerPart> parts;

    public SemVer(String ver) {
        parts = Arrays.stream(ver.split("\\.")).map(SemVerPart::new)
                .collect(Collectors.toList());
    }


    @Override
    public int compareTo(SemVer semVer) {
        Iterator<SemVerPart> a = parts.iterator();
        Iterator<SemVerPart> b = semVer.parts.iterator();
        while(a.hasNext() && b.hasNext()) {
            int r = a.next().compareTo(b.next());
            if (r != 0) {
                return r;
            }
        }
        if (a.hasNext()) {
            return 1;
        }
        return -1;
    }
}

class SemVerPart implements Comparable<SemVerPart> {
    private String value;
    public SemVerPart(String part) {
        this.value = part;
    }

    public <T extends Comparable<T>> T getValue() {
        try {
            return (T)(Integer)Integer.parseInt(value);
        }
        catch(NumberFormatException x) {
            return (T)value;
        }
    }

    @Override
    public int compareTo(SemVerPart semVerPart) {
        if (semVerPart == null) {
            return 1;
        }
        return getValue().compareTo(semVerPart.getValue());
    }
}
