# ido Browser (Android starter)

This is a beginner-friendly native Android starter project for **ido Browser**.

## Included in this first build
- Android WebView browser
- Address/search bar
- Back/forward/refresh
- Download listener for normal downloadable HTTP/HTTPS files
- Uses Android Download Manager for notifications and saved files
- Basic URL-based ad/tracker blocking
- Bookmark saving
- Desktop user-agent toggle
- Basic page color inversion toggle
- Basic private-mode toggle (not a full privacy guarantee)
- GitHub Actions workflow that builds a debug APK

## Not included yet (do not assume these work)
- Independent multiple tabs (the button currently opens a fresh home page in the current view)
- Reliable media sniffing from every website
- HLS/DASH stream downloading
- DRM-protected video downloading
- Blob URL extraction
- Multi-thread download engine
- Torrent engine
- Real VPN tunnel/provider
- Full download queue/file manager

These need additional implementation and testing. A VPN button alone is not a VPN; a real VPN needs a legitimate VPN provider/protocol. Some streaming websites block downloading or use DRM, and this app does not bypass those restrictions.

## Build APK on GitHub using an Android phone
1. Create a GitHub repository. The repository name can be `ido-browser` (hyphens work; spaces do not).
2. Upload all files/folders from this project to the repository, preserving the directory structure.
3. Open the repository's **Actions** tab and enable Actions if asked.
4. Select **Build ido Browser APK** on the left, tap **Run workflow**, then **Run workflow** again.
5. Wait for the workflow to finish with a green check.
6. Open that run, scroll to **Artifacts**, and download `idoBrowser-debug-apk`.
7. Extract the downloaded ZIP and install `app-debug.apk`. Android may ask permission to install unknown apps for the browser/file app you used.

If you upload files using GitHub's website, upload the contents of this folder (not the enclosing ZIP file) and preserve the folder structure. GitHub's web upload interface may not conveniently upload empty folders; this project has no required empty folders.

## App ID
`com.ido.browser`

## License
Personal starter project; review and add a license before public distribution.
