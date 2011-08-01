LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)


LOCAL_MODULE    := ebookdroid

LOCAL_ARM_MODE := arm

LOCAL_CFLAGS := -DHAVE_CONFIG_H -DTHREADMODEL=NOTHREADS -DDEBUGLVL=0

LOCAL_SRC_FILES := \
	pdfdroidbridge.c \
	xpsdroidbridge.c \
	DjvuDroidBridge.cpp

LOCAL_C_INCLUDES := \
	$(LOCAL_PATH)/../mupdf/mupdf/fitz \
	$(LOCAL_PATH)/../mupdf/mupdf/pdf \
	$(LOCAL_PATH)/../mupdf/mupdf/xps \
	$(LOCAL_PATH)/../djvu

LOCAL_CXX_INCLUDES := \
	$(LOCAL_PATH)/../libdjvu

LOCAL_STATIC_LIBRARIES := mupdf djvu jpeg

# uses Android log and z library (Android-3 Native API)
LOCAL_LDLIBS := -llog -lz

include $(BUILD_SHARED_LIBRARY)

