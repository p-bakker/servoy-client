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
package com.servoy.j2db.debug;

import java.util.Properties;

import com.servoy.j2db.util.Utils;

/**
 * Preferences holder for developer settings.
 * 
 * @author rgansevles
 * 
 */
public class DeveloperPreferences
{
	public static final String DUMMY_AUTHENTICATION_SETTING = "developer.useDummyAuth";
	public static final boolean DUMMY_AUTHENTICATION_DEFAULT = true; // note that the pref is hidden now

	public static final String SERIALIZING_PROXY_SETTING = "developer.useSerializingProxy";
	public static final boolean SERIALIZING_PROXY_DEFAULT = true; // note that the pref is hidden now

	private final Properties settings;

	public DeveloperPreferences(Properties settings)
	{
		this.settings = settings;
	}

	public boolean getUseDummyAuth()
	{
		return Utils.getAsBoolean(settings.getProperty(DUMMY_AUTHENTICATION_SETTING, String.valueOf(DUMMY_AUTHENTICATION_DEFAULT)));
	}

	public void setUseDummyAuth(boolean useDummyAuth)
	{
		settings.setProperty(DUMMY_AUTHENTICATION_SETTING, String.valueOf(useDummyAuth));
	}

	public boolean useSerializingDataserverProxy()
	{
		return Utils.getAsBoolean(settings.getProperty(SERIALIZING_PROXY_SETTING, String.valueOf(SERIALIZING_PROXY_DEFAULT)));
	}
}
