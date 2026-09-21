#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Nuvexa Drive contributors
# SPDX-License-Identifier: AGPL-3.0-or-later

from __future__ import annotations

import base64
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "app"


def replace_exact(path: Path, old: str, new: str, minimum: int = 1) -> None:
    text = path.read_text(encoding="utf-8")
    count = text.count(old)
    if count < minimum:
        raise RuntimeError(f"{path}: expected at least {minimum} occurrence(s) of {old!r}, found {count}")
    path.write_text(text.replace(old, new), encoding="utf-8")


def write(path: Path, text: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding="utf-8")


def decode_brand_asset() -> Path:
    source = ROOT / "branding" / "nuvexa-logo-128.jpg.b64"
    target = APP / "src/main/res/drawable-nodpi/nuvexa_logo.jpg"
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_bytes(base64.b64decode(source.read_text(encoding="ascii").strip()))
    write(
        ROOT / "branding" / "Nuvexa-Brand-LICENSE.txt",
        """SPDX-FileCopyrightText: 2026 Nuvexa Drive
SPDX-License-Identifier: LicenseRef-Nuvexa-Brand
""",
    )
    return target


def generate_launcher_assets(brand_source: Path) -> None:
    from PIL import Image

    image = Image.open(brand_source).convert("RGBA")
    sizes = {
        "mipmap-mdpi": 48,
        "mipmap-hdpi": 72,
        "mipmap-xhdpi": 96,
        "mipmap-xxhdpi": 144,
        "mipmap-xxxhdpi": 192,
    }
    for folder_name, size in sizes.items():
        folder = APP / "src/main/res" / folder_name
        folder.mkdir(parents=True, exist_ok=True)
        image.resize((size, size), Image.Resampling.LANCZOS).save(
            folder / "ic_launcher.png", format="PNG", optimize=True
        )

    for png in APP.glob("src/**/ic_launcher*.png"):
        normalized = str(png).replace("\\", "/")
        if "/main/res/mipmap-" in normalized:
            continue
        try:
            existing = Image.open(png)
            size = max(existing.size)
        except Exception:
            size = 512
        image.resize((size, size), Image.Resampling.LANCZOS).save(
            png, format="PNG", optimize=True
        )

    for png in ROOT.glob("**/fastlane/**/images/icon.png"):
        image.resize((512, 512), Image.Resampling.LANCZOS).save(
            png, format="PNG", optimize=True
        )

    write(
        APP / "src/main/res/drawable/ic_launcher_foreground.xml",
        """<?xml version="1.0" encoding="utf-8"?>
<!-- SPDX-FileCopyrightText: 2026 Nuvexa Drive -->
<!-- SPDX-License-Identifier: LicenseRef-Nuvexa-Brand -->
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <item
        android:width="92dp"
        android:height="92dp"
        android:gravity="center">
        <bitmap
            android:gravity="fill"
            android:src="@drawable/nuvexa_logo" />
    </item>
</layer-list>
""",
    )
    write(
        APP / "src/main/res/drawable/ic_launcher_background.xml",
        """<?xml version="1.0" encoding="utf-8"?>
<!-- SPDX-FileCopyrightText: 2026 Nuvexa Drive contributors -->
<!-- SPDX-License-Identifier: AGPL-3.0-or-later -->
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <solid android:color="#081421" />
</shape>
""",
    )
    write(
        APP / "src/main/res/drawable/nuvexa_monochrome.xml",
        """<?xml version="1.0" encoding="utf-8"?>
<!-- SPDX-FileCopyrightText: 2026 Nuvexa Drive -->
<!-- SPDX-License-Identifier: LicenseRef-Nuvexa-Brand -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path
        android:fillColor="#FFFFFFFF"
        android:pathData="M30,82 L30,26 L41,26 L67,64 L67,26 L78,26 L78,82 L67,82 L41,44 L41,82 Z" />
</vector>
""",
    )
    adaptive = APP / "src/main/res/mipmap-anydpi/ic_launcher.xml"
    if adaptive.exists():
        text = adaptive.read_text(encoding="utf-8")
        text = text.replace(
            '<monochrome android:drawable="@drawable/ic_launcher_foreground"/>',
            '<monochrome android:drawable="@drawable/nuvexa_monochrome"/>',
        ).replace(
            '<monochrome android:drawable="@drawable/ic_launcher_foreground" />',
            '<monochrome android:drawable="@drawable/nuvexa_monochrome" />',
        )
        adaptive.write_text(text, encoding="utf-8")

    write(
        APP / "src/main/res/drawable/nextcloud_logo.xml",
        """<?xml version="1.0" encoding="utf-8"?>
<!-- SPDX-FileCopyrightText: 2026 Nuvexa Drive -->
<!-- SPDX-License-Identifier: LicenseRef-Nuvexa-Brand -->
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:gravity="center">
        <bitmap
            android:gravity="center"
            android:src="@drawable/nuvexa_logo" />
    </item>
</layer-list>
""",
    )


def patch_gradle() -> None:
    path = APP / "build.gradle.kts"
    replace_exact(path, "val versionMajor = 35", "val versionMajor = 1")
    replace_exact(
        path,
        'applicationId = "com.nextcloud.client"',
        'applicationId = "com.nuvexa.drive"',
        minimum=3,
    )
    replace_exact(
        path,
        'applicationId = "com.nextcloud.android.beta"',
        'applicationId = "com.nuvexa.drive.beta"',
    )
    replace_exact(
        path,
        'applicationId = "com.nextcloud.android.qa"',
        'applicationId = "com.nuvexa.drive.qa"',
    )
    replace_exact(
        ROOT / "settings.gradle.kts",
        'rootProject.name = "Nextcloud"',
        'rootProject.name = "Nuvexa Drive"',
    )


def patch_setup() -> None:
    path = APP / "src/main/res/values/setup.xml"
    replacements = {
        '<string name="app_name">Nextcloud</string>':
            '<string name="app_name">Nuvexa Drive</string>',
        '<string name="account_type">nextcloud</string>':
            '<string name="account_type">com.nuvexa.drive</string>',
        '<string name="authority">org.nextcloud</string>':
            '<string name="authority">com.nuvexa.drive.provider</string>',
        '<string name="users_and_groups_search_authority">com.nextcloud.android.providers.UsersAndGroupsSearchProvider</string>':
            '<string name="users_and_groups_search_authority">com.nuvexa.drive.providers.UsersAndGroupsSearchProvider</string>',
        '<string name="users_and_groups_share_with">com.nextcloud.android.providers.UsersAndGroupsSearchProvider.action.SHARE_WITH</string>':
            '<string name="users_and_groups_share_with">com.nuvexa.drive.providers.UsersAndGroupsSearchProvider.action.SHARE_WITH</string>',
        '<string name="document_provider_authority">org.nextcloud.documents</string>':
            '<string name="document_provider_authority">com.nuvexa.drive.documents</string>',
        '<string name="file_provider_authority">org.nextcloud.files</string>':
            '<string name="file_provider_authority">com.nuvexa.drive.files</string>',
        '<string name="image_cache_provider_authority">org.nextcloud.imageCache.provider</string>':
            '<string name="image_cache_provider_authority">com.nuvexa.drive.imagecache.provider</string>',
        '<string name="db_file">nextcloud.db</string>':
            '<string name="db_file">nuvexa.db</string>',
        '<string name="db_name">nextcloud</string>':
            '<string name="db_name">nuvexa</string>',
        '<string name="data_folder">nextcloud</string>':
            '<string name="data_folder">nuvexa</string>',
        '<string name="default_display_name_for_root_folder">Nextcloud</string>':
            '<string name="default_display_name_for_root_folder">Nuvexa Drive</string>',
        '<string name="nextcloud_user_agent">Mozilla/5.0 (Android) Nextcloud-android/%1$s%2$s</string>':
            '<string name="nextcloud_user_agent">Mozilla/5.0 (Android) Nuvexa-Drive/%1$s%2$s</string>',
        '<string name="office_user_agent">Mozilla/5.0 (Android %1$s) Mobile Nextcloud-android/%2$s</string>':
            '<string name="office_user_agent">Mozilla/5.0 (Android %1$s) Mobile Nuvexa-Drive/%2$s</string>',
        '<string name="name_for_branded_user_agent"></string>':
            '<string name="name_for_branded_user_agent">Nuvexa Drive</string>',
        '<bool name="help_enabled">true</bool>':
            '<bool name="help_enabled">false</bool>',
        '<bool name="privacy_enabled">true</bool>':
            '<bool name="privacy_enabled">false</bool>',
        '<bool name="recommend_enabled">true</bool>':
            '<bool name="recommend_enabled">false</bool>',
        '<bool name="participate_enabled">true</bool>':
            '<bool name="participate_enabled">false</bool>',
        '<string name="sourcecode_url" translatable="false">https://github.com/nextcloud/android</string>':
            '<string name="sourcecode_url" translatable="false">https://github.com/MukaSanches/Nuvexa-Drive</string>',
        '<string name="url_app_download">"https://play.google.com/store/apps/details?id=com.nextcloud.client"</string>':
            '<string name="url_app_download">https://github.com/MukaSanches/Nuvexa-Drive/releases</string>',
        '<string name="translation_link" translatable="false">https://app.transifex.com/nextcloud/nextcloud/android/</string>':
            '<string name="translation_link" translatable="false">https://github.com/MukaSanches/Nuvexa-Drive</string>',
        '<string name="report_issue_link" translatable="false">https://github.com/nextcloud/android/issues/new?template=bug_report.yml</string>':
            '<string name="report_issue_link" translatable="false">https://github.com/MukaSanches/Nuvexa-Drive/issues/new</string>',
        '<string name="report_issue_empty_link" translatable="false">https://github.com/nextcloud/android/issues/new/choose</string>':
            '<string name="report_issue_empty_link" translatable="false">https://github.com/MukaSanches/Nuvexa-Drive/issues/new/choose</string>',
        '<string name="login_data_own_scheme" translatable="false">nc</string>':
            '<string name="login_data_own_scheme" translatable="false">nuvexa</string>',
        '<color name="primary">#0082c9</color>':
            '<color name="primary">#0B5FFF</color>',
        '<color name="primary_dark">#006AA3</color>':
            '<color name="primary_dark">#081421</color>',
        '<color name="color_accent">#007cc2</color>':
            '<color name="color_accent">#377DFF</color>',
    }
    for old, new in replacements.items():
        replace_exact(path, old, new)


def write_docs() -> None:
    write(
        ROOT / "README.md",
        """# Nuvexa Drive

Nuvexa Drive 1.0.0 is an Android cloud, files, photos, media, backup and sync client built from the stable Nextcloud Android 35.0.0 codebase and evolved as an independent product.

## Current baseline

This repository intentionally imports the complete functional Android baseline instead of rebuilding mature WebDAV/account/sharing behavior from scratch.

Current baseline includes file browsing, upload/download, folders, search, favorites, recent files, sharing, offline files, auto-upload, albums, Media3 playback, tags, E2EE support, multiple accounts, Android document/share integration, passcode/device credentials, WorkManager/background transfer infrastructure and the upstream locale catalog.

## Nuvexa 1.0 engineering priorities

1. Data integrity and reliable transfer recovery.
2. Simple, professional UI without button/card overload.
3. Automatic Android locale selection.
4. Light/dark/adaptive Android layouts.
5. Signed, reproducible release artifacts.
6. Continuous regression testing of known cloud-client failure modes.

## Reliability contract

Nuvexa must never silently lose, corrupt, endlessly retransmit or duplicate a file because of network loss, process death or restart. Every Nuvexa-specific reliability change must be covered by an appropriate regression test before it is described as complete.

See RELIABILITY.md and TESTING.md.

## Build

The project currently targets Android API 36 and is built with Java 21. See BUILDING.md.

## Source and licensing

Nuvexa Drive is based on Nextcloud Android. Original copyright/SPDX notices and applicable GPL/AGPL obligations are retained. Nuvexa branding assets are separate brand assets and are not Nextcloud trademarks.

See LICENSE.txt, LICENSES/, REUSE.toml and THIRD_PARTY_NOTICES.md.

## Status

The repository is in the Nuvexa 1.0 hardening phase. Documentation distinguishes implemented inherited functionality from Nuvexa-specific work still requiring validation; a feature is not considered done merely because it compiles.
""",
    )

    write(
        ROOT / "CHANGELOG.md",
        """# Changelog

## 1.0.0 - development

### Base
- Imported stable Nextcloud Android 35.0.0 as the initial functional baseline.
- Set independent Nuvexa Drive application identity and provider authorities.
- Preserved automatic Android locale resources and RTL support.
- Added Nuvexa branding assets and independent source/issue links.

### Quality
- Added Nuvexa build/validation documentation.
- Added CI bootstrap/build workflow.
- Reliability hardening items remain gated by regression tests and are not marked complete until validated.
""",
    )

    write(
        ROOT / "BUILDING.md",
        """# Building Nuvexa Drive

## Requirements
- JDK 21
- Android SDK with API 37 compile platform
- Android build tools
- Gradle wrapper from this repository

## Generic GitHub build

    ./gradlew :app:assembleGenericRelease --stacktrace --no-daemon

The generic release APK produced by Gradle is intentionally unsigned unless a secure release-signing environment is supplied. A private keystore must never be committed to this public repository.

Target SDK is 36.
""",
    )

    write(
        ROOT / "ARCHITECTURE.md",
        """# Nuvexa Architecture

The initial release preserves the mature Nextcloud Android architecture and adds Nuvexa changes incrementally.

Nuvexa-specific boundaries:

- NUVEXA CORE - accounts, server/WebDAV and metadata.
- NUVEXA SYNC - persistent queue, transfer identity, verification, recovery and offline journal.
- NUVEXA PHOTOS - timeline, albums and viewer.
- NUVEXA MEDIA - playback isolated from transfer state.
- NUVEXA DIRECT / CONNECT - future device-to-device capabilities.
- NUVEXA SECURITY - credentials, biometric/app lock and future vault.
- NUVEXA STORAGE - cache, storage analysis and exact-duplicate detection.

No module is allowed to weaken E2EE or data-integrity guarantees to simplify UI work.
""",
    )

    write(
        ROOT / "RELIABILITY.md",
        """# Nuvexa Reliability Contract

Target Reliability Engine:

Persistent Queue -> Transfer Identity -> Network Guard -> Chunk/Resume -> Server Verification -> Commit -> Recovery.

Mandatory invariants:
- UI/activity lifetime cannot be the source of truth for transfer completion.
- Before retrying a failed completion response, verify whether the remote object already exists and is complete.
- Do not mark a non-empty source as successfully uploaded to a zero-byte remote object.
- Wi-Fi-only policy is re-evaluated during transfers, not only before they start.
- Retry is bounded with backoff/circuit-breaker behavior.
- Partial downloads remain temporary until verification and atomic completion.
- Startup/reboot recovery inspects interrupted work rather than silently abandoning it.
- Reinstall/re-login reconciliation must prevent blind re-upload of an existing library.

These items are requirements. Individual mechanisms are only marked implemented after code and regression tests exist.
""",
    )

    write(
        ROOT / "TESTING.md",
        """# Nuvexa Test Matrix

Release candidates must cover, as infrastructure permits:
- Samsung / One UI, Xiaomi/Redmi/POCO, Motorola and Pixel.
- Wi-Fi to mobile-data handoff and Wi-Fi-only enforcement.
- Airplane mode and unstable networks.
- App/process death and device reboot during transfer.
- Server offline/recovery and response loss after upload commit.
- Zero-byte and duplicate-name conflict scenarios.
- Large uploads/downloads including resumable cases.
- Full local/server storage.
- Reinstall/re-login reconciliation.
- Remote-only file open, media playback and share sheet.
- Light/dark mode, long translated strings and RTL.
- Release APK installation and smoke test, not only debug builds.

A failing critical test blocks an approved release.
""",
    )

    write(
        ROOT / "SIGNING.md",
        """# Release Signing

Nuvexa production releases use one stable signing identity.

Rules:
- The private keystore and its password are never committed to this repository.
- CI may receive signing material only through an authorized secret store.
- Release tooling must verify the expected signing certificate fingerprint.
- A release must not be labelled signed unless the final APK has been verified.
- Google Play builds must follow Play App Signing requirements.
- Direct GitHub builds may use the same authorized signing identity but must not bypass Android package-installer security.
""",
    )

    write(
        ROOT / "PRIVACY.md",
        """# Privacy principles

Nuvexa Drive is designed around data minimization. The application must not add advertising trackers or silently upload private file contents for analytics. Credentials and tokens must not be logged. Server-owning users must not be forced into an unrelated central Nuvexa account merely to access their own server.
""",
    )

    write(
        ROOT / "THIRD_PARTY_NOTICES.md",
        """# Third-party notices

Nuvexa Drive is derived from the Nextcloud Android client and retains the upstream licensing files, SPDX notices and third-party dependency declarations.

The Nuvexa name and Nuvexa visual assets are not Nextcloud assets. Upstream Nextcloud trademarks must not be presented as Nuvexa branding.

For authoritative per-file licensing, consult the retained SPDX headers, LICENSE.txt, LICENSES/ and REUSE.toml.
""",
    )

    write(
        ROOT / "NUVEXA_V1_VALIDATION.md",
        """# Nuvexa Drive 1.0 validation

Status: IN PROGRESS

A build is not considered an approved Nuvexa 1.0 release solely because compilation succeeds.

## Baseline
- Upstream: Nextcloud Android 35.0.0
- Nuvexa version: 1.0.0
- Target SDK: 36
- Compile SDK: 37

## Required before final approval
- [ ] CI build passes
- [ ] release APK installed and smoke-tested
- [ ] signing certificate verified
- [ ] upload/download regression suite
- [ ] background/reboot recovery
- [ ] network-policy transitions
- [ ] zero-byte protection
- [ ] duplicate/conflict regression
- [ ] large-file resume
- [ ] reinstall reconciliation
- [ ] locale/RTL visual checks
- [ ] license/trademark audit
- [ ] no known P0/P1 release blocker

Unchecked items are not claimed as complete.
""",
    )


def write_brand_manifest() -> None:
    write(
        ROOT / "branding/README.md",
        """# Nuvexa brand assets

The encoded JPEG in this directory is an optimized build copy of the official Nuvexa logo selected for the project. The higher-resolution source is maintained separately from the public-source bootstrap asset.

SPDX-License-Identifier: LicenseRef-Nuvexa-Brand
""",
    )


def main() -> None:
    patch_gradle()
    patch_setup()
    brand = decode_brand_asset()
    generate_launcher_assets(brand)
    write_docs()
    write_brand_manifest()
    (ROOT / ".nuvexa").mkdir(exist_ok=True)
    (ROOT / ".nuvexa/bootstrap-complete").write_text(
        "Nuvexa Drive 1.0 bootstrap applied from Nextcloud Android 35.0.0\n",
        encoding="utf-8",
    )
    print("Nuvexa bootstrap patches applied successfully.")


if __name__ == "__main__":
    main()
