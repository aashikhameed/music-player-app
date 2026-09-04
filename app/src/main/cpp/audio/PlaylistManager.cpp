#include "PlaylistManager.h"
#include <media/NdkMediaExtractor.h>
#include <media/NdkMediaFormat.h>
#include <android/log.h>
#include <dirent.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h>
#include <algorithm>
#include <random>

#define LOG_TAG "PlaylistManager"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)

namespace audio {

PlaylistManager::PlaylistManager() = default;

bool PlaylistManager::isAudioFile(const std::string& filePath) {
    size_t dotPos = filePath.find_last_of('.');
    if (dotPos == std::string::npos) return false;

    std::string ext = filePath.substr(dotPos);
    std::transform(ext.begin(), ext.end(), ext.begin(), ::tolower);

    return (ext == ".mp3" || ext == ".m4a" || ext == ".aac" ||
            ext == ".flac" || ext == ".wav" || ext == ".ogg");
}

static void collectAudioFilesRecursive(const std::string& dirPath, std::vector<std::string>& outFiles) {
    DIR* dir = opendir(dirPath.c_str());
    if (!dir) return;

    struct dirent* entry;
    while ((entry = readdir(dir)) != nullptr) {
        if (strcmp(entry->d_name, ".") == 0 || strcmp(entry->d_name, "..") == 0) {
            continue;
        }

        std::string fullPath = dirPath;
        if (fullPath.back() != '/') fullPath += '/';
        fullPath += entry->d_name;

        struct stat st{};
        if (stat(fullPath.c_str(), &st) == 0) {
            if (S_ISDIR(st.st_mode)) {
                collectAudioFilesRecursive(fullPath, outFiles);
            } else if (S_ISREG(st.st_mode)) {
                size_t dotPos = fullPath.find_last_of('.');
                if (dotPos != std::string::npos) {
                    std::string ext = fullPath.substr(dotPos);
                    std::transform(ext.begin(), ext.end(), ext.begin(), ::tolower);
                    if (ext == ".mp3" || ext == ".m4a" || ext == ".aac" ||
                        ext == ".flac" || ext == ".wav" || ext == ".ogg") {
                        outFiles.push_back(fullPath);
                    }
                }
            }
        }
    }
    closedir(dir);
}

TrackMetadata PlaylistManager::extractMetadata(const std::string& filePath) {
    TrackMetadata meta;
    meta.path = filePath;

    // Default title from filename
    size_t lastSlash = filePath.find_last_of('/');
    std::string filename = (lastSlash == std::string::npos) ? filePath : filePath.substr(lastSlash + 1);
    size_t dotPos = filename.find_last_of('.');
    meta.title = (dotPos == std::string::npos) ? filename : filename.substr(0, dotPos);
    meta.artist = "Unknown Artist";
    meta.album = "Unknown Album";

    AMediaExtractor* extractor = AMediaExtractor_new();
    if (!extractor) {
        return meta;
    }

    media_status_t err = AMEDIA_ERROR_UNKNOWN;
    int fd = ::open(filePath.c_str(), O_RDONLY);
    if (fd >= 0) {
        struct stat st{};
        if (::fstat(fd, &st) == 0 && st.st_size > 0) {
            err = AMediaExtractor_setDataSourceFd(extractor, fd, 0, st.st_size);
        }
        ::close(fd);
    }
    if (err != AMEDIA_OK) {
        err = AMediaExtractor_setDataSource(extractor, filePath.c_str());
    }

    if (err == AMEDIA_OK) {
        size_t numTracks = AMediaExtractor_getTrackCount(extractor);
        for (size_t i = 0; i < numTracks; ++i) {
            AMediaFormat* format = AMediaExtractor_getTrackFormat(extractor, i);
            if (!format) continue;

            const char* mime = nullptr;
            if (AMediaFormat_getString(format, AMEDIAFORMAT_KEY_MIME, &mime) && mime) {
                if (strncmp(mime, "audio/", 6) == 0) {
                    meta.mimeType = mime;

                    int64_t durationUs = 0;
                    AMediaFormat_getInt64(format, AMEDIAFORMAT_KEY_DURATION, &durationUs);
                    meta.durationMs = durationUs > 0 ? (durationUs / 1000) : 0;

                    int32_t sampleRate = 0, channels = 0, bitrate = 0;
                    AMediaFormat_getInt32(format, AMEDIAFORMAT_KEY_SAMPLE_RATE, &sampleRate);
                    AMediaFormat_getInt32(format, AMEDIAFORMAT_KEY_CHANNEL_COUNT, &channels);
                    AMediaFormat_getInt32(format, AMEDIAFORMAT_KEY_BIT_RATE, &bitrate);
                    meta.sampleRate = sampleRate;
                    meta.channels = channels;
                    meta.bitrate = bitrate;

                    const char* tagStr = nullptr;
                    if (AMediaFormat_getString(format, "title", &tagStr) && tagStr && strlen(tagStr) > 0) {
                        meta.title = tagStr;
                    }
                    if (AMediaFormat_getString(format, "artist", &tagStr) && tagStr && strlen(tagStr) > 0) {
                        meta.artist = tagStr;
                    }
                    if (AMediaFormat_getString(format, "album", &tagStr) && tagStr && strlen(tagStr) > 0) {
                        meta.album = tagStr;
                    }

                    AMediaFormat_delete(format);
                    break;
                }
            }
            AMediaFormat_delete(format);
        }
    }

    AMediaExtractor_delete(extractor);
    return meta;
}

std::vector<TrackMetadata> PlaylistManager::scanDirectory(
    const std::string& directoryPath,
    const std::function<void(int loaded, int total)>& progressCallback
) {
    std::vector<std::string> filePaths;
    collectAudioFilesRecursive(directoryPath, filePaths);

    std::vector<TrackMetadata> results;
    results.reserve(filePaths.size());

    int total = static_cast<int>(filePaths.size());
    int loaded = 0;

    for (const auto& path : filePaths) {
        results.push_back(extractMetadata(path));
        loaded++;
        if (progressCallback && (loaded % 10 == 0 || loaded == total)) {
            progressCallback(loaded, total);
        }
    }

    return results;
}

void PlaylistManager::setPlaylist(const std::vector<TrackMetadata>& tracks, int startIndex) {
    std::lock_guard<std::mutex> lock(mMutex);
    mTracks = tracks;
    rebuildPlaybackOrder();

    mCurrentOrderIndex = 0;
    if (startIndex >= 0 && startIndex < static_cast<int>(mTracks.size())) {
        for (size_t i = 0; i < mPlaybackOrder.size(); ++i) {
            if (mPlaybackOrder[i] == static_cast<size_t>(startIndex)) {
                mCurrentOrderIndex = i;
                break;
            }
        }
    }
}

void PlaylistManager::addTrack(const TrackMetadata& track) {
    std::lock_guard<std::mutex> lock(mMutex);
    mTracks.push_back(track);
    mPlaybackOrder.push_back(mTracks.size() - 1);
}

void PlaylistManager::clear() {
    std::lock_guard<std::mutex> lock(mMutex);
    mTracks.clear();
    mPlaybackOrder.clear();
    mCurrentOrderIndex = 0;
}

int PlaylistManager::getCurrentIndex() const {
    std::lock_guard<std::mutex> lock(mMutex);
    if (mPlaybackOrder.empty() || mCurrentOrderIndex >= mPlaybackOrder.size()) {
        return -1;
    }
    return static_cast<int>(mPlaybackOrder[mCurrentOrderIndex]);
}

TrackMetadata PlaylistManager::getCurrentTrack() const {
    std::lock_guard<std::mutex> lock(mMutex);
    if (mPlaybackOrder.empty() || mCurrentOrderIndex >= mPlaybackOrder.size()) {
        return TrackMetadata{};
    }
    size_t trackIdx = mPlaybackOrder[mCurrentOrderIndex];
    return mTracks[trackIdx];
}

size_t PlaylistManager::getTrackCount() const {
    std::lock_guard<std::mutex> lock(mMutex);
    return mTracks.size();
}

bool PlaylistManager::hasNext() const {
    std::lock_guard<std::mutex> lock(mMutex);
    if (mPlaybackOrder.empty()) return false;
    if (mRepeatMode != RepeatMode::OFF) return true;
    return mCurrentOrderIndex + 1 < mPlaybackOrder.size();
}

bool PlaylistManager::hasPrevious() const {
    std::lock_guard<std::mutex> lock(mMutex);
    if (mPlaybackOrder.empty()) return false;
    if (mRepeatMode != RepeatMode::OFF) return true;
    return mCurrentOrderIndex > 0;
}

TrackMetadata PlaylistManager::getNextTrack() {
    std::lock_guard<std::mutex> lock(mMutex);
    if (mPlaybackOrder.empty()) return TrackMetadata{};

    if (mRepeatMode == RepeatMode::ONE) {
        // Repeat current track
        return mTracks[mPlaybackOrder[mCurrentOrderIndex]];
    }

    if (mCurrentOrderIndex + 1 < mPlaybackOrder.size()) {
        mCurrentOrderIndex++;
    } else if (mRepeatMode == RepeatMode::ALL) {
        mCurrentOrderIndex = 0;
    }

    return mTracks[mPlaybackOrder[mCurrentOrderIndex]];
}

TrackMetadata PlaylistManager::getPreviousTrack() {
    std::lock_guard<std::mutex> lock(mMutex);
    if (mPlaybackOrder.empty()) return TrackMetadata{};

    if (mRepeatMode == RepeatMode::ONE) {
        return mTracks[mPlaybackOrder[mCurrentOrderIndex]];
    }

    if (mCurrentOrderIndex > 0) {
        mCurrentOrderIndex--;
    } else if (mRepeatMode == RepeatMode::ALL) {
        mCurrentOrderIndex = mPlaybackOrder.size() - 1;
    }

    return mTracks[mPlaybackOrder[mCurrentOrderIndex]];
}

void PlaylistManager::setShuffleMode(bool enabled) {
    std::lock_guard<std::mutex> lock(mMutex);
    if (mShuffleEnabled == enabled) return;

    size_t currentTrackIdx = 0;
    if (!mPlaybackOrder.empty() && mCurrentOrderIndex < mPlaybackOrder.size()) {
        currentTrackIdx = mPlaybackOrder[mCurrentOrderIndex];
    }

    mShuffleEnabled = enabled;
    rebuildPlaybackOrder();

    // Preserve current playing track
    for (size_t i = 0; i < mPlaybackOrder.size(); ++i) {
        if (mPlaybackOrder[i] == currentTrackIdx) {
            mCurrentOrderIndex = i;
            break;
        }
    }
}

void PlaylistManager::setRepeatMode(RepeatMode mode) {
    std::lock_guard<std::mutex> lock(mMutex);
    mRepeatMode = mode;
}

void PlaylistManager::rebuildPlaybackOrder() {
    mPlaybackOrder.resize(mTracks.size());
    for (size_t i = 0; i < mTracks.size(); ++i) {
        mPlaybackOrder[i] = i;
    }

    if (mShuffleEnabled && mTracks.size() > 1) {
        std::random_device rd;
        std::mt19937 g(rd());
        std::shuffle(mPlaybackOrder.begin(), mPlaybackOrder.end(), g);
    }
}

} // namespace audio
