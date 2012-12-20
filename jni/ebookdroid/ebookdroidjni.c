#include <jni.h>
#include <dlfcn.h>
#include <android/log.h>

#include <fitz.h>

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *jvm, void *reserved)
{
    __android_log_print(ANDROID_LOG_DEBUG, "EBookDroid",
        "initializing EBookDroid JNI library based on MuPDF and DjVuLibre");
    return JNI_VERSION_1_4;
}

JNIEXPORT void JNICALL JNI_OnUnload(JavaVM *jvm, void *reserved)
{
    __android_log_print(ANDROID_LOG_DEBUG, "EBookDroid", "Unloading EBookDroid JNI library based on MuPDF and DjVuLibre");
}

JNIEXPORT jboolean JNICALL
Java_org_ebookdroid_EBookDroidLibraryLoader_free(JNIEnv *env, jobject this)
{
    __android_log_print(ANDROID_LOG_DEBUG, "EBookDroid", "Free EBookDroid JNI library");
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
