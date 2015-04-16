LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := mupdfcore
LOCAL_SRC_FILES := \
	$(subst $(LOCAL_PATH)/,, \
	    $(wildcard $(LOCAL_PATH)/mupdf/source/fitz/*.c) \
	    $(wildcard $(LOCAL_PATH)/mupdf/source/pdf/*.c) \
	    $(wildcard $(LOCAL_PATH)/mupdf/source/xps/*.c) \
	    $(wildcard $(LOCAL_PATH)/mupdf/source/cbz/*.c) \
	    $(wildcard $(LOCAL_PATH)/mupdf/source/html/*.c) \
    ) \
	mupdf/source/pdf/js/pdf-js.c \
	mupdf/source/pdf/js/pdf-jsimp-mu.c

ifeq ($(TARGET_ARCH),arm)
LOCAL_CFLAGS += -DARCH_ARM -DARCH_THUMB -DARCH_ARM_CAN_LOAD_UNALIGNED
endif
LOCAL_CFLAGS += -DAA_BITS=8

LOCAL_C_INCLUDES := \
	$(LOCAL_PATH)/mupdf/thirdparty/jbig2dec \
	$(LOCAL_PATH)/mupdf/thirdparty/openjpeg/libopenjpeg \
	$(LOCAL_PATH)/mupdf/thirdparty/jpeg \
	$(LOCAL_PATH)/mupdf/thirdparty/mujs \
	$(LOCAL_PATH)/mupdf/thirdparty/zlib \
	$(LOCAL_PATH)/mupdf/thirdparty/freetype/include \
	$(LOCAL_PATH)/mupdf/source/fitz \
	$(LOCAL_PATH)/mupdf/source/pdf \
	$(LOCAL_PATH)/mupdf/source/xps \
	$(LOCAL_PATH)/mupdf/source/cbz \
	$(LOCAL_PATH)/mupdf/source/img \
	$(LOCAL_PATH)/mupdf/source/tiff \
	$(LOCAL_PATH)/mupdf/scripts/freetype \
	$(LOCAL_PATH)/mupdf/scripts/jpeg \
	$(LOCAL_PATH)/mupdf/scripts/openjpeg \
	$(LOCAL_PATH)/mupdf/generated \
	$(LOCAL_PATH)/mupdf/resources \
	$(LOCAL_PATH)/mupdf/include \
	$(LOCAL_PATH)/mupdf

LOCAL_LDLIBS    := -lm -llog -ljnigraphics

include $(BUILD_STATIC_LIBRARY)
