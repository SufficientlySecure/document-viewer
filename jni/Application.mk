#APP_ABI := armeabi armeabi-v7a x86
#APP_ABI := x86

APP_CFLAGS := -DHAVE_CONFIG_H -DTHREADMODEL=NOTHREADS -DDEBUGLVL=0 -Os

APP_MODULES := jpeg libdjvu mupdf ebookdroid hqx