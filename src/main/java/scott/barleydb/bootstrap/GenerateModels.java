package scott.barleydb.bootstrap;

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

import scott.barleydb.api.specification.DefinitionsSpec;
import scott.barleydb.api.specification.SpecRegistry;
import scott.barleydb.build.specification.modelgen.GenerateDataModels;
import scott.barleydb.build.specification.modelgen.GenerateEnums;
import scott.barleydb.build.specification.modelgen.GenerateQueryModels;
import scott.barleydb.build.specification.staticspec.StaticDefinitions;
import scott.barleydb.build.specification.staticspec.processor.StaticDefinitionProcessor;

public class GenerateModels {

    public static void execute(Class<?> specClass) {
        SpecRegistry registry = new SpecRegistry();

        StaticDefinitionProcessor processor = new StaticDefinitionProcessor();
        try {
            DefinitionsSpec vvlSpec = processor.process((StaticDefinitions)specClass.newInstance(), registry);

            GenerateEnums generateEnums = new GenerateEnums();
            generateEnums.generateEnums("src/main/java", vvlSpec);

            GenerateDataModels generateDataModels = new GenerateDataModels();
            generateDataModels.generateDataModels("src/main/java", vvlSpec);

            GenerateQueryModels generateQueryModels = new GenerateQueryModels();
            generateQueryModels.generateQueryModels("src/main/java", vvlSpec);
        }
        catch(Exception x) {
            x.printStackTrace(System.err);
        }
    }

}
