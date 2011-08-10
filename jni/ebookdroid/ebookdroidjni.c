#include <jni.h>
#include <android/log.h>

#include <fitz.h>


JNI_OnLoad(JavaVM *jvm, void *reserved)
{
	__android_log_print(ANDROID_LOG_DEBUG, "EBookDroid", "initializing EBookDroid JNI library based on MuPDF and DjVuLibre");
	fz_accelerate();
	return JNI_VERSION_1_2;
}
