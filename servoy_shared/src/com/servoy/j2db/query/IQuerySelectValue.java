/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2010 Servoy BV

 This program is free software; you can redistribute it and/or modify it under
 the terms of the GNU Affero General Public License as published by the Free
 Software Foundation; either version 3 of the License, or (at your option) any
 later version.

 This program is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License along
 with this program; if not, see http://www.gnu.org/licenses or write to the Free
 Software Foundation,Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301
 */
package com.servoy.j2db.query;

import com.servoy.base.query.BaseColumnType;
import com.servoy.base.query.IBaseQuerySelectValue;
import com.servoy.base.query.TypeInfo;


/** Interface for selectable values in a select statement.
 * @author rgansevles
 *
 */
public interface IQuerySelectValue extends IBaseQuerySelectValue, IQueryElement
{
	String getAlias();

	default QueryColumn getColumn()
	{
		return null;
	}

	IQuerySelectValue asAlias(String alias);

	default String getAliasOrName()
	{
		String alias = getAlias();
		if (alias != null)
		{
			return alias;
		}

		QueryColumn qcol = getColumn();
		if (qcol != null)
		{
			return qcol.getName();
		}

		return toString();
	}

	default BaseColumnType getColumnType()
	{
		QueryColumn qcol = getColumn();
		if (qcol != null)
		{
			return qcol.getColumnType();
		}
		return null;
	}

	default String getNativeTypename()
	{
		QueryColumn qcol = getColumn();
		if (qcol != null)
		{
			return qcol.getNativeTypename();
		}
		return null;
	}

	default TypeInfo getTypeInfo()
	{
		return new TypeInfo(getColumnType(), getNativeTypename());
	}

	default int getFlags()
	{
		QueryColumn qcol = getColumn();
		if (qcol != null)
		{
			return qcol.getFlags();
		}
		return 0;
	}
}
