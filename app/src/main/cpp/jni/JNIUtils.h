#ifndef MUSIC_PLAYER_JNI_UTILS_H
#define MUSIC_PLAYER_JNI_UTILS_H

#include <jni.h>
#include <string>

namespace jni {

extern JavaVM* g_jvm;
extern jobject g_bridgeObj;

inline JNIEnv* getEnv(bool* outAttached = nullptr) {
    if (!g_jvm) return nullptr;

    JNIEnv* env = nullptr;
    jint result = g_jvm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6);
    if (result == JNI_EDETACHED) {
        if (g_jvm->AttachCurrentThread(&env, nullptr) == JNI_OK) {
            if (outAttached) *outAttached = true;
            return env;
        }
        return nullptr;
    } else if (result == JNI_OK) {
        if (outAttached) *outAttached = false;
        return env;
    }
    return nullptr;
}

inline void detachEnv() {
    if (g_jvm) {
        g_jvm->DetachCurrentThread();
    }
}

inline std::string jstringToString(JNIEnv* env, jstring jstr) {
    if (!env || !jstr) return "";
    const char* chars = env->GetStringUTFChars(jstr, nullptr);
    if (!chars) return "";
    std::string result(chars);
    env->ReleaseStringUTFChars(jstr, chars);
    return result;
}

inline jstring stringToJstring(JNIEnv* env, const std::string& str) {
    if (!env) return nullptr;
    return env->NewStringUTF(str.c_str());
}

} // namespace jni

#endif // MUSIC_PLAYER_JNI_UTILS_H
