/* AUTO-GENERATED FILE.  DO NOT MODIFY.
 *
 * This class was automatically generated by 
 * Appcelerator. It should not be modified by hand.
 */
package ${config['appid']};

import org.appcelerator.kroll.runtime.v8.V8Runtime;

import org.appcelerator.kroll.KrollModule;
import org.appcelerator.kroll.KrollModuleInfo;
import org.appcelerator.kroll.KrollRuntime;
import org.appcelerator.kroll.util.KrollAssetHelper;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.TiRootActivity;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;

import android.util.Log;


public final class ${config['classname']}Application extends TiApplication
{
	private static final String TAG = "${config['classname']}Application";

	@Override
	public void onCreate()
	{
		super.onCreate();

		appInfo = new ${config['classname']}AppInfo(this);
		postAppInfo();

		% if config['compile_js']:
		    KrollAssetHelper.setAssetCrypt(new AssetCryptImpl());
		% endif

		V8Runtime runtime = new V8Runtime();

		% for module in custom_modules:
		<%
		manifest = module['manifest']
		className = module['module_apiName']
		isJSMod = (module.has_key('is_native_js_module') and module['is_native_js_module'])
		%>
		runtime.addExternalModule("${manifest.moduleid}", ${manifest.moduleid}.${className}Bootstrap.class);
		% if isJSMod:
		runtime.addExternalCommonJsModule("${manifest.moduleid}", ${manifest.moduleid}.CommonJsSourceProvider.class);
		% endif
		% endfor

		KrollRuntime.init(this, runtime);

		stylesheet = new ApplicationStylesheet();
		postOnCreate();

		<%def name="onAppCreate(module)" filter="trim">
			% if module['on_app_create'] != None:
			try {
				${module['class_name']}.${module['on_app_create']}(this);
			} catch (Exception e) {
				StringBuilder error = new StringBuilder();
				error.append("Error running onAppCreate method ")
					.append("\"${module['on_app_create']}\"")
					.append(" for module ")
					.append("${module.get('api_name') or module.get('manifest').moduleid}: ")
					.append(e.getMessage());
				Log.e(TAG, error.toString(), e);
			}
			% endif
		</%def>

		% for module in app_modules:
		${onAppCreate(module)} \
		% endfor

		% if len(custom_modules) > 0:
		// Custom modules
		KrollModuleInfo moduleInfo;
		% endif

		% for module in custom_modules:
		${onAppCreate(module)} \

		<%
		manifest = module['manifest']
		isJSMod = (module.has_key('is_native_js_module') and module['is_native_js_module'])
		%>

		moduleInfo = new KrollModuleInfo(
			"${manifest.name}", "${manifest.moduleid}", "${manifest.guid}", "${manifest.version}",
			"${manifest.description}", "${manifest.author}", "${manifest.license}",
			"${manifest.copyright}");

		% if manifest.has_property("licensekey"):
		moduleInfo.setLicenseKey("${manifest.licensekey}");
		% endif

		% if isJSMod:
		moduleInfo.setIsJSModule(true);
		% endif

		KrollModule.addCustomModuleInfo(moduleInfo);
		% endfor
	}

	@Override
	public void verifyCustomModules(TiRootActivity rootActivity)
	{
	}
}
