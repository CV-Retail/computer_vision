"""Domain data structures for person detection."""

from dataclasses import dataclass


@dataclass(frozen=True)
class BoundingBox:
    """Normalized bounding box coordinates in range [0.0, 1.0]."""

    x1: float
    y1: float
    x2: float
    y2: float

    def __post_init__(self) -> None:
        if not (0.0 <= self.x1 <= 1.0 and 0.0 <= self.y1 <= 1.0):
            raise ValueError(f"Coordinates must be in [0.0, 1.0], got x1={self.x1}, y1={self.y1}")
        if not (0.0 <= self.x2 <= 1.0 and 0.0 <= self.y2 <= 1.0):
            raise ValueError(f"Coordinates must be in [0.0, 1.0], got x2={self.x2}, y2={self.y2}")
        if self.x2 < self.x1 or self.y2 < self.y1:
            raise ValueError(
                f"Invalid dimensions: x2 ({self.x2}) < x1 ({self.x1}) "
                f"or y2 ({self.y2}) < y1 ({self.y1})"
            )

    @property
    def width(self) -> float:
        return self.x2 - self.x1

    @property
    def height(self) -> float:
        return self.y2 - self.y1

    @property
    def area(self) -> float:
        return self.width * self.height

    def to_pixels(self, frame_width: int, frame_height: int) -> tuple[int, int, int, int]:
        """Convert normalized coordinates to absolute integer pixel bounds (px1, py1, px2, py2)."""
        if frame_width <= 0 or frame_height <= 0:
            raise ValueError(
                f"Dimensions must be positive, got width={frame_width}, height={frame_height}"
            )
        px1 = int(round(self.x1 * frame_width))
        py1 = int(round(self.y1 * frame_height))
        px2 = int(round(self.x2 * frame_width))
        py2 = int(round(self.y2 * frame_height))
        return (px1, py1, px2, py2)

    @classmethod
    def from_pixels(
        cls, px1: int, py1: int, px2: int, py2: int, frame_width: int, frame_height: int
    ) -> "BoundingBox":
        """Factory creating BoundingBox from pixel coordinates."""
        if frame_width <= 0 or frame_height <= 0:
            raise ValueError(
                f"Dimensions must be positive, got width={frame_width}, height={frame_height}"
            )
        if px2 < px1 or py2 < py1:
            raise ValueError(
                f"Invalid pixel bounds: px2 ({px2}) < px1 ({px1}) or py2 ({py2}) < py1 ({py1})"
            )
        return cls(
            x1=max(0.0, min(1.0, px1 / frame_width)),
            y1=max(0.0, min(1.0, py1 / frame_height)),
            x2=max(0.0, min(1.0, px2 / frame_width)),
            y2=max(0.0, min(1.0, py2 / frame_height)),
        )


@dataclass(frozen=True)
class PersonDetection:
    """Individual person detection with bounding box and confidence score."""

    box: BoundingBox
    confidence: float

    def __post_init__(self) -> None:
        if not (0.0 <= self.confidence <= 1.0):
            raise ValueError(f"Confidence must be in [0.0, 1.0], got {self.confidence}")


@dataclass(frozen=True)
class DetectionResult:
    """Aggregated output of a frame detection pass."""

    detections: list[PersonDetection]
    count: int
    latency_ms: float

    def __post_init__(self) -> None:
        if self.count < 0:
            raise ValueError(f"Count must be non-negative, got {self.count}")
        if self.latency_ms < 0.0:
            raise ValueError(f"Latency must be non-negative, got {self.latency_ms}")
        if len(self.detections) != self.count:
            raise ValueError(
                f"Count ({self.count}) does not match detections length ({len(self.detections)})"
            )
