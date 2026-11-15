#!/usr/bin/env python3
"""
Duplicate File Finder for Windows
---------------------------------
Finds files with identical content across directories.
Reports: path, size, last modified, same-content, same-filename.
Supports: videos, photos, text files, code repos.

Usage:
  python find_duplicates.py <directory_to_scan> [options]

Example:
  python find_duplicates.py "C:\\Toshiba Data"
  python find_duplicates.py C:\\ --exclude "C:\\Windows,C:\\Program Files" --min-size 1024
"""

import hashlib
import os
import argparse
from pathlib import Path
from datetime import datetime
from collections import defaultdict


def get_file_hash(filepath, chunk_size=8192):
    """
    Compute SHA-256 hash of file content (chunked for large files like videos).
    Returns None if file cannot be read.
    """
    try:
        hasher = hashlib.sha256()
        with open(filepath, "rb") as f:
            for chunk in iter(lambda: f.read(chunk_size), b""):
                hasher.update(chunk)
        return hasher.hexdigest()
    except (OSError, PermissionError):
        return None


def format_size(size_bytes):
    """Convert bytes to human-readable format."""
    for unit in ("B", "KB", "MB", "GB", "TB"):
        if size_bytes < 1024:
            return f"{size_bytes:.1f} {unit}"
        size_bytes /= 1024
    return f"{size_bytes:.1f} PB"


def scan_directory(root_dir, exclude_dirs, min_size, max_size, extensions):
    """
    Scan directory and build a map: (size, hash) -> [list of file infos]
    First groups by size (fast), then by hash (content) for same-sized files.
    """
    size_to_files = defaultdict(list)
    root_path = Path(root_dir)

    for dirpath, dirnames, filenames in os.walk(root_path, topdown=True):
        # Skip excluded directories
        dirpath_abs = Path(dirpath).resolve()
        if any(str(dirpath_abs).startswith(ex) for ex in exclude_dirs):
            dirnames.clear()
            continue

        for filename in filenames:
            try:
                filepath = Path(dirpath) / filename
                if not filepath.is_file():
                    continue

                stat = filepath.stat()
                size = stat.st_size

                if min_size and size < min_size:
                    continue
                if max_size and size > max_size:
                    continue
                if extensions and filepath.suffix.lower() not in extensions:
                    continue

                mtime = datetime.fromtimestamp(stat.st_mtime)
                size_to_files[size].append({
                    "path": str(filepath.resolve()),
                    "name": filename,
                    "size": size,
                    "mtime": mtime,
                    "dirname": str(filepath.parent),
                })
            except (OSError, PermissionError):
                pass

    return size_to_files


def find_duplicates(size_to_files):
    """Among same-sized files, compare hashes to find true content duplicates."""
    hash_to_files = defaultdict(list)

    for size, file_list in size_to_files.items():
        if len(file_list) < 2:
            continue

        for info in file_list:
            content_hash = get_file_hash(info["path"])
            if content_hash:
                info["hash"] = content_hash
                hash_to_files[(size, content_hash)].append(info)

    # Keep only groups with 2+ files (actual duplicates)
    return {k: v for k, v in hash_to_files.items() if len(v) >= 2}


def is_code_repo(filepath):
    """Check if path is inside a code repository (.git, .hg, etc.)."""
    p = Path(filepath)
    for parent in p.parents:
        if (parent / ".git").exists() or (parent / ".hg").exists():
            return True
    return False


def get_repo_root(filepath):
    """Get the repository root if it's a code repo."""
    p = Path(filepath).resolve()
    for parent in p.parents:
        if (parent / ".git").exists():
            return str(parent)
        if (parent / ".hg").exists():
            return str(parent)
    return None


def recommend_keep(duplicates, prefer_newest=True):
    """
    Recommend which file to keep among duplicates.
    For code repos: prefer newest, and presence of .git at repo root.
    """
    def score(info):
        path = info["path"]
        repo_root = get_repo_root(path)
        s = 0
        if repo_root and (Path(repo_root) / ".git").exists():
            s += 100  # Has .git
        if prefer_newest:
            s += info["mtime"].timestamp()  # Newer = higher
        else:
            s += 1e12 - info["mtime"].timestamp()
        return s

    return max(duplicates, key=score)


def main():
    parser = argparse.ArgumentParser(
        description="Find duplicate files by content. Reports path, size, last modified, same-content, same-filename."
    )
    parser.add_argument(
        "directory",
        nargs="?",
        default=".",
        help="Root directory to scan (default: current directory)",
    )
    parser.add_argument(
        "--exclude",
        type=str,
        default="",
        help='Comma-separated paths to exclude (e.g. "C:\\Windows,C:\\Program Files")',
    )
    parser.add_argument(
        "--min-size",
        type=int,
        default=0,
        help="Minimum file size in bytes (skip smaller files, 0 = no limit)",
    )
    parser.add_argument(
        "--max-size",
        type=int,
        default=0,
        help="Maximum file size in bytes (skip larger files, 0 = no limit)",
    )
    parser.add_argument(
        "--extensions",
        type=str,
        default="",
        help='Comma-separated extensions to include (e.g. ".mp4,.jpg,.txt"). Empty = all.',
    )
    parser.add_argument(
        "--output",
        type=str,
        default="",
        help="Save report to file (e.g. duplicates_report.txt)",
    )
    parser.add_argument(
        "--csv",
        action="store_true",
        help="Output as CSV for easy import to Excel",
    )
    args = parser.parse_args()

    root = os.path.abspath(args.directory)
    exclude_dirs = [p.strip() for p in args.exclude.split(",") if p.strip()]
    extensions = None
    if args.extensions:
        ext_set = {e.strip().lower() if e.strip().startswith(".") else "." + e.strip().lower()
                   for e in args.extensions.split(",") if e.strip()}
        extensions = ext_set

    print(f"Scanning: {root}")
    if exclude_dirs:
        print(f"Excluding: {exclude_dirs}")
    print("This may take a while for large directories and video files...\n")

    size_to_files = scan_directory(root, exclude_dirs, args.min_size, args.max_size, extensions)

    # Count files to hash (only same-sized groups with 2+ files)
    to_hash = sum(len(files) for size, files in size_to_files.items() if len(files) >= 2)
    print(f"Comparing content (hashing) for {to_hash} candidate files...\n")

    duplicates = find_duplicates(size_to_files)
    total_duplicate_groups = len(duplicates)
    total_duplicate_files = sum(len(g) for g in duplicates.values())

    lines = []
    if args.csv:
        lines.append("Group,Path,Filename,Size_Bytes,Size_Readable,Last_Modified,Same_Content,Same_Filename,Is_Code_Repo,Recommended_Keep")
    else:
        lines.append("=" * 100)
        lines.append("DUPLICATE FILE REPORT")
        lines.append("=" * 100)
        lines.append(f"Total duplicate groups: {total_duplicate_groups}")
        lines.append(f"Total duplicate files: {total_duplicate_files}")
        lines.append(f"Potential space to reclaim: (sum of (group_size * (copies - 1)) per group)")
        lines.append("")

    group_num = 0
    for (size, _), file_list in sorted(duplicates.items(), key=lambda x: -x[0][0]):
        group_num += 1
        filenames = [f["name"] for f in file_list]
        all_same_name = len(set(filenames)) == 1
        recommended = recommend_keep(file_list)

        if args.csv:
            for f in file_list:
                rec = "YES" if f["path"] == recommended["path"] else ""
                repo = "Yes" if is_code_repo(f["path"]) else "No"
                lines.append(f'{group_num},"{f["path"]}","{f["name"]}",{f["size"]},{format_size(f["size"])},{f["mtime"].strftime("%Y-%m-%d %H:%M:%S")},Yes,{"Yes" if all_same_name else "No"},{repo},{rec}')
        else:
            lines.append("-" * 100)
            lines.append(f"GROUP {group_num} | Size: {format_size(size)} | Copies: {len(file_list)}")
            lines.append(f"  Same filename: {'YES' if all_same_name else 'NO'}")
            lines.append(f"  Same content:  YES (SHA-256 verified)")
            lines.append("")
            for f in file_list:
                repo_note = " [CODE REPO]" if is_code_repo(f["path"]) else ""
                keep_note = " <<< RECOMMENDED TO KEEP (newest/has .git)" if f["path"] == recommended["path"] else ""
                lines.append(f"    {f['path']}{repo_note}{keep_note}")
                lines.append(f"      Size: {format_size(f['size'])}, Modified: {f['mtime'].strftime('%Y-%m-%d %H:%M:%S')}")
            lines.append("")

    report = "\n".join(lines)

    if args.output:
        with open(args.output, "w", encoding="utf-8") as f:
            f.write(report)
        print(f"Report saved to: {args.output}")

    print(report)

    if total_duplicate_groups == 0:
        print("No duplicate files found.")
    else:
        print("\nTIPS for code repos:")
        print("  - 'RECOMMENDED TO KEEP' prefers: (1) path under a .git repo, (2) newest modification time")
        print("  - Manually verify the recommended copy works before deleting others")
        print("  - Consider backing up before bulk deletion")


if __name__ == "__main__":
    main()
