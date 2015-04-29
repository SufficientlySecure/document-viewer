LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_C_INCLUDES := \
	$(LOCAL_PATH)/../mupdf/mupdf/thirdparty/jpeg \
	$(LOCAL_PATH)/../mupdf/mupdf/scripts/jpeg \
	$(LOCAL_PATH)/djvulibre/libdjvu \
	$(LOCAL_PATH)

LOCAL_MODULE    := djvu
LOCAL_CFLAGS    := -fexceptions -DHAVE_CONFIG_H -DTHREADMODEL=POSIXTHREADS -DDIR_DATADIR=\"/usr/local/share\"

ifeq ($(TARGET_ARCH_ABI),armeabi)
    LOCAL_ARM_MODE := arm
endif # TARGET_ARCH_ABI == armeabi

LOCAL_SRC_FILES := \
	djvulibre/libdjvu/Arrays.cpp \
	djvulibre/libdjvu/BSByteStream.cpp \
	djvulibre/libdjvu/BSEncodeByteStream.cpp \
	djvulibre/libdjvu/ByteStream.cpp \
	djvulibre/libdjvu/DataPool.cpp \
	djvulibre/libdjvu/DjVmDir.cpp \
	djvulibre/libdjvu/DjVmDir0.cpp \
	djvulibre/libdjvu/DjVmDoc.cpp \
	djvulibre/libdjvu/DjVmNav.cpp \
	djvulibre/libdjvu/DjVuAnno.cpp \
	djvulibre/libdjvu/DjVuDocument.cpp \
	djvulibre/libdjvu/DjVuDumpHelper.cpp \
	djvulibre/libdjvu/DjVuErrorList.cpp \
	djvulibre/libdjvu/DjVuFile.cpp \
	djvulibre/libdjvu/DjVuFileCache.cpp \
	djvulibre/libdjvu/DjVuGlobal.cpp \
	djvulibre/libdjvu/DjVuGlobalMemory.cpp \
	djvulibre/libdjvu/DjVuImage.cpp \
	djvulibre/libdjvu/DjVuInfo.cpp \
	djvulibre/libdjvu/DjVuMessage.cpp \
	djvulibre/libdjvu/DjVuMessageLite.cpp \
	djvulibre/libdjvu/DjVuNavDir.cpp \
	djvulibre/libdjvu/DjVuPalette.cpp \
	djvulibre/libdjvu/DjVuPort.cpp \
	djvulibre/libdjvu/DjVuText.cpp \
	djvulibre/libdjvu/GBitmap.cpp \
	djvulibre/libdjvu/GContainer.cpp \
	djvulibre/libdjvu/GException.cpp \
	djvulibre/libdjvu/GIFFManager.cpp \
	djvulibre/libdjvu/GMapAreas.cpp \
	djvulibre/libdjvu/GOS.cpp \
	djvulibre/libdjvu/GPixmap.cpp \
	djvulibre/libdjvu/GRect.cpp \
	djvulibre/libdjvu/GScaler.cpp \
	djvulibre/libdjvu/GSmartPointer.cpp \
	djvulibre/libdjvu/GString.cpp \
	djvulibre/libdjvu/GThreads.cpp \
	djvulibre/libdjvu/GURL.cpp \
	djvulibre/libdjvu/GUnicode.cpp \
	djvulibre/libdjvu/IFFByteStream.cpp \
	djvulibre/libdjvu/IW44Image.cpp \
	djvulibre/libdjvu/IW44EncodeCodec.cpp \
	djvulibre/libdjvu/JB2Image.cpp \
	djvulibre/libdjvu/JPEGDecoder.cpp \
	djvulibre/libdjvu/MMRDecoder.cpp \
	djvulibre/libdjvu/MMX.cpp \
	djvulibre/libdjvu/UnicodeByteStream.cpp \
	djvulibre/libdjvu/XMLParser.cpp \
	djvulibre/libdjvu/XMLTags.cpp \
	djvulibre/libdjvu/ZPCodec.cpp \
	djvulibre/libdjvu/atomic.cpp \
	djvulibre/libdjvu/debug.cpp \
	djvulibre/libdjvu/ddjvuapi.cpp \
	djvulibre/libdjvu/miniexp.cpp

include $(BUILD_STATIC_LIBRARY)

