#ifndef MUSIC_PLAYER_RING_BUFFER_H
#define MUSIC_PLAYER_RING_BUFFER_H

#include <vector>
#include <atomic>
#include <cstdint>
#include <cstring>
#include <algorithm>

namespace audio {

/**
 * Single-Producer Single-Consumer (SPSC) Lock-Free Circular Buffer.
 * Designed for real-time audio playback where OpenSL ES callbacks must never block or wait on a mutex.
 */
class RingBuffer {
public:
    explicit RingBuffer(size_t capacity = 512 * 1024)
        : mCapacity(capacity),
          mBuffer(capacity),
          mReadIndex(0),
          mWriteIndex(0) {}

    ~RingBuffer() = default;

    // Reset read/write pointers (e.g. on seek/stop)
    void reset() {
        mReadIndex.store(0, std::memory_order_relaxed);
        mWriteIndex.store(0, std::memory_order_relaxed);
    }

    size_t getCapacity() const {
        return mCapacity;
    }

    // Number of bytes available to read by audio callback
    size_t getAvailableRead() const {
        const size_t write = mWriteIndex.load(std::memory_order_acquire);
        const size_t read = mReadIndex.load(std::memory_order_relaxed);
        if (write >= read) {
            return write - read;
        }
        return mCapacity - (read - write);
    }

    // Free space in bytes for decoder thread to write
    size_t getAvailableWrite() const {
        // Leave 1 byte unwritten to disambiguate full from empty
        return (mCapacity - 1) - getAvailableRead();
    }

    // Write bytes into buffer (Producer / Decoder Thread)
    size_t write(const uint8_t* data, size_t bytesToWrite) {
        if (!data || bytesToWrite == 0) return 0;

        const size_t available = getAvailableWrite();
        const size_t count = std::min(bytesToWrite, available);
        if (count == 0) return 0;

        const size_t writeIdx = mWriteIndex.load(std::memory_order_relaxed);
        const size_t firstPart = std::min(count, mCapacity - writeIdx);
        std::memcpy(&mBuffer[writeIdx], data, firstPart);

        const size_t secondPart = count - firstPart;
        if (secondPart > 0) {
            std::memcpy(&mBuffer[0], data + firstPart, secondPart);
        }

        const size_t newWriteIdx = (writeIdx + count) % mCapacity;
        mWriteIndex.store(newWriteIdx, std::memory_order_release);
        return count;
    }

    // Read bytes from buffer (Consumer / OpenSL ES Audio Thread)
    size_t read(uint8_t* outData, size_t bytesToRead) {
        if (!outData || bytesToRead == 0) return 0;

        const size_t available = getAvailableRead();
        const size_t count = std::min(bytesToRead, available);
        if (count == 0) return 0;

        const size_t readIdx = mReadIndex.load(std::memory_order_relaxed);
        const size_t firstPart = std::min(count, mCapacity - readIdx);
        std::memcpy(outData, &mBuffer[readIdx], firstPart);

        const size_t secondPart = count - firstPart;
        if (secondPart > 0) {
            std::memcpy(outData + firstPart, &mBuffer[0], secondPart);
        }

        const size_t newReadIdx = (readIdx + count) % mCapacity;
        mReadIndex.store(newReadIdx, std::memory_order_release);
        return count;
    }

private:
    const size_t mCapacity;
    std::vector<uint8_t> mBuffer;
    alignas(64) std::atomic<size_t> mReadIndex;
    alignas(64) std::atomic<size_t> mWriteIndex;
};

} // namespace audio

#endif // MUSIC_PLAYER_RING_BUFFER_H
