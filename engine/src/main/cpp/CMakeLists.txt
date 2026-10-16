# KARTUN engine - native upscaler over the prebuilt NCNN android-vulkan package (tools/fetch-ncnn.mjs).
cmake_minimum_required(VERSION 3.22)
project(kartun_engine)

set(ncnn_DIR ${CMAKE_SOURCE_DIR}/../../../third_party/ncnn-20260526-android-vulkan/${ANDROID_ABI}/lib/cmake/ncnn)
# A local ncnn build (for checking an ncnn fix on the device, researches/08 section 11) replaces the release when the
# environment names its cmake package dir; builds without the variable use the pinned release above.
if(DEFINED ENV{KARTUN_NCNN_DIR})
    set(ncnn_DIR $ENV{KARTUN_NCNN_DIR})
    message(STATUS "ncnn from KARTUN_NCNN_DIR: ${ncnn_DIR}")
endif()
find_package(ncnn REQUIRED)

# libtiff 4.7.2 (tools/fetch-libtiff.mjs, pinned by SHA-256) for TIFF in the second decoder: static, no tools/tests/docs,
# codecs that need no extra library (LZW, PackBits, CCITT, ...) plus Deflate over the NDK's zlib; JPEG-in-TIFF is off.
set(BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)
foreach(opt tiff-tools tiff-tests tiff-contrib tiff-docs tiff-install cxx jpeg old-jpeg jbig lerc lzma zstd webp libdeflate)
    set(${opt} OFF CACHE BOOL "" FORCE)
endforeach()
add_subdirectory(${CMAKE_SOURCE_DIR}/../../../third_party/tiff-4.7.2 ${CMAKE_BINARY_DIR}/libtiff EXCLUDE_FROM_ALL)

# image_decode.cpp: the second decoder for the formats ImageDecoder refuses (stb_image + qoi.h in third_party/, libtiff;
# plans/08).
add_library(kartun_engine SHARED kartun_engine.cpp image_decode.cpp)
target_link_libraries(kartun_engine ncnn tiff android jnigraphics log)
# 16 KB memory pages - required by Google Play for native code (research 03, section D).
target_link_options(kartun_engine PRIVATE "-Wl,-z,max-page-size=16384")
