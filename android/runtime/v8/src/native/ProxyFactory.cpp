/*
 * Appcelerator Titanium Mobile
 * Copyright (c) 2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */

#include "ProxyFactory.h"

#include <map>
#include <v8.h>

#include "AndroidUtil.h"
#include "JNIUtil.h"
#include "TypeConverter.h"

using namespace std;
using namespace v8;

namespace titanium {

typedef struct {
	FunctionTemplate* v8ProxyTemplate;
	jmethodID javaProxyCreator;
} ProxyInfo;

typedef map<jclass, ProxyInfo> ProxyFactoryMap;
static ProxyFactoryMap factories;

#define GET_PROXY_INFO(jclass, info) \
	ProxyFactoryMap::iterator i = factories.find(jclass); \
	info = i != factories.end() ? &i->second : NULL;

#define LOG_JNIENV_ERROR(msgMore) \
	LOGE("ProxyFactory", "Unable to fetch JNI Environment %s", msgMore)

Handle<Object> ProxyFactory::createV8Proxy(jclass javaClass, jobject javaProxy)
{
	ProxyInfo* info;
	GET_PROXY_INFO(javaClass, info)
	if (!info) {
		LOG_JNIENV_ERROR("while creating V8 Proxy.");
		return Handle<Object>();
	}

	HandleScope scope;

	Local<Function> creator = info->v8ProxyTemplate->GetFunction();
	Local<Value> external = External::New(javaProxy);
	Local<Object> v8Proxy = creator->NewInstance(1, &external);

	return scope.Close(v8Proxy);
}

jobject ProxyFactory::createJavaProxy(jclass javaClass, Local<Object> v8Proxy, const Arguments& args)
{
	ProxyInfo* info;
	GET_PROXY_INFO(javaClass, info)
	if (!info) {
		LOGE("ProxyFactory", "No proxy info found for class.");
		return NULL;
	}

	// Create a persistent handle to the V8 proxy
	// and cast it to a pointer. The Java proxy needs
	// a reference to the V8 proxy for later use.
	jlong pv8Proxy = (jlong) *Persistent<Object>::New(v8Proxy);

	JNIEnv* env = JNIUtil::getJNIEnv();
	if (!env) {
		LOG_JNIENV_ERROR("while creating Java proxy.");
		return NULL;
	}

	// Convert the V8 arguments into Java types so they can be
	// passed to the Java creator method.
	jobjectArray javaArgs = TypeConverter::jsArgumentsToJavaArray(args, env);

	// Create the java proxy using the creator static method provided.
	// Send along a pointer to the v8 proxy so the two are linked.
	jobject javaProxy = env->CallStaticObjectMethod(javaClass, info->javaProxyCreator, pv8Proxy, javaArgs);

	env->DeleteLocalRef(javaArgs);

	return javaProxy;
}

jobject ProxyFactory::unwrapJavaProxy(const Arguments& args)
{
	if (args.Length() != 1)
		return NULL;
	Local<Value> firstArgument = args[0];
	return firstArgument->IsExternal() ? (jobject)External::Unwrap(firstArgument) : NULL;
}

void ProxyFactory::registerProxyPair(jclass javaProxyClass, FunctionTemplate* v8ProxyTemplate)
{
	JNIEnv* env = JNIUtil::getJNIEnv();
	if (!env) {
		LOG_JNIENV_ERROR("while registering proxy pair.");
		return;
	}

	ProxyInfo info;
	info.v8ProxyTemplate = v8ProxyTemplate;
	info.javaProxyCreator = env->GetStaticMethodID(javaProxyClass, "create", "([Ljava/lang/Object;J)Ljava/lang/Object");

	factories[javaProxyClass] = info;
}

}

