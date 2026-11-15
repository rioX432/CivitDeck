"""Offline fixtures that run scripts/package_unsigned_ipa.sh against synthetic device builds."""
import hashlib
import os
from pathlib import Path
import plistlib
import shutil
import stat
import subprocess
import tempfile
import unittest
import zipfile

SCRIPT = Path(__file__).parents[1] / "package_unsigned_ipa.sh"
EXECUTABLE_BYTES = b"\xcf\xfa\xed\xfe fake arm64 executable"


def device_info(**overrides):
    info = {
        "CFBundleExecutable": "CivitDeck",
        "CFBundleIdentifier": "com.riox432.civitdeck",
        "CFBundleShortVersionString": "2.5",
        "CFBundleVersion": "10",
        "CFBundleSupportedPlatforms": ["iPhoneOS"],
        "DTPlatformName": "iphoneos",
    }
    info.update(overrides)
    return {key: value for key, value in info.items() if value is not None}


def make_app(products, name="CivitDeck.app", info=None, executable=EXECUTABLE_BYTES):
    app = products / name
    (app / "Frameworks" / "Lib.framework" / "_CodeSignature").mkdir(parents=True)
    with open(app / "Info.plist", "wb") as handle:
        plistlib.dump(info or device_info(), handle, fmt=plistlib.FMT_BINARY)
    if executable is not None:
        binary = app / "CivitDeck"
        binary.write_bytes(executable)
        binary.chmod(0o755)
    (app / "Assets.car").write_bytes(b"assets")
    (app / "Assets.car").chmod(0o644)
    (app / "Frameworks" / "Lib.framework" / "Lib").write_bytes(b"lib")
    (app / "Frameworks" / "Lib.framework" / "_CodeSignature" / "CodeResources").write_bytes(b"sig")
    os.symlink("Lib", app / "Frameworks" / "Lib.framework" / "Current")
    (app / "_CodeSignature").mkdir()
    (app / "_CodeSignature" / "CodeResources").write_bytes(b"sig")
    (app / "embedded.mobileprovision").write_bytes(b"profile")
    return app


def tree_digest(root):
    digest = hashlib.sha256()
    for path in sorted(Path(root).rglob("*")):
        digest.update(str(path.relative_to(root)).encode())
        digest.update(oct(path.lstat().st_mode).encode())
        if path.is_symlink():
            digest.update(os.readlink(path).encode())
        elif path.is_file():
            digest.update(path.read_bytes())
    return digest.hexdigest()


class UnsignedIpaTests(unittest.TestCase):
    def setUp(self):
        self._tmp = tempfile.TemporaryDirectory(prefix="ipa fixture ")
        self.root = Path(self._tmp.name)
        self.products = self.root / "Build Products" / "Release-iphoneos"
        self.products.mkdir(parents=True)
        self.out_dir = self.root / "out dir"
        self.out_dir.mkdir()
        self.ipa = self.out_dir / "CivitDeck-0.0.0-ios-unsigned.ipa"

    def tearDown(self):
        self._tmp.cleanup()

    def run_script(self, *args, env=None, products=None, ipa=None, version="1.2.3", build="1.0.1"):
        argv = list(args) or [str(products or self.products), str(ipa or self.ipa), version, build]
        return subprocess.run(["bash", str(SCRIPT), *argv], capture_output=True, text=True,
                              env=env, check=False)

    def assert_rejected(self, result, message):
        self.assertNotEqual(result.returncode, 0, result.stdout)
        self.assertIn(message, result.stderr)
        self.assertEqual(sorted(p.name for p in self.out_dir.iterdir()), [],
                         "no output or staging may remain after a failure")

    def test_packages_device_app_with_stamped_versions_and_checksum(self):
        app = make_app(self.products)
        before = tree_digest(app)

        result = self.run_script()

        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(tree_digest(app), before, "the build output must stay untouched")
        self.assertEqual(sorted(p.name for p in self.out_dir.iterdir()),
                         [self.ipa.name, self.ipa.name + ".sha256"])
        with zipfile.ZipFile(self.ipa) as archive:
            names = archive.namelist()
            apps = {n.split("/")[1] for n in names if n.startswith("Payload/") and n.count("/") >= 2}
            self.assertEqual(apps, {"CivitDeck.app"})
            info = plistlib.loads(archive.read("Payload/CivitDeck.app/Info.plist"))
            self.assertEqual(info["CFBundleShortVersionString"], "1.2.3")
            self.assertEqual(info["CFBundleVersion"], "1.0.1")
            self.assertEqual(info["CFBundleIdentifier"], "com.riox432.civitdeck")
            self.assertEqual(archive.read("Payload/CivitDeck.app/CivitDeck"), EXECUTABLE_BYTES)
            mode = archive.getinfo("Payload/CivitDeck.app/CivitDeck").external_attr >> 16
            self.assertEqual(stat.S_IMODE(mode), 0o755)
            asset_mode = archive.getinfo("Payload/CivitDeck.app/Assets.car").external_attr >> 16
            self.assertEqual(stat.S_IMODE(asset_mode), 0o644)
            link = archive.getinfo("Payload/CivitDeck.app/Frameworks/Lib.framework/Current")
            self.assertTrue(stat.S_ISLNK(link.external_attr >> 16))
            self.assertEqual(archive.read(link), b"Lib")
            self.assertFalse([n for n in names if "_CodeSignature" in n or "mobileprovision" in n])

        sidecar = (self.out_dir / (self.ipa.name + ".sha256")).read_text()
        expected = hashlib.sha256(self.ipa.read_bytes()).hexdigest()
        self.assertEqual(sidecar, f"{expected}  {self.ipa.name}\n")
        check = subprocess.run(["shasum", "-a", "256", "-c", self.ipa.name + ".sha256"],
                               cwd=self.out_dir, capture_output=True, text=True, check=False)
        self.assertEqual(check.returncode, 0, check.stderr)
        self.assertIn(f"{self.ipa.name}: OK", check.stdout)

    def test_preserves_xml_plist_format(self):
        app = self.products / "CivitDeck.app"
        make_app(self.products)
        with open(app / "Info.plist", "wb") as handle:
            plistlib.dump(device_info(), handle, fmt=plistlib.FMT_XML)

        result = self.run_script()

        self.assertEqual(result.returncode, 0, result.stderr)
        with zipfile.ZipFile(self.ipa) as archive:
            self.assertTrue(archive.read("Payload/CivitDeck.app/Info.plist").startswith(b"<?xml"))

    def test_rejects_missing_app(self):
        self.assert_rejected(self.run_script(), "found 0")

    def test_rejects_multiple_apps(self):
        make_app(self.products)
        make_app(self.products, name="Other.app")
        self.assert_rejected(self.run_script(), "found 2")

    def test_rejects_simulator_build(self):
        make_app(self.products, info=device_info(CFBundleSupportedPlatforms=["iPhoneSimulator"],
                                                 DTPlatformName="iphonesimulator"))
        self.assert_rejected(self.run_script(), "CFBundleSupportedPlatforms")

    def test_rejects_simulator_platform_name(self):
        make_app(self.products, info=device_info(DTPlatformName="iphonesimulator"))
        self.assert_rejected(self.run_script(), "DTPlatformName")

    def test_rejects_absent_executable_key_and_file(self):
        make_app(self.products, info=device_info(CFBundleExecutable=None))
        self.assert_rejected(self.run_script(), "CFBundleExecutable is missing")
        shutil.rmtree(self.products / "CivitDeck.app")
        make_app(self.products, executable=None)
        self.assert_rejected(self.run_script(), "executable not found")

    def test_rejects_empty_executable(self):
        make_app(self.products, executable=b"")
        self.assert_rejected(self.run_script(), "executable is empty")

    def test_rejects_non_mach_o_executable(self):
        make_app(self.products, executable=b"#!/bin/sh\necho not an app\n")
        self.assert_rejected(self.run_script(), "not a Mach-O binary")

    def test_rejects_escaping_executable(self):
        for name in ("../CivitDeck", "Frameworks/Lib.framework/Lib", "..", ""):
            with self.subTest(name=name):
                app = make_app(self.products, info=device_info(CFBundleExecutable=name))
                self.assert_rejected(self.run_script(), "CFBundleExecutable")
                shutil.rmtree(app)

    def test_rejects_symlinked_executable(self):
        outside = self.root / "outside"
        outside.write_bytes(EXECUTABLE_BYTES)
        app = make_app(self.products, executable=None)
        os.symlink(outside, app / "CivitDeck")
        self.assert_rejected(self.run_script(), "must not be a symlink")

    def test_rejects_malformed_version_and_build(self):
        make_app(self.products)
        cases = [("1.2", "1.0.1"), ("v1.2.3", "1.0.1"), ("1.2.3-beta", "1.0.1"), ("01.2.3", "1.0.1"),
                 ("1.2.3", "1.0"), ("1.2.3", "1.0.1.4"), ("1.2.3", "1.a.1"), ("1.2.3", ""),
                 ("1.2.3", "1234567890.0.1")]
        for version, build in cases:
            with self.subTest(version=version, build=build):
                self.assert_rejected(self.run_script(version=version, build=build), "must be")

    def test_rejects_output_collisions(self):
        make_app(self.products)
        for existing in (self.ipa, Path(str(self.ipa) + ".sha256")):
            with self.subTest(existing=existing.name):
                existing.write_bytes(b"keep")
                result = self.run_script()
                self.assertNotEqual(result.returncode, 0)
                self.assertIn("refusing to overwrite", result.stderr)
                self.assertEqual(existing.read_bytes(), b"keep")
                self.assertEqual([p.name for p in self.out_dir.iterdir()], [existing.name])
                existing.unlink()

    def test_rejects_non_ipa_output_and_wrong_arity(self):
        make_app(self.products)
        self.assert_rejected(self.run_script(ipa=self.out_dir / "app.zip"), "must end with .ipa")
        self.assert_rejected(self.run_script(str(self.products)), "usage")

    def test_cleans_staging_when_a_late_step_fails(self):
        make_app(self.products)
        fake_bin = self.root / "fake bin"
        fake_bin.mkdir()
        fake = fake_bin / "shasum"
        fake.write_text("#!/bin/sh\nexit 3\n")
        fake.chmod(0o755)
        env = dict(os.environ, PATH=f"{fake_bin}{os.pathsep}{os.environ['PATH']}")

        self.assert_rejected(self.run_script(env=env), "checksum failed")


if __name__ == "__main__":
    unittest.main()
