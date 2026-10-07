"""Command line entrypoint of the vision service."""

from kiosk_vision import __version__

SERVICE_NAME = "kiosk-vision-service"


def main(argv: list[str] | None = None) -> int:
    """Print the service name and version.

    Later features replace this with the capture and processing loop.
    """
    print(f"{SERVICE_NAME} {__version__}")
    return 0
