# KARTUN engine - native upscaler over the prebuilt NCNN android-vulkan package (tools/fetch-ncnn.mjs).
cmake_minimum_required(VERSION 3.22)
project(kartun_engine)

set(ncnn_DIR ${CMAKE_SOURCE_DIR}/../../../third_party/ncnn-20260526-android-vulkan/${ANDROID_ABI}/lib/cmake/ncnn)
find_package(ncnn REQUIRED)

add_library(kartun_engine SHARED kartun_engine.cpp)
target_link_libraries(kartun_engine ncnn android jnigraphics log)
# 16 KB memory pages - required by Google Play for native code (research 03, section D).
target_link_options(kartun_engine PRIVATE "-Wl,-z,max-page-size=16384")
