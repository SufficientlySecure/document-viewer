LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

ifneq ($(TARGET_ARCH_ABI),x86)
ifneq ($(TARGET_ARCH_ABI),mips)
    LOCAL_ARM_MODE := arm
endif # TARGET_ARCH_ABI != mips
endif # TARGET_ARCH_ABI != x86

LOCAL_MODULE    := hqx
LOCAL_SRC_FILES := hq4x.c hq2x.c hq3x.c

include $(BUILD_STATIC_LIBRARY)
