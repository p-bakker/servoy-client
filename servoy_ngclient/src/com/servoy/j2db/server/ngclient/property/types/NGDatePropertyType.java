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

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONWriter;
import org.sablo.BaseWebObject;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.IBrowserConverterContext;
import org.sablo.specification.property.types.DatePropertyType;
import org.sablo.util.ValueReference;
import org.sablo.websocket.utils.DataConversion;
import org.sablo.websocket.utils.JSONUtils;

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.server.ngclient.FormElementContext;
import com.servoy.j2db.server.ngclient.INGFormElement;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IDesignToFormElement;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IFormElementToTemplateJSON;

/**
 *
 * @author acostescu, gboros
 */
public class NGDatePropertyType extends DatePropertyType implements IDesignToFormElement<Long, Date, Date>, IFormElementToTemplateJSON<Date, Date>
{

	public final static NGDatePropertyType NG_INSTANCE = new NGDatePropertyType();

	@Override
	public Date toFormElementValue(Long designValue, PropertyDescription pd, FlattenedSolution flattenedSolution, INGFormElement formElement,
		PropertyPath propertyPath)
	{
		return fromJSON(designValue, null, pd, null, null);
	}

	@Override
	public JSONWriter toTemplateJSONValue(JSONWriter writer, String key, Date formElementValue, PropertyDescription pd, DataConversion browserConversionMarkers,
		FormElementContext formElementContext) throws JSONException
	{
		return toJSON(writer, key, formElementValue, pd, browserConversionMarkers, null);
	}


	@Override
	public Date fromJSON(Object newValue, Date previousValue, PropertyDescription pd, IBrowserConverterContext dataConverterContext,
		ValueReference<Boolean> returnValueAdjustedIncommingValue)
	{
		if (newValue instanceof String)
		{
			String sDate = (String)newValue;

			// if no date conversion replace client time zone with server time zone
			if (hasNoDateConversion(dataConverterContext))
			{
				sDate = removeTimeZone(sDate);
				sDate += OffsetDateTime.now().getOffset().getId();
			}

			OffsetDateTime odt = OffsetDateTime.parse(sDate);
			return Date.from(odt.toInstant());
		}
		return null;
	}

	@Override
	public JSONWriter toJSON(JSONWriter writer, String key, Date value, PropertyDescription pd, DataConversion clientConversion,
		IBrowserConverterContext dataConverterContext) throws JSONException
	{
		if (clientConversion != null) clientConversion.convert("svy_date"); //$NON-NLS-1$
		JSONUtils.addKeyIfPresent(writer, key);
		String sDate = OffsetDateTime.ofInstant(value.toInstant(), ZoneId.systemDefault()).toString();

		// remove time zone info from sDate if no date conversion
		if (hasNoDateConversion(dataConverterContext))
		{
			sDate = removeTimeZone(sDate);
		}

		return writer.value(sDate);
	}

	private static boolean hasNoDateConversion(IBrowserConverterContext dataConverterContext)
	{
		boolean hasNoDateConversion = false;

		BaseWebObject wo = dataConverterContext.getWebObject();
		if (wo != null)
		{
			Object clientProperty = wo.getProperty("clientProperty");
			if (clientProperty instanceof Map< ? , ? >)
			{
				Map< ? , ? > clientPropertyMap = (Map< ? , ? >)clientProperty;
				Object noDateConversionObj = clientPropertyMap.get(IApplication.NG_NO_DATE_CONVERSION);
				hasNoDateConversion = noDateConversionObj instanceof Boolean && ((Boolean)noDateConversionObj).booleanValue();
			}
		}

		return hasNoDateConversion;
	}

	private static String removeTimeZone(String sDate)
	{
		String sDateWithoutTimeZone = sDate;
		int tzIdx = sDateWithoutTimeZone.indexOf('+');
		if (tzIdx == -1)
		{
			tzIdx = sDateWithoutTimeZone.indexOf('Z');
		}
		if (tzIdx != -1)
		{
			sDateWithoutTimeZone = sDateWithoutTimeZone.substring(0, tzIdx);
		}
		return sDateWithoutTimeZone;
	}
}
