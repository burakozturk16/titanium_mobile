/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */

package ti.modules.titanium.contacts;

import java.util.ArrayList;
import java.util.Map;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.titanium.TiBlob;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.TiContext;

import android.graphics.Bitmap;

@Kroll.proxy(parentModule=ContactsModule.class, propertyAccessors={
	"lastName", "firstName", "middleName", "firstPhonetic", "lastPhonetic", "middlePhonetic", "department",
	"jobTitle", "nickname", "note", "organization", "prefix", "suffix", "birthday", "created", "modified", "kind", "email", 
	"phone", "address", TiC.PROPERTY_URL, TiC.PROPERTY_INSTANTMSG, TiC.PROPERTY_RELATED_NAMES, TiC.PROPERTY_DATE
})
public class PersonProxy extends KrollProxy
{
	private TiBlob image = null;
	public long id = -1;
	private boolean imageFetched; // lazy load these bitmap images
	protected boolean hasImage = false;
	private String fullName = "";
	
	// Contact Modifications
	private boolean nameModified = false;
	private boolean bdayModified = false;
	private boolean organizationModified = false;
	private boolean noteModified = false;
	private boolean nickNameModified = false;
	private boolean imageModified = false;

	public PersonProxy()
	{
		super();
	}

	public PersonProxy(TiContext tiContext)
	{
		this();
	}

	private boolean isPhotoFetchable()
	{
		long id = (Long) getProperty("id");
		return (id > 0 && hasImage );
	}
	
	public void finishModification()
	{
		nameModified = false;
		bdayModified = false;
		organizationModified = false;
		noteModified = false;
		nickNameModified = false;
		imageModified = false;
	}
	
	public boolean getNameModified()
	{
		return nameModified;
	}
	
	public boolean getBdayModified()
	{
		return bdayModified;
	}
	
	public boolean getOrganizationModified()
	{
		return organizationModified;
	}
	
	public boolean getNoteModified()
	{
		return noteModified;
	}
	
	public boolean getNickNameModified()
	{
		return nickNameModified;
	}
	
	public boolean getImageModified()
	{
		return imageModified;
	}
	
	@Kroll.method @Kroll.getProperty
	public String getFullName() 
	{
		return fullName;
	}
	
	public void setFullName(String fname) 
	{
		fullName = fname;
	}
	
	@Kroll.method @Kroll.getProperty
	public long getId() 
	{
		return id;
	}
	
	public void setId(long i) 
	{
		id = i;
	}

	@Kroll.method @Kroll.getProperty
	public TiBlob getImage()
	{
		if (this.image != null) {
			return this.image;
		} else if (!imageFetched && isPhotoFetchable()) {
			long id = (Long) getProperty("id");
			Bitmap photo = CommonContactsApi.getContactImage(id);
			if (photo != null) {
				this.image = TiBlob.blobFromImage(photo);
			}
			imageFetched = true;
		}
		return this.image;
	}
	
	@Kroll.method @Kroll.setProperty
	public void setImage(TiBlob blob)
	{
		image = blob;
		hasImage = true;
		imageFetched = true;
		imageModified = true;
	}

	private KrollDict contactMethodMapToDict(Map<String, ArrayList<String>> map)
	{
		KrollDict result = new KrollDict();
		for (String key : map.keySet()) {
			ArrayList<String> values = map.get(key);
			result.put(key, values.toArray());
		}
		return result;
	}

	protected void setEmailFromMap(Map<String, ArrayList<String>> map)
	{
		setProperty("email", contactMethodMapToDict(map));
	}
	
	protected void setPhoneFromMap(Map<String, ArrayList<String>> map)
	{
		setProperty("phone", contactMethodMapToDict(map));
	}
	
	protected void setAddressFromMap(Map<String, ArrayList<String>> map)
	{
		// We're supposed to support "Street", "CountryCode", "State", etc.
		// But Android 1.6 does not have structured addresses so we're just put
		// everything in Street.
		KrollDict address = new KrollDict();
		for (String key: map.keySet()) {
			ArrayList<String> values = map.get(key);
			KrollDict[] dictValues = new KrollDict[values.size()];
			for (int i = 0; i < dictValues.length; i++) {
				dictValues[i] = new KrollDict();
				dictValues[i].put("Street", values.get(i));
			}
			address.put(key, dictValues);
		}

		setProperty("address", address);
	}
	
	public void onPropertyChanged(String name, Object value)
	{
		super.onPropertyChanged(name, value);
		if (name.equals(TiC.PROPERTY_FIRSTNAME) || name.equals(TiC.PROPERTY_MIDDLENAME) || name.equals(TiC.PROPERTY_LASTNAME)) {
			nameModified = true;
		} else if (name.equals(TiC.PROPERTY_BIRTHDAY)) {
			bdayModified = true;
		} else if (name.equals(TiC.PROPERTY_ORGANIZATION)) {
			organizationModified = true;
		} else if (name.equals(TiC.PROPERTY_NOTE)) {
			noteModified = true;
		} else if (name.equals(TiC.PROPERTY_NICKNAME)) {
			nickNameModified = true;
		} 
	}
}
