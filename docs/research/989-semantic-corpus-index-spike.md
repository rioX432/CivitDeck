# Semantic Corpus Index Spike — Go/No-Go for SigLIP-2 as Primary Search (Issue #989)

Status: **Decision — NO-GO on semantic-primary.** Adopt Option C (CivitAI API retrieval +
optional Android-only re-ranker). Revisit only if the GO criteria below are met.
Date: 2026-07-19
Related: #602 (SigLIP-2 parent), #988 (unified search bar — must keep semantic experimental),
#805 (iOS text encoder), #700/#701 (encoder conversion)

> Per `.claude/rules/ai-ops.md` (Research → Implementation Gate): this document is a
> **decision doc only**. It does not authorize building a corpus index. If a future
> decision flips to GO, that becomes a new issue that passes weekly review first.

## TL;DR

On-device SigLIP-2 semantic search is **not a shipped feature** and must not become the primary
search path. It currently indexes only models the user opened in detail (dozens), not the
CivitAI catalog (~1M+ models); iOS/Desktop have no working encoder; and the Android tokenizer is
not parity with the real SigLIP-2 tokenizer. Promoting it to primary would ship an effectively
empty search screen plus a ~300 MB APK. Keep CivitAI's REST search as the authoritative retrieval
layer. Semantic ranking stays an **Android-only experiment**, at most an optional re-ranker over
keyword results — never the retrieval layer.

## Verified current state (the reality check)

| Platform | Text encoder | Vision encoder | Reality |
|---|---|---|---|
| Android | ONNX + simplified tokenizer (`SigLipTokenizer.kt`); model+vocab **gitignored, not committed** | ONNX (`siglip2_vision_q4f16.onnx`, githubFull-only, flag-gated) | Only works in a locally built `-Pcivitdeck.enableSimilaritySearch=true` `githubFull` APK |
| iOS | **Stub** — `isAvailable = false`, `embed()` throws (`core-ml/src/iosMain/.../TextEmbeddingModelImpl.kt`) | Bridge exists but **no `.mlpackage` bundled** | Non-functional |
| Desktop/JVM | **No-op** by design (`core-ml/src/jvmMain/.../*Impl.kt`) | No-op | Non-functional |

- The index is built **only for models opened in detail** (`EmbedOnBrowseUseCase`) → search
  covers a tiny local subset, not CivitAI.
- Retrieval is O(N) cosine over all cached vectors (`ModelEmbeddingRepositoryImpl.findSimilar`) —
  fine for dozens of vectors, not a catalog.
- Feature is gated OFF everywhere by default: Android `BuildConfig.FEATURE_SIMILARITY_SEARCH`
  (default `false`, and always `false` on the `fdroid` flavor); iOS `FeatureFlags.similaritySearch = false`.

### Tokenizer parity gap (correctness blocker, not a nicety)

`SigLipTokenizer.kt` implements a simplified SentencePiece **Unigram** loader: lowercases, one
leading `▁`, small exported vocab, no digit splitting, ad-hoc unknown fallback. The real
`google/siglip2-base-patch16-224` ships the multilingual **Gemma** tokenizer (`GemmaTokenizerFast`,
**256k** vocab, byte-fallback, digit splitting, case preserved, `max_length=64`) — a 34 MB
`tokenizer.json`. Corpus vectors and query vectors **must** use the identical tokenizer,
model revision, image preprocessing, sequence length, normalization and quantization, or cosine
similarity is meaningless. The current tokenizer does not meet this bar. The
`SigLipTokenizerParityTest` added in this issue pins the *Unigram algorithm* the code claims to
implement; a full HF-parity fixture (real `tokenizer.json` golden vectors) is a follow-up
prerequisite for any GO.

Sources: [SigLIP-2 blog](https://huggingface.co/blog/siglip2),
[HF SigLIP2 docs](https://huggingface.co/docs/transformers/model_doc/siglip2),
[model card](https://huggingface.co/google/siglip2-base-patch16-224).

## Options evaluated

| Option | What it is | Decision |
|---|---|---|
| **A. Downloadable precomputed ANN corpus** | Backend batch-embeds the catalog, builds an HNSW index, app downloads + delta-syncs. On-device query embed + local ANN. | **No-go now** |
| **B. Hosted vector search service** | Backend hosts embeddings in a vector DB (pgvector/Qdrant/managed); app sends query embedding or text to a search endpoint. | **No-go now** |
| **C. CivitAI API retrieval + optional re-ranker** | CivitAI keyword/tag/filter API is the retrieval layer; SigLIP re-ranks top candidates on capable Android devices only. | **Recommended direction** (still gated — not authorized by this doc) |
| **D. Drop on-device semantic entirely** | Remove the encoders/UI. | Acceptable fallback if C still adds ~300 MB or unsustainable cross-platform maintenance |

### Option A — cost/risk (index is bigger than it looks)

- **Vector size**: 1M × 768-d ≈ **3.1 GB FP32 / 1.5 GB FP16 / 0.8 GB INT8**, before HNSW graph,
  IDs, metadata and update structures. Product quantization shrinks this but adds a
  recall-evaluation project. The ~300 MB encoder is *separate* from the corpus.
- **Pipeline**: catalog crawling (deleted/private content, rate limits, thumbnail downloads,
  dedup, NSFW handling), deterministic model/tokenizer/preprocessing/quantization versioning,
  atomic index updates, corruption recovery, schema migrations.
- **Delta-sync**: HNSW is awkward to delta-sync — inserts are OK, but deletions/compaction and
  reproducible snapshots eventually force full index replacement.
- **Staleness**: a stale index returns plausible-but-unavailable hits unless every result is
  re-validated against CivitAI (which reintroduces the network round-trip A was meant to avoid).
- A avoids a query *server*, but still needs backend infra to produce and publish signed snapshots.

### Option B — cost/risk (worst organizational fit)

- Cleaner freshness/coverage, but creates a **backend business**: hosting, monitoring, abuse
  prevention, keys, rate limiting, backups, incident response, egress. Embedding generation and
  image ingestion — not vector storage — are the large recurring liabilities.
- Sending query text → network-dependent search + privacy boundary. Sending client vectors →
  keeps the tokenizer/model parity problem and complicates upgrades.
- Only attractive if semantic search becomes a funded, differentiating product feature.

## Decision & rationale

**Semantic-primary is rejected for the current product stage. CivitAI remains the retrieval
system; local ML may be evaluated only as an optional candidate re-ranker.**

Why:
1. **Core value misfit** — CivitDeck's core values (comfortable browsing; send models to local
   ComfyUI; mobile UX) do not include being a search engine. A/B both build search
   infrastructure the product does not need. Fails the one-step Core Value test.
2. **Solo-dev cost** — no backend today, no store distribution, wants zero/low recurring cost. A
   is a data-engineering project; B is an ops commitment. Neither is justified by a non-core feature.
3. **Correctness debt** — tokenizer and cross-platform encoder parity are unmet. A primary KMP
   feature cannot be Android-only-correct.
4. **Unproven benefit** — SigLIP-2 is image/text *alignment*-oriented; it is not automatically a
   good text-to-text ranker over titles/tags/descriptions. No benchmark yet shows it beats the
   CivitAI API ranking for model discovery.

### Recommended shape (Option C, when/if pursued as an experiment)

1. CivitAI keyword/tag/filter API retrieves the complete candidate set (unchanged default order).
2. Optionally re-rank the top ~20–100 candidates on capable Android devices, behind the existing
   experimental flag.
3. Ship the encoder as an **explicit downloadable component**, so normal installs never gain ~300 MB.
4. Silent, complete fallback to API ranking on iOS/Desktop and on failure.
5. **Open problem to note**: re-ranking *unseen* results still needs candidate representations —
   the detail-page cache cannot supply them. The experiment must embed candidate metadata at query
   time or download+embed result thumbnails (slower/costlier). This is why re-ranking is scoped as
   an experiment, not a promised feature, and why #988 must keep semantic non-default.

## Isolation invariant (for #988 unified search)

Semantic/SigLIP retrieval must stay **off the default retrieval path**. In a unified search bar it
may only re-order keyword results as an opt-in Android experiment; it must never be the primary
retrieval layer. Availability of search must never depend on embeddings.

## GO criteria (revisit trigger)

Reconsider semantic-primary only when **all** hold:
- Offline semantic catalog search is validated as a core user need (not gut feeling — 2-axis test).
- A representative benchmark shows meaningful ranking gains over the CivitAI API.
- The index covers ~the full searchable catalog with defined freshness targets.
- Model/tokenizer/preprocessing parity is proven on Android, iOS **and** Desktop against a shared
  golden suite (incl. a real HF `tokenizer.json` fixture).
- Download size, cold-start latency, memory and update costs meet explicit budgets.
- The pipeline/service has sustainable ownership and funding.
- Keyword/API fallback remains available for freshness and failure recovery.

## Follow-ups (NOT implemented here — tracked in #995, pending weekly review)

1. **HF-parity tokenizer fixture** — regenerate the Android tokenizer from the real Gemma
   `tokenizer.json`; commit golden (text → token-id) vectors and extend `SigLipTokenizerParityTest`
   to assert against them. Prerequisite for any GO.
2. **Re-ranker experiment (Option C)** — Android-only, downloadable encoder, top-K re-rank over
   CivitAI results, benchmarked vs API ranking before any wider rollout.

## Cross-review

Corpus-index options and the go/no-go were cross-reviewed with Codex MCP (per
`.claude/rules/behavior.md`). Codex independently recommended **NO-GO on semantic-primary**,
Option **C** with **D** as fallback, corrected the index-size estimates above, and flagged both
the Gemma tokenizer mismatch as a correctness blocker and that SigLIP-2 is not inherently a good
text-to-text catalog ranker.
