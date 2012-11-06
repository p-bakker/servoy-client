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
package com.servoy.j2db.server.headlessclient;

import java.util.Collection;

import org.apache.wicket.ResourceReference;
import org.apache.wicket.Response;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.MultiFileUploadField;
import org.apache.wicket.markup.html.internal.HeaderResponse;
import org.apache.wicket.markup.html.resources.JavascriptResourceReference;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.servoy.j2db.IApplication;

/**
 * @author jcompagner
 *
 */
public class MultiFileUpload extends MultiFileUploadField
{

	private static final ResourceReference JS = new JavascriptResourceReference(MultiFileUpload.class, "MultiFileUpload.js");

	public MultiFileUpload(String id, IModel<Collection<FileUpload>> model, IApplication application)
	{
		super(id, model);
		add(new Button("filebutton", new Model<String>(application.getI18NMessage("servoy.filechooser.button.upload"))));
	}

	@Override
	public void renderHead(final IHeaderResponse response)
	{
		response.renderJavascriptReference(JS); // overwrites the MultiFileUploadField.js completely (can't be extended using prototypes because it assigns it's methods in the constructor)

		// the HeaderResponse dummy is a hack to be able to intercept the object creation and replace some of the code in parent JS (it is using private fields)
		super.renderHead(new HeaderResponse()
		{
			@Override
			public void renderJavascriptReference(ResourceReference reference)
			{
				response.renderJavascriptReference(reference);
			}

			@Override
			public void renderOnDomReadyJavascript(String javascript)
			{
				int splitIdx = javascript.lastIndexOf(".addElement("); //$NON-NLS-1$
				if (splitIdx >= 0)
				{
					String constructorCall = javascript.substring(0, splitIdx);
					String functionCall = javascript.substring(splitIdx);
					response.renderOnDomReadyJavascript("var o = " + constructorCall + "; MultipleFileUploadInterceptor(o)" + functionCall);
				}
				else response.renderOnDomReadyJavascript(javascript);
			}

			@Override
			protected Response getRealResponse()
			{
				// not interested
				return null;
			}
		});
	}

}
