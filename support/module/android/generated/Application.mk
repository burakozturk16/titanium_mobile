# Main NDK build script for @MODULE_ID@

APP_BUILD_SCRIPT := jni/Android.mk
TARGET_PLATFORM := android-16
APP_ABI := armeabi armeabi-v7a x86

APP_STL := gnustl_static
APP_CPPFLAGS += -frtti 
APP_CPPFLAGS += -fexceptions

#APP_ABI := all
NDK_TOOLCHAIN_VERSION=4.4.3
APP_PLATFORM := android-16