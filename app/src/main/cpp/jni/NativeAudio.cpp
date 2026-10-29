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

    listener.onThreadStarted = []() {
        if (jni::g_jvm) {
            JNIEnv* env = nullptr;
            jni::g_jvm->AttachCurrentThread(&env, nullptr);
        }
    };

    listener.onThreadExiting = []() {
        if (jni::g_jvm) {
            jni::g_jvm->DetachCurrentThread();
        }
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
    JNIEnv* env, jobject thiz, jstring filepath, jlong position_ms, jboolean is_auto_crossfade) {
    std::string path = jni::jstringToString(env, filepath);
    auto res = audio::AudioPlayer::getInstance().playTrack(path, position_ms, is_auto_crossfade == JNI_TRUE);
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

JNIEXPORT void JNICALL
Java_com_aashik_music_nativeaudio_NativeAudioBridge_setCrossfadeDuration(
    JNIEnv* env, jobject thiz, jfloat seconds) {
    audio::AudioPlayer::getInstance().setCrossfadeDuration(seconds);
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

JNIEXPORT void JNICALL
Java_com_aashik_music_nativeaudio_NativeAudioBridge_setEqualizerEnabled(
    JNIEnv* env, jobject thiz, jboolean enabled) {
    audio::AudioPlayer::getInstance().getEqualizer().setEnabled(enabled);
    audio::AudioPlayer::getInstance().syncEqualizer();
}

JNIEXPORT jboolean JNICALL
Java_com_aashik_music_nativeaudio_NativeAudioBridge_isEqualizerEnabled(
    JNIEnv* env, jobject thiz) {
    return static_cast<jboolean>(audio::AudioPlayer::getInstance().getEqualizer().isEnabled());
}

JNIEXPORT void JNICALL
Java_com_aashik_music_nativeaudio_NativeAudioBridge_setEqualizerPreset(
    JNIEnv* env, jobject thiz, jint preset) {
    audio::AudioPlayer::getInstance().getEqualizer().setPreset(preset);
    audio::AudioPlayer::getInstance().syncEqualizer();
}

JNIEXPORT jint JNICALL
Java_com_aashik_music_nativeaudio_NativeAudioBridge_getEqualizerPreset(
    JNIEnv* env, jobject thiz) {
    return static_cast<jint>(audio::AudioPlayer::getInstance().getEqualizer().getPreset());
}

JNIEXPORT void JNICALL
Java_com_aashik_music_nativeaudio_NativeAudioBridge_setEqualizerBandGain(
    JNIEnv* env, jobject thiz, jint band, jfloat gainDb) {
    audio::AudioPlayer::getInstance().getEqualizer().setBandGain(static_cast<size_t>(band), gainDb);
    audio::AudioPlayer::getInstance().syncEqualizer();
}

JNIEXPORT jfloat JNICALL
Java_com_aashik_music_nativeaudio_NativeAudioBridge_getEqualizerBandGain(
    JNIEnv* env, jobject thiz, jint band) {
    return static_cast<jfloat>(audio::AudioPlayer::getInstance().getEqualizer().getBandGain(static_cast<size_t>(band)));
}

JNIEXPORT void JNICALL
Java_com_aashik_music_nativeaudio_NativeAudioBridge_setEqualizerHpfMode(
    JNIEnv* env, jobject thiz, jint mode) {
    audio::AudioPlayer::getInstance().getEqualizer().setHpfMode(mode);
    audio::AudioPlayer::getInstance().syncEqualizer();
}

JNIEXPORT jint JNICALL
Java_com_aashik_music_nativeaudio_NativeAudioBridge_getEqualizerHpfMode(
    JNIEnv* env, jobject thiz) {
    return static_cast<jint>(audio::AudioPlayer::getInstance().getEqualizer().getHpfMode());
}

JNIEXPORT void JNICALL
Java_com_aashik_music_nativeaudio_NativeAudioBridge_setTimeAlignment(
    JNIEnv* env, jobject thiz, jint mode) {
    audio::AudioPlayer::getInstance().getEqualizer().setTimeAlignmentMode(mode);
    audio::AudioPlayer::getInstance().syncEqualizer();
}

JNIEXPORT jint JNICALL
Java_com_aashik_music_nativeaudio_NativeAudioBridge_getTimeAlignment(
    JNIEnv* env, jobject thiz) {
    return static_cast<jint>(audio::AudioPlayer::getInstance().getEqualizer().getTimeAlignmentMode());
}

JNIEXPORT void JNICALL
Java_com_aashik_music_nativeaudio_NativeAudioBridge_setBassNormalizer(
    JNIEnv* env, jobject thiz, jint mode) {
    audio::AudioPlayer::getInstance().getEqualizer().setBassNormalizerMode(mode);
    audio::AudioPlayer::getInstance().syncEqualizer();
}

JNIEXPORT jint JNICALL
Java_com_aashik_music_nativeaudio_NativeAudioBridge_getBassNormalizer(
    JNIEnv* env, jobject thiz) {
    return static_cast<jint>(audio::AudioPlayer::getInstance().getEqualizer().getBassNormalizerMode());
}

JNIEXPORT void JNICALL
Java_com_aashik_music_nativeaudio_NativeAudioBridge_setVirtualSurroundMode(
    JNIEnv* env, jobject thiz, jint mode) {
    audio::AudioPlayer::getInstance().getEqualizer().setVirtualSurroundMode(mode);
    audio::AudioPlayer::getInstance().syncEqualizer();
}

JNIEXPORT jint JNICALL
Java_com_aashik_music_nativeaudio_NativeAudioBridge_getVirtualSurroundMode(
    JNIEnv* env, jobject thiz) {
    return static_cast<jint>(audio::AudioPlayer::getInstance().getEqualizer().getVirtualSurroundMode());
}

JNIEXPORT void JNICALL
Java_com_aashik_music_nativeaudio_NativeAudioBridge_setSpeedCompensation(
    JNIEnv* env, jobject thiz, jfloat speedKmh, jint mode) {
    audio::AudioPlayer::getInstance().getEqualizer().setSpeedCompensation(speedKmh, mode);
    audio::AudioPlayer::getInstance().syncEqualizer();
}

JNIEXPORT jfloat JNICALL
Java_com_aashik_music_nativeaudio_NativeAudioBridge_getSpeedCompensationDb(
    JNIEnv* env, jobject thiz) {
    return static_cast<jfloat>(audio::AudioPlayer::getInstance().getEqualizer().getSpeedCompensationDb());
}

JNIEXPORT void JNICALL
Java_com_aashik_music_nativeaudio_NativeAudioBridge_setBalance(
    JNIEnv* env, jobject thiz, jfloat balance) {
    audio::AudioPlayer::getInstance().getEqualizer().setBalance(balance);
    audio::AudioPlayer::getInstance().syncEqualizer();
}

JNIEXPORT jfloat JNICALL
Java_com_aashik_music_nativeaudio_NativeAudioBridge_getBalance(
    JNIEnv* env, jobject thiz) {
    return static_cast<jfloat>(audio::AudioPlayer::getInstance().getEqualizer().getBalance());
}

JNIEXPORT void JNICALL
Java_com_aashik_music_nativeaudio_NativeAudioBridge_setFader(
    JNIEnv* env, jobject thiz, jfloat fader) {
    audio::AudioPlayer::getInstance().getEqualizer().setFader(fader);
    audio::AudioPlayer::getInstance().syncEqualizer();
}

JNIEXPORT jfloat JNICALL
Java_com_aashik_music_nativeaudio_NativeAudioBridge_getFader(
    JNIEnv* env, jobject thiz) {
    return static_cast<jfloat>(audio::AudioPlayer::getInstance().getEqualizer().getFader());
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
    return Java_com_aashik_music_nativeaudio_NativeAudioBridge_playTrack(env, thiz, filepath, position_ms, JNI_FALSE);
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

JNIEXPORT void JNICALL
Java_com_example_musicplayer_NativeAudio_setEqualizerEnabled(
    JNIEnv* env, jobject thiz, jboolean enabled) {
    Java_com_aashik_music_nativeaudio_NativeAudioBridge_setEqualizerEnabled(env, thiz, enabled);
}

JNIEXPORT jboolean JNICALL
Java_com_example_musicplayer_NativeAudio_isEqualizerEnabled(
    JNIEnv* env, jobject thiz) {
    return Java_com_aashik_music_nativeaudio_NativeAudioBridge_isEqualizerEnabled(env, thiz);
}

JNIEXPORT void JNICALL
Java_com_example_musicplayer_NativeAudio_setEqualizerPreset(
    JNIEnv* env, jobject thiz, jint preset) {
    Java_com_aashik_music_nativeaudio_NativeAudioBridge_setEqualizerPreset(env, thiz, preset);
}

JNIEXPORT jint JNICALL
Java_com_example_musicplayer_NativeAudio_getEqualizerPreset(
    JNIEnv* env, jobject thiz) {
    return Java_com_aashik_music_nativeaudio_NativeAudioBridge_getEqualizerPreset(env, thiz);
}

JNIEXPORT void JNICALL
Java_com_example_musicplayer_NativeAudio_setEqualizerBandGain(
    JNIEnv* env, jobject thiz, jint band, jfloat gainDb) {
    Java_com_aashik_music_nativeaudio_NativeAudioBridge_setEqualizerBandGain(env, thiz, band, gainDb);
}

JNIEXPORT jfloat JNICALL
Java_com_example_musicplayer_NativeAudio_getEqualizerBandGain(
    JNIEnv* env, jobject thiz, jint band) {
    return Java_com_aashik_music_nativeaudio_NativeAudioBridge_getEqualizerBandGain(env, thiz, band);
}

JNIEXPORT void JNICALL
Java_com_example_musicplayer_NativeAudio_setEqualizerHpfMode(
    JNIEnv* env, jobject thiz, jint mode) {
    Java_com_aashik_music_nativeaudio_NativeAudioBridge_setEqualizerHpfMode(env, thiz, mode);
}

JNIEXPORT jint JNICALL
Java_com_example_musicplayer_NativeAudio_getEqualizerHpfMode(
    JNIEnv* env, jobject thiz) {
    return Java_com_aashik_music_nativeaudio_NativeAudioBridge_getEqualizerHpfMode(env, thiz);
}

JNIEXPORT void JNICALL
Java_com_example_musicplayer_NativeAudio_setTimeAlignment(
    JNIEnv* env, jobject thiz, jint mode) {
    Java_com_aashik_music_nativeaudio_NativeAudioBridge_setTimeAlignment(env, thiz, mode);
}

JNIEXPORT jint JNICALL
Java_com_example_musicplayer_NativeAudio_getTimeAlignment(
    JNIEnv* env, jobject thiz) {
    return Java_com_aashik_music_nativeaudio_NativeAudioBridge_getTimeAlignment(env, thiz);
}

JNIEXPORT void JNICALL
Java_com_example_musicplayer_NativeAudio_setBassNormalizer(
    JNIEnv* env, jobject thiz, jint mode) {
    Java_com_aashik_music_nativeaudio_NativeAudioBridge_setBassNormalizer(env, thiz, mode);
}

JNIEXPORT jint JNICALL
Java_com_example_musicplayer_NativeAudio_getBassNormalizer(
    JNIEnv* env, jobject thiz) {
    return Java_com_aashik_music_nativeaudio_NativeAudioBridge_getBassNormalizer(env, thiz);
}

} // extern "C"
