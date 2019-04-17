APP_ABI := armeabi-v7a arm64-v8a x86
APP_PLATFORM := android-14
APP_STL := c++_static
APP_OPTIM := release
VA_ROOT          := $(call my-dir)
NDK_MODULE_PATH  := $(NDK_MODULE_PATH):$(VA_ROOT)