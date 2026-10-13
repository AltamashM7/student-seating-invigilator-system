"""Check syntax and local HTML references without starting a browser or changing data."""
import argparse
import ast
import subprocess
from html.parser import HTMLParser
from pathlib import Path
from urllib.parse import unquote, urlsplit

parser = argparse.ArgumentParser()
parser.add_argument('--node', default='node', help='Node executable for --check')
args = parser.parse_args()
root = Path(__file__).resolve().parents[1]
web = root / 'src/main/webapp'
errors = []
javascript = list((web / 'assets/js').glob('*.js'))
python = list((root / 'scripts').glob('*.py'))
for path in javascript:
    result = subprocess.run([args.node, '--check', str(path)], capture_output=True, text=True)
    if result.returncode:
        errors.append(f'{path}: {result.stderr}')
for path in python:
    try:
        ast.parse(path.read_text(encoding='utf-8-sig'), filename=str(path))
    except SyntaxError as error:
        errors.append(str(error))

class References(HTMLParser):
    def __init__(self):
        super().__init__()
        self.references = []

    def handle_starttag(self, tag, attrs):
        self.references.extend(value for key, value in attrs if value and key in ('href', 'src'))

html = list(web.rglob('*.html'))
count = 0
for path in html:
    references = References()
    references.feed(path.read_text(encoding='utf-8-sig'))
    for value in references.references:
        url = urlsplit(value)
        if url.scheme or url.netloc or not url.path or url.path.startswith('/api/'):
            continue
        target = web / url.path.lstrip('/') if url.path.startswith('/') else path.parent / unquote(url.path)
        count += 1
        if not target.exists():
            errors.append(f'{path.relative_to(web)}: missing {value}')
report = (f'{len(javascript)} JavaScript files, {len(python)} Python files parsed; '
          f'{len(html)} HTML files / {count} local references checked.\n'
          + ('\n'.join(errors) if errors else 'PASS: no syntax errors or missing static references.') + '\n')
(root / 'docs/testing/evidence/static-results.txt').write_text(report, encoding='utf-8')
print(report)
raise SystemExit(bool(errors))
