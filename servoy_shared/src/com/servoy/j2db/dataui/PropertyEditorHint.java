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
package com.servoy.j2db.dataui;

import java.beans.PropertyDescriptor;
import java.util.HashMap;
import java.util.Map;


/**
 * Class for providing a hint to Servoy Developer on which property editor to use for a bean property.
 * <p>@see {@link PropertyEditorClass} for available editor classes.
 * <p>@see {@link PropertyEditorOption} for available options.
 * 
 * <p>Example:
 *  <pre>
 *  public class ExampleBeanBeanInfo extends SimpleBeanInfo
 *  {
 *  		@Override
 *  		public PropertyDescriptor[] getPropertyDescriptors()
 *  		{
 *  			try
 *  			{
 *  				PropertyDescriptor myDataprovider = new PropertyDescriptor("myDataprovider", ExampleBean.class);
 *  				PropertyEditorHint dpHint = new PropertyEditorHint(PropertyEditorClass.dataprovider);
 *  				dpHint.setOption(PropertyEditorOption.includeForm, Boolean.FALSE); // filter out form variables
 *  				dpHint.setOption(PropertyEditorOption.propertyOrder, new Integer(1)); // configure order of properties (optional)
 *  				myDataprovider.setValue(PropertyEditorHint.PROPERTY_EDITOR_HINT, dpHint);
 *  	
 *  				// the MoreInfo class acts as a bean, its properties can be set via expanding a node in the Properties View.
 *  				PropertyDescriptor moreInfo = new PropertyDescriptor("moreInfo", MoreInfo.class);
 *  				dpHint = new PropertyEditorHint(PropertyEditorClass.defaultEditor);
 *  				// configure a factory method for the MoreInfo class, if not specified, the default constructor will be used
 *  				dpHint.setOption(PropertyEditorOption.subPropertyFactoryMethod, "createMoreInfo"); // instance method ExampleBean.createMoreInfo()
 *  				dpHint.setOption(PropertyEditorOption.propertyOrder, new Integer(2)); // configure order of properties (optional)
 *  				moreInfo.setValue(PropertyEditorHint.PROPERTY_EDITOR_HINT, dpHint);
 *  				
 *  				....
 *  	
 *  				return new PropertyDescriptor[] { myDataprovider, moreInfo  , .... };
 *  			}
 *  			catch (IntrospectionException e)
 *  			{
 *  				Debug.error("ExampleBeanBeanInfo: unexpected exception: " + e);
 *  				return null;
 *  			}
 *  		}
 *  	}
 *  }
 *  </pre>
 * 
 * @author rgansevles
 * 
 * @since 5.0
 */
public class PropertyEditorHint
{
	/**
	 * Constant for configuring bean property editor hint.
	 * @see PropertyDescriptor#setValue
	 */
	public static final String PROPERTY_EDITOR_HINT = "servoy_property_editor_hint"; //$NON-NLS-1$

	private final PropertyEditorClass propertyEditorClass;
	private final Map<PropertyEditorOption, Object> options = new HashMap<PropertyEditorOption, Object>();

	public PropertyEditorHint(PropertyEditorClass propertyEditorClass)
	{
		this.propertyEditorClass = propertyEditorClass;
	}

	public PropertyEditorClass getPropertyEditorClass()
	{
		return propertyEditorClass;
	}

	public void setOption(PropertyEditorOption key, Object value)
	{
		options.put(key, value);
	}

	public Object getOption(PropertyEditorOption key)
	{
		Object option = options.get(key);
		if (option != null)
		{
			return option;
		}
		return getDefaultOption(key);
	}

	protected Object getDefaultOption(PropertyEditorOption key)
	{
		if (key == PropertyEditorOption.styleLookupName || key == PropertyEditorOption.propertyOrder || key == PropertyEditorOption.subPropertyFactoryMethod)
		{
			return null;
		}
		// the rest are boolean options, default is all true
		return Boolean.TRUE;
	}
}