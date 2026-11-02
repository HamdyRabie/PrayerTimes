#!/usr/bin/env python3
"""Build a signed trial APK using the official Android SDK; no Gradle required.

Set ANDROID_HOME to an SDK containing platforms/android-36 and build-tools/36.0.0.
JAVA_HOME may point to the JDK bundled with Android Studio. Python 3.9+ required.
"""
import os
from pathlib import Path
import shutil
import subprocess
import sys
import zipfile
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
SDK = Path(os.environ.get('ANDROID_HOME', os.environ.get('ANDROID_SDK_ROOT', '')))
PLATFORM = Path(os.environ.get('PRAYER_ANDROID_JAR', str(SDK / 'platforms/android-36/android.jar')))
TOOLS = Path(os.environ.get('PRAYER_BUILD_TOOLS', str(SDK / 'build-tools/36.0.0')))
JAVA = str(Path(os.environ['JAVA_HOME']) / 'bin' / ('java.exe' if os.name == 'nt' else 'java')) if os.environ.get('JAVA_HOME') else 'java'
KEYTOOL = str(Path(JAVA).parent / ('keytool.exe' if os.name == 'nt' else 'keytool')) if os.environ.get('JAVA_HOME') else 'keytool'
BUILD = ROOT / 'build-manual'
OUT = ROOT / 'dist'

def run(args):
    subprocess.run([str(a) for a in args], check=True, cwd=ROOT)

def tool(name):
    path = TOOLS / (name + ('.exe' if os.name == 'nt' else ''))
    if not path.is_file(): raise SystemExit('Missing SDK tool: ' + str(path))
    return path

def main():
    if not PLATFORM.is_file(): raise SystemExit('Install Android SDK Platform 36, then set ANDROID_HOME.')
    if BUILD.exists(): shutil.rmtree(BUILD)
    for p in [BUILD / 'gen', BUILD / 'classes', BUILD / 'dex', OUT]: p.mkdir(parents=True, exist_ok=True)
    source = ROOT / 'app/src/main'
    lib = ROOT / 'app/libs/adhan-1.2.1.jar'
    ET.register_namespace('android', 'http://schemas.android.com/apk/res/android')
    manifest = ET.parse(source / 'AndroidManifest.xml')
    manifest.getroot().set('package', 'com.attech.prayertimes')
    manifest.write(BUILD / 'AndroidManifest.xml', encoding='utf-8', xml_declaration=True)
    run([tool('aapt2'), 'compile', '--dir', source / 'res', '-o', BUILD / 'resources.zip'])
    run([tool('aapt2'), 'link', '-o', BUILD / 'resources.apk', '-I', PLATFORM,
         '--manifest', BUILD / 'AndroidManifest.xml', '--java', BUILD / 'gen',
         '--min-sdk-version', '26', '--target-sdk-version', '36',
         '--version-code', '2', '--version-name', '1.1', '-0', 'mp3', '-0', 'wav', BUILD / 'resources.zip'])
    files = sorted((source / 'java').rglob('*.java')) + sorted((BUILD / 'gen').rglob('*.java'))
    run([JAVA, 'com.sun.tools.javac.Main', '-encoding', 'UTF-8', '--release', '8',
         '-classpath', str(PLATFORM) + os.pathsep + str(lib), '-d', BUILD / 'classes'] + files)
    classes = BUILD / 'classes.jar'
    with zipfile.ZipFile(classes, 'w') as z:
        for path in sorted((BUILD / 'classes').rglob('*.class')): z.write(path, path.relative_to(BUILD / 'classes').as_posix())
    run([JAVA, '-cp', TOOLS / 'lib/d8.jar', 'com.android.tools.r8.D8', '--min-api', '26',
         '--lib', PLATFORM, '--output', BUILD / 'dex', classes, lib])
    unsigned = BUILD / 'unsigned.apk'
    shutil.copy2(BUILD / 'resources.apk', unsigned)
    with zipfile.ZipFile(unsigned, 'a', zipfile.ZIP_DEFLATED) as z:
        for path in sorted((BUILD / 'dex').glob('*.dex')): z.write(path, path.name)
    run([tool('zipalign'), '-f', '-p', '4', unsigned, BUILD / 'aligned.apk'])
    key = ROOT / '.signing/debug.keystore'
    key.parent.mkdir(exist_ok=True)
    if not key.exists():
        run([KEYTOOL, '-genkeypair', '-keystore', key, '-storepass', 'android', '-keypass', 'android',
             '-alias', 'androiddebugkey', '-keyalg', 'RSA', '-keysize', '2048', '-validity', '10000',
             '-dname', 'CN=Prayer Times Trial, O=AT-Tech, C=EG'])
    apk = OUT / 'PrayerTimes_1.1.apk'
    run([JAVA, '-jar', TOOLS / 'lib/apksigner.jar', 'sign', '--ks', key, '--ks-key-alias', 'androiddebugkey',
         '--ks-pass', 'pass:android', '--key-pass', 'pass:android', '--out', apk, BUILD / 'aligned.apk'])
    run([JAVA, '-jar', TOOLS / 'lib/apksigner.jar', 'verify', '--verbose', apk])
    print('Created:', apk)

if __name__ == '__main__': main()
