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

import javax.swing.text.Style;

import com.servoy.j2db.util.FixedStyleSheet;

/**
 * Interface for components that support css row styling
 * @author gboros
 */
public interface ISupportRowStyling
{
	String CLASS_ODD = "odd"; //$NON-NLS-1$
	String CLASS_EVEN = "even"; //$NON-NLS-1$
	String CLASS_SELECTED = "selected"; //$NON-NLS-1$
	String CLASS_HEADER = "header"; //$NON-NLS-1$

	enum ATTRIBUTE
	{
		BGCOLOR, FGCOLOR, FONT, BORDER
	}

	public void setRowStyles(FixedStyleSheet styleSheet, Style oddStyle, Style evenStyle, Style selectedStyle, Style headerStyle);

	public FixedStyleSheet getRowStyleSheet();

	public Style getRowOddStyle();

	public Style getRowEvenStyle();

	public Style getRowSelectedStyle();

	public Style getHeaderStyle();
}
