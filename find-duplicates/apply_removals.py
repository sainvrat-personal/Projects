import os
import shutil
import time
from datetime import datetime

ROOT = os.path.abspath(os.path.dirname(__file__))
REPORT = os.path.join(ROOT, 'duplicates_report.txt')
LOG = os.path.join(ROOT, 'deletions_log.txt')

if not os.path.exists(REPORT):
    print('Report not found:', REPORT)
    raise SystemExit(1)

entries = []
with open(REPORT, 'r', encoding='utf-8') as f:
    for line in f:
        line = line.rstrip('\n')
        if '|' not in line:
            continue
        parts = [p.strip() for p in line.split('|')]
        if len(parts) < 2:
            continue
        # last token indicates KEEP/REMOVE in this report format
        tag = parts[-1]
        path = parts[0]
        if tag == 'REMOVE':
            entries.append(path)

if not entries:
    print('No REMOVE entries found in report.')
    raise SystemExit(0)

results = []
for p in entries:
    try:
        if os.path.islink(p):
            os.unlink(p)
            results.append((p, 'symlink removed'))
        elif os.path.isfile(p):
            os.remove(p)
            results.append((p, 'file removed'))
        elif os.path.isdir(p):
            shutil.rmtree(p)
            results.append((p, 'directory removed'))
        else:
            results.append((p, 'not found'))
    except Exception as e:
        results.append((p, f'error: {e}'))

# write log
with open(LOG, 'a', encoding='utf-8') as lf:
    lf.write(f"Deletion run: {datetime.now().isoformat(sep=' ', timespec='seconds')}\n")
    for p, r in results:
        lf.write(f"{p} | {r}\n")
    lf.write('\n')

# summary
removed = [1 for p, r in results if 'removed' in r]
errors = [1 for p, r in results if r.startswith('error')]
print(f"Processed {len(results)} entries: {len(removed)} removed, {len(errors)} errors. Log: {LOG}")
