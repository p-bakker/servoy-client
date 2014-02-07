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

package com.servoy.j2db.server.webclient2.component;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;

import org.json.JSONException;

import com.servoy.j2db.util.Debug;

/**
 * Class responsible for loading a set of web component packages and specs.
 * @author acostescu
 */
@SuppressWarnings("nls")
public class WebComponentSpecProvider
{

	private final Map<String, WebComponentSpec> cachedDescriptions = new HashMap<>();

	private final File[] packages;

	public WebComponentSpecProvider(File[] packages)
	{
		this.packages = packages;
		for (File f : packages)
		{
			if (f.exists())
			{
				WebComponentPackage p;
				if (f.isDirectory()) p = new WebComponentPackage(new WebComponentPackage.DirPackageReader(f));
				else p = new WebComponentPackage(new WebComponentPackage.JarPackageReader(f));

				try
				{
					cache(p.getWebComponentDescriptions());
				}
				catch (IOException | JSONException e)
				{
					Debug.error("Cannot read web component package: " + f.getAbsolutePath(), e); //$NON-NLS-1$
				}
				finally
				{
					p.dispose();
				}
			}
			else
			{
				Debug.error("A web component package location does not exist: " + f.getAbsolutePath()); //$NON-NLS-1$
			}
		}
		instance = this;
	}

	private void cache(List<WebComponentSpec> webComponentDescriptions)
	{
		for (WebComponentSpec desc : webComponentDescriptions)
		{
			WebComponentSpec old = cachedDescriptions.put(desc.getName(), desc);
			if (old != null) Debug.error("Conflict found! Duplicate web component definition name: " + old.getName());
		}
	}

	public WebComponentSpec getWebComponentDescription(String componentTypeName)
	{
		return cachedDescriptions.get(componentTypeName);
	}

	public WebComponentSpec[] getWebComponentDescriptions()
	{
		return cachedDescriptions.values().toArray(new WebComponentSpec[cachedDescriptions.size()]);
	}

	// TODO get rid of static access
	private static final String[] WEB_COMPONENT_PACKAGE_LOCATIONS = { "/servoydefault", "/webcomponents" }; // TODO take these from the solution somehow
//	private static final String[] WEB_COMPONENT_PACKAGE_LOCATIONS = { "/servoydefault/servoydefault.jar", "/webcomponents/webcomponents.jar" }; // TODO take these from the solution somehow

	private static volatile WebComponentSpecProvider instance;

	public static WebComponentSpecProvider getInstance()
	{
		return instance;
	}

	public static WebComponentSpecProvider init(ServletContext servletContext)
	{
		if (instance == null)
		{
			synchronized (WebComponentSpecProvider.class)
			{
				if (instance == null)
				{
					// TODO remove File access, work only with input streams
					File[] locations = new File[WEB_COMPONENT_PACKAGE_LOCATIONS.length];
					for (int i = WEB_COMPONENT_PACKAGE_LOCATIONS.length - 1; i >= 0; i--)
					{
						locations[i] = new File(servletContext.getRealPath(WEB_COMPONENT_PACKAGE_LOCATIONS[i]));
					}

					instance = new WebComponentSpecProvider(locations);
				}
			}
		}
		return instance;
	}

	public static void reload()
	{
		synchronized (WebComponentSpecProvider.class)
		{
			instance = new WebComponentSpecProvider(instance.packages);
		}
	}

}
