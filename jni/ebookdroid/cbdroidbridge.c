#include <string.h>
#include <jni.h>
#include <stdint.h>
#include "hqxcommon.h"
#include "hqx.h"

JNIEXPORT void JNICALL
Java_org_ebookdroid_core_bitmaps_RawBitmap_nativeHq4x(JNIEnv* env, jclass classObject, jintArray srcArray,
                                                          jintArray dstArray, jint width, jint height)
{

    jint* src;
    jint* dst;

    src = (*env)->GetIntArrayElements(env, srcArray, 0);
    dst = (*env)->GetIntArrayElements(env, dstArray, 0);

    hq4x_32(src, dst, width, height);

    (*env)->ReleaseIntArrayElements(env, srcArray, src, 0);
    (*env)->ReleaseIntArrayElements(env, dstArray, dst, 0);
}

JNIEXPORT void JNICALL
 Java_org_ebookdroid_core_bitmaps_RawBitmap_nativeHq3x(JNIEnv* env, jclass classObject, jintArray srcArray,
                                                          jintArray dstArray, jint width, jint height)
{

    jint* src;
    jint* dst;

    src = (*env)->GetIntArrayElements(env, srcArray, 0);
    dst = (*env)->GetIntArrayElements(env, dstArray, 0);

    hq3x_32(src, dst, width, height);

    (*env)->ReleaseIntArrayElements(env, srcArray, src, 0);
    (*env)->ReleaseIntArrayElements(env, dstArray, dst, 0);
}

JNIEXPORT void JNICALL
 Java_org_ebookdroid_core_bitmaps_RawBitmap_nativeHq2x(JNIEnv* env, jclass classObject, jintArray srcArray,
                                                          jintArray dstArray, jint width, jint height)
{

    jint* src;
    jint* dst;

    src = (*env)->GetIntArrayElements(env, srcArray, 0);
    dst = (*env)->GetIntArrayElements(env, dstArray, 0);

    hq2x_32(src, dst, width, height);

    (*env)->ReleaseIntArrayElements(env, srcArray, src, 0);
    (*env)->ReleaseIntArrayElements(env, dstArray, dst, 0);
}
