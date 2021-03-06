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

import com.servoy.j2db.util.IStyleRule;
import com.servoy.j2db.util.IStyleSheet;


/**
 * Interface for components that support css row styling
 * @author gboros
 */
public interface ISupportRowStyling
{
	String CLASS_ODD = "odd"; //$NON-NLS-1$
	String CLASS_EVEN = "even"; //$NON-NLS-1$
	String CLASS_SELECTED = "selected"; //$NON-NLS-1$
	String CLASS_HEADER = "grid_header"; //$NON-NLS-1$

	enum ATTRIBUTE
	{
		BGIMAGE, BGCOLOR, FGCOLOR, FONT, BORDER, MARGIN
	}

	public void setRowStyles(IStyleSheet styleSheet, IStyleRule oddStyle, IStyleRule evenStyle, IStyleRule selectedStyle, IStyleRule headerStyle);

	public IStyleSheet getRowStyleSheet();

	public IStyleRule getRowOddStyle();

	public IStyleRule getRowEvenStyle();

	public IStyleRule getRowSelectedStyle();

	public IStyleRule getHeaderStyle();
}
