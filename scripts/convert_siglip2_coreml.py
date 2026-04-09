#!/usr/bin/env python3
"""
Convert the SigLIP-2 vision encoder to a Core ML .mlpackage (FP16).

Requirements (Python 3.10+):
    pip install torch transformers coremltools>=8.0 pillow numpy

Usage:
    python scripts/convert_siglip2_coreml.py

Output:
    iosApp/iosApp/Resources/SigLIP2.mlpackage
"""

from __future__ import annotations

import sys
from pathlib import Path

import coremltools as ct
import numpy as np
import torch
from transformers import AutoModel, SiglipImageProcessor


MODEL_ID = "google/siglip2-base-patch16-224"
OUTPUT_DIR = Path(__file__).resolve().parent.parent / "iosApp" / "iosApp" / "Resources"
OUTPUT_PATH = OUTPUT_DIR / "SigLIP2.mlpackage"

IMAGE_SIZE = 224
EMBEDDING_DIM = 768


class VisionEncoderWrapper(torch.nn.Module):
    """Wraps the SigLIP-2 vision model to output L2-normalized pooler output."""

    def __init__(self, vision_model: torch.nn.Module) -> None:
        super().__init__()
        self.vision_model = vision_model

    def forward(self, pixel_values: torch.Tensor) -> torch.Tensor:
        outputs = self.vision_model(pixel_values=pixel_values)
        # pooler_output is the CLS-pooled embedding (batch, 768)
        pooled = outputs.pooler_output
        # L2 normalize
        return torch.nn.functional.normalize(pooled, p=2, dim=-1)


def main() -> None:
    print(f"Loading {MODEL_ID} ...")
    model = AutoModel.from_pretrained(MODEL_ID)
    vision_model = model.vision_model
    vision_model.eval()

    wrapper = VisionEncoderWrapper(vision_model)
    wrapper.eval()

    # Trace with dummy input
    dummy_input = torch.randn(1, 3, IMAGE_SIZE, IMAGE_SIZE)
    print("Tracing vision encoder ...")
    with torch.no_grad():
        traced = torch.jit.trace(wrapper, dummy_input)

    # Convert to Core ML
    print("Converting to Core ML .mlpackage (FP16) ...")
    mlmodel = ct.convert(
        traced,
        inputs=[
            ct.TensorType(
                name="pixel_values",
                shape=(1, 3, IMAGE_SIZE, IMAGE_SIZE),
                dtype=np.float32,
            )
        ],
        outputs=[ct.TensorType(name="embedding", dtype=np.float32)],
        convert_to="mlprogram",
        compute_precision=ct.precision.FLOAT16,
        minimum_deployment_target=ct.target.iOS16,
    )

    # Save
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    mlmodel.save(str(OUTPUT_PATH))
    print(f"Saved to {OUTPUT_PATH}")

    # Print size
    import shutil
    size_mb = sum(
        f.stat().st_size for f in OUTPUT_PATH.rglob("*") if f.is_file()
    ) / (1024 * 1024)
    print(f"Model size: {size_mb:.1f} MB")


if __name__ == "__main__":
    main()
