LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := ebookdroid

ifeq ($(TARGET_ARCH_ABI),armeabi)
    LOCAL_ARM_MODE := arm
endif # TARGET_ARCH_ABI == armeabi

LOCAL_CFLAGS := -DHAVE_CONFIG_H

LOCAL_SRC_FILES := \
	ebookdroidjni.c \
	djvudroidbridge.cpp \
	bytebufferbitmapbridge.c \
	mupdfdroidbridge.c \
	jni_concurrent.c \
	PageCropper.c \
	javahelpers.c

LOCAL_C_INCLUDES := \
	$(LOCAL_PATH)/../mupdf/mupdf/include \
	$(LOCAL_PATH)/../mupdf/mupdf/source/fitz \
	$(LOCAL_PATH)/../mupdf/mupdf/source/pdf \
	$(LOCAL_PATH)/../djvu/djvulibre/libdjvu

LOCAL_CXX_INCLUDES := \
	$(LOCAL_PATH)/../djvu/djvulibre/libdjvu

LOCAL_STATIC_LIBRARIES := djvu mupdfcore mupdfthirdparty

# uses Android log and z library (Android-3 Native API)
LOCAL_LDLIBS := -llog -lz

# Hack to work around "error: undefined reference to 'sigsetjmp'" link errors 
# when linking x86 and mips libebookdroid.so with NDK r12b.
# Seems to be an old bug resurfaced? https://code.google.com/p/android/issues/detail?id=19851
ifeq ($(TARGET_ARCH_ABI),x86)
	LOCAL_ALLOW_UNDEFINED_SYMBOLS := true
endif # TARGET_ARCH_ABI == x86
ifeq ($(TARGET_ARCH_ABI),mips)
	LOCAL_ALLOW_UNDEFINED_SYMBOLS := true
endif # TARGET_ARCH_ABI == mips

include $(BUILD_SHARED_LIBRARY)
