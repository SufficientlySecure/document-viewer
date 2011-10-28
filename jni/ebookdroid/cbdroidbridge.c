#include <string.h>
#include <jni.h>
#include <stdint.h>
#include "hqxcommon.h"
#include "hqx.h"

void
Java_org_ebookdroid_cbdroid_codec_CbxPage_nativeHq4x(
		JNIEnv* env,
        jclass classObject,
        jintArray srcArray,
        jintArray dstArray,
        jint width,
        jint height) {

	jint* src;
	jint* dst;

	src = (*env)->GetIntArrayElements(env, srcArray, 0);
	dst = (*env)->GetIntArrayElements(env, dstArray, 0);

	hq4x_32(src, dst, width, height);

	(*env)->ReleaseIntArrayElements(env, srcArray, src, 0);
	(*env)->ReleaseIntArrayElements(env, dstArray, dst, 0);
}

void
Java_org_ebookdroid_cbdroid_codec_CbxPage_nativeHq2x(
		JNIEnv* env,
        jclass classObject,
        jintArray srcArray,
        jintArray dstArray,
        jint width,
        jint height) {

	jint* src;
	jint* dst;

	src = (*env)->GetIntArrayElements(env, srcArray, 0);
	dst = (*env)->GetIntArrayElements(env, dstArray, 0);

	hq2x_32(src, dst, width, height);

	(*env)->ReleaseIntArrayElements(env, srcArray, src, 0);
	(*env)->ReleaseIntArrayElements(env, dstArray, dst, 0);
}
