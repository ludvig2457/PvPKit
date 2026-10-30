import json
import os
import re
import time
from pathlib import Path

import requests

API = "https://api.modrinth.com/v2"
TOKEN = os.environ["MODRINTH_TOKEN"]
HEADERS = {"Authorization": f"Bearer {TOKEN}", "User-Agent": "ludvig2457/pvpkit-publisher/1.0"}
DRY_RUN = os.environ.get("DRY_RUN", "false").lower() == "true"
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
        "slug": "kitpvp-plugin",
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


def jar_info(jar):
    match = re.search(r"-(\d+\.\d+\.\d+)-mc([0-9.]+)\.jar$", jar.name)
    if not match:
        return None
    plugin_version, mc_version = match.groups()
    return plugin_version, mc_version, f"{plugin_version}+mc{mc_version}"


def initial_version(repo, jar, plugin_version, mc_version, version_number):
    return {
        "version_title": f"{plugin_version} for {mc_version}",
        "version_number": version_number,
        "version_body": changelog(repo),
        "game_versions": [mc_version],
        "release_channel": "release",
        "loaders": ["paper", "folia"],
        "featured": False,
        "dependencies": [],
        "file_parts": ["file"],
        "primary_file": "file",
        "status": "listed",
    }


def find_or_create(repo):
    meta = PROJECTS[repo]
    existing = requests.get(f"{API}/project/{meta['slug']}", headers=HEADERS, timeout=60)
    if existing.status_code == 200:
        project = existing.json()
        print(f"project exists: {meta['title']} ({project['id']})", flush=True)
        return project["id"], False
    if existing.status_code != 404:
        print(f"GET project {meta['slug']} -> {existing.status_code}: {existing.text[:1000]}", flush=True)
        existing.raise_for_status()

    jars = sorted((DIST / repo).glob("*.jar"))
    if not jars:
        raise RuntimeError(f"no jars for {repo}")
    first = jars[0]
    match = jar_info(first)
    if not match:
        raise RuntimeError(f"unrecognized jar: {first.name}")
    plugin_version, mc_version, version_number = match

    payload = {
        "slug": meta["slug"],
        "title": meta["title"],
        "description": meta["description"],
        "body": project_body(repo),
        "categories": meta["categories"],
        "client_side": "unsupported",
        "server_side": "required",
        "license_id": "MIT",
        "source_url": f"https://github.com/ludvig2457/{repo}",
        "issues_url": f"https://github.com/ludvig2457/{repo}/issues",
        "initial_versions": [initial_version(repo, first, plugin_version, mc_version, version_number)],
        "is_draft": True,
    }
    print(f"creating project: {meta['title']} as plugin", flush=True)
    if DRY_RUN:
        return None, False

    icon_path = Path(meta["icon"])
    with first.open("rb") as jar_handle, icon_path.open("rb") as icon_handle:
        response = request(
            "POST",
            "/project",
            data={"data": json.dumps(payload)},
            files={
                "icon": (icon_path.name, icon_handle, "image/png"),
                "file": (first.name, jar_handle, "application/java-archive"),
            },
        )
    project = response.json()
    print(f"created project: {meta['title']} ({project['id']})", flush=True)
    return project["id"], True


def upload_versions(repo, project_id):
    folder = DIST / repo
    jars = sorted(folder.glob("*.jar"))
    existing = request("GET", f"/project/{project_id}/version").json()
    existing_numbers = {version["version_number"] for version in existing}
    for jar in jars:
        info = jar_info(jar)
        if not info:
            print(f"skip unrecognized jar: {jar.name}", flush=True)
            continue
        plugin_version, mc_version, version_number = info
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
        project_id, created = find_or_create(repo)
        if project_id:
            upload_versions(repo, project_id)
            if created and not DRY_RUN:
                request("PATCH", f"/project/{project_id}", json={"requested_status": "approved"})
                print(f"submitted for review: {repo}", flush=True)
    print("done", flush=True)


if __name__ == "__main__":
    main()
