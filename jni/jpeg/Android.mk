LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := jpeg

ifneq ($(TARGET_ARCH_ABI),x86)
ifneq ($(TARGET_ARCH_ABI),mips)
    LOCAL_ARM_MODE := arm
endif # TARGET_ARCH_ABI != mips
endif # TARGET_ARCH_ABI != x86

LOCAL_SRC_FILES := \
	jcapimin.c \
	jcapistd.c \
	jcarith.c \
	jctrans.c \
	jcparam.c \
	jdatadst.c \
	jcinit.c \
	jcmaster.c \
	jcmarker.c \
	jcmainct.c \
	jcprepct.c \
	jccoefct.c \
	jccolor.c \
	jcsample.c \
	jchuff.c \
	jcdctmgr.c \
	jfdctfst.c \
	jfdctflt.c \
	jfdctint.c \
	jdapimin.c \
	jdapistd.c \
	jdarith.c \
	jdtrans.c \
	jdatasrc.c \
	jdmaster.c \
	jdinput.c \
	jdmarker.c \
	jdhuff.c \
	jdmainct.c \
	jdcoefct.c \
	jdpostct.c \
	jddctmgr.c \
	jidctfst.c \
	jidctflt.c \
	jidctint.c \
	jdsample.c \
	jdcolor.c \
	jquant1.c \
	jquant2.c \
	jdmerge.c \
	jaricom.c \
	jcomapi.c \
	jutils.c \
	jerror.c \
	jmemmgr.c \
	jmemnobs.c

LOCAL_C_INCLUDES := $(LOCAL_PATH)

include $(BUILD_STATIC_LIBRARY)
