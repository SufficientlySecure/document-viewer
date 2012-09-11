#include <jni.h>
#include <dlfcn.h>
#include <android/log.h>

#include <nativebitmap.h>

#include <fitz.h>

static void* handler = NULL;
static int present = 0;

void* NativeBitmapInit();
void closeHandler();

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *jvm, void *reserved)
{
    __android_log_print(ANDROID_LOG_DEBUG, "EBookDroid",
        "initializing EBookDroid JNI library based on MuPDF and DjVuLibre");
#ifndef __mips__
     atexit(closeHandler);
#endif
    NativeBitmap_getInfo = NULL;
    NativeBitmap_lockPixels = NULL;
    NativeBitmap_unlockPixels = NULL;

    handler = NativeBitmapInit();
    if (handler)
        present = 1;
    return JNI_VERSION_1_4;
}

JNIEXPORT void JNICALL JNI_OnUnload(JavaVM *jvm, void *reserved)
{
    __android_log_print(ANDROID_LOG_DEBUG, "EBookDroid", "Unloading EBookDroid JNI library based on MuPDF and DjVuLibre");
    closeHandler();
}

JNIEXPORT jboolean JNICALL
Java_org_ebookdroid_EBookDroidLibraryLoader_free(JNIEnv *env, jobject this)
{
    __android_log_print(ANDROID_LOG_DEBUG, "EBookDroid", "Free EBookDroid JNI library");
    closeHandler();
}

JNIEXPORT jboolean JNICALL
Java_org_ebookdroid_EBookDroidLibraryLoader_isNativeGraphicsAvailable(JNIEnv *env, jobject this)
{
    return present;
}

void* NativeBitmapInit()
{
    void* bitmap_library = dlopen("/system/lib/libjnigraphics.so", RTLD_LAZY);
    if (bitmap_library == NULL)
        return NULL;
    void* bitmapGetInfo = dlsym(bitmap_library, "AndroidBitmap_getInfo");
    if (bitmapGetInfo == NULL)
    {
        dlclose(bitmap_library);
        return NULL;
    }
    NativeBitmap_getInfo = (AndroidBitmap_getInfo) bitmapGetInfo;

    void* bitmapLockPixels = dlsym(bitmap_library, "AndroidBitmap_lockPixels");
    if (bitmapLockPixels == NULL)
    {
        dlclose(bitmap_library);
        return NULL;
    }
    NativeBitmap_lockPixels = (AndroidBitmap_lockPixels) bitmapLockPixels;

    void* bitmapUnlockPixels = dlsym(bitmap_library, "AndroidBitmap_unlockPixels");
    if (bitmapUnlockPixels == NULL)
    {
        dlclose(bitmap_library);
        return NULL;
    }
    NativeBitmap_unlockPixels = (AndroidBitmap_unlockPixels) bitmapUnlockPixels;
    return bitmap_library;
}

void closeHandler()
{
    __android_log_print(ANDROID_LOG_DEBUG, "EBookDroid", "closeHandler");
    present = 0;
    if (handler)
        dlclose(handler);
    handler = NULL;
}


/**
 * Get file descriptor from FileDescriptor class.
 */
int getDescriptor(JNIEnv *env, jobject fd)
{
    __android_log_print(ANDROID_LOG_DEBUG, "EBookDroid", "getDescriptor");
    jclass fd_class = (*env)->GetObjectClass(env, fd);
    jfieldID field_id = (*env)->GetFieldID(env, fd_class, "descriptor", "I");
    return (*env)->GetIntField(env, fd, field_id);
}

const char* GetStringUTFChars(JNIEnv *env, jstring jstr, jboolean* iscopy)
{
    return jstr != NULL ? (*env)->GetStringUTFChars(env, jstr, iscopy) : NULL ;
}

void ReleaseStringUTFChars(JNIEnv *env, jstring jstr, const char* str)
{
    if (jstr && str)
    {
        (*env)->ReleaseStringUTFChars(env, jstr, str);
    }
}

JNIEXPORT jint JNICALL Java_org_ebookdroid_common_bitmaps_NativeTextureRef_nativeSetPixels(JNIEnv *env, jclass cls,
                                                                                           jint ref, jint ownwidth,
                                                                                           jint ownheight,
                                                                                           jintArray pixels, jint width,
                                                                                           jint height)
{
    jint *storage = (jint*) ref;

    // __android_log_print(ANDROID_LOG_DEBUG, "EBookDroid", "nativeSetPixels(%p, [%i, %i], [%i, %i]): start", storage, ownwidth, ownheight, width, height);

    jint *buffer = (*env)->GetPrimitiveArrayCritical(env, pixels, 0);
    if (buffer == NULL)
    {
        // __android_log_print(ANDROID_LOG_DEBUG, "EBookDroid", "nativeSetPixels(): cannot lock pixels");
        if (storage != NULL)
        {
            free(storage);
        }
        // __android_log_print(ANDROID_LOG_DEBUG, "EBookDroid", "nativeSetPixels(): end");
        return (jint) NULL ;
    }

    off_t stsize = ownwidth * ownwidth * sizeof(jint);

    if (storage == NULL)
    {
        storage = (jint*) malloc(stsize);
    }

    if (storage != NULL)
    {
        if (ownwidth != width || ownheight != ownheight)
        {
            int offset = 0;
            int ownoffset = 0;
            int y;
            for (y = 0; y < height; y++)
            {
                memcpy(storage + ownoffset, buffer + offset, width * sizeof(jint));
                offset += width;
                ownoffset += ownwidth;
            }
        }
        else
        {
            memcpy(storage, buffer, stsize);
        }
    }
    else
    {
        // __android_log_print(ANDROID_LOG_DEBUG, "EBookDroid", "nativeSetPixels(): cannot alloc memory");
    }

    (*env)->ReleasePrimitiveArrayCritical(env, pixels, buffer, 0);

    // __android_log_print(ANDROID_LOG_DEBUG, "EBookDroid", "nativeSetPixels(%p): end", storage);

    return (jint) storage;
}

JNIEXPORT void JNICALL Java_org_ebookdroid_common_bitmaps_NativeTextureRef_nativeGetRegionPixels(JNIEnv *env,
                                                                                                 jclass cls, jint ref,
                                                                                                 jint ownwidth,
                                                                                                 jint ownheight,
                                                                                                 jintArray pixels,
                                                                                                 jint left, jint top,
                                                                                                 jint width,
                                                                                                 jint height)
{

    jint *storage = (jint*) ref;
    // __android_log_print(ANDROID_LOG_DEBUG, "EBookDroid", "nativeGetRegionPixels(%p, [%i, %i], [%i, %i], [%i, %i]): start", storage, ownwidth, ownheight, left, top, width, height);

    if (storage != NULL)
    {
        jint *buffer = (*env)->GetPrimitiveArrayCritical(env, pixels, 0);

        int ownoffset = top * ownwidth + left;
        int offset = 0;
        int y;
        for (y = 0; y < height; y++)
        {
            memcpy(buffer + offset, storage + ownoffset, width * sizeof(jint));
            offset += width;
            ownoffset += ownwidth;
        }

        (*env)->ReleasePrimitiveArrayCritical(env, pixels, buffer, 0);
    }

    // __android_log_print(ANDROID_LOG_DEBUG, "EBookDroid", "nativeGetRegionPixels(): end");

}

JNIEXPORT void JNICALL Java_org_ebookdroid_common_bitmaps_NativeTextureRef_nativeGetPixels(JNIEnv *env, jclass cls,
                                                                                           jint ref, jintArray pixels,
                                                                                           jint width, jint height)
{
    jint *storage = (jint*) ref;

    // __android_log_print(ANDROID_LOG_DEBUG, "EBookDroid", "nativeGetPixels(%p, %i, %i): start", storage, width, height);
    if (storage != NULL)
    {
        jint *buffer = (*env)->GetPrimitiveArrayCritical(env, pixels, 0);
        memcpy(buffer, storage, width * height * sizeof(jint));
        (*env)->ReleasePrimitiveArrayCritical(env, pixels, buffer, 0);
    }

    // __android_log_print(ANDROID_LOG_DEBUG, "EBookDroid", "nativeGetPixels(): end");

}

JNIEXPORT jint JNICALL Java_org_ebookdroid_common_bitmaps_NativeTextureRef_nativeEraseColor(JNIEnv *env, jclass cls,
                                                                                            jint ref, jint color,
                                                                                            jint width, jint height)
{
    jint* storage = (jint*) ref;

    // __android_log_print(ANDROID_LOG_DEBUG, "EBookDroid", "nativeEraseColor(%p, %i, %i, %i): start", storage, color, width, height);

    int size = width * height;

    if (storage == NULL)
    {
        storage = (jint*) malloc(size * sizeof(jint));
    }

    if (storage != NULL)
    {
        int i;
        for (i = 0; i < size; i++)
        {
            storage[i] = color;
        }
    }
    else
    {
        // __android_log_print(ANDROID_LOG_DEBUG, "EBookDroid", "nativeEraseColor(): cannot alloc memory");
    }

    // __android_log_print(ANDROID_LOG_DEBUG, "EBookDroid", "nativeEraseColor(%p): end", storage);

    return (jint) storage;
}

JNIEXPORT jint JNICALL Java_org_ebookdroid_common_bitmaps_NativeTextureRef_nativeRecycle(JNIEnv *env, jclass cls,
                                                                                         jint ref)
{
    // __android_log_print(ANDROID_LOG_DEBUG, "EBookDroid", "nativeRecycle(): start");

    jint *storage = (jint*) ref;
    if (storage != NULL)
    {
        free(storage);
    }

    // __android_log_print(ANDROID_LOG_DEBUG, "EBookDroid", "nativeRecycle(): end");

    return (jint) NULL ;
}

