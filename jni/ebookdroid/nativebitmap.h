#ifndef NATIVEBITMAP_H
#define NATIVEBITMAP_H

#include <stdint.h>
#include <jni.h>

#ifdef __cplusplus
extern "C"
{
#endif

#define ANDROID_BITMAP_RESUT_SUCCESS            0
#define ANDROID_BITMAP_RESULT_BAD_PARAMETER     -1
#define ANDROID_BITMAP_RESULT_JNI_EXCEPTION     -2
#define ANDROID_BITMAP_RESULT_ALLOCATION_FAILED -3

enum AndroidBitmapFormat
{
    ANDROID_BITMAP_FORMAT_NONE = 0,
    ANDROID_BITMAP_FORMAT_RGBA_8888 = 1,
    ANDROID_BITMAP_FORMAT_RGB_565 = 4,
    ANDROID_BITMAP_FORMAT_RGBA_4444 = 7,
    ANDROID_BITMAP_FORMAT_A_8 = 8,
};

typedef struct
{
    uint32_t width;
    uint32_t height;
    uint32_t stride;
    int32_t format;
    uint32_t flags;      // 0 for now
} AndroidBitmapInfo;

typedef int (*AndroidBitmap_getInfo)(JNIEnv* env, jobject jbitmap, AndroidBitmapInfo* info);
typedef int (*AndroidBitmap_lockPixels)(JNIEnv* env, jobject jbitmap, void** addrPtr);
typedef int (*AndroidBitmap_unlockPixels)(JNIEnv* env, jobject jbitmap);

AndroidBitmap_getInfo NativeBitmap_getInfo;
AndroidBitmap_lockPixels NativeBitmap_lockPixels;
AndroidBitmap_unlockPixels NativeBitmap_unlockPixels;

#ifdef __cplusplus
}
#endif

#endif
