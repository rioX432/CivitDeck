#!/usr/bin/env python3
"""Check generated Android identities and update eligibility, without signing/installing APKs."""
import argparse
from pathlib import Path
import re

RELEASE_ID = "com.riox432.civitdeck"
VARIANTS = {
    "githubFull/debug": (RELEASE_ID + ".debug", False),
    "githubFull/release": (RELEASE_ID, True),
    "fdroid/debug": (RELEASE_ID + ".debug", False),
    "fdroid/release": (RELEASE_ID, False),
}


def check_variant(source: str, application_id: str, self_update: bool) -> None:
    """Read AGP's resolved values, rather than guessing flavor/build-type precedence."""
    expected = {
        "APPLICATION_ID": f'"{application_id}"',
        "FEATURE_SELF_UPDATE": str(self_update).lower(),
    }
    for field, value in expected.items():
        matches = re.findall(rf"\b{field}\s*=\s*([^;]+);", source)
        if matches != [value]:
            raise ValueError(f"{field}: expected {value}, got {matches}")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=Path("androidApp/build/generated/source/buildConfig"))
    args = parser.parse_args()
    for variant, (application_id, self_update) in VARIANTS.items():
        files = list((args.root / variant).rglob("BuildConfig.java"))
        if len(files) != 1:
            raise ValueError(f"{variant}: expected one generated BuildConfig.java, got {len(files)}")
        check_variant(files[0].read_text(), application_id, self_update)
        print(f"PASS {variant}: {application_id}, self-update={self_update}")


if __name__ == "__main__":
    main()
