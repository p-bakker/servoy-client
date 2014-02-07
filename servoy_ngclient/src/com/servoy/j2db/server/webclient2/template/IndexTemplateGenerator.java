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

package com.servoy.j2db.server.webclient2.template;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.Solution;

import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.Version;

/**
 *  This class will be obsolete when lazy loading of scripts comes into play
 * @author obuligan
 *
 */
@SuppressWarnings("nls")
public class IndexTemplateGenerator
{
	private final Configuration cfg;

	public IndexTemplateGenerator(FlattenedSolution fs)
	{
		cfg = new Configuration();

		cfg.setTemplateLoader(new ClassTemplateLoader(getClass(), "templates"));
		cfg.setObjectWrapper(new IndexTemplateObjectWrapper(fs));
		cfg.setDefaultEncoding("UTF-8");
		cfg.setTemplateExceptionHandler(TemplateExceptionHandler.HTML_DEBUG_HANDLER);
		cfg.setIncompatibleImprovements(new Version(2, 3, 20));
	}

	public void generate(FlattenedSolution fs, String templateName, Writer writer) throws IOException
	{
		Template template = cfg.getTemplate(templateName);
		try
		{
			template.process(fs, writer);
		}
		catch (TemplateException e)
		{
			throw new RuntimeException(e);
		}
	}

	private static class IndexTemplateObjectWrapper extends DefaultObjectWrapper
	{
		private final FlattenedSolution fs;

		/**
		 * @param fs
		 */
		public IndexTemplateObjectWrapper(FlattenedSolution fs)
		{
			this.fs = fs;
		}

		@Override
		public TemplateModel wrap(Object obj) throws TemplateModelException
		{
			Object wrapped;
			if (obj instanceof FlattenedSolution)
			{
				wrapped = new FormReferencesWrapper((FlattenedSolution)obj);
			}
			else
			{
				wrapped = obj;
			}
			return super.wrap(wrapped);
		}

	}

	public static class FormReferencesWrapper
	{
		private final FlattenedSolution fs;

		public FormReferencesWrapper(FlattenedSolution fs)
		{
			this.fs = fs;
		}

		/** exposed in tpl file */
		public Collection<String> getFormScriptReferences()
		{
			List<String> forms = new ArrayList<>();
			Iterator<Form> it = fs.getForms(false);
			while (it.hasNext())
			{
				Form form = it.next();

				Solution sol = (Solution)form.getAncestor(IRepository.SOLUTIONS);
				forms.add("solutions/" + sol.getName() + "/forms/" + form.getName() + ".js");
			}
			return forms;
		}
	}
}
