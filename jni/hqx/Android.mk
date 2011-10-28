LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_ARM_MODE := arm

LOCAL_MODULE    := hqx
LOCAL_SRC_FILES := hq4x.c hq2x.c hq3x.c

include $(BUILD_STATIC_LIBRARY)
