#include <jni.h>
#include <stdint.h>

#include <android/log.h>

#include <javahelpers.h>

#define COLUMN_HALF_HEIGHT 15

#define V_LINE_SIZE 5

#define H_LINE_SIZE 5

#define LINE_MARGIN 20

#define WHITE_THRESHOLD 0.005

#define COLUMN_WIDTH 5

#define MAX(a,b) (((a) > (b)) ? (a) : (b))
#define MIN(a,b) (((a) < (b)) ? (a) : (b))

#define LCTX "EBookDroid.PageCropper"

#define DEBUG(args...) \
    __android_log_print(ANDROID_LOG_DEBUG, LCTX, args)

#define ERROR(args...) \
    __android_log_print(ANDROID_LOG_ERROR, LCTX, args)

#define INFO(args...) \
    __android_log_print(ANDROID_LOG_INFO, LCTX, args)


int calculateAvgLum(uint8_t* src, int width, int height, int sub_x, int sub_y, int sub_w, int sub_h);
float getLeftBound(uint8_t* src, int width, int height, int avgLum);
float getLeftColumnBound(uint8_t* src, int width, int height, int avgLum, float x, float y);
float getTopBound(uint8_t* src, int width, int height, int avgLum);
float getRightBound(uint8_t* src, int width, int height, int avgLum);
float getRightColumnBound(uint8_t* src, int width, int height, int avgLum, float x, float y);
float getBottomBound(uint8_t* src, int width, int height, int avgLum);

JNIEXPORT jobject JNICALL
Java_org_ebookdroid_core_crop_PageCropper_nativeGetCropBounds(JNIEnv* env, jclass clz, jobject pixels, jint width,
                                                              jint height, jfloat left, jfloat top, jfloat right,
                                                              jfloat bottom)
{

    uint8_t* src;
    src = (uint8_t*) ((*env)->GetDirectBufferAddress(env, pixels));
    if (!src)
    {
        ERROR("Can not get direct buffer");
        return;
    }

    RectFHelper rectfh;

    if (!RectFHelper_init(&rectfh, env))
    {
        DEBUG("search(): JNI helper initialization failed");
        return NULL ;
    }

    int avgLum;
    avgLum = calculateAvgLum(src, width, height, 0, 0, width, height);

    float coords[4];
    coords[0] = getLeftBound(src, width, height, avgLum) * (right - left) + left;
    coords[1] = getTopBound(src, width, height, avgLum) * (bottom - top) + top;
    coords[2] = getRightBound(src, width, height, avgLum) * (right - left) + left;
    coords[3] = getBottomBound(src, width, height, avgLum) * (bottom - top) + top;

    jobject rect = RectFHelper_create(&rectfh);

    if (rect)
    {
        RectFHelper_setRectF(&rectfh, rect, coords);
    }
    return rect;
}

JNIEXPORT jobject JNICALL
Java_org_ebookdroid_core_crop_PageCropper_nativeGetColumn(JNIEnv* env, jclass clz, jobject pixels, jint width,
                                                          jint height, jfloat x, jfloat y)
{

    uint8_t* src;
    src = (uint8_t*) ((*env)->GetDirectBufferAddress(env, pixels));
    if (!src)
    {
        ERROR("Can not get direct buffer");
        return;
    }

    RectFHelper rectfh;

    if (!RectFHelper_init(&rectfh, env))
    {
        DEBUG("search(): JNI helper initialization failed");
        return NULL ;
    }

    int pointX = (int) (width * x);
    int pointY = (int) (height * y);
    int top = MAX(0, pointY - COLUMN_HALF_HEIGHT);
    int bottom = MIN(height - 1, pointY + COLUMN_HALF_HEIGHT);
    int left = MAX(0, pointX - COLUMN_HALF_HEIGHT);
    int right = MIN(width - 1, pointX + COLUMN_HALF_HEIGHT);

    int avgLum;
    avgLum = calculateAvgLum(src, width, height, top, bottom, right - left, bottom - top);

    float coords[4];
    coords[0] = getLeftColumnBound(src, width, height, avgLum, x, y);
    coords[1] = 0;
    coords[2] = getRightColumnBound(src, width, height, avgLum, x, y);
    coords[3] = 1;

    jobject rect = RectFHelper_create(&rectfh);

    if (rect)
    {
        RectFHelper_setRectF(&rectfh, rect, coords);
    }
    return rect;
}

int calculateAvgLum(uint8_t* src, int width, int height, int sub_x, int sub_y, int sub_w, int sub_h)
{
    int i, a;
    int midBright = 0;

    int x, y;
    for (y = 0; y < sub_h; y++)
    {
        for (x = 0; x < sub_w; x++)
        {
            i = ((y + sub_y) * width + sub_x + x) * 4;
            midBright += (MIN(src[i+2],MIN(src[i + 1],src[i])) + MAX(src[i+2],MAX(src[i + 1],src[i]))) / 2;
        }
    }
    midBright /= (sub_w * sub_h);

    return midBright;
}

float getLeftBound(uint8_t* src, int width, int height, int avgLum)
{
    int w = width / 3;
    int whiteCount = 0;
    int x = 0;

    for (x = 0; x < w; x += V_LINE_SIZE)
    {
        //DEBUG("getLeftBound: %d", x);
        int white = isRectWhite(src, width, height, x, LINE_MARGIN, V_LINE_SIZE, height - 2 * LINE_MARGIN, avgLum);
        if (white)
        {
            whiteCount++;
        }
        else
        {
            if (whiteCount >= 1)
            {
                return (float) (MAX(0, x - V_LINE_SIZE)) / width;
            }
            whiteCount = 0;
        }
    }
    return whiteCount > 0 ? (float) (MAX(0, x - V_LINE_SIZE)) / width : 0;
}

float getLeftColumnBound(uint8_t* src, int width, int height, int avgLum, float x, float y)
{
    int blackFound = 0;
    int pointX = (int) (width * x);
    int pointY = (int) (height * y);
    int top = MAX(0, pointY - COLUMN_HALF_HEIGHT);
    int bottom = MIN(height - 1, pointY + COLUMN_HALF_HEIGHT);

    int left;
    for (left = pointX; left >= 0; left -= COLUMN_WIDTH)
    {
        if (isRectWhite(src, width, height, left, top, COLUMN_WIDTH, bottom - top, avgLum))
        {
            if (blackFound)
            {
                return ((float) left / width);
            }
        }
        else
        {
            blackFound = 1;
        }
    }
    return 0;
}

float getTopBound(uint8_t* src, int width, int height, int avgLum)
{
    int h = height / 3;
    int whiteCount = 0;
    int y = 0;

    for (y = 0; y < h; y += H_LINE_SIZE)
    {
        int white = isRectWhite(src, width, height, LINE_MARGIN, y, width - 2 * LINE_MARGIN, H_LINE_SIZE, avgLum);
        if (white)
        {
            whiteCount++;
        }
        else
        {
            if (whiteCount >= 1)
            {
                return (float) (MAX(0, y - H_LINE_SIZE)) / height;
            }
            whiteCount = 0;
        }
    }
    return whiteCount > 0 ? (float) (MAX(0, y - H_LINE_SIZE)) / height : 0;
}

float getRightBound(uint8_t* src, int width, int height, int avgLum)
{
    int w = width / 3;
    int whiteCount = 0;
    int x = 0;

    for (x = width - V_LINE_SIZE; x > width - w; x -= V_LINE_SIZE)
    {
        int white = isRectWhite(src, width, height, x, LINE_MARGIN, V_LINE_SIZE, height - 2 * LINE_MARGIN, avgLum);
        if (white)
        {
            whiteCount++;
        }
        else
        {
            if (whiteCount >= 1)
            {
                return (float) (MIN(width, x + 2 * V_LINE_SIZE)) / width;
            }
            whiteCount = 0;
        }
    }
    return whiteCount > 0 ? (float) (MIN(width, x + 2 * V_LINE_SIZE)) / width : 1;
}

float getRightColumnBound(uint8_t* src, int width, int height, int avgLum, float x, float y)
{
    int blackFound = 0;
    int pointX = (int) (width * x);
    int pointY = (int) (height * y);
    int top = MAX(0, pointY - COLUMN_HALF_HEIGHT);
    int bottom = MIN(height - 1, pointY + COLUMN_HALF_HEIGHT);

    int left;
    for (left = pointX; left < width - COLUMN_WIDTH; left += COLUMN_WIDTH)
    {
        if (isRectWhite(src, width, height, left, top, COLUMN_WIDTH, bottom - top, avgLum))
        {
            if (blackFound)
            {
                return ((float) (left + COLUMN_WIDTH) / width);
            }
        }
        else
        {
            blackFound = 1;
        }
    }
    return 1;
}

float getBottomBound(uint8_t* src, int width, int height, int avgLum)
{
    int h = height / 3;
    int whiteCount = 0;
    int y = 0;
    for (y = height - H_LINE_SIZE; y > height - h; y -= H_LINE_SIZE)
    {
        int white = isRectWhite(src, width, height, LINE_MARGIN, y, width - 2 * LINE_MARGIN, H_LINE_SIZE, avgLum);
        if (white)
        {
            whiteCount++;
        }
        else
        {
            if (whiteCount >= 1)
            {
                return (float) (MIN(height, y + 2 * H_LINE_SIZE)) / height;
            }
            whiteCount = 0;
        }
    }
    return whiteCount > 0 ? (float) (MIN(height, y + 2 * H_LINE_SIZE)) / height : 1;
}

int isRectWhite(uint8_t* src, int width, int height, int sub_x, int sub_y, int sub_w, int sub_h, int avgLum)
{
    int count = 0;

    int x, y;
    for (y = 0; y < sub_h; y++)
    {
        for (x = 0; x < sub_w; x++)
        {
            int i = ((y + sub_y) * width + sub_x + x) * 4;
            int minLum = MIN(src[i+2],MIN(src[i + 1],src[i]));
            int maxLum = MAX(src[i+2],MAX(src[i + 1],src[i]));
            int lum = (minLum + maxLum) / 2;
            if ((lum < avgLum) && ((avgLum - lum) * 10 > avgLum))
            {
                count++;
            }
        }
    }
    float white = (float) count / (sub_w * sub_h);
    //DEBUG("White: %f %d", white, count);
    return white < WHITE_THRESHOLD ? 1 : 0;
}

