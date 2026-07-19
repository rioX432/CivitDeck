package com.riox432.civitdeck.domain.ml

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Parity harness for [SigLipTokenizer].
 *
 * The tokenizer claims to implement SentencePiece **Unigram** segmentation (Viterbi
 * over log-probability scores). These golden fixtures pin that algorithm against a
 * small, hand-verified reference vocabulary: whitespace-prefix (`▁`), lowercasing,
 * best-score segmentation, EOS append, padding, truncation, and unknown-token
 * fallback each have a case with an expected token-id sequence computed by hand.
 *
 * IMPORTANT — scope of "parity": this proves the tokenizer matches the SentencePiece
 * Unigram reference *algorithm*, NOT the real `google/siglip2-base-patch16-224`
 * tokenizer. SigLIP-2 actually ships the multilingual **Gemma** tokenizer
 * (`GemmaTokenizerFast`, 256k vocab, byte-fallback, digit splitting, case preserved),
 * which this simplified encoder does not reproduce. Closing that gap requires a
 * committed golden fixture generated from the real tokenizer.json — tracked in
 * `docs/research/989-semantic-corpus-index-spike.md` and its follow-up issue.
 */
class SigLipTokenizerParityTest {

    /**
     * Reference Unigram vocab. Index = token id; second element = log-prob score.
     * Higher (less negative) score wins during Viterbi segmentation.
     */
    private fun vocabJson(maxSeqLen: Int, addEos: Boolean = true): String = """
        {
          "pieces": [
            ["<pad>", 0.0],
            ["<eos>", 0.0],
            ["<bos>", 0.0],
            ["<unk>", 0.0],
            ["▁", -3.0],
            ["▁a", -1.0],
            ["▁ab", -1.5],
            ["b", -2.0],
            ["a", -2.5],
            ["▁cat", -1.0],
            ["▁c", -4.0],
            ["at", -4.0]
          ],
          "special": {"pad": 0, "eos": 1, "bos": 2, "unk": 3},
          "max_seq_len": $maxSeqLen,
          "add_eos": $addEos,
          "add_bos": false
        }
    """.trimIndent()

    private fun encode(text: String, maxSeqLen: Int = 8, addEos: Boolean = true): List<Long> =
        SigLipTokenizer.fromVocabJson(vocabJson(maxSeqLen, addEos)).encode(text).toList()

    @Test
    fun singlePieceMatch_appendsEosAndPads() {
        // "▁cat" (id 9, -1.0) beats "▁c"+"at" (-8.0). EOS(1) then pad(0).
        assertEquals(listOf(9L, 1L, 0L, 0L, 0L, 0L, 0L, 0L), encode("cat"))
    }

    @Test
    fun lowercasesBeforeSegmentation() {
        // Upper-case input must map to the same ids as lower-case.
        assertEquals(encode("cat"), encode("CAT"))
    }

    @Test
    fun prefersLongerHigherScorePiece() {
        // "▁ab" (id 6, -1.5) beats "▁a"+"b" (-3.0) and "▁"+"a"+"b" (-7.5).
        assertEquals(listOf(6L, 1L, 0L, 0L, 0L, 0L, 0L, 0L), encode("ab"))
    }

    @Test
    fun unknownCharacterFallsBackToUnkToken() {
        // "▁x": "▁"(id4) then 'x' has no piece → unk(id3) fallback, then EOS(1).
        assertEquals(listOf(4L, 3L, 1L, 0L, 0L, 0L, 0L, 0L), encode("x"))
    }

    @Test
    fun truncationDropsEosWhenSequenceIsFull() {
        // "▁abab" segments to ["▁ab"(6), "a"(8), "b"(7)]; maxSeqLen=2 keeps the
        // first two ids and leaves no room for EOS.
        assertEquals(listOf(6L, 8L), encode("abab", maxSeqLen = 2))
    }

    @Test
    fun addEosDisabled_producesNoTrailingEos() {
        assertEquals(listOf(9L, 0L, 0L, 0L, 0L, 0L, 0L, 0L), encode("cat", addEos = false))
    }
}
