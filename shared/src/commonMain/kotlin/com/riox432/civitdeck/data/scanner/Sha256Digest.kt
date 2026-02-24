@file:Suppress("MagicNumber")

package com.riox432.civitdeck.data.scanner

/**
 * Pure Kotlin streaming SHA-256 implementation (FIPS 180-4).
 * Used on iOS where platform.CommonCrypto is unavailable on simulator targets.
 */
internal class Sha256Digest {
    private val state = intArrayOf(
        0x6a09e667.toInt(),
        0xbb67ae85.toInt(),
        0x3c6ef372,
        0xa54ff53a.toInt(),
        0x510e527f,
        0x9b05688c.toInt(),
        0x1f83d9ab,
        0x5be0cd19,
    )
    private val block = ByteArray(BLOCK_SIZE)
    private var blockOffset = 0
    private var totalBytes = 0L

    fun update(data: ByteArray, offset: Int = 0, length: Int = data.size) {
        var pos = offset
        var remaining = length
        totalBytes += remaining

        if (blockOffset > 0) {
            val fill = minOf(BLOCK_SIZE - blockOffset, remaining)
            data.copyInto(block, blockOffset, pos, pos + fill)
            blockOffset += fill
            pos += fill
            remaining -= fill
            if (blockOffset == BLOCK_SIZE) {
                processBlock(block)
                blockOffset = 0
            }
        }

        while (remaining >= BLOCK_SIZE) {
            data.copyInto(block, 0, pos, pos + BLOCK_SIZE)
            processBlock(block)
            pos += BLOCK_SIZE
            remaining -= BLOCK_SIZE
        }

        if (remaining > 0) {
            data.copyInto(block, 0, pos, pos + remaining)
            blockOffset = remaining
        }
    }

    fun digest(): ByteArray {
        val totalBits = totalBytes * 8
        block[blockOffset++] = 0x80.toByte()
        if (blockOffset > BLOCK_SIZE - 8) {
            block.fill(0, blockOffset, BLOCK_SIZE)
            processBlock(block)
            blockOffset = 0
        }
        block.fill(0, blockOffset, BLOCK_SIZE - 8)
        for (i in 0 until 8) {
            block[BLOCK_SIZE - 8 + i] = (totalBits ushr (56 - i * 8)).toByte()
        }
        processBlock(block)

        val result = ByteArray(DIGEST_SIZE)
        for (i in 0 until 8) {
            result[i * 4] = (state[i] ushr 24).toByte()
            result[i * 4 + 1] = (state[i] ushr 16).toByte()
            result[i * 4 + 2] = (state[i] ushr 8).toByte()
            result[i * 4 + 3] = state[i].toByte()
        }
        return result
    }

    private fun processBlock(data: ByteArray) {
        val w = IntArray(SCHEDULE_SIZE)
        for (i in 0 until 16) {
            w[i] = (data[i * 4].toInt() and 0xFF shl 24) or
                (data[i * 4 + 1].toInt() and 0xFF shl 16) or
                (data[i * 4 + 2].toInt() and 0xFF shl 8) or
                (data[i * 4 + 3].toInt() and 0xFF)
        }
        for (i in 16 until SCHEDULE_SIZE) {
            w[i] = sigma1(w[i - 2]) + w[i - 7] + sigma0(w[i - 15]) + w[i - 16]
        }

        var a = state[0]
        var b = state[1]
        var c = state[2]
        var d = state[3]
        var e = state[4]
        var f = state[5]
        var g = state[6]
        var h = state[7]

        for (i in 0 until SCHEDULE_SIZE) {
            val t1 = h + bigSigma1(e) + ch(e, f, g) + K[i] + w[i]
            val t2 = bigSigma0(a) + maj(a, b, c)
            h = g
            g = f
            f = e
            e = d + t1
            d = c
            c = b
            b = a
            a = t1 + t2
        }

        state[0] += a
        state[1] += b
        state[2] += c
        state[3] += d
        state[4] += e
        state[5] += f
        state[6] += g
        state[7] += h
    }

    companion object {
        private const val BLOCK_SIZE = 64
        private const val DIGEST_SIZE = 32
        private const val SCHEDULE_SIZE = 64

        private fun ch(x: Int, y: Int, z: Int) = (x and y) xor (x.inv() and z)
        private fun maj(x: Int, y: Int, z: Int) = (x and y) xor (x and z) xor (y and z)
        private fun bigSigma0(x: Int) = x.rotateRight(2) xor x.rotateRight(13) xor x.rotateRight(22)
        private fun bigSigma1(x: Int) = x.rotateRight(6) xor x.rotateRight(11) xor x.rotateRight(25)
        private fun sigma0(x: Int) = x.rotateRight(7) xor x.rotateRight(18) xor (x ushr 3)
        private fun sigma1(x: Int) = x.rotateRight(17) xor x.rotateRight(19) xor (x ushr 10)

        @Suppress("LargeClass")
        private val K = intArrayOf(
            0x428a2f98, 0x71374491, -0x4a3f0431, -0x164a245b,
            0x3956c25b, 0x59f111f1, -0x6dc07d5c, -0x54e3a12b,
            -0x27f85568, 0x12835b01, 0x243185be, 0x550c7dc3,
            0x72be5d74, -0x7f214e02, -0x6423f959, -0x3e640e8c,
            -0x1b64963f, -0x1041b87a, 0x0fc19dc6, 0x240ca1cc,
            0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
            -0x67c1aeae, -0x57ce3993, -0x4ffcd838, -0x40a68039,
            -0x391ff40d, -0x2a586eb9, 0x06ca6351, 0x14292967,
            0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13,
            0x650a7354, 0x766a0abb, -0x7e3d36d2, -0x6d8dd37b,
            -0x5d40175f, -0x57e599b5, -0x3db47490, -0x3893ae5d,
            -0x2e6d17e7, -0x2966f9dc, -0x0bf1ca7b, 0x106aa070,
            0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5,
            0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
            0x748f82ee, 0x78a5636f, -0x7b3787ec, -0x7338fdf8,
            -0x6f410006, -0x5baf9315, -0x41065c09, -0x398e870e,
        )

        fun digestToHex(bytes: ByteArray): String =
            bytes.joinToString("") { byte ->
                (byte.toInt() and 0xFF).toString(16).padStart(2, '0')
            }

        private fun Int.rotateRight(n: Int): Int =
            (this ushr n) or (this shl (32 - n))
    }
}
