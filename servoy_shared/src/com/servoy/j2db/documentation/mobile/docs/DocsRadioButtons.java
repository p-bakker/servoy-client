/*

 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2013 Servoy BV

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

package com.servoy.j2db.documentation.mobile.docs;

import com.servoy.base.scripting.annotations.ServoyClientSupport;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.Field;

/**
 * Dummy class for use in the documentation generator.
 * 
 * @author rgansevles
 */
@ServoyDocumented(category = ServoyDocumented.DESIGNTIME, publicName = "RadioButtons", scriptingName = "RadioButtons", displayType = Field.RADIOS, realClass = Field.class)
@ServoyClientSupport(mc = true, wc = false, sc = false)
public class DocsRadioButtons extends BaseDocsField
{
	/**
	 * Show the radios as horizontal or vertical set.
	 */
	public boolean getHorizontal()
	{
		return false;
	}

	@SuppressWarnings("unused")
	public void setHorizontal(boolean arg)
	{
	}
}