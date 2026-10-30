import json
import os
import re
import sys
import time
from pathlib import Path

import requests

API = "https://api.modrinth.com/v2"
TOKEN = os.environ["MODRINTH_TOKEN"]
HEADERS = {"Authorization": f"Bearer {TOKEN}", "User-Agent": "ludvig2457/pvpkit-publisher/1.0"}
DRY_RUN = os.environ.get("DRY_RUN", "false").lower() == "true"
TAG = os.environ.get("RELEASE_TAG", "v1.0.0")
DIST = Path("dist")

PROJECTS = {
    "CombatPlusPlus": {
        "slug": "combatplusplus",
        "title": "Combat++",
        "description": "Combat systems for Paper/Folia: 1.8.9, 1.12 and modern 1.21.11.",
        "categories": ["game-mechanics", "pvp", "utility"],
        "icon": "branding/combatplus.png",
    },
    "SpawnPlusPlus": {
        "slug": "spawnplusplus",
        "title": "Spawn++",
        "description": "Combat-safe /spawn with cooldowns, bypass and admin controls for Paper/Folia.",
        "categories": ["management", "utility", "pvp"],
        "icon": "branding/spawnplus.png",
    },
    "KitPvP": {
        "slug": "kitpvp",
        "title": "KitPvP",
        "description": "16 GUI kits with cooldowns, previews and combat-aware claiming for Paper/Folia.",
        "categories": ["minigame", "pvp", "equipment"],
        "icon": "branding/kitpvp.png",
    },
    "ReportPlusPlus": {
        "slug": "reportplusplus",
        "title": "Report++",
        "description": "Player reports with persistence, staff notifications and a GUI for Paper/Folia.",
        "categories": ["management", "utility"],
        "icon": "branding/reportplus.png",
    },
}


def request(method, path, **kwargs):
    response = requests.request(method, f"{API}{path}", headers=HEADERS, timeout=60, **kwargs)
    if response.status_code >= 400:
        print(f"{method} {path} -> {response.status_code}: {response.text[:2000]}", flush=True)
        response.raise_for_status()
    return response


def project_body(repo):
    readme = DIST / repo / "README.md"
    if readme.exists():
        return readme.read_text(encoding="utf-8")
    return PROJECTS[repo]["description"]


def changelog(repo):
    path = DIST / repo / "CHANGELOG.md"
    if path.exists():
        return path.read_text(encoding="utf-8")
    return "Initial release."


def find_or_create(repo):
    meta = PROJECTS[repo]
    existing = requests.get(f"{API}/project/{meta['slug']}", headers=HEADERS, timeout=60)
    if existing.status_code == 200:
        project = existing.json()
        print(f"project exists: {meta['title']} ({project['id']})", flush=True)
        return project["id"]
    if existing.status_code != 404:
        print(f"GET project {meta['slug']} -> {existing.status_code}: {existing.text[:1000]}", flush=True)
        existing.raise_for_status()

    payload = {
        "slug": meta["slug"],
        "title": meta["title"],
        "description": meta["description"],
        "body": project_body(repo),
        "categories": meta["categories"],
        "client_side": "unsupported",
        "server_side": "required",
        "project_type": "plugin",
        "license_id": "MIT",
        "source_url": f"https://github.com/ludvig2457/{repo}",
        "issues_url": f"https://github.com/ludvig2457/{repo}/issues",
        "initial_versions": [],
    }
    print(f"creating project: {meta['title']}", flush=True)
    if DRY_RUN:
        return None

    icon_path = Path(meta["icon"])
    files = {"icon": (icon_path.name, icon_path.open("rb"), "image/png")}
    response = request(
        "POST",
        "/project",
        data={"data": json.dumps(payload)},
        files=files,
    )
    project = response.json()
    print(f"created project: {meta['title']} ({project['id']})", flush=True)
    return project["id"]


def upload_versions(repo, project_id):
    folder = DIST / repo
    jars = sorted(folder.glob("*.jar"))
    if not jars:
        print(f"no jars for {repo}", flush=True)
        return
    existing = request("GET", f"/project/{project_id}/version").json()
    existing_numbers = {version["version_number"] for version in existing}
    for jar in jars:
        match = re.search(r"-(\d+\.\d+\.\d+)-mc([0-9.]+)\.jar$", jar.name)
        if not match:
            print(f"skip unrecognized jar: {jar.name}", flush=True)
            continue
        plugin_version, mc_version = match.groups()
        version_number = f"{plugin_version}+mc{mc_version}"
        if version_number in existing_numbers:
            print(f"version exists: {repo} {version_number}", flush=True)
            continue
        payload = {
            "project_id": project_id,
            "name": f"{plugin_version} for {mc_version}",
            "version_number": version_number,
            "changelog": changelog(repo),
            "game_versions": [mc_version],
            "version_type": "release",
            "loaders": ["paper", "folia"],
            "featured": False,
            "dependencies": [],
            "file_parts": ["file"],
            "primary_file": "file",
        }
        print(f"uploading: {repo} {version_number}", flush=True)
        if DRY_RUN:
            continue
        with jar.open("rb") as handle:
            request(
                "POST",
                "/version",
                data={"data": json.dumps(payload)},
                files={"file": (jar.name, handle, "application/java-archive")},
            )
        time.sleep(0.3)


def main():
    selected = [name.strip() for name in os.environ.get("PROJECTS", ",".join(PROJECTS)).split(",") if name.strip()]
    for repo in selected:
        if repo not in PROJECTS:
            print(f"unknown project: {repo}", flush=True)
            continue
        project_id = find_or_create(repo)
        if project_id:
            upload_versions(repo, project_id)
    print("done", flush=True)


if __name__ == "__main__":
    main()
