package com.riox432.civitdeck.data.local.repository

/**
 * Minimal IEEE-754 binary16 (half precision) codec used for storing image embeddings on disk.
 *
 * Encoding is little-endian: byte layout is `[lo, hi]` per fp16 value, so a 768-d vector
 * occupies 1536 bytes. Decoding reconstructs an fp32 [FloatArray] of the requested length.
 *
 * The half-precision format gives ~3 decimal digits of precision, more than sufficient for
 * cosine retrieval over L2-normalized SigLIP-2 embeddings — Mercari and others ship the same
 * representation in production.
 *
 * Pure-Kotlin to keep `commonMain` portable. Performance is not critical here: encode runs
 * once per cached model, decode runs once per similarity scan over <10 k vectors.
 */
internal object Fp16 {

    private const val BYTES_PER_VALUE = 2
    private const val MASK_LOW_BYTE = 0xFF
    private const val SHIFT_HIGH_BYTE = 8

    private const val FP32_SIGN_SHIFT = 31
    private const val FP32_EXP_SHIFT = 23
    private const val FP32_EXP_MASK = 0xFF
    private const val FP32_MANTISSA_MASK = 0x7FFFFF

    private const val FP16_SIGN_SHIFT = 15
    private const val FP16_EXP_SHIFT = 10
    private const val FP16_EXP_MASK = 0x1F
    private const val FP16_MANTISSA_MASK = 0x3FF
    private const val FP16_EXP_MAX = 0x1F
    private const val FP16_EXP_BIAS_DELTA = 112 // 127 (fp32 bias) - 15 (fp16 bias)
    private const val FP32_EXP_MAX = 0xFF

    private const val FP32_NAN_PAYLOAD = 0x200000
    private const val FP16_NAN_PAYLOAD = 0x200
    private const val FP16_INF_BITS = 0x7C00

    fun encode(values: FloatArray): ByteArray {
        val out = ByteArray(values.size * BYTES_PER_VALUE)
        for (i in values.indices) {
            val half = floatToHalf(values[i])
            val offset = i * BYTES_PER_VALUE
            out[offset] = (half and MASK_LOW_BYTE).toByte()
            out[offset + 1] = ((half ushr SHIFT_HIGH_BYTE) and MASK_LOW_BYTE).toByte()
        }
        return out
    }

    fun decode(bytes: ByteArray, dim: Int): FloatArray {
        require(bytes.size >= dim * BYTES_PER_VALUE) {
            "Encoded embedding too short: ${bytes.size} bytes for $dim values"
        }
        val out = FloatArray(dim)
        for (i in 0 until dim) {
            val offset = i * BYTES_PER_VALUE
            val lo = bytes[offset].toInt() and MASK_LOW_BYTE
            val hi = bytes[offset + 1].toInt() and MASK_LOW_BYTE
            out[i] = halfToFloat((hi shl SHIFT_HIGH_BYTE) or lo)
        }
        return out
    }

    @Suppress("MagicNumber", "ReturnCount")
    private fun floatToHalf(value: Float): Int {
        val bits = value.toRawBits()
        val sign = (bits ushr FP32_SIGN_SHIFT) and 0x1
        var exp = (bits ushr FP32_EXP_SHIFT) and FP32_EXP_MASK
        var mantissa = bits and FP32_MANTISSA_MASK

        if (exp == FP32_EXP_MAX) {
            // NaN or Inf
            val halfMantissa = if (mantissa != 0) FP16_NAN_PAYLOAD else 0
            return (sign shl FP16_SIGN_SHIFT) or FP16_INF_BITS or halfMantissa
        }

        exp -= FP16_EXP_BIAS_DELTA
        if (exp >= FP16_EXP_MAX) {
            // Overflow → Inf
            return (sign shl FP16_SIGN_SHIFT) or FP16_INF_BITS
        }
        if (exp <= 0) {
            // Subnormal or underflow to zero
            if (exp < -10) return sign shl FP16_SIGN_SHIFT
            mantissa = (mantissa or 0x800000) shr (1 - exp)
            // round-to-nearest-even
            if ((mantissa and 0x1000) != 0) mantissa += 0x2000
            return (sign shl FP16_SIGN_SHIFT) or (mantissa shr 13)
        }
        // Normal: round-to-nearest-even on the truncated bits
        if ((mantissa and 0x1000) != 0) {
            mantissa += 0x2000
            if ((mantissa and 0x800000) != 0) {
                mantissa = 0
                exp += 1
                if (exp >= FP16_EXP_MAX) {
                    return (sign shl FP16_SIGN_SHIFT) or FP16_INF_BITS
                }
            }
        }
        return (sign shl FP16_SIGN_SHIFT) or (exp shl FP16_EXP_SHIFT) or (mantissa shr 13)
    }

    @Suppress("MagicNumber")
    private fun halfToFloat(half: Int): Float {
        val sign = (half ushr FP16_SIGN_SHIFT) and 0x1
        var exp = (half ushr FP16_EXP_SHIFT) and FP16_EXP_MASK
        var mantissa = half and FP16_MANTISSA_MASK

        when (exp) {
            0 -> {
                if (mantissa == 0) {
                    // ±0
                    return Float.fromBits(sign shl FP32_SIGN_SHIFT)
                }
                // Subnormal: normalize
                while ((mantissa and 0x400) == 0) {
                    mantissa = mantissa shl 1
                    exp -= 1
                }
                exp += 1
                mantissa = mantissa and FP16_MANTISSA_MASK.inv() and 0x3FF
            }
            FP16_EXP_MAX -> {
                // Inf or NaN
                val fp32Mantissa = if (mantissa != 0) FP32_NAN_PAYLOAD else 0
                return Float.fromBits(
                    (sign shl FP32_SIGN_SHIFT) or (FP32_EXP_MAX shl FP32_EXP_SHIFT) or fp32Mantissa,
                )
            }
        }
        val fp32Exp = exp + FP16_EXP_BIAS_DELTA
        return Float.fromBits(
            (sign shl FP32_SIGN_SHIFT) or (fp32Exp shl FP32_EXP_SHIFT) or (mantissa shl 13),
        )
    }
}
