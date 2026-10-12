"""Offline fixtures for the generated BuildConfig channel guard."""
import importlib.util
from pathlib import Path
import unittest

SPEC = importlib.util.spec_from_file_location(
    "channels", Path(__file__).parents[1] / "check_android_update_channels.py"
)
CHANNELS = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(CHANNELS)


def config(application_id, self_update):
    return (f'public static final String APPLICATION_ID = "{application_id}";\n'
            f'public static final boolean FEATURE_SELF_UPDATE = {str(self_update).lower()};\n')


class UpdateChannelTests(unittest.TestCase):
    def test_all_four_expected_variants(self):
        for application_id, enabled in CHANNELS.VARIANTS.values():
            with self.subTest(application_id=application_id, enabled=enabled):
                CHANNELS.check_variant(config(application_id, enabled), application_id, enabled)

    def test_rejects_previous_debug_release_update_offer(self):
        with self.assertRaisesRegex(ValueError, "FEATURE_SELF_UPDATE"):
            CHANNELS.check_variant(config(CHANNELS.RELEASE_ID + ".debug", True),
                                   CHANNELS.RELEASE_ID + ".debug", False)

    def test_rejects_release_identity_change(self):
        with self.assertRaisesRegex(ValueError, "APPLICATION_ID"):
            CHANNELS.check_variant(config(CHANNELS.RELEASE_ID + ".debug", True),
                                   CHANNELS.RELEASE_ID, True)

    def test_rejects_fdroid_self_update(self):
        with self.assertRaisesRegex(ValueError, "FEATURE_SELF_UPDATE"):
            CHANNELS.check_variant(config(CHANNELS.RELEASE_ID, True), CHANNELS.RELEASE_ID, False)

    def test_rejects_missing_or_duplicate_fields(self):
        for source in ("", config(CHANNELS.RELEASE_ID, True) * 2):
            with self.subTest(source=source), self.assertRaises(ValueError):
                CHANNELS.check_variant(source, CHANNELS.RELEASE_ID, True)


if __name__ == "__main__":
    unittest.main()
