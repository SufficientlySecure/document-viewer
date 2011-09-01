#include <jni.h>
#include <dlfcn.h>
#include <android/log.h>

#include <nativebitmap.h>

#include <fitz.h>

static void* handler = NULL;
static int present = 0;

void* NativeBitmapInit();

JNI_OnLoad(JavaVM *jvm, void *reserved)
{
    __android_log_print(ANDROID_LOG_DEBUG, "EBookDroid", "initializing EBookDroid JNI library based on MuPDF and DjVuLibre");
    fz_accelerate();
    NativeBitmap_getInfo = NULL;
    NativeBitmap_lockPixels = NULL;
    NativeBitmap_unlockPixels = NULL;

    handler = NativeBitmapInit();
    if(handler) present = 1;
    return JNI_VERSION_1_2;
}


JNIEXPORT void JNICALL JNI_OnUnload(JavaVM *jvm, void *reserved)
{
    __android_log_print(ANDROID_LOG_DEBUG, "EBookDroid", "Unloading EBookDroid JNI library based on MuPDF and DjVuLibre");
    if(handler)
	dlclose(handler);
    present = 0;
}

int NativePresent()
{
    return present;
}


void* NativeBitmapInit()
{
	void* bitmap_library = dlopen("/system/lib/libjnigraphics.so", RTLD_LAZY);
	if(bitmap_library == NULL)
	    return NULL;
	void* bitmapGetInfo = dlsym(bitmap_library,"AndroidBitmap_getInfo");
	if(bitmapGetInfo == NULL) 
	{
	    dlclose(bitmap_library);
	    return NULL;
	}
	NativeBitmap_getInfo = (AndroidBitmap_getInfo)bitmapGetInfo;

        void* bitmapLockPixels = dlsym(bitmap_library,"AndroidBitmap_lockPixels");
	if(bitmapLockPixels == NULL)
	{
	    dlclose(bitmap_library);
	    return NULL;
	}
	NativeBitmap_lockPixels = (AndroidBitmap_lockPixels)bitmapLockPixels;

	void* bitmapUnlockPixels = dlsym(bitmap_library,"AndroidBitmap_unlockPixels");
	if(bitmapUnlockPixels == NULL)
	{
	    dlclose(bitmap_library);
	    return NULL;
	}
	NativeBitmap_unlockPixels = (AndroidBitmap_unlockPixels)bitmapUnlockPixels;
	return bitmap_library;
}

