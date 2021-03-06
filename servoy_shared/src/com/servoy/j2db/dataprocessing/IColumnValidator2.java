/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2020 Servoy BV

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

package com.servoy.j2db.dataprocessing;

import java.util.Map;

/**
 * Implement this ColumnValidator interface if you want to get more information into the validate method
 * A  {@link IRecordMarkers} and an optional state is given that can be passed on to the actual validation and
 * problems can be reported on that instead of throwing exceptions.
 *
 * @author jcompagner
 * @since 2020.09
 *
 */
public interface IColumnValidator2 extends IColumnValidator
{

	/**
	 * Validate an argument, repots against the {@link IRecordMarkers} if there are problems.
	 *
	 * @param props
	 * @param value
	 * @param recordMarkers
	 * @param state
	 */
	public void validate(Map<String, String> props, Object value, String dataprovider, IRecordMarkers recordMarkers, Object state);

}
