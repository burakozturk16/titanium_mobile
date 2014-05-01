package com.titanium.test;

import org.appcelerator.titanium.ITiAppInfo;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.TiProperties;

/* GENERATED CODE
 * Warning - this class was generated from your application's tiapp.xml
 * Any changes you make here will be overwritten
 */
public final class TitaniumtestAppInfo implements ITiAppInfo
{
	private static final String LCAT = "AppInfo";
	
	public TitaniumtestAppInfo(TiApplication app) {
		TiProperties properties = app.getSystemProperties();
		TiProperties appProperties = app.getAppProperties();
					
					properties.setString("ti.ui.defaultunit", "dp");
					appProperties.setString("ti.ui.defaultunit", "dp");
					properties.setBool("ti.android.bug2373.finishfalseroot", true);
					appProperties.setBool("ti.android.bug2373.finishfalseroot", true);
					properties.setBool("ti.android.fastdev", false);
					appProperties.setBool("ti.android.fastdev", false);
	}
	
	public String getId() {
		return "com.titanium.test";
	}
	
	public String getName() {
		return "TitaniumTest";
	}
	
	public String getVersion() {
		return "1.0";
	}
	
	public String getPublisher() {
		return "mguillon";
	}
	
	public String getUrl() {
		return "http://";
	}
	
	public String getCopyright() {
		return "2013 by mguillon";
	}
	
	public String getDescription() {
		return "not specified";
	}
	
	public String getIcon() {
		return "appicon.png";
	}
	
	public boolean isAnalyticsEnabled() {
		return false;
	}
	
	public String getGUID() {
		return "e2a4daff-06c7-49b8-af43-34c25428e689";
	}
	
	public boolean isFullscreen() {
		return false;
	}
	
	public boolean isNavBarHidden() {
		return false;
	}

	@Override
	public String getDeployType() {		//  Auto-generated method stub
		return "development";
	}
}
