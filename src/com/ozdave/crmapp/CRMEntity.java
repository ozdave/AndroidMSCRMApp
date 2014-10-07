package com.ozdave.crmapp;

import java.io.IOException;
import java.util.HashMap;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.util.Log;




public class CRMEntity {

	
	private static final String TAG = "CRMApp";

	private String m_id;
	
	private String m_name;
	
	private HashMap<String, String> m_attributes = new HashMap<String, String>();
	
	
	/**
	 * Create a CRM entity given an XML parser that is sittign on the start of the 'entity' node in CRM soap response.
	 * Example:
	 * 
		<a:Entity>
			<a:Attributes xmlns:b="http://schemas.datacontract.org/2004/07/System.Collections.Generic">
				<a:KeyValuePairOfstringanyType>
					<b:key>name</b:key>
					<b:value i:type="c:string" xmlns:c="http://www.w3.org/2001/XMLSchema">Account 1</b:value>
				</a:KeyValuePairOfstringanyType>
				<a:KeyValuePairOfstringanyType>
					<b:key>telephone1</b:key>
					<b:value i:type="c:string" xmlns:c="http://www.w3.org/2001/XMLSchema">949 111 1111</b:value>
				</a:KeyValuePairOfstringanyType>
				<a:KeyValuePairOfstringanyType>
					<b:key>accountid</b:key>
					<b:value i:type="c:guid" xmlns:c="http://schemas.microsoft.com/2003/10/Serialization/">d39b3b78-d93b-e411-b297-6c3be5a8fdb8</b:value>
				</a:KeyValuePairOfstringanyType>
			</a:Attributes>
			<a:EntityState i:nil="true"/>
			<a:FormattedValues xmlns:b="http://schemas.datacontract.org/2004/07/System.Collections.Generic"/>
			<a:Id>d39b3b78-d93b-e411-b297-6c3be5a8fdb8</a:Id>
			<a:LogicalName>account</a:LogicalName>
			<a:RelatedEntities xmlns:b="http://schemas.datacontract.org/2004/07/System.Collections.Generic"/>
		</a:Entity>
	 * 
	 * @param parser
	 */
	public CRMEntity(XmlPullParser parser) {
		Boolean endOfEntity = false;
		
		try {
		    if (parser.getEventType() != XmlPullParser.START_TAG) {
		        throw new IllegalStateException();
		    }
			while (!endOfEntity) {
				String curName = parser.getName();
				//Log.d(TAG, "CRMEntity ctor: current name: " + curName);
				if (curName.equalsIgnoreCase("a:Id")) {
					m_id = readText(parser);
				}
				else if (curName.equalsIgnoreCase("a:LogicalName")) {
					m_name = readText(parser);
				}
				else if (curName.equalsIgnoreCase("a:Attributes")) {
					readAttrs(parser);
				}
				int code = parser.next();
				//Log.d(TAG, "CRMEntity ctor: code = " + code + "  current name: " + parser.getName());
				endOfEntity = (code == XmlPullParser.END_TAG) && (parser.getName().equalsIgnoreCase("a:Entity"));
			}
		} catch(Exception ex) {
			Log.d(TAG, "CRMEntity - execption parsing XML", ex);
		}
		Log.d(TAG, "CRMEntity.ctor returning entity id = " + m_id);
	}
	
	
	
	private void readAttrs(XmlPullParser parser) {
		String key = null, val = null;
		Boolean endOfAttrs = false;
		try {
		    if (parser.getEventType() != XmlPullParser.START_TAG) {
		        throw new IllegalStateException();
		    }
			while (!endOfAttrs) {
				String curName = parser.getName();
				//Log.d(TAG, "CRMEntity.readAttrs: current name: " + curName);
				if (curName.equalsIgnoreCase("a:KeyValuePairOfstringanyType")) {
					for (int i = 0; i < 2; i++) {
						parser.next();
						curName = parser.getName();
						if (curName.equalsIgnoreCase("b:key")) {
							key = readText(parser);
						}
						else if (curName.equalsIgnoreCase("b:value")) {
							val = readText(parser);
						}
					}
					m_attributes.put(key, val);
					Log.d(TAG, "CRMEntity.readAttrs added key = " + key + "  val = " + val + "  parser.curName = " + parser.getName());
					// now read the end tag
					int code = parser.next();
					//Log.d(TAG, "CRMEntity.readAttrs loop: code = " + code + "  current name: " + parser.getName());
				}
				int code = parser.next();
				//Log.d(TAG, "CRMEntity.readAttrs: code = " + code + "  current name: " + parser.getName());
				endOfAttrs = (code == XmlPullParser.END_TAG) && (parser.getName().equalsIgnoreCase("a:Attributes"));
			}
		} catch(Exception ex) {
			Log.d(TAG, "CRMEntity.readAttrs - exception parsing XML", ex);
		}
		
	}

	
	public static  String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
	    String result = "";
	    if (parser.next() == XmlPullParser.TEXT) {
	        result = parser.getText();
	        parser.nextTag();
	    }
	    return result;
	}
	
	
	
	// see http://developer.android.com/training/basics/network-ops/xml.html
	public static void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
	    if (parser.getEventType() != XmlPullParser.START_TAG) {
	        throw new IllegalStateException();
	    }
	    int depth = 1;
	    while (depth != 0) {
	        switch (parser.next()) {
	        case XmlPullParser.END_TAG:
	            depth--;
	            break;
	        case XmlPullParser.START_TAG:
	            depth++;
	            break;
	        }
	    }
	 }
	
	
	public String getId() {
		return m_id;
	}

	public void setId(String m_id) {
		this.m_id = m_id;
	}

	public String getName() {
		return m_name;
	}

	public void setName(String m_name) {
		this.m_name = m_name;
	}

	public HashMap<String, String> getAttributes() {
		return m_attributes;
	}

	public void setAttributes(HashMap<String, String> m_attributes) {
		this.m_attributes = m_attributes;
	}
	
	
}
