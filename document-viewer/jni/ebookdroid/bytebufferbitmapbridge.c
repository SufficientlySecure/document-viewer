#include <string.h>
#include <jni.h>
#include <stdint.h>
#include <math.h>

#include <android/log.h>

#define MAX(a,b) (((a) > (b)) ? (a) : (b))
#define MIN(a,b) (((a) < (b)) ? (a) : (b))

#define LCTX "EBookDroid.ByteBufferBitmap"

#define DEBUG(args...) \
    __android_log_print(ANDROID_LOG_DEBUG, LCTX, args)

#define ERROR(args...) \
    __android_log_print(ANDROID_LOG_ERROR, LCTX, args)

#define INFO(args...) \
    __android_log_print(ANDROID_LOG_INFO, LCTX, args)

JNIEXPORT jobject JNICALL
Java_org_ebookdroid_common_bitmaps_ByteBufferBitmap_create(JNIEnv* env, jclass classObject, jint size)
{
    void* buf = malloc(size);
    jobject obj = (*env)->NewDirectByteBuffer(env, buf, size);
    return (*env)->NewGlobalRef(env, obj);
}

JNIEXPORT void JNICALL
Java_org_ebookdroid_common_bitmaps_ByteBufferBitmap_free(JNIEnv* env, jclass classObject, jobject obj)
{
    if (!obj)
    {
        return;
    }
    void* buf = (*env)->GetDirectBufferAddress(env, obj);
    if (!buf)
    {
        return;
    }
    free(buf);
}

JNIEXPORT void JNICALL
Java_org_ebookdroid_common_bitmaps_ByteBufferBitmap_nativeInvert(JNIEnv* env, jclass classObject, jobject srcBuffer,
                                                                 jint width, jint height)
{
    int i;

    uint8_t* src;
    src = (uint8_t*) ((*env)->GetDirectBufferAddress(env, srcBuffer));
    if (!src)
    {
        ERROR("Can not get direct buffer");
        return;
    }

    for (i = 0; i < width * height * 4; i += 4)
    {
        int bright = (((src[i + 2] * 77 + src[i + 1] * 150 + src[i] * 29) >> 8) ^ 0xFF) & 0xFF;
        src[i] = bright;
        src[i + 1] = bright;
        src[i + 2] = bright;
    }
}

JNIEXPORT void JNICALL
Java_org_ebookdroid_common_bitmaps_ByteBufferBitmap_nativeFillAlpha(JNIEnv* env, jclass classObject, jobject srcBuffer,
                                                                    jint width, jint height, jint value)
{
    int i;

    uint8_t* src;
    src = (uint8_t*) ((*env)->GetDirectBufferAddress(env, srcBuffer));
    if (!src)
    {
        ERROR("Can not get direct buffer");
        return;
    }

    for (i = 0; i < width * height * 4; i += 4)
    {
        src[i + 3] = value;
    }
}

JNIEXPORT void JNICALL
Java_org_ebookdroid_common_bitmaps_ByteBufferBitmap_nativeEraseColor(JNIEnv* env, jclass classObject, jobject srcBuffer,
                                                                     jint width, jint height, jint c)
{
    int i;

    uint8_t* src;
    src = (uint8_t*) ((*env)->GetDirectBufferAddress(env, srcBuffer));
    if (!src)
    {
        ERROR("Can not get direct buffer");
        return;
    }
    uint8_t a = (uint8_t) ((c >> 24) & 0xFF);
    uint8_t r = (uint8_t) ((c >> 16) & 0xFF);
    uint8_t g = (uint8_t) ((c >> 8) & 0xFF);
    uint8_t b = (uint8_t) (c & 0xFF);

    for (i = 0; i < width * height * 4; i += 4)
    {
        src[i + 0] = r;
        src[i + 1] = g;
        src[i + 2] = b;
        src[i + 3] = a;
    }
}

JNIEXPORT void JNICALL
Java_org_ebookdroid_common_bitmaps_ByteBufferBitmap_nativeContrast(JNIEnv* env, jclass classObject, jobject srcBuffer,
                                                                   jint width, jint height, jint contrast)
{
    int i, a;
    uint8_t buf[256];
    int midBright = 0;
    uint8_t* src;
    src = (uint8_t*) ((*env)->GetDirectBufferAddress(env, srcBuffer));
    if (!src)
    {
        ERROR("Can not get direct buffer");
        return;
    }

    for (i = 0; i < width * height * 4; i += 4)
    {
        midBright += src[i + 2] * 77 + src[i + 1] * 150 + src[i] * 29;
    }
    midBright /= (256 * width * height);

    for (i = 0; i < 256; i++)
    {
        a = (((i - midBright) * contrast) / 256) + midBright;
        if (a < 0)
        {
            buf[i] = 0;
        }
        else if (a > 255)
        {
            buf[i] = 255;
        }
        else
        {
            buf[i] = a;
        }
    }

    for (i = 0; i < width * height * 4; i += 4)
    {
        src[i] = buf[src[i]];
        src[i + 1] = buf[src[i + 1]];
        src[i + 2] = buf[src[i + 2]];
    }
}

JNIEXPORT void JNICALL
Java_org_ebookdroid_common_bitmaps_ByteBufferBitmap_nativeGamma(JNIEnv* env, jclass classObject, jobject srcBuffer,
                                                                jint width, jint height, jint gamma)
{
    uint8_t map[256];
    int i;

    uint8_t* src;
    src = (uint8_t*) ((*env)->GetDirectBufferAddress(env, srcBuffer));
    if (!src)
    {
        ERROR("Can not get direct buffer");
        return;
    }

    for (i = 0; i < 256; i++)
    {
        map[i] = MIN(MAX(pow(i / 255.0f, gamma / 100.0f) * 255, 0), 255);
    }

    for (i = 0; i < width * height * 4; i += 4)
    {
        src[i] = map[src[i]];
        src[i + 1] = map[src[i + 1]];
        src[i + 2] = map[src[i + 2]];
    }

}

JNIEXPORT jint JNICALL
Java_org_ebookdroid_common_bitmaps_ByteBufferBitmap_nativeAvgLum(JNIEnv* env, jclass classObject, jobject srcBuffer,
                                                                 jint width, jint height, jint contrast)
{
    int i, a;
    int midBright = 0;
    uint8_t* src;
    src = (uint8_t*) ((*env)->GetDirectBufferAddress(env, srcBuffer));
    if (!src)
    {
        ERROR("Can not get direct buffer");
        return;
    }

    for (i = 0; i < width * height * 4; i += 4)
    {
        midBright += (MIN(src[i+2],MIN(src[i + 1],src[i])) + MAX(src[i+2],MAX(src[i + 1],src[i]))) / 2;
    }
    midBright /= (width * height);

    return (jint) midBright;
}

JNIEXPORT void JNICALL
Java_org_ebookdroid_common_bitmaps_ByteBufferBitmap_nativeExposure(JNIEnv* env, jclass classObject, jobject srcBuffer,
                                                                   jint width, jint height, jint exp)
{
    int i, a;
    uint8_t* src;
    src = (uint8_t*) ((*env)->GetDirectBufferAddress(env, srcBuffer));
    if (!src)
    {
        ERROR("Can not get direct buffer");
        return;
    }

    for (i = 0; i < width * height * 4; i += 4)
    {
        src[i] = MIN(MAX(src[i] + exp * 11 / 100, 0), 255);
        src[i + 1] = MIN(MAX(src[i+1] + exp * 59 / 100, 0), 255);
        src[i + 2] = MIN(MAX(src[i+2] + exp * 30 / 100, 0), 255);
    }
}

JNIEXPORT void JNICALL
Java_org_ebookdroid_common_bitmaps_ByteBufferBitmap_nativeAutoLevels(JNIEnv* env, jclass classObject, jobject srcBuffer,
                                                                     jint width, jint height)
{
    int i;
    int histoR[256];
    int cumulativeFreqR[256];
    int histoG[256];
    int cumulativeFreqG[256];
    int histoB[256];
    int cumulativeFreqB[256];

    int numpixels = width * height;

    uint8_t* src;
    src = (uint8_t*) ((*env)->GetDirectBufferAddress(env, srcBuffer));
    if (!src)
    {
        ERROR("Can not get direct buffer");
        return;
    }

    for (i = 0; i < 256; i++)
    {
        histoR[i] = 0;
        histoG[i] = 0;
        histoB[i] = 0;
    }
    for (i = 0; i < numpixels * 4; i += 4)
    {
        histoR[src[i]]++;
        histoG[src[i + 1]]++;
        histoB[src[i + 2]]++;
    }

    for (i = 0; i < 256; i++)
    {
        cumulativeFreqR[i] = histoR[i] + (i > 0 ? cumulativeFreqR[i - 1] : 0);
        cumulativeFreqG[i] = histoG[i] + (i > 0 ? cumulativeFreqG[i - 1] : 0);
        cumulativeFreqB[i] = histoB[i] + (i > 0 ? cumulativeFreqB[i - 1] : 0);
    }

    for (i = 0; i < numpixels * 4; i += 4)
    {
        src[i] = MIN(MAX(cumulativeFreqR[src[i]] * 255 / numpixels, 0), 255);
        src[i + 1] = MIN(MAX(cumulativeFreqG[src[i+1]] * 255 / numpixels, 0), 255);
        src[i + 2] = MIN(MAX(cumulativeFreqB[src[i+2]] * 255 / numpixels, 0), 255);
    }
}

JNIEXPORT void JNICALL
Java_org_ebookdroid_common_bitmaps_ByteBufferBitmap_nativeAutoLevels2(JNIEnv* env, jclass classObject,
                                                                      jobject srcBuffer, jint width, jint height)
{
    int i;
    int histoR[256];
    int cumulativeFreqR[256];
    int histoG[256];
    int cumulativeFreqG[256];
    int histoB[256];
    int cumulativeFreqB[256];

    int numpixels = width * height;
    int minR = 0, minG = 0, minB = 0;
    int maxR = 0, maxG = 0, maxB = 0;

    DEBUG("nativeAutoLevels");

    uint8_t* src;
    src = (uint8_t*) ((*env)->GetDirectBufferAddress(env, srcBuffer));
    if (!src)
    {
        ERROR("Can not get direct buffer");
        return;
    }
    for (i = 0; i < 256; i++)
    {
        histoR[i] = 0;
        histoG[i] = 0;
        histoB[i] = 0;
    }
    for (i = 0; i < numpixels * 4; i += 4)
    {
        histoR[src[i]]++;
        histoG[src[i + 1]]++;
        histoB[src[i + 2]]++;
    }

    for (i = 0; i < 256; i++)
    {
        cumulativeFreqR[i] = histoR[i] + (i > 0 ? cumulativeFreqR[i - 1] : 0);
        cumulativeFreqG[i] = histoG[i] + (i > 0 ? cumulativeFreqG[i - 1] : 0);
        cumulativeFreqB[i] = histoB[i] + (i > 0 ? cumulativeFreqB[i - 1] : 0);
    }

    for (i = 0; i < 256; i++)
    {
        if (cumulativeFreqR[i] > 5 * numpixels / 100 && minR == 0)
        {
            minR = MAX(0, i - 1);
        }
        if (cumulativeFreqG[i] > 5 * numpixels / 100 && minG == 0)
        {
            minG = MAX(0, i - 1);
        }
        if (cumulativeFreqB[i] > 5 * numpixels / 100 && minB == 0)
        {
            minB = MAX(0, i - 1);
        }
    }

    for (i = 255; i >= 0; i--)
    {
        if (cumulativeFreqR[i] < 95 * numpixels / 100 && maxR == 0)
        {
            maxR = MIN(255, i + 1);
        }
        if (cumulativeFreqG[i] < 95 * numpixels / 100 && maxG == 0)
        {
            maxG = MIN(255, i + 1);
        }
        if (cumulativeFreqB[i] < 95 * numpixels / 100 && maxB == 0)
        {
            maxB = MIN(255, i + 1);
        }
    }

    if (maxR - minR < 10)
    {
        minR = MAX(0, minR - 5);
        maxR = MIN(255, maxR + 5);
    }

    if (maxG - minG < 10)
    {
        minG = MAX(0, minG - 5);
        maxG = MIN(255, maxG + 5);
    }

    if (maxB - minB < 10)
    {
        minB = MAX(0, minB - 5);
        maxB = MIN(255, maxB + 5);
    }

    int min = MIN(MIN(minR, minG), minB);
    int max = MAX(MAX(maxR, maxG), maxB);

    for (i = 0; i < numpixels * 4; i += 4)
    {
        src[i] = MIN(MAX((src[i] - minR) * 255 / (maxR - minR), 0), 255);
        src[i + 1] = MIN(MAX((src[i+1] - minG) * 255 / (maxG - minG), 0), 255);
        src[i + 2] = MIN(MAX((src[i+2] - minB) * 255 / (maxB - minB), 0), 255);
    }
}

JNIEXPORT void JNICALL
Java_org_ebookdroid_common_bitmaps_ByteBufferBitmap_nativeFillRect(JNIEnv* env, jclass classObject, jobject srcBuffer,
                                                                   jint srcWidth, jobject dstBuffer, jint dstWidth,
                                                                   jint sub_x, jint sub_y, jint width, jint height)
{
    uint8_t* src;
    uint8_t* dst;
    int y;

    src = (uint8_t*) ((*env)->GetDirectBufferAddress(env, srcBuffer));
    if (!src)
    {
        ERROR("Can not get direct buffer");
        return;
    }
    dst = (uint8_t*) ((*env)->GetDirectBufferAddress(env, dstBuffer));
    if (!dst)
    {
        ERROR("Can not get direct buffer");
        return;
    }

    for (y = 0; y < height; y++)
    {
        uint8_t* row = src + ((y + sub_y) * srcWidth + sub_x) * 4;
        memcpy(dst + y * dstWidth * 4, row, width * 4);
    }
}
