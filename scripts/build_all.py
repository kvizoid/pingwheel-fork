#!/usr/bin/env python3
import argparse
import os
import subprocess
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent

BRANCH_MATRIX = {
    "main": {"mc": "26.3", "java": 25, "loaders": ["fabric"]},
    "1.21.4": {"mc": "1.21.4", "java": 21, "loaders": ["fabric", "neoforge"]},
    "1.21.1": {"mc": "1.21.1", "java": 21, "loaders": ["fabric", "neoforge"]},
    "1.20.1": {"mc": "1.20.1", "java": 17, "loaders": ["fabric", "forge"]},
}

def run_cmd(cmd, cwd=None, check=True):
    print(f"\033[36m-> {' '.join(cmd) if isinstance(cmd, list) else cmd}\033[0m")
    res = subprocess.run(cmd, cwd=cwd or REPO_ROOT, shell=isinstance(cmd, str))
    if check and res.returncode != 0:
        sys.exit(res.returncode)
    return res

def cmd_trigger(args):
    tag = args.tag or "v1.1.5"
    print(f"\033[32mTriggering GitHub Actions build for all Minecraft versions ({', '.join(BRANCH_MATRIX.keys())})...\033[0m")
    run_cmd(["gh", "workflow", "run", "build-all.yml", "-f", f"release_tag={tag}"])
    print(f"\033[32mWorkflow triggered successfully! Monitoring runs...\033[0m")
    run_cmd(["gh", "run", "list", "--workflow=build-all.yml", "-L", "3"])

def cmd_status(args):
    run_cmd(["gh", "run", "list", "--workflow=build-all.yml", "-L", "5"])

def cmd_watch(args):
    run_cmd(["gh", "run", "watch"])

def cmd_sync_assets(args):
    current_branch = subprocess.check_output(
        ["git", "rev-parse", "--abbrev-ref", "HEAD"], cwd=REPO_ROOT, text=True
    ).strip()

    icon_src = REPO_ROOT / ".github" / "icon.png"
    if not icon_src.exists():
        print(f"\033[31mIcon not found at {icon_src}\033[0m")
        return

    print(f"\033[32mSyncing icon across branches: {list(BRANCH_MATRIX.keys())}\033[0m")
    for branch in BRANCH_MATRIX:
        if branch == current_branch:
            continue
        print(f"Applying icon to {branch}...")
        try:
            run_cmd(["git", "checkout", branch])
            dest1 = REPO_ROOT / "common" / "src" / "main" / "resources" / "icon.png"
            dest2 = REPO_ROOT / ".github" / "icon.png"
            dest1.parent.mkdir(parents=True, exist_ok=True)
            dest2.parent.mkdir(parents=True, exist_ok=True)
            import shutil
            shutil.copy2(icon_src, dest1)
            shutil.copy2(icon_src, dest2)
            run_cmd(["git", "add", "common/src/main/resources/icon.png", ".github/icon.png"])
            run_cmd(["git", "commit", "-m", "chore: sync official icon", "--allow-empty"])
            run_cmd(["git", "push", "origin", branch])
        except Exception as e:
            print(f"\033[33mWarning on {branch}: {e}\033[0m")
        finally:
            run_cmd(["git", "checkout", current_branch])

def cmd_build_local(args):
    branch = args.branch or "main"
    info = BRANCH_MATRIX.get(branch)
    if not info:
        print(f"\033[31mUnknown branch {branch}. Available: {list(BRANCH_MATRIX.keys())}\033[0m")
        return

    req_java = info["java"]
    java_home = f"/usr/lib/jvm/java-{req_java}-openjdk"
    if not Path(java_home).exists():
        print(f"\033[31mRequired Java {req_java} not found at {java_home}!\033[0m")
        print(f"Install it via: sudo pacman -S jdk{req_java}-openjdk")
        print("Or use GitHub Actions build: python3 scripts/build_all.py trigger")
        return

    env = os.environ.copy()
    env["JAVA_HOME"] = java_home
    env["PATH"] = f"{java_home}/bin:" + env.get("PATH", "")

    for loader in info["loaders"]:
        print(f"\033[32mBuilding {loader} on {info['mc']} (Java {req_java})...\033[0m")
        subprocess.run(["./gradlew", f":{loader}:build", "-x", "test"], cwd=REPO_ROOT, env=env, check=True)

def main():
    parser = argparse.ArgumentParser(description="Ping Wheel Multi-Version Build & Release Automator")
    subparsers = parser.add_subparsers(dest="command", required=True)

    p_trigger = subparsers.add_parser("trigger", help="Trigger GitHub Actions build matrix across all MC versions")
    p_trigger.add_argument("--tag", "-t", default="v1.1.5", help="Release tag to attach jars to (default: v1.1.5)")
    p_trigger.set_defaults(func=cmd_trigger)

    p_status = subparsers.add_parser("status", help="Check status of GitHub Actions workflow runs")
    p_status.set_defaults(func=cmd_status)

    p_watch = subparsers.add_parser("watch", help="Watch latest GitHub Actions run live")
    p_watch.set_defaults(func=cmd_watch)

    p_sync = subparsers.add_parser("sync-assets", help="Sync icon and common assets across all git branches")
    p_sync.set_defaults(func=cmd_sync_assets)

    p_local = subparsers.add_parser("build-local", help="Build a specific branch locally with detected JDK")
    p_local.add_argument("branch", nargs="?", default="main", choices=list(BRANCH_MATRIX.keys()))
    p_local.set_defaults(func=cmd_build_local)

    args = parser.parse_args()
    args.func(args)

if __name__ == "__main__":
    main()
