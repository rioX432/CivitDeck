#!/usr/bin/env python3
"""
Export the SigLIP-2 text encoder to ONNX and extract the tokenizer vocabulary.

This script produces two files required by the Android TextEmbeddingModel:
  1. siglip2_text_q4f16.onnx  — quantised text encoder (INT8 weights + FP16 activations)
  2. siglip2_vocab.json       — tokenizer vocabulary extracted from the SentencePiece model

Both files should be placed in androidApp/src/main/assets/ml/.
The ONNX model is too large to commit (~155 MB after optimum quantization);
download it from a release or generate it with this script.

Requirements (Python 3.10+):
    pip install torch transformers optimum[onnxruntime] onnxruntime sentencepiece

Usage:
    python scripts/export_siglip2_text_onnx.py [--output-dir OUTPUT_DIR]

Output (default):
    androidApp/src/main/assets/ml/siglip2_text_int8.onnx
    androidApp/src/main/assets/ml/siglip2_vocab.json
"""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

import numpy as np
import torch
from transformers import AutoModel, AutoTokenizer


MODEL_ID = "google/siglip2-base-patch16-224"
DEFAULT_OUTPUT = Path(__file__).resolve().parent.parent / "androidApp" / "src" / "main" / "assets" / "ml"

EMBEDDING_DIM = 768
MAX_SEQ_LEN = 64


class TextEncoderWrapper(torch.nn.Module):
    """Wraps the SigLIP-2 text model to output L2-normalised pooler output."""

    def __init__(self, text_model: torch.nn.Module) -> None:
        super().__init__()
        self.text_model = text_model

    def forward(self, input_ids: torch.Tensor) -> torch.Tensor:
        outputs = self.text_model(input_ids=input_ids)
        pooled = outputs.pooler_output  # (batch, 768)
        return torch.nn.functional.normalize(pooled, p=2, dim=-1)


def export_onnx(output_dir: Path) -> None:
    """Export text encoder to ONNX with INT8 quantisation."""
    print(f"Loading {MODEL_ID} ...")
    model = AutoModel.from_pretrained(MODEL_ID)
    text_model = model.text_model
    text_model.eval()

    wrapper = TextEncoderWrapper(text_model)
    wrapper.eval()

    # Trace with dummy input
    dummy_input = torch.randint(0, 256000, (1, MAX_SEQ_LEN), dtype=torch.long)
    onnx_path = output_dir / "siglip2_text_int8.onnx"
    onnx_fp32_path = output_dir / "siglip2_text_fp32.onnx"

    print("Exporting text encoder to ONNX (FP32) ...")
    with torch.no_grad():
        torch.onnx.export(
            wrapper,
            (dummy_input,),
            str(onnx_fp32_path),
            input_names=["input_ids"],
            output_names=["pooler_output"],
            dynamic_axes={
                "input_ids": {0: "batch_size", 1: "sequence_length"},
                "pooler_output": {0: "batch_size"},
            },
            opset_version=17,
        )

    print(f"FP32 model saved: {onnx_fp32_path} ({onnx_fp32_path.stat().st_size / 1024 / 1024:.1f} MB)")

    # Quantise to INT8
    print("Quantising to INT8 ...")
    try:
        from onnxruntime.quantization import quantize_dynamic, QuantType
        quantize_dynamic(
            str(onnx_fp32_path),
            str(onnx_path),
            weight_type=QuantType.QInt8,
        )
        print(f"INT8 model saved: {onnx_path} ({onnx_path.stat().st_size / 1024 / 1024:.1f} MB)")
        # Clean up FP32 model
        onnx_fp32_path.unlink()
    except ImportError:
        print("WARNING: onnxruntime.quantization not available, keeping FP32 model")
        onnx_fp32_path.rename(onnx_path)

    # Verify the model
    print("Verifying ONNX model ...")
    import onnxruntime as ort
    session = ort.InferenceSession(str(onnx_path))
    test_input = np.random.randint(0, 256000, (1, MAX_SEQ_LEN)).astype(np.int64)
    outputs = session.run(None, {"input_ids": test_input})
    embedding = outputs[0][0]
    print(f"  Output shape: {outputs[0].shape}")
    print(f"  Embedding dim: {len(embedding)}")
    norm = np.linalg.norm(embedding)
    print(f"  L2 norm: {norm:.4f} (should be ~1.0)")


def export_vocab(output_dir: Path) -> None:
    """Extract tokenizer vocabulary as a compact JSON file.

    The JSON contains:
      - pieces: list of [piece_string, score] ordered by token ID
      - special: map of special token names to IDs
      - max_seq_len: maximum sequence length for the text encoder
    """
    print(f"Loading tokenizer from {MODEL_ID} ...")
    tokenizer = AutoTokenizer.from_pretrained(MODEL_ID)

    # Extract vocab: id -> (piece, score)
    # The SentencePiece model stores pieces with scores.
    # We extract them via the HF tokenizer's get_vocab().
    vocab = tokenizer.get_vocab()

    # Build ordered list by token ID
    max_id = max(vocab.values())
    pieces = [None] * (max_id + 1)
    for piece, token_id in vocab.items():
        # Score is not directly available from HF tokenizer.get_vocab(),
        # so we use 0.0 as placeholder. The actual Unigram scores are
        # embedded in tokenizer.model (SentencePiece protobuf).
        pieces[token_id] = piece

    # Fill any gaps with empty strings
    for i in range(len(pieces)):
        if pieces[i] is None:
            pieces[i] = ""

    # Try to get scores from SentencePiece model directly
    scores = [0.0] * len(pieces)
    try:
        import sentencepiece as spm
        sp = spm.SentencePieceProcessor()
        # Download tokenizer.model
        from huggingface_hub import hf_hub_download
        model_path = hf_hub_download(repo_id=MODEL_ID, filename="tokenizer.model")
        sp.Load(model_path)
        for i in range(sp.GetPieceSize()):
            scores[i] = float(sp.GetScore(i))
            # Also update piece string from SP model (more accurate)
            pieces[i] = sp.IdToPiece(i)
    except ImportError:
        print("WARNING: sentencepiece not installed, using zero scores (tokenization may differ)")

    # Special tokens
    special = {}
    if tokenizer.pad_token_id is not None:
        special["pad"] = tokenizer.pad_token_id
    if tokenizer.eos_token_id is not None:
        special["eos"] = tokenizer.eos_token_id
    if tokenizer.bos_token_id is not None:
        special["bos"] = tokenizer.bos_token_id
    if tokenizer.unk_token_id is not None:
        special["unk"] = tokenizer.unk_token_id

    vocab_data = {
        "pieces": [[p, s] for p, s in zip(pieces, scores)],
        "special": special,
        "max_seq_len": MAX_SEQ_LEN,
        "add_eos": True,
        "add_bos": False,
    }

    vocab_path = output_dir / "siglip2_vocab.json"
    with open(vocab_path, "w", encoding="utf-8") as f:
        json.dump(vocab_data, f, ensure_ascii=False)
    size_mb = vocab_path.stat().st_size / 1024 / 1024
    print(f"Vocabulary saved: {vocab_path} ({size_mb:.1f} MB, {len(pieces)} tokens)")

    # Quick test
    test_text = "a photo of a cat"
    encoded = tokenizer.encode(test_text, add_special_tokens=True)
    print(f"  Test: '{test_text}' -> {encoded[:10]}{'...' if len(encoded) > 10 else ''}")
    print(f"  Token count: {len(encoded)}")


def main() -> None:
    parser = argparse.ArgumentParser(description="Export SigLIP-2 text encoder to ONNX")
    parser.add_argument(
        "--output-dir",
        type=Path,
        default=DEFAULT_OUTPUT,
        help=f"Output directory (default: {DEFAULT_OUTPUT})",
    )
    parser.add_argument(
        "--vocab-only",
        action="store_true",
        help="Only export vocabulary, skip ONNX model",
    )
    args = parser.parse_args()

    args.output_dir.mkdir(parents=True, exist_ok=True)

    if not args.vocab_only:
        export_onnx(args.output_dir)
    export_vocab(args.output_dir)

    print("\n--- Done ---")
    print(f"Place the following files in androidApp/src/main/assets/ml/:")
    print(f"  - siglip2_text_int8.onnx  (text encoder model)")
    print(f"  - siglip2_vocab.json      (tokenizer vocabulary)")


if __name__ == "__main__":
    main()
