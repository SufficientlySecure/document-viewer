#include <jni.h>
#include <stdlib.h>

#include <android/log.h>

#include <javahelpers.h>

/* Debugging helper */

#define DEBUG(args...) \
    __android_log_print(ANDROID_LOG_DEBUG, "EBookDroid.FB2", args)

#define ERROR(args...) \
    __android_log_print(ANDROID_LOG_ERROR, "EBookDroid.FB2", args)

#define INFO(args...) \
    __android_log_print(ANDROID_LOG_INFO, "EBookDroid.FB2", args)



void ThrowFB2Error(JNIEnv* env, const char* msg)
{
    jclass exceptionClass = env->FindClass("java/lang/RuntimeException");
    if (!exceptionClass)
        return;
    if (!msg)
        env->ThrowNew(exceptionClass, "FB2 decoding error!");
    else env->ThrowNew(exceptionClass, msg);
}

extern "C" void Java_org_ebookdroid_droids_fb2_codec_parsers_Duckbill2Parser_nativeParse(JNIEnv *env, jclass cls, 
     jcharArray xmlChars, jint length, jobject handler)
{
    DEBUG("nativeParse start");

}


