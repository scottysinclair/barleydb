package scott.barleydb.api.dto;

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

import java.util.LinkedList;
import java.util.List;

import scott.barleydb.api.config.Definitions;
import scott.barleydb.api.config.EntityType;
import scott.barleydb.api.config.NodeType;
import scott.barleydb.api.exception.SortRuntimeException;

public class DtoNToMList<N extends BaseDto,M extends BaseDto> extends DtoList<M> {

  private static final long serialVersionUID = 1L;

  private final Definitions definitions;
  private final DtoHelper helper;
  private final NodeType nodeType;
  private final List<N> joinDtos = new LinkedList<>();

  public DtoNToMList(Definitions definitions, DtoHelper helper, NodeType nodeType) {
    super();
    this.definitions = definitions;
    this.helper = helper;
    this.nodeType = nodeType;
  }

  public void addJoinDto(N joinDto) {
    M pastJoinDto = getDtoPastJoin(joinDto);
    add(pastJoinDto);
    joinDtos.add(joinDto);
  }

  public List<N> getJoinDtos() {
    return joinDtos;
  }

  private M getDtoPastJoin(N joinDto) {
    /*
     * the join property on the join entity
     */
    String joinProperty = nodeType.getJoinProperty();
    /*
     * the entity type of the join entity
     */
    EntityType et = definitions.getEntityTypeMatchingInterface(nodeType.getRelationInterfaceName(), true);
    /*
     * the node type on the join entity which gets the entity on "the other side"
     */
    NodeType joinNode = et.getNodeType(joinProperty, true);
    /*
     * call the dto method to get the reference
     */
    M reffedDto = helper.callPropertyGetter(joinDto, joinNode);
    if (reffedDto == null) {
      throw new SortRuntimeException("JoinDto has null reference for onward join " + joinDto);
    }
    return reffedDto;
  }

}
