"""Export pre-trained PyTorch / Torchvision detection model to ONNX.

Model: SSDLite320-MobileNetV3 (Torchvision, BSD-3-Clause license)
Dataset: COCO (Class 1 = person)
Input shape: (1, 3, 320, 320) normalized RGB float32 [0.0, 1.0]
Output tensors:
  - boxes: [N, 4] normalized or pixel bounding coordinates (x1, y1, x2, y2)
  - scores: [N] detection confidence scores in [0.0, 1.0]
  - labels: [N] integer class labels (1 = person)
"""

from __future__ import annotations

import argparse
import sys
from pathlib import Path


def export_ssdlite320(output_path: Path, opset_version: int = 14) -> Path:
    """Load SSDLite320-MobileNetV3 and export to ONNX format.

    Requires torch and torchvision (offline ML environment).
    """
    try:
        import torch
        from torchvision.models.detection import (
            SSDLite320_MobileNet_V3_Large_Weights,
            ssdlite320_mobilenet_v3_large,
        )
    except ImportError as err:
        raise RuntimeError(
            "PyTorch and torchvision are required in the ml/ export environment. "
            "Install with: pip install torch torchvision"
        ) from err

    output_path.parent.mkdir(parents=True, exist_ok=True)
    print(f"Loading pretrained SSDLite320-MobileNetV3 (COCO weights)...")
    weights = SSDLite320_MobileNet_V3_Large_Weights.DEFAULT
    model = ssdlite320_mobilenet_v3_large(weights=weights)
    model.eval()

    dummy_input = torch.randn(1, 3, 320, 320, dtype=torch.float32)

    print(f"Exporting ONNX model (opset {opset_version}) to {output_path}...")
    torch.onnx.export(
        model,
        dummy_input,
        str(output_path),
        export_params=True,
        opset_version=opset_version,
        do_constant_folding=True,
        input_names=["input"],
        output_names=["boxes", "scores", "labels"],
        dynamic_axes={
            "input": {0: "batch_size"},
            "boxes": {0: "num_detections"},
            "scores": {0: "num_detections"},
            "labels": {0: "num_detections"},
        },
    )

    try:
        import onnx

        print("Validating exported ONNX model structure...")
        onnx_model = onnx.load(str(output_path))
        onnx.checker.check_model(onnx_model)
        print("ONNX model structure is valid.")
    except ImportError:
        print("Note: 'onnx' package not found; skipped onnx.checker validation.")

    print(f"Export completed successfully: {output_path}")
    return output_path


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(
        description="Export pretrained Torchvision SSDLite320 person detector to ONNX."
    )
    parser.add_argument(
        "--output",
        "-o",
        type=Path,
        default=Path("ml/models/ssdlite320_mobilenet_v3_coco.onnx"),
        help="Target path for the exported .onnx model file",
    )
    parser.add_argument(
        "--opset",
        type=int,
        default=14,
        help="ONNX opset version (default: 14)",
    )
    args = parser.parse_args(argv)

    try:
        export_ssdlite320(output_path=args.output, opset_version=args.opset)
        return 0
    except Exception as exc:
        print(f"Error during export: {exc}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
