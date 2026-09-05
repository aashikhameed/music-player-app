cmake_minimum_required(VERSION 3.10)
project(MusicPlayer)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Optional prebuilt FFmpeg support if supplied in libs/
if (EXISTS "${CMAKE_CURRENT_SOURCE_DIR}/libs/libffmpeg.so")
    add_library(ffmpeg_lib SHARED IMPORTED)
    set_target_properties(ffmpeg_lib PROPERTIES
        IMPORTED_LOCATION "${CMAKE_CURRENT_SOURCE_DIR}/libs/libffmpeg.so"
    )
    add_definitions(-DHAVE_FFMPEG)
    set(FFMPEG_TARGET ffmpeg_lib)
    include_directories(third_party/ffmpeg/include)
else()
    set(FFMPEG_TARGET "")
endif()

# Core Native Audio Engine
add_library(audio-player SHARED
    audio/AudioPlayer.cpp
    audio/AudioDecoder.cpp
    audio/OpenSLESPlayer.cpp
    audio/PlaylistManager.cpp
    jni/NativeAudio.cpp
)

target_include_directories(audio-player PRIVATE
    audio/
    jni/
)

target_link_libraries(audio-player
    ${FFMPEG_TARGET}
    OpenSLES
    mediandk
    log
    android
)

# ─── ARM aarch64 Compiler Optimizations ──────────────────────────────────────
# Target: Blaupunkt Santa Rosa 985 — ARM Cortex-A55 aarch64 @ 2.5 GHz
# -O3               Full optimization; eliminates bounds checks, inlines loops
# -flto             Link-Time Optimization across all C++ translation units
# -fvisibility=hidden Hide non-JNI symbols to allow devirtualization and smaller DSO
# -march=armv8-a    Enable ARMv8-A ISA (NEON SIMD, crypto extensions)
# -ftree-vectorize  Auto-vectorise the PCM decode loop using NEON registers
# -ffast-math       Relax IEEE-754 strictness for the seekbar sine calculation
# -fomit-frame-pointer  Free one general-purpose register for tighter loops
# -ffunction-sections / -fdata-sections + --gc-sections  Dead-strip unused code
# ─────────────────────────────────────────────────────────────────────────────
target_compile_options(audio-player PRIVATE
    -O3
    -flto
    -fvisibility=hidden
    -march=armv8-a
    -ftree-vectorize
    -ffast-math
    -fomit-frame-pointer
    -ffunction-sections
    -fdata-sections
    -DNDEBUG
)

target_link_options(audio-player PRIVATE
    -flto
    -Wl,--gc-sections
    -Wl,--strip-all
)
