cmake_minimum_required(VERSION 3.4.1)
project(superpoweredSynth)

# Set the path to the Superpowered SDK
set(SUPERPOWERED_PATH ${CMAKE_CURRENT_SOURCE_DIR}/superpowered)

# Add the synthesizer library
add_library(superpoweredSynth SHARED
        SuperpoweredSynth.cpp
        ${SUPERPOWERED_PATH}/SuperpoweredAndroidAudioIO.cpp
)

# Include Superpowered headers
include_directories(${SUPERPOWERED_PATH})

# Set compiler flags
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -O3 -fvisibility=hidden")

# Add precompiled Superpowered library
add_library(superpowered STATIC IMPORTED)
set_target_properties(superpowered PROPERTIES
        IMPORTED_LOCATION ${SUPERPOWERED_PATH}/libs/${ANDROID_ABI}/libSuperpowered.a
)

# Link against Android libraries and Superpowered
target_link_libraries(superpoweredSynth
        superpowered
        log
        android
        OpenSLES
)