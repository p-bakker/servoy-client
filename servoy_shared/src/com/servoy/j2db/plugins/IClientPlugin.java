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
package com.servoy.j2db.plugins;


import java.beans.PropertyChangeListener;

import javax.swing.Icon;

import com.servoy.j2db.persistence.ISupportName;
import com.servoy.j2db.scripting.IScriptableProvider;

/**
 * Base interface for all client plugins.
 *
 * @author jblok
 */
public interface IClientPlugin extends IPlugin, PropertyChangeListener, ISupportName, IScriptableProvider
{
	/**
	 * Called on startup after client application started.
	 */
	public void initialize(IClientPluginAccess app) throws PluginException;

	/**
	 * returns the (JavaScript)name for the plugin,name SHOULD apply to the JAVA identifier rules.
	 */
	public String getName();

	/**
	 * Get the plugin image (16x16 px), used in the developer debugger treeview and preference tabpanel.
	 * @deprecated client plugins need to implement the {@link com.servoy.j2db.plugins.IIconProvider} interface
	 *        and return the image resource url from the {@link com.servoy.j2db.plugins.IIconProvider.getIconUrl()} method,
	 *        for optimal display of the plugin icon on all OS systems.
	 *
	 * @return ImageIcon (null means not available)
	 */
	@Deprecated
	public Icon getImage();
}
