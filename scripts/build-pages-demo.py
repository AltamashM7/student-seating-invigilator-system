"""Regenerate the static training UI from the real webapp; keep the authored simulator."""
from pathlib import Path
import re, shutil

root=Path(__file__).resolve().parents[1]
source=root/'src/main/webapp'
output=root/'demo'
for file in source.rglob('*'):
    if not file.is_file() or 'WEB-INF' in file.parts: continue
    relative=file.relative_to(source)
    destination=output/relative
    destination.parent.mkdir(parents=True,exist_ok=True)
    if file.suffix=='.html':
        text=file.read_text(encoding='utf-8')
        prefix='../'*len(relative.parts[:-1])
        text=text.replace('</head>',f'  <link rel="stylesheet" href="{prefix}demo.css?v=1">\n</head>')
        api=re.search(r'<script src="[^"]*assets/js/api\.js[^\"]*"></script>',text)
        if not api: raise ValueError(f'Missing API script: {relative}')
        insert=api.group(0)+f'<script src="{prefix}engine.js?v=1"></script><script src="{prefix}runtime.js?v=2"></script>'
        text=text[:api.start()]+insert+text[api.end():]
        destination.write_text(text,encoding='utf-8')
    else: shutil.copyfile(file,destination)
(output/'.nojekyll').touch()
print('Built team demo from the current webapp; Java source was not changed.')
