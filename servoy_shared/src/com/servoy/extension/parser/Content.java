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

package com.servoy.extension.parser;


/**
 * Stores the 'content' (as declared in the package.xml).<br>
 * This content is used to do special developer install actions. (things to do when installing an extension in developer)
 * @author acostescu
 */
public class Content
{
	/** Relative zip paths pointing to .servoy files that should be imported in developer. */
	public final String[] solutionToImportPaths;
	/** Relative zip paths pointing to .css Servoy style files that should be imported in developer. */
	public final String[] styleToImportPaths;
	/** Relative zip paths pointing to .psf files that should be imported in developer. */
	public final String[] teamProjectSetPaths;
	/** Eclipse update sites to be added to developer. */
	public final String[] eclipseUpdateSiteURLs;

	public Content(String[] solutionToImportPaths, String[] styleToImportPaths, String[] teamProjectSetPaths, String[] eclipseUpdateSiteURLs)
	{
		this.solutionToImportPaths = solutionToImportPaths;
		this.styleToImportPaths = styleToImportPaths;
		this.teamProjectSetPaths = teamProjectSetPaths;
		this.eclipseUpdateSiteURLs = eclipseUpdateSiteURLs;
	}
}