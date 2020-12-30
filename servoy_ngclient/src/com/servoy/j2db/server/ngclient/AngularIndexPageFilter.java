/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2018 Servoy BV

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

package com.servoy.j2db.server.ngclient;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;

/**
 * @author jcompagner
 * @since 2021.03
 *
 */
@SuppressWarnings("nls")
@WebFilter(urlPatterns = { "/solution/*" }, dispatcherTypes = { DispatcherType.REQUEST, DispatcherType.FORWARD })
public class AngularIndexPageFilter implements Filter
{
	public static final String SOLUTIONS_PATH = "solution/";
	private String indexPage = null;

	@Override
	public void init(FilterConfig filterConfig) throws ServletException
	{
		try (InputStream rs = filterConfig.getServletContext().getResourceAsStream("WEB-INF/angular-index.html"))
		{
			indexPage = IOUtils.toString(rs, Charset.forName("UTF-8"));
		}
		catch (IOException e)
		{
			throw new ServletException(e);
		}
		if (indexPage == null) throw new ServletException("Couldn't read 'WEB-INF/angular-index.html' from the context to get the angular index page");
	}

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) throws IOException, ServletException
	{
		HttpServletRequest request = (HttpServletRequest)servletRequest;
		String requestURI = request.getRequestURI();
		String solutionName = getSolutionNameFromURI(requestURI);
		if ("GET".equalsIgnoreCase(request.getMethod()) && solutionName != null)
		{
			if ((requestURI.endsWith("/") || requestURI.endsWith("/" + solutionName) || requestURI.toLowerCase().endsWith("/index.html")))
			{
				request.getSession();
				AngularIndexPageWriter.writeIndexPage(this.indexPage, request, (HttpServletResponse)servletResponse, solutionName);
				return;
			}
			else if (requestURI.toLowerCase().endsWith("/startup.js"))
			{
				AngularIndexPageWriter.writeStartupJs(request, (HttpServletResponse)servletResponse, solutionName);
				return;
			}
		}
		chain.doFilter(servletRequest, servletResponse);
	}

	@Override
	public void destroy()
	{
	}

	private String getSolutionNameFromURI(String uri)
	{
		int solutionIndex = uri.indexOf(SOLUTIONS_PATH);
		int solutionEndIndex = uri.indexOf("/", solutionIndex + SOLUTIONS_PATH.length() + 1);
		if (solutionEndIndex == -1) solutionEndIndex = uri.length();
		if (solutionIndex >= 0 && solutionEndIndex > solutionIndex)
		{
			String possibleSolutionName = uri.substring(solutionIndex + SOLUTIONS_PATH.length(), solutionEndIndex);
			// skip all names that have a . in them
			if (possibleSolutionName.contains(".")) return null;
			return possibleSolutionName;
		}
		return null;
	}

}
