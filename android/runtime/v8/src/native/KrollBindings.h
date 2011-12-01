/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#ifndef KROLL_BINDINGS_H
#define KROLL_BINDINGS_H

#include <map>
#include <v8.h>

namespace titanium {

namespace bindings {
	typedef void (*BindCallback)(v8::Handle<v8::Object> exports);
	typedef void (*DisposeCallback)();

	struct BindEntry {
		const char *name;
		BindCallback bind;
		DisposeCallback dispose;
	};

} // namespace bindings

class KrollBindings
{
private:
	static std::map<const char *, const char *> externalModules;
	static std::map<const char *, bindings::BindEntry*> externalBindings;

public:
	static void initNatives(v8::Handle<v8::Object> exports);
	static void initTitanium(v8::Handle<v8::Object> exports);
	static void disposeTitanium();

	static v8::Handle<v8::String> getMainSource();

	static v8::Handle<v8::Value> getBinding(const v8::Arguments& args);
	static v8::Handle<v8::Object> getBinding(v8::Handle<v8::String> binding);

	static void addExternalModule(const char *moduleId, const char *libName)
	{
		externalModules[moduleId] = libName;
	}

	static void addExternalBinding(const char *name, bindings::BindEntry *binding)
	{
		externalBindings[name] = binding;
	}

	static void dispose();
};

} // namespace titanium

#endif
