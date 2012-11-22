#ifndef JAVAHELPERS_H
#define JAVAHELPERS_H

#include <jni.h>

#ifdef __cplusplus

class ArrayListHelper
{
private:
    JNIEnv *jenv;

    jclass cls;
    jmethodID cid;
    jmethodID midAdd;

public:
    bool valid;

public:
    ArrayListHelper(JNIEnv *env)
        : jenv(env)
    {
        cls = jenv->FindClass("java/util/ArrayList");
        if (cls)
        {
            cid = jenv->GetMethodID(cls, "<init>", "()V");
            midAdd = jenv->GetMethodID(cls, "add", "(Ljava/lang/Object;)Z");
        }
        valid = cls && cid && midAdd;
    }

    jobject create()
    {
        return valid ? jenv->NewObject(cls, cid) : NULL;
    }

    void add(jobject arrayList, jobject obj)
    {
        if (valid && arrayList)
        {
            jenv->CallBooleanMethod(arrayList, midAdd, obj);
        }
    }
};

class StringHelper
{
private:
    JNIEnv *jenv;

    jclass cls;
    jmethodID midToLowerCase;
    jmethodID midIndexOf;

public:
    bool valid;

public:
    StringHelper(JNIEnv *env)
        : jenv(env)
    {
        cls = jenv->FindClass("java/lang/String");
        if (cls)
        {
            midToLowerCase = jenv->GetMethodID(cls, "toLowerCase", "()Ljava/lang/String;");
            midIndexOf = jenv->GetMethodID(cls, "indexOf", "(Ljava/lang/String;)I");
        }

        valid = cls && midToLowerCase && midIndexOf;
    }

    jstring toString(const char* str)
    {
        return jenv->NewStringUTF(str);
    }

    void release(jstring str)
    {
        jenv->DeleteLocalRef(str);
    }

    jstring toLowerCase(jstring str)
    {
        return valid && str ? (jstring) jenv->CallObjectMethod(str, midToLowerCase) : NULL;
    }

    int indexOf(jstring str, jstring pattern)
    {
        return valid && str ? jenv->CallIntMethod(str, midIndexOf, pattern) : -1;
    }
};

class CodecPageInfoHelper
{
private:
    JNIEnv *jenv;

    jclass cls;
    jfieldID fidWidth;
    jfieldID fidHeight;
    jfieldID fidDpi;
    jfieldID fidRotation;
    jfieldID fidVersion;

public:
    bool valid;

public:
    CodecPageInfoHelper(JNIEnv *env)
        : jenv(env)
    {
        cls = jenv->FindClass("org/ebookdroid/core/codec/CodecPageInfo");
        if (cls)
        {
            fidWidth = env->GetFieldID(cls, "width", "I");
            fidHeight = env->GetFieldID(cls, "height", "I");
            fidDpi = env->GetFieldID(cls, "dpi", "I");
            fidRotation = env->GetFieldID(cls, "rotation", "I");
            fidVersion = env->GetFieldID(cls, "version", "I");
        }

        valid = cls && fidWidth && fidHeight && fidDpi && fidRotation && fidVersion;
    }

    jobject setSize(jobject cpi, int width, int height)
    {
        if (valid && cpi)
        {
            jenv->SetIntField(cpi, fidWidth, width);
            jenv->SetIntField(cpi, fidHeight, height);
        }
        return cpi;
    }

    jobject setDpi(jobject cpi, int dpi)
    {
        if (valid && cpi)
        {
            jenv->SetIntField(cpi, fidDpi, dpi);
        }
        return cpi;
    }

    jobject setRotation(jobject cpi, int rotation)
    {
        if (valid && cpi)
        {
            jenv->SetIntField(cpi, fidRotation, rotation);
        }
        return cpi;
    }

    jobject setVersion(jobject cpi, int version)
    {
        if (valid && cpi)
        {
            jenv->SetIntField(cpi, fidVersion, version);
        }
        return cpi;
    }
};

class PageTextBoxHelper
{
private:
    JNIEnv *jenv;

    jclass cls;
    jmethodID cid;
    jfieldID fidLeft;
    jfieldID fidTop;
    jfieldID fidRight;
    jfieldID fidBottom;
    jfieldID fidText;

public:
    bool valid;

public:
    PageTextBoxHelper(JNIEnv *env)
        : jenv(env)
    {
        cls = jenv->FindClass("org/ebookdroid/core/codec/PageTextBox");
        if (cls)
        {
            cid = jenv->GetMethodID(cls, "<init>", "()V");
            fidLeft = env->GetFieldID(cls, "left", "F");
            fidTop = env->GetFieldID(cls, "top", "F");
            fidRight = env->GetFieldID(cls, "right", "F");
            fidBottom = env->GetFieldID(cls, "bottom", "F");
            fidText = env->GetFieldID(cls, "text", "Ljava/lang/String;");
        }

        valid = cls && cid && fidLeft && fidTop && fidRight && fidBottom && fidText;
    }

    jobject create()
    {
        return jenv->NewObject(cls, cid);
    }

    jobject setRect(jobject ptb, const int* coords)
    {
        if (valid && ptb)
        {
            jenv->SetFloatField(ptb, fidLeft, (jfloat) (float) coords[0]);
            jenv->SetFloatField(ptb, fidTop, (jfloat) (float) coords[1]);
            jenv->SetFloatField(ptb, fidRight, (jfloat) (float) coords[2]);
            jenv->SetFloatField(ptb, fidBottom, (jfloat) (float) coords[3]);
        }
        return ptb;
    }

    jobject setText(jobject ptb, jstring text)
    {
        if (valid && ptb)
        {
            jenv->SetObjectField(ptb, fidText, text);
        }
        return ptb;
    }

};

#else /* not __cplusplus */

typedef struct CharacterHelper_s CharacterHelper;
typedef struct ArrayListHelper_s ArrayListHelper;
typedef struct PageTextBoxHelper_s PageTextBoxHelper;
typedef struct RectFHelper_s RectFHelper;

struct CharacterHelper_s
{
    JNIEnv* jenv;
    jclass cls;
    jmethodID midToLowerCase;
    int valid;
};

int CharacterHelper_init(CharacterHelper* that, JNIEnv* env);

unsigned short CharacterHelper_toLowerCase(CharacterHelper* that, unsigned short ch);

struct ArrayListHelper_s
{
    JNIEnv* jenv;
    jclass cls;
    jmethodID cid;
    jmethodID midAdd;
    int valid;
};

int ArrayListHelper_init(ArrayListHelper* that, JNIEnv* env);

jobject ArrayListHelper_create(ArrayListHelper* that);

void ArrayListHelper_add(ArrayListHelper* that, jobject arrayList, jobject obj);

struct PageTextBoxHelper_s
{
    JNIEnv* jenv;
    jclass cls;
    jmethodID cid;
    jfieldID fidLeft;
    jfieldID fidTop;
    jfieldID fidRight;
    jfieldID fidBottom;
    jfieldID fidText;
    int valid;
};

int PageTextBoxHelper_init(PageTextBoxHelper* that, JNIEnv* env);

jobject PageTextBoxHelper_create(PageTextBoxHelper* that);

jobject PageTextBoxHelper_setRect(PageTextBoxHelper* that, jobject ptb, const int* coords);

jobject PageTextBoxHelper_setText(PageTextBoxHelper* that, jobject ptb, jstring text);

struct RectFHelper_s
{
    JNIEnv* jenv;
    jclass cls;
    jmethodID cid;
    jfieldID fidLeft;
    jfieldID fidTop;
    jfieldID fidRight;
    jfieldID fidBottom;
    int valid;
};

int RectFHelper_init(RectFHelper* that, JNIEnv* env);

jobject RectFHelper_create(RectFHelper* that);

jobject RectFHelper_setRectF(RectFHelper* that, jobject rectf, const float* coords);
#endif

#endif
