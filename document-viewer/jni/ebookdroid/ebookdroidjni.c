#include <jni.h>
#include <dlfcn.h>
#include <android/log.h>

#include <fitz.h>

#define LCTX "EBookDroid"

#define DEBUG(args...) \
    __android_log_print(ANDROID_LOG_DEBUG, LCTX, args)

#define ERROR(args...) \
    __android_log_print(ANDROID_LOG_ERROR, LCTX, args)

#define INFO(args...) \
    __android_log_print(ANDROID_LOG_INFO, LCTX, args)

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *jvm, void *reserved)
{
    DEBUG("Initializing EBookDroid JNI library based on MuPDF and DjVuLibre");
    return JNI_VERSION_1_4;
}

JNIEXPORT void JNICALL JNI_OnUnload(JavaVM *jvm, void *reserved)
{
    DEBUG("Unloading EBookDroid JNI library based on MuPDF and DjVuLibre");
}

JNIEXPORT jboolean JNICALL
Java_org_ebookdroid_EBookDroidLibraryLoader_free(JNIEnv *env, jobject this)
{
    DEBUG("Free EBookDroid JNI library");
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
