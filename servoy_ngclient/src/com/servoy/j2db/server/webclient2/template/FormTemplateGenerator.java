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

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Bean;
import com.servoy.j2db.persistence.Field;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.GraphicalComponent;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.TabPanel;

import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.Version;

/**
 * @author lvostinar, rgansevles
 *
 */
@SuppressWarnings("nls")
public class FormTemplateGenerator
{
	private final Configuration cfg;

	public FormTemplateGenerator(FlattenedSolution fs)
	{
		cfg = new Configuration();

		cfg.setTemplateLoader(new ClassTemplateLoader(getClass(), "templates"));
		cfg.setObjectWrapper(new FormTemplateObjectWrapper(fs));
		cfg.setDefaultEncoding("UTF-8");
		cfg.setTemplateExceptionHandler(TemplateExceptionHandler.HTML_DEBUG_HANDLER);
		cfg.setIncompatibleImprovements(new Version(2, 3, 20));
	}

	public void generate(Form form, String templateName, Writer writer) throws IOException
	{
		Template template = cfg.getTemplate(templateName);
		try
		{
			template.process(form, writer);
		}
		catch (TemplateException e)
		{
			throw new RuntimeException(e);
		}
	}

	public static String getComponentTypeName(IFormElement persist)
	{
		if (persist instanceof Bean)
		{
			String beanClass = ((Bean)persist).getBeanClassName();
			if (beanClass != null)
			{
				int index = beanClass.indexOf(":");
				if (index >= 0)
				{
					return beanClass.substring(index + 1);
				}
			}
		}
		else
		{
			if (persist instanceof GraphicalComponent)
			{
				if (com.servoy.j2db.component.ComponentFactory.isButton((GraphicalComponent)persist))
				{
					return "svy-button";
				}
				return "svy-label";
			}
			if (persist instanceof Field)
			{
				switch (((Field)persist).getDisplayType())
				{
					case Field.COMBOBOX :
						return "svy-combobox";
					case Field.TEXT_FIELD :
						return "svy-textfield";
					case Field.RADIOS :
						return "svy-radiogroup";
					case Field.CHECKS :
						return "svy-checkgroup";
					case Field.CALENDAR :
						return "svy-calendar";
					case Field.TYPE_AHEAD :
						return "svy-typeahead";
				}
			}
			if (persist instanceof TabPanel)
			{
				return "svy-tabpanel";
			}
		}
		throw new RuntimeException("unknown persist type: " + persist);
	}
}
