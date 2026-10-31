#!/usr/bin/env python3
"""
Utility script to delete old APK assets from GitHub Releases.
Usage:
    python3 scripts/cleanup_release_apks.py <GITHUB_TOKEN> [KEEP_COUNT]
    
Example:
    python3 scripts/cleanup_release_apks.py ghp_xxx 2
"""
import sys
import os
import urllib.request
import json

def main():
    token = sys.argv[1] if len(sys.argv) > 1 else os.environ.get("GITHUB_TOKEN")
    if not token:
        print("Error: GITHUB_TOKEN is required.")
        print("Usage: python3 scripts/cleanup_release_apks.py <GITHUB_TOKEN> [KEEP_COUNT]")
        sys.exit(1)

    keep_count = int(sys.argv[2]) if len(sys.argv) > 2 else 2
    repo = "aashikhameed/music-player-app"
    headers = {
        "Authorization": f"Bearer {token}",
        "Accept": "application/vnd.github+json",
        "X-GitHub-Api-Version": "2022-11-28"
    }

    print(f"Fetching releases for {repo}...")
    req = urllib.request.Request(f"https://api.github.com/repos/{repo}/releases?per_page=100", headers=headers)
    try:
        with urllib.request.urlopen(req) as resp:
            releases = json.loads(resp.read().decode("utf-8"))
    except Exception as e:
        print(f"Failed to fetch releases: {e}")
        sys.exit(1)

    print(f"Total releases found: {len(releases)}")
    print(f"Keeping APKs for the latest {keep_count} releases.")

    to_clean = releases[keep_count:]
    deleted = 0

    for r in to_clean:
        tag = r.get("tag_name")
        for asset in r.get("assets", []):
            name = asset.get("name", "")
            if name.endswith(".apk"):
                asset_id = asset["id"]
                del_req = urllib.request.Request(
                    f"https://api.github.com/repos/{repo}/releases/assets/{asset_id}",
                    headers=headers,
                    method="DELETE"
                )
                try:
                    urllib.request.urlopen(del_req)
                    print(f"  [DELETED] {name} from {tag} (id: {asset_id})")
                    deleted += 1
                except Exception as e:
                    print(f"  [FAILED] {name} from {tag}: {e}")

    print(f"\nDone! Cleaned up {deleted} old APK files from releases.")

if __name__ == "__main__":
    main()
