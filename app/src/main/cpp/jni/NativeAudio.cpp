#include "JNIUtils.h"
#include "../audio/AudioPlayer.h"
#include <android/log.h>
#include <mutex>

#define LOG_TAG "NativeAudioJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace jni {
JavaVM* g_jvm = nullptr;
jobject g_bridgeObj = nullptr;
static std::mutex g_bridgeMutex;
}

static jmethodID g_midOnStateChanged = nullptr;
static jmethodID g_midOnPositionChanged = nullptr;
static jmethodID g_midOnDurationUpdated = nullptr;
static jmethodID g_midOnTrackEnded = nullptr;
static jmethodID g_midOnError = nullptr;

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
    jni::g_jvm = vm;
    LOGI("JNI_OnLoad: Native audio engine library loaded");
    return JNI_VERSION_1_6;
}

static void registerCallbacks(JNIEnv* env, jobject bridgeObj) {
    std::lock_guard<std::mutex> lock(jni::g_bridgeMutex);
    if (jni::g_bridgeObj) {
        env->DeleteGlobalRef(jni::g_bridgeObj);
        jni::g_bridgeObj = nullptr;
    }
    jni::g_bridgeObj = env->NewGlobalRef(bridgeObj);

    jclass cls = env->GetObjectClass(bridgeObj);
    g_midOnStateChanged = env->GetMethodID(cls, "onPlaybackStateChanged", "(I)V");
    g_midOnPositionChanged = env->GetMethodID(cls, "onPositionChanged", "(J)V");
    g_midOnDurationUpdated = env->GetMethodID(cls, "onDurationUpdated", "(J)V");
    g_midOnTrackEnded = env->GetMethodID(cls, "onTrackEnded", "()V");
    g_midOnError = env->GetMethodID(cls, "onError", "(ILjava/lang/String;)V");

    audio::AudioPlayerListener listener;

    listener.onPlaybackStateChanged = [](audio::PlaybackState state) {
        bool attached = false;
        JNIEnv* jenv = jni::getEnv(&attached);
        if (jenv && jni::g_bridgeObj && g_midOnStateChanged) {
            jenv->CallVoidMethod(jni::g_bridgeObj, g_midOnStateChanged, static_cast<jint>(state));
        }
        if (attached) jni::detachEnv();
    };

    listener.onPositionChanged = [](int64_t positionMs) {
        bool attached = false;
        JNIEnv* jenv = jni::getEnv(&attached);
        if (jenv && jni::g_bridgeObj && g_midOnPositionChanged) {
            jenv->CallVoidMethod(jni::g_bridgeObj, g_midOnPositionChanged, static_cast<jlong>(positionMs));
        }
        if (attached) jni::detachEnv();
    };

    listener.onDurationUpdated = [](int64_t durationMs) {
        bool attached = false;
        JNIEnv* jenv = jni::getEnv(&attached);
        if (jenv && jni::g_bridgeObj && g_midOnDurationUpdated) {
            jenv->CallVoidMethod(jni::g_bridgeObj, g_midOnDurationUpdated, static_cast<jlong>(durationMs));
        }
        if (attached) jni::detachEnv();
    };

    listener.onTrackEnded = []() {
        bool attached = false;
        JNIEnv* jenv = jni::getEnv(&attached);
        if (jenv && jni::g_bridgeObj && g_midOnTrackEnded) {
            jenv->CallVoidMethod(jni::g_bridgeObj, g_midOnTrackEnded);
        }
        if (attached) jni::detachEnv();
    };

    listener.onError = [](int32_t errorCode, const std::string& message) {
        bool attached = false;
        JNIEnv* jenv = jni::getEnv(&attached);
        if (jenv && jni::g_bridgeObj && g_midOnError) {
            jstring jmsg = jni::stringToJstring(jenv, message);
            jenv->CallVoidMethod(jni::g_bridgeObj, g_midOnError, errorCode, jmsg);
            if (jmsg) jenv->DeleteLocalRef(jmsg);
        }
        if (attached) jni::detachEnv();
    };

    audio::AudioPlayer::getInstance().setListener(listener);
}

// ─────────────────────────────────────────────────────────────────────────────
// JNI Exports for com.aashik.music.nativeaudio.NativeAudioBridge
// ─────────────────────────────────────────────────────────────────────────────

extern "C" {

JNIEXPORT jint JNICALL
Java_com_aashik_music_nativeaudio_NativeAudioBridge_init(JNIEnv* env, jobject thiz) {
    registerCallbacks(env, thiz);
    return static_cast<jint>(audio::AudioPlayer::getInstance().init());
}

JNIEXPORT void JNICALL
Java_com_aashik_music_nativeaudio_NativeAudioBridge_shutdown(JNIEnv* env, jobject thiz) {
    audio::AudioPlayer::getInstance().shutdown();
    std::lock_guard<std::mutex> lock(jni::g_bridgeMutex);
    if (jni::g_bridgeObj) {
        env->DeleteGlobalRef(jni::g_bridgeObj);
        jni::g_bridgeObj = nullptr;
    }
}

JNIEXPORT jint JNICALL
Java_com_aashik_music_nativeaudio_NativeAudioBridge_playTrack(
    JNIEnv* env, jobject thiz, jstring filepath, jlong position_ms) {
    std::string path = jni::jstringToString(env, filepath);
    auto res = audio::AudioPlayer::getInstance().playTrack(path, position_ms);
    return static_cast<jint>(res);
}

JNIEXPORT void JNICALL
Java_com_aashik_music_nativeaudio_NativeAudioBridge_pause(JNIEnv* env, jobject thiz) {
    audio::AudioPlayer::getInstance().pause();
}

JNIEXPORT void JNICALL
Java_com_aashik_music_nativeaudio_NativeAudioBridge_resume(JNIEnv* env, jobject thiz) {
    audio::AudioPlayer::getInstance().resume();
}

JNIEXPORT void JNICALL
Java_com_aashik_music_nativeaudio_NativeAudioBridge_stop(JNIEnv* env, jobject thiz) {
    audio::AudioPlayer::getInstance().stop();
}

JNIEXPORT jint JNICALL
Java_com_aashik_music_nativeaudio_NativeAudioBridge_seek(
    JNIEnv* env, jobject thiz, jlong position_ms) {
    auto res = audio::AudioPlayer::getInstance().seek(position_ms);
    return static_cast<jint>(res);
}

JNIEXPORT jlong JNICALL
Java_com_aashik_music_nativeaudio_NativeAudioBridge_getCurrentPosition(JNIEnv* env, jobject thiz) {
    return static_cast<jlong>(audio::AudioPlayer::getInstance().getCurrentPosition());
}

JNIEXPORT jlong JNICALL
Java_com_aashik_music_nativeaudio_NativeAudioBridge_getDuration(JNIEnv* env, jobject thiz) {
    return static_cast<jlong>(audio::AudioPlayer::getInstance().getDuration());
}

JNIEXPORT void JNICALL
Java_com_aashik_music_nativeaudio_NativeAudioBridge_setVolume(
    JNIEnv* env, jobject thiz, jfloat volume) {
    audio::AudioPlayer::getInstance().setVolume(volume);
}

JNIEXPORT jint JNICALL
Java_com_aashik_music_nativeaudio_NativeAudioBridge_getPlaybackState(JNIEnv* env, jobject thiz) {
    return static_cast<jint>(audio::AudioPlayer::getInstance().getState());
}

JNIEXPORT void JNICALL
Java_com_aashik_music_nativeaudio_NativeAudioBridge_setShuffleMode(
    JNIEnv* env, jobject thiz, jboolean enabled) {
    audio::AudioPlayer::getInstance().getPlaylistManager().setShuffleMode(enabled);
}

JNIEXPORT void JNICALL
Java_com_aashik_music_nativeaudio_NativeAudioBridge_setRepeatMode(
    JNIEnv* env, jobject thiz, jint mode) {
    audio::AudioPlayer::getInstance().getPlaylistManager().setRepeatMode(
        static_cast<audio::RepeatMode>(mode));
}

// ─────────────────────────────────────────────────────────────────────────────
// Compatibility Aliases for prompt specification com.example.musicplayer.NativeAudio
// ─────────────────────────────────────────────────────────────────────────────

JNIEXPORT jint JNICALL
Java_com_example_musicplayer_NativeAudio_init(JNIEnv* env, jobject thiz) {
    return Java_com_aashik_music_nativeaudio_NativeAudioBridge_init(env, thiz);
}

JNIEXPORT void JNICALL
Java_com_example_musicplayer_NativeAudio_shutdown(JNIEnv* env, jobject thiz) {
    Java_com_aashik_music_nativeaudio_NativeAudioBridge_shutdown(env, thiz);
}

JNIEXPORT jint JNICALL
Java_com_example_musicplayer_NativeAudio_playTrack(
    JNIEnv* env, jobject thiz, jstring filepath, jlong position_ms) {
    return Java_com_aashik_music_nativeaudio_NativeAudioBridge_playTrack(env, thiz, filepath, position_ms);
}

JNIEXPORT void JNICALL
Java_com_example_musicplayer_NativeAudio_pause(JNIEnv* env, jobject thiz) {
    Java_com_aashik_music_nativeaudio_NativeAudioBridge_pause(env, thiz);
}

JNIEXPORT void JNICALL
Java_com_example_musicplayer_NativeAudio_resume(JNIEnv* env, jobject thiz) {
    Java_com_aashik_music_nativeaudio_NativeAudioBridge_resume(env, thiz);
}

JNIEXPORT void JNICALL
Java_com_example_musicplayer_NativeAudio_stop(JNIEnv* env, jobject thiz) {
    Java_com_aashik_music_nativeaudio_NativeAudioBridge_stop(env, thiz);
}

JNIEXPORT jint JNICALL
Java_com_example_musicplayer_NativeAudio_seek(
    JNIEnv* env, jobject thiz, jlong position_ms) {
    return Java_com_aashik_music_nativeaudio_NativeAudioBridge_seek(env, thiz, position_ms);
}

JNIEXPORT jlong JNICALL
Java_com_example_musicplayer_NativeAudio_getCurrentPosition(JNIEnv* env, jobject thiz) {
    return Java_com_aashik_music_nativeaudio_NativeAudioBridge_getCurrentPosition(env, thiz);
}

JNIEXPORT jlong JNICALL
Java_com_example_musicplayer_NativeAudio_getDuration(JNIEnv* env, jobject thiz) {
    return Java_com_aashik_music_nativeaudio_NativeAudioBridge_getDuration(env, thiz);
}

JNIEXPORT void JNICALL
Java_com_example_musicplayer_NativeAudio_setVolume(
    JNIEnv* env, jobject thiz, jfloat volume) {
    Java_com_aashik_music_nativeaudio_NativeAudioBridge_setVolume(env, thiz, volume);
}

JNIEXPORT jint JNICALL
Java_com_example_musicplayer_NativeAudio_getPlaybackState(JNIEnv* env, jobject thiz) {
    return Java_com_aashik_music_nativeaudio_NativeAudioBridge_getPlaybackState(env, thiz);
}

JNIEXPORT void JNICALL
Java_com_example_musicplayer_NativeAudio_setShuffleMode(
    JNIEnv* env, jobject thiz, jboolean enabled) {
    Java_com_aashik_music_nativeaudio_NativeAudioBridge_setShuffleMode(env, thiz, enabled);
}

JNIEXPORT void JNICALL
Java_com_example_musicplayer_NativeAudio_setRepeatMode(
    JNIEnv* env, jobject thiz, jint mode) {
    Java_com_aashik_music_nativeaudio_NativeAudioBridge_setRepeatMode(env, thiz, mode);
}

} // extern "C"
