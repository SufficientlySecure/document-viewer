LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

ifeq ($(TARGET_ARCH_ABI),armeabi)
    LOCAL_ARM_MODE := arm
endif # TARGET_ARCH_ABI == armeabi

LOCAL_MODULE    := hqx
LOCAL_SRC_FILES := hq4x.c hq2x.c hq3x.c

include $(BUILD_STATIC_LIBRARY)
