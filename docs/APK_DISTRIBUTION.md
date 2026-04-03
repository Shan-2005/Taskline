# APK Distribution and Update Setup

This project supports APK-only distribution using GitHub Releases.

## 1) Configure app-side update source

Add these values to `~/.gradle/gradle.properties` (or project `gradle.properties`):

```
APK_UPDATE_REPO_OWNER=your-github-username-or-org
APK_UPDATE_REPO_NAME=Taskline
APK_UPDATE_ASSET_PREFIX=Taskline-vc
```

The app checks GitHub latest release API and looks for APK assets with names like:

```
Taskline-vc123.apk
```

`123` is treated as the remote versionCode.

## 2) Configure GitHub repository secrets

In GitHub repo settings, add:

- `ANDROID_KEYSTORE_BASE64`: base64 text of your keystore file
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

To generate base64 (PowerShell):

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("app\release.jks"))
```

If your keystore is not at `app/release.jks`, use the helper script:

```powershell
.\scripts\generate-github-secrets.ps1 -KeystorePath "D:\path\to\release.jks" -KeyAlias "your_alias"
```

It generates all four values in `github-secrets.txt`:

- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

## 3) Publish a release APK

Option A: Push a tag:

```bash
git tag v1.0.1
git push origin v1.0.1
```

Option B: Run workflow manually and provide:

- `tag_name`: example `v1.0.1`
- `version_code`: integer higher than all previous builds

The workflow creates a GitHub Release and uploads an APK asset named `Taskline-vc<version_code>.apk`.

## 4) How users update

- App dashboard shows an "Update available" card when a higher `versionCode` exists in latest release.
- Tapping "Download APK" opens the release asset URL.
- User installs APK manually (unknown sources permission may be required).

## Notes

- This is not silent auto-update; Android sideload updates always require user install confirmation.
- Keep `version_code` strictly increasing for every release.
