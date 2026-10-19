# Machine Learning Workspace (`ml/`)

This directory contains scripts for training, fine-tuning, evaluating, and exporting neural network models for the smart kiosk project.

Per the architectural guidelines in `AGENTS.md` and `docs/kiosko-arquitectura-mvp.md`:
- `ml/` is an offline development environment with PyTorch and torchvision.
- Production services (`services/vision-service`) **do not** include PyTorch in their runtime images. Instead, models are exported to `.onnx` and loaded using `onnxruntime` or `cv2.dnn` on CPU.

## Person Detector Export (KIO-12)

The primary person detection architecture is **SSDLite320-MobileNetV3** pre-trained on COCO from torchvision.

### Licensing
- Architecture & implementation: PyTorch / Torchvision (BSD-3-Clause).
- Pretrained weights: Trained on MS COCO dataset (Creative Commons Attribution 4.0 International).
- Ultralytics YOLO models (YOLOv5, YOLOv8, YOLOv11) are licensed under AGPL-3.0 and are **strictly prohibited** in this project.

### Exporting to ONNX

In your local Python environment with `torch` and `torchvision` installed:

```bash
pip install torch torchvision onnx
python ml/export_person_detector.py --output services/vision-service/src/kiosk_vision/detection/models/ssdlite320_person.onnx
```

### Tensor Specifications
- **Input:** `input` tensor with shape `(batch_size, 3, 320, 320)`, type `float32`, RGB order normalized to `[0.0, 1.0]`.
- **Outputs:**
  - `boxes`: `(num_detections, 4)` in `(x1, y1, x2, y2)` pixel or normalized coordinates.
  - `scores`: `(num_detections,)` float32 confidence scores in `[0.0, 1.0]`.
  - `labels`: `(num_detections,)` integer class labels. Label `1` corresponds to `person` in COCO.
