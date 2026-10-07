import importlib
import subprocess
import sys

import pytest

import kiosk_vision
from kiosk_vision.cli import SERVICE_NAME, main


def test_version_is_exposed():
    assert isinstance(kiosk_vision.__version__, str)
    assert kiosk_vision.__version__


def test_main_prints_name_and_version(capsys):
    assert main() == 0
    out = capsys.readouterr().out
    assert SERVICE_NAME in out
    assert kiosk_vision.__version__ in out


def test_module_entrypoint():
    result = subprocess.run(
        [sys.executable, "-m", "kiosk_vision"], capture_output=True, text=True, check=False
    )
    assert result.returncode == 0
    assert SERVICE_NAME in result.stdout


@pytest.mark.parametrize("name", ["cameras", "detection", "tracking", "attributes", "events"])
def test_reserved_subpackages_import(name):
    module = importlib.import_module(f"kiosk_vision.{name}")
    assert module.__doc__
