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
package com.servoy.j2db.scripting.solutionmodel;

import org.mozilla.javascript.annotations.JSGetter;
import org.mozilla.javascript.annotations.JSSetter;

import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.RelationItem;
import com.servoy.j2db.scripting.IConstantsObject;
import com.servoy.j2db.scripting.IJavaScriptType;
import com.servoy.j2db.scripting.IReturnedTypesProvider;
import com.servoy.j2db.scripting.ScriptObjectRegistry;
import com.servoy.j2db.solutionmodel.ISMRelationItem;

/**
 * @author jcompagner
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME)
public class JSRelationItem extends JSBase<RelationItem> implements IJavaScriptType, IConstantsObject, ISMRelationItem
{
	static
	{
		ScriptObjectRegistry.registerReturnedTypesProviderForClass(JSRelationItem.class, new IReturnedTypesProvider()
		{
			public Class< ? >[] getAllReturnedTypes()
			{
				return new Class< ? >[] { JSRelationItem.class };
			}
		});
	}


	public JSRelationItem(RelationItem relationItem, JSRelation parent, boolean isNew)
	{
		super(parent, relationItem, isNew);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.RelationItem#getForeignColumnName()
	 * 
	 * @sample
	 * 	var relation = solutionModel.newRelation('parentToChild', 'db:/example_data/parent_table', 'db:/example_data/child_table', JSRelation.INNER_JOIN);
	 * var criteria = relation.newRelationItem('parent_table_id', '=', 'child_table_parent_id');
	 * criteria.primaryDataProviderID = 'parent_table_text';
	 * criteria.foreignColumnName = 'child_table_text';
	 * criteria.operator = '<';
	 */
	@JSGetter
	public String getForeignColumnName()
	{
		return getBaseComponent(false).getForeignColumnName();
	}

	@JSSetter
	public void setForeignColumnName(String arg)
	{
		checkModification();
		getBaseComponent(true).setForeignColumnName(arg);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.RelationItem#getOperator()
	 * 
	 * @sampleas getForeignColumnName()
	 */
	@JSGetter
	public String getOperator()
	{
		return RelationItem.getOperatorAsString(getBaseComponent(false).getOperator());
	}

	@JSSetter
	public void setOperator(String operator)
	{
		checkModification();
		int validOperator = RelationItem.getValidOperator(operator, RelationItem.RELATION_OPERATORS, null);
		if (validOperator == -1) throw new IllegalArgumentException("Operator " + operator + " is not a valid relation operator"); //$NON-NLS-1$ //$NON-NLS-2$
		getBaseComponent(true).setOperator(validOperator);
	}

	/**
	 * @clonedesc com.servoy.j2db.persistence.RelationItem#getPrimaryDataProviderID()
	 * 
	 * @sampleas getForeignColumnName()
	 */
	@JSGetter
	public String getPrimaryDataProviderID()
	{
		return getBaseComponent(false).getPrimaryDataProviderID();
	}

	@JSSetter
	public void setPrimaryDataProviderID(String arg)
	{
		checkModification();
		getBaseComponent(true).setPrimaryDataProviderID(arg);
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return "JSRelationItem[" + getBaseComponent(false).getPrimaryDataProviderID() + getOperator() + getBaseComponent(false).getForeignColumnName() + ']'; //$NON-NLS-1$
	}
}
