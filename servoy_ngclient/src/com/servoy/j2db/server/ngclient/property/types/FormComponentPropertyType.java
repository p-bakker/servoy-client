/*
 * Copyright (C) 2014 Servoy BV
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.servoy.j2db.server.ngclient.property.types;

import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.sablo.BaseWebObject;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebObjectFunctionDefinition;
import org.sablo.specification.WebObjectSpecification;
import org.sablo.specification.property.IBrowserConverterContext;
import org.sablo.specification.property.IConvertedPropertyType;
import org.sablo.specification.property.types.DefaultPropertyType;
import org.sablo.specification.property.types.StringPropertyType;
import org.sablo.util.ValueReference;
import org.sablo.websocket.utils.DataConversion;

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.PositionComparator;
import com.servoy.j2db.scripting.solutionmodel.JSForm;
import com.servoy.j2db.scripting.solutionmodel.JSNGWebComponent;
import com.servoy.j2db.scripting.solutionmodel.JSWebComponent;
import com.servoy.j2db.server.ngclient.ComponentFactory;
import com.servoy.j2db.server.ngclient.DataAdapterList;
import com.servoy.j2db.server.ngclient.FormElement;
import com.servoy.j2db.server.ngclient.FormElementContext;
import com.servoy.j2db.server.ngclient.FormElementHelper;
import com.servoy.j2db.server.ngclient.IFormElementCache;
import com.servoy.j2db.server.ngclient.INGFormElement;
import com.servoy.j2db.server.ngclient.WebFormComponent;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IFormElementToSabloComponent;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IFormElementToTemplateJSON;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.ISabloComponentToRhino;
import com.servoy.j2db.server.ngclient.template.FormLayoutGenerator;
import com.servoy.j2db.server.ngclient.template.FormTemplateGenerator;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.IFormComponentRhinoConverter;
import com.servoy.j2db.util.IFormComponentType;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;

/**
 * @author jcompagner
 */
public class FormComponentPropertyType extends DefaultPropertyType<Object> implements IConvertedPropertyType<Object>, ISabloComponentToRhino<Object>,
	IFormElementToTemplateJSON<Object, Object>, IFormElementToSabloComponent<Object, Object>, IFormComponentType
{
	public static final String SVY_FORM = "svy_form";

	public static final FormComponentPropertyType INSTANCE = new FormComponentPropertyType();
	public static final String TYPE_NAME = "formcomponent";

	protected FormComponentPropertyType()
	{
	}

	@Override
	public String getName()
	{
		return TYPE_NAME;
	}

	@Override
	public Object parseConfig(JSONObject json)
	{
		return json;
	}

	@Override
	public Object fromJSON(Object newJSONValue, Object previousSabloValue, PropertyDescription propertyDescription, IBrowserConverterContext context,
		ValueReference<Boolean> returnValueAdjustedIncommingValue)
	{
		return previousSabloValue;
	}

	@Override
	public JSONWriter toJSON(JSONWriter writer, String key, Object sabloValue, PropertyDescription pd, DataConversion clientConversion,
		IBrowserConverterContext dataConverterContext) throws JSONException
	{
		return writer;
	}

	@Override
	public boolean isValueAvailableInRhino(Object webComponentValue, PropertyDescription pd, BaseWebObject componentOrService)
	{
		return true;
	}

	@Override
	public Object toRhinoValue(Object webComponentValue, PropertyDescription pd, BaseWebObject componentOrService, Scriptable startScriptable)
	{
		// TODO return here a NativeScriptable object that understand the full hiearchy?
		return webComponentValue;
	}

	@Override
	public Object defaultValue(PropertyDescription pd)
	{
		return null;
	}

	@Override
	public JSONWriter toTemplateJSONValue(JSONWriter writer, String key, Object formElementValue, PropertyDescription pd,
		DataConversion browserConversionMarkers, FormElementContext formElementContext) throws JSONException
	{
		FlattenedSolution fs = formElementContext.getFlattenedSolution();
		Form form = getForm(formElementValue, fs);
		if (form != null)
		{
			// TODO now we generated the the full template per property
			// so for more then 1 property of the same formcomp (over multiply forms or the same form) this is a lot of duplication.
			// problem is that the JSON definition can change per form component so it could be different for any property...
			// but only for special case like a legacy Field if that gets a valuelist then it maps to a typeahead instead of a textfied...
			writer.key(key);
			final List<FormElement> list = FormElementHelper.INSTANCE.generateFormComponentElements(formElementContext.getFormElement(), pd,
				(JSONObject)formElementValue, form, fs, null);
			IFormElementCache cache = new IFormElementCache()
			{
				@Override
				public FormElement getFormElement(IFormElement component, FlattenedSolution flattendSol, PropertyPath path, boolean design)
				{
					for (FormElement formElement : list)
					{
						if (component.getUUID().equals(formElement.getPersistIfAvailable().getUUID()))
						{
							return formElement;
						}
					}
					return FormElementHelper.INSTANCE.getFormElement(component, flattendSol, path, design);
				}
			};
			writer.value(FormLayoutGenerator.generateFormComponent(form, fs, cache));
		}
		return writer;
	}

	/**
	 * @param formElementValue
	 * @param fs
	 * @return
	 */
	public Form getForm(Object formElementValue, FlattenedSolution fs)
	{
		Object formId = formElementValue;
		if (formId instanceof JSONObject)
		{
			formId = ((JSONObject)formId).optString("svy_form");
		}
		Form form = null;
		if (formId instanceof Integer)
		{
			form = fs.getForm(((Integer)formId).intValue());
		}
		else if (formId instanceof String || formId instanceof UUID)
		{

			UUID uuid = Utils.getAsUUID(formId, false);
			if (uuid != null) form = (Form)fs.searchPersist(uuid);
			else form = fs.getForm((String)formId);
		}
		else if (formId instanceof JSForm)
		{
			return ((JSForm)formId).getSupportChild();
		}
		return form;
	}


	@Override
	public Object toSabloComponentValue(Object formElementValue, PropertyDescription pd, INGFormElement formElement, WebFormComponent component,
		DataAdapterList dataAdapterList)
	{
		Form form = getForm(formElementValue, dataAdapterList.getApplication().getFlattenedSolution());
		if (form != null)
		{
			List<FormElement> elements = FormElementHelper.INSTANCE.getFormComponentElements(formElement, pd, (JSONObject)formElementValue, form,
				dataAdapterList.getApplication().getFlattenedSolution(), null);
			for (FormElement element : elements)
			{
				WebFormComponent comp = ComponentFactory.createComponent(dataAdapterList.getApplication(), dataAdapterList, element, null,
					dataAdapterList.getForm().getForm());
				component.getParent().add(comp);
			}
		}
		return formElementValue;
	}

	@Override
	public IFormComponentRhinoConverter getFormComponentRhinoConverter(final Object currentValue, final IApplication application, JSWebComponent webComponnent)
	{
		return new FormComponentValue((JSONObject)currentValue, application, webComponnent);
	}

	public PropertyDescription getPropertyDescription(JSONObject currentValue, FlattenedSolution fs)
	{
		PropertyDescription pd = new PropertyDescription("formcomponent", null);
		PropertyDescription formDesc = new PropertyDescription(SVY_FORM, StringPropertyType.INSTANCE);
		pd.putProperty(SVY_FORM, formDesc);
		String formName = currentValue.optString(SVY_FORM);
		Form form = getForm(formName, fs);
		if (form != null)
		{
			List<IFormElement> formelements = form.getFlattenedObjects(PositionComparator.XY_PERSIST_COMPARATOR);
			for (IFormElement element : formelements)
			{
				if (element.getName() != null)
				{
					WebObjectSpecification spec = WebComponentSpecProvider.getInstance().getWebComponentSpecification(
						FormTemplateGenerator.getComponentTypeName(element));
					pd.putProperty(element.getName(), spec);
				}
			}
		}
		return pd;
	}

	private class FormComponentValue implements IFormComponentRhinoConverter
	{
		private final PropertyDescription pd;
		private final IApplication application;
		private final JSONObject currentValue;
		private final JSWebComponent webComponent;

		public FormComponentValue(JSONObject currentValue, IApplication application, JSWebComponent webComponent)
		{
			this.webComponent = webComponent;
			this.currentValue = currentValue == null ? new JSONObject() : currentValue;
			this.application = application;
			this.pd = getPropertyDescription(currentValue, application.getFlattenedSolution());
		}

		public JSONObject setRhinoToDesignValue(String property, Object value)
		{
			if ("".equals(property))
			{
				// special case, this is just the form itself or it is a get of the complete jsonobject (current value)
				if (value == currentValue) return (JSONObject)value;
				Form form = getForm(value, application.getFlattenedSolution());
				if (form == null)
				{
					// form is reset just remove all the properties
					for (String key : Utils.iterate(currentValue.keys()))
					{
						currentValue.remove(key);
					}
				}
				else
				{
					currentValue.put(SVY_FORM, form.getUUID());
				}
			}
			else
			{
				String[] split = property.split("\\.");
				Pair<PropertyDescription, JSONObject> context = getContext(split, true);
				PropertyDescription propertyPD = context.getLeft();
				Pair<PropertyDescription, String> lastDescription = getLastDescription(propertyPD, split[split.length - 1]);
				if (value == Context.getUndefinedValue())
				{
					context.getRight().remove(lastDescription.getRight());
				}
				else
				{
					PropertyDescription last = lastDescription.getLeft();
					if (last != null)
					{
						context.getRight().put(lastDescription.getRight(),
							JSNGWebComponent.fromRhinoToDesignValue(value, last, application, webComponent, last.getName()));
					}
					else
					{
						Debug.log("Setting a property " + property + "  a value " + value + " on " + webComponent + " that has no spec");
					}
				}
			}
			return currentValue;
		}

		@Override
		public Object getDesignToRhinoValue(String property)
		{
			if ("".equals(property))
			{
				Form form = getForm(currentValue, application.getFlattenedSolution());
				return form != null ? application.getScriptEngine().getSolutionModifier().getForm(form.getName()) : null;
			}
			else
			{
				String[] split = property.split("\\.");
				Pair<PropertyDescription, JSONObject> context = getContext(split, false);
				PropertyDescription propertyPD = context.getLeft();

				Pair<PropertyDescription, String> lastDescription = getLastDescription(propertyPD, split[split.length - 1]);

				PropertyDescription last = lastDescription.getLeft();
				if (last != null)
				{
					Object value = context.getRight().opt(lastDescription.getRight());
					return JSNGWebComponent.fromDesignToRhinoValue(value, last, application, webComponent, property);
				}
				else
				{
					Debug.log("getting a property " + property + "  a value " + context.getRight().opt(lastDescription.getRight()) + " on " + webComponent +
						" that has no spec");
				}
				return null;
			}
		}

		private Pair<PropertyDescription, JSONObject> getContext(String[] split, boolean create)
		{
			JSONObject obj = currentValue;
			PropertyDescription propertyPD = pd;
			for (int i = 0; i < split.length - 1; i++)
			{
				propertyPD = propertyPD != null ? propertyPD.getProperty(split[i]) : null;
				JSONObject tmp = obj.optJSONObject(split[i]);
				if (tmp == null)
				{
					if (!create) break;
					tmp = new JSONObject();
					obj.put(split[i], tmp);
				}
				obj = tmp;
			}
			return new Pair<PropertyDescription, JSONObject>(propertyPD, obj);
		}

		private Pair<PropertyDescription, String> getLastDescription(PropertyDescription propertyPD, String property)
		{
			String propname = property;
			PropertyDescription last = null;
			if (propertyPD != null)
			{
				last = propertyPD.getProperty(propname);
				if (last == null)
				{
					last = propertyPD.getProperty(propname + "ID"); // legacy
					if (last != null)
					{
						propname = propname + "ID";
					}
				}
				if (last == null && propertyPD instanceof WebObjectSpecification)
				{
					// its a handler
					WebObjectFunctionDefinition handler = ((WebObjectSpecification)propertyPD).getHandler(propname);
					if (handler == null)
					{
						handler = ((WebObjectSpecification)propertyPD).getHandler(propname + "MethodID"); // legacy
						if (handler != null) propname = propname + "MethodID";
					}
					if (handler != null) last = handler.getAsPropertyDescription();
				}
			}
			return new Pair<PropertyDescription, String>(last, propname);
		}
	}
}