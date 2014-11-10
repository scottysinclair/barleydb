package scott.sort.api.query;

/*
 * #%L
 * Simple Object Relational Framework
 * %%
 * Copyright (C) 2014 Scott Sinclair <scottysinclair@gmail.com>
 * %%
 * All rights reserved.
 * #L%
 */


import java.io.Serializable;

public class QAliasGenerator implements Serializable {

    private static final long serialVersionUID = 1L;

    private String nextAlias = "a";

    public QAliasGenerator() {}

    public String nextAlias() {
        String alias = nextAlias;
        char lastChar = nextAlias.charAt(nextAlias.length() - 1);
        if (lastChar == 'z') {
            nextAlias = nextAlias.substring(0, nextAlias.length() - 1) + "aa";
        }
        else {
            nextAlias = nextAlias.substring(0, nextAlias.length() - 1) + (char) (lastChar + 1);
        }
        return alias;
    }

}