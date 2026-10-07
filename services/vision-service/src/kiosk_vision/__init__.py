"""Vision service of the smart kiosk."""

from importlib import metadata

try:
    __version__ = metadata.version("kiosk-vision-service")
except metadata.PackageNotFoundError:  # pragma: no cover - only when not installed
    __version__ = "0+unknown"

__all__ = ["__version__"]
