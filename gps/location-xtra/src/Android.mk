LOCAL_PATH:=$(call my-dir)
include $(CLEAR_VARS)

LOC_API_APP_PATH:=$(LOCAL_PATH)

LOCAL_SRC_FILES:= \
    xtra_config.c \
    xtra_servers.c \
    xtra_sntp_linux.c \
    xtra_http_linux.c \
    xtra_system_interface.c

LOCAL_CFLAGS:= \
    -DDEBUG

LOCAL_C_INCLUDES:= \
    $(LOCAL_PATH)/../privinc \
    $(LOCAL_PATH)/../pubinc
LOCAL_HEADER_LIBRARIES := \
    libgps.utils_headers \
    libloc_pla_headers

LOCAL_SHARED_LIBRARIES := \
    libutils \
    libcutils \
    libgps.utils \
    libloc_pla

LOCAL_PRELINK_MODULE:=false

LOCAL_CFLAGS+=$(GPS_FEATURES)

LOCAL_PROPRIETARY_MODULE := true
LOCAL_MODULE_OWNER := qti
LOCAL_MODULE:=libloc_xtra
LOCAL_MODULE_TAGS := optional
include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libloc_xtra_headers
LOCAL_EXPORT_C_INCLUDE_DIRS :=  $(LOCAL_PATH)/../privinc \
                                $(LOCAL_PATH)/../pubinc
include $(BUILD_HEADER_LIBRARY)