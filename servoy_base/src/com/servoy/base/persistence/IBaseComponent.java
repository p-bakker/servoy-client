/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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

package com.servoy.base.persistence;

import com.servoy.base.scripting.annotations.ServoyClientSupport;


/**
 * Base interface for graphical components (for mobile as well as other clients).
 * 
 * @author rgansevles
 *
 * @since 7.0
 */

@ServoyClientSupport(mc = true, wc = true, sc = true)
public interface IBaseComponent
{
	/**
	 * The visible property of the component, default true.
	 * 
	 * @return visible property
	 */
	boolean getVisible();

	void setVisible(boolean args);

	/**
	 * The enable state of the component, default true.
	 * 
	 * @return enabled state
	 */
	boolean getEnabled();

	void setEnabled(boolean arg);

	/**
	 * The name of the component. Through this name it can also accessed in methods.
	 */
	String getName();

	void setName(String arg);

	/**
	 * Returns the groupID.
	 * 
	 * @return int
	 */
	String getGroupID();

	void setGroupID(String arg);

	/**
	 * The name of the style class that should be applied to this component.
	 * 
	 * When defining style classes for specific component types, their names
	 * must be prefixed according to the type of the component. For example 
	 * in order to define a class names 'fancy' for fields, in the style
	 * definition the class must be named 'field.fancy'. If it would be 
	 * intended for labels, then it would be named 'label.fancy'. When specifying
	 * the class name for a component, the prefix is dropped however. Thus the
	 * field or the label will have its styleClass property set to 'fancy' only.
	 */
	String getStyleClass();

	void setStyleClass(String arg);


	// size and location are not listed here because the awt types cannot be used in gwt.
	//
	// See ISupportSize and ISupportBounds
//	/**
//	 * The width and height (in pixels), separated by a comma.
//	 */
//	java.awt.Dimension getSize();
//
//	void setSize(java.awt.Dimension arg);

//	/**
//	 * The x and y position of the component, in pixels, separated by a comma.
//	 */
//	java.awt.Point getLocation();
//
//	void setLocation(java.awt.Point arg);
}