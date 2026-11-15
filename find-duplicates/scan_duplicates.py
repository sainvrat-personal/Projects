import os
import sys
import time
from datetime import datetime

ROOT = os.path.abspath(os.path.dirname(__file__))
REPORT_NAME = "duplicates_report.txt"
SKIP_NAMES = {os.path.basename(__file__), REPORT_NAME}


def human_time(ts):
    return datetime.fromtimestamp(ts).isoformat(sep=' ', timespec='seconds')


def dir_size(path):
    total = 0
    for dpath, dnames, fnames in os.walk(path):
        for f in fnames:
            fp = os.path.join(dpath, f)
            try:
                total += os.path.getsize(fp)
            except OSError:
                pass
    return total


def main():
    files_by_name = {}
    dirs_by_name = {}

    for dirpath, dirnames, filenames in os.walk(ROOT):
        # record directory occurrence
        bname = os.path.basename(dirpath)
        dirs_by_name.setdefault(bname, []).append(dirpath)

        for fname in filenames:
            if fname in SKIP_NAMES:
                continue
            full = os.path.join(dirpath, fname)
            try:
                st = os.stat(full)
            except OSError:
                continue
            files_by_name.setdefault(fname, []).append({
                'path': full,
                'size': st.st_size,
                'mtime': st.st_mtime,
            })

    lines = []
    lines.append(f"Duplicate scan report for: {ROOT}")
    lines.append(f"Generated: {human_time(time.time())}")
    lines.append("")

    # Files with same name
    file_groups = {k: v for k, v in files_by_name.items() if len(v) > 1}
    if file_groups:
        lines.append("Duplicate files (by filename):")
        for name, items in sorted(file_groups.items()):
            lines.append(f"\n-- {name} ({len(items)} copies) --")
            # choose keep: latest mtime; tie-breaker larger size
            keep = max(items, key=lambda x: (x['mtime'], x['size']))
            for it in sorted(items, key=lambda x: x['path']):
                mark = "KEEP" if it['path'] == keep['path'] else "REMOVE"
                lines.append(f"{it['path']} | {it['size']} bytes | {human_time(it['mtime'])} | {mark}")
    else:
        lines.append("No duplicate filenames found.")

    # Directories with same basename
    dir_groups = {k: v for k, v in dirs_by_name.items() if len(v) > 1}
    if dir_groups:
        lines.append("")
        lines.append("Duplicate directories (by directory name):")
        for name, paths in sorted(dir_groups.items()):
            lines.append(f"\n-- {name} ({len(paths)} copies) --")
            dir_infos = []
            for p in paths:
                try:
                    mtime = os.path.getmtime(p)
                except OSError:
                    mtime = 0
                size = dir_size(p)
                dir_infos.append({'path': p, 'mtime': mtime, 'size': size})
            keep = max(dir_infos, key=lambda x: (x['mtime'], x['size']))
            for di in sorted(dir_infos, key=lambda x: x['path']):
                mark = "KEEP" if di['path'] == keep['path'] else "REMOVE"
                lines.append(f"{di['path']} | {di['size']} bytes | {human_time(di['mtime'])} | {mark}")
    else:
        lines.append("")
        lines.append("No duplicate directory names found.")

    # Write report
    report_path = os.path.join(ROOT, REPORT_NAME)
    with open(report_path, 'w', encoding='utf-8') as f:
        f.write('\n'.join(lines))

    print(f"Report written to: {report_path}")


if __name__ == '__main__':
    main()
