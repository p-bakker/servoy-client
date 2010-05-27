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
package com.servoy.j2db.ui;

import com.servoy.j2db.annotations.ServoyDocumented;


/**
 * @author jcompagner
 *
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME, publicName = "Portal")
public interface IScriptPortalComponentMethods extends IScriptBaseMethods, IScriptScrollableMethods, IScriptReadOnlyMethods
{
	/**
	 * Get record index.
	 * 
	 * @see com.servoy.j2db.dataprocessing.FoundSet#js_getSelectedIndex()
	 */
	@Deprecated
	public int js_getRecordIndex();

	@Deprecated
	public void js_setRecordIndex(int i);

	/**
	 * Gets the selected record index in the current cached foundset in the specified portal.
	 *
	 * @sample
	 * //gets the selected record index in the foundset
	 * var current = %%prefix%%%%elementName%%.getSelectedIndex();
	 * 
	 * //sets the next record index in the foundset
	 * %%prefix%%%%elementName%%.setSelectedIndex(current+1);
	 * 
	 * @return The selected index (integer).
	 */
	public int jsFunction_getSelectedIndex();

	/**
	 * Sets the selected record index in the current cached foundset in the specified portal.
	 * 
	 * @sampleas jsFunction_getSelectedIndex()
	 * 
	 * @param index the specified record index
	 */
	public void jsFunction_setSelectedIndex(int index);

	/**
	 * @see com.servoy.j2db.dataprocessing.FoundSet#js_getSize()
	 */
	@Deprecated
	public int js_getMaxRecordIndex();

	/**
	 * Deletes the currently selected portal row in the foundset of the specified portal.
	 * 
	 * @sample
	 * %%prefix%%%%elementName%%.deleteRecord();
	 */
	public void js_deleteRecord();

	/**
	 * Creates a new portal row in the foundset of the specified portal. 
	 * 
	 * @sample
	 * // foreign key data is only filled in for equals (=) relation items 
	 * //adds the new record on top
	 * %%prefix%%%%elementName%%.newRecord(true);
	 * 
	 * @param addOnTop optional adds the new portal record as the topmost row of the foundset, default value is true
	 */
	public void js_newRecord(Object[] vargs);

	/**
	 * Duplicates the currently selected portal row in the foundset of the specified portal.
	 * 
	 * @sample
	 * //adds the duplicated record on top
	 * %%prefix%%%%elementName%%.duplicateRecord(true);
	 * 
	 * @param addOnTop optional
	 * adds the duplicated record as the topmost record of the foundset, default value is true
	 */
	public void js_duplicateRecord(Object[] vargs);

	/**
	 * Returns the sort columns names of the current portal (as comma separated string).
	 * 
	 * @sample
	 * var w = %%prefix%%%%elementName%%.getSortColumns();
	 * 
	 * @return array with column names
	 */
	public String js_getSortColumns();
}