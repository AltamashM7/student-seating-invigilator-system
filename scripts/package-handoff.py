"""Create non-overlapping review bundles from a verified local baseline; never push/commit."""
import argparse, hashlib, json, subprocess, zipfile
from pathlib import Path

parser=argparse.ArgumentParser()
parser.add_argument('--baseline',required=True,help='Clean original repository checkout')
args=parser.parse_args()
root=Path(__file__).resolve().parents[1]
baseline=Path(args.baseline).resolve()
sha='973df5d7005961e57b31583b6b7808d411b12e6e'
def git(*values): return subprocess.check_output(['git','--no-optional-locks','-c',f'safe.directory={baseline.as_posix()}','-C',str(baseline),*values],text=True).strip()
if git('rev-parse','HEAD')!=sha or git('status','--porcelain'): raise SystemExit('Requires the clean verified baseline checkout.')
baselineFiles={p:(baseline/p).read_bytes() for p in git('ls-files').splitlines()}
output=root/'handoff';output.mkdir(exist_ok=True)
members=[
 ('B24CE1030','Problem design','Database upgrade and ER model'),
 ('B24CE1031','Problem design','Academic rules and evaluator acceptance'),
 ('B24CE1032','Problem design','Algorithm explanation and user walkthrough'),
 ('B24CE1027','Backend','Academic CRUD and annual CSV enrollment'),
 ('B24CE1028','Backend','Exam conflicts, authentication and shared JDBC utilities'),
 ('B24CE1029','Backend','Seating, overrides, audit and public lookup'),
 ('B24CE1033','Frontend','Academic management screens and dashboard'),
 ('B24CE1034','Frontend','Exam, seating and import workflows'),
 ('B24CE1035','Frontend','Student/auth flows and responsive styles'),
 ('B24CE1036','Testing and QA','Algorithm tests and fictional data generation'),
 ('B24CE1037','Testing and QA','API, migration and browser verification'),
 ('B24CE1038','Testing and QA','Runtime, packaging, README and KPI report')]

def owner(path):
    name=Path(path).name
    if path in ('database/schema.sql','database/migrations/001_s1_s6_upgrade.sql','docs/diagrams/er-diagram.md'): return 0
    if path in ('docs/implementation/current-rules.md','docs/implementation/local-delivery-plan.md','docs/requirements/s5-s6-scope.md','docs/requirements/account-rules.md','docs/architecture/auth-approach.md'): return 1
    if path in ('docs/implementation/user-guide.md','docs/implementation/ui-refresh.md','docs/algorithms/seating-allocation.md'): return 2
    if name in ('AcademicDao.java','AcademicServlet.java','CourseServlet.java','RoomServlet.java','StudentImportDao.java','StudentImportServlet.java'): return 3
    if name in ('ExamDao.java','ExamServlet.java','TimetableServlet.java','Sql.java','Input.java','JsonUtil.java','EncodingFilter.java','LoginServlet.java','ResetPasswordServlet.java','pom.xml'): return 4
    if name in ('SeatingAllocator.java','SeatingDao.java','SeatingServlet.java','StudentSeatingServlet.java'): return 5
    if path.startswith('src/main/webapp/pages/admin/'):
        if name in ('exams.html','seating.html','import-students.html'): return 7
        if name=='create-user.html': return 8
        return 6
    if path.startswith('src/main/webapp/assets/js/'):
        if name in ('admin.js','rooms.js','courses.js'): return 6
        if name in ('exams.js','seating.js','import-students.js'): return 7
        return 8
    if path.startswith('src/main/webapp/'): return 8
    if path.startswith('src/test/') or path in ('scripts/test-algorithm.ps1','scripts/generate-demo-data.py','database/demo-data.sql','database/student_import_template.csv','database/evaluator-import.csv','docs/testing/evidence/algorithm-results.txt'): return 9
    if path in ('scripts/test-integration.py','scripts/test-migration.py','docs/testing/auth-test-cases.md') or path.startswith('scripts/fixtures/'): return 10
    if path.startswith('docs/testing/evidence/') and name not in ('build-results.txt','demo-results.json','static-results.txt','runtime-results.txt'): return 10
    if path.startswith('scripts/') or path in ('README.md','.gitignore','docs/testing/kpi-report.md') or path.startswith('docs/testing/evidence/'): return 11
    raise ValueError(f'Unassigned changed file: {path}')

excluded={'.git','.local-runtime','target','__pycache__','.agents','.codex','.idea','.vscode','handoff'}
source={}
for file in root.rglob('*'):
    path=file.relative_to(root).as_posix()
    if not file.is_file() or any(part in excluded for part in file.relative_to(root).parts): continue
    data=file.read_bytes()
    # Preserve baseline bytes for text files whose only difference is line endings.
    if path in baselineFiles and data.replace(b'\r\n',b'\n')==baselineFiles[path].replace(b'\r\n',b'\n'):
        data=baselineFiles[path]
        if file.read_bytes()!=data:file.write_bytes(data)
    source[path]=data
if any(p not in source for p in baselineFiles): raise ValueError('A baseline file was removed; deletion handoff is not supported.')
changed={p:data for p,data in source.items() if baselineFiles.get(p)!=data}
groups=[[] for _ in members]
for path in sorted(changed):groups[owner(path)].append(path)
if any(not g for g in groups): raise ValueError('Every member must have concrete owned files.')
guide="""MEMBER UPLOAD INSTRUCTIONS

Repository: https://github.com/AltamashM7/student-seating-invigilator-system
Use the ZIP named with your roll number. Replace B24CE1030 with YOUR roll
number wherever it appears in the commands.

1. DOWNLOAD THE REPOSITORY
Install Git if needed. Open PowerShell in a folder where you want to keep
this project, then run:

git clone https://github.com/AltamashM7/student-seating-invigilator-system.git
cd student-seating-invigilator-system
git switch -c member/B24CE1030

2. COPY YOUR FILES
Extract your roll-number ZIP. Copy all the files and folders INSIDE it into
student-seating-invigilator-system. Choose Replace/Merge when Windows asks.

Copy into the folder containing pom.xml, src, database and docs.
Do not put the whole B24CE1030 folder or the ZIP itself inside the repository.

3. COMMIT AND PUSH
From the same PowerShell window, run these commands. Replace the name and
email with your own name and the email linked to your GitHub account:

git config user.name "Your Name"
git config user.email "Your GitHub-linked email"
git add .
git commit -m "S1-S6 updates - B24CE1030"
git push -u origin member/B24CE1030

If Git asks you to sign in, use your own GitHub account. If it says you do
not have permission to push, ask the repository owner for access.

Your task is complete once the push finishes. The coordinator will combine
all members' branches later.
"""
(output/'MEMBER_UPLOAD_INSTRUCTIONS.txt').write_text(guide,encoding='utf-8')
bundleNames=[f'{student}.zip' for student,_,_ in members]

def digest(data):return hashlib.sha256(data).hexdigest()
manifest={'baseline':sha,'changedFiles':len(changed),'sourceFiles':len(source),'members':[]}
for index,(student,team,responsibility) in enumerate(members):
    record={'studentId':student,'team':team,'responsibility':responsibility,'zip':bundleNames[index],'files':[{'path':p,'sha256':digest(source[p])} for p in sorted(groups[index])]}
    with zipfile.ZipFile(output/bundleNames[index],'w',zipfile.ZIP_DEFLATED) as z:
        for p in sorted(groups[index]):z.writestr(p,source[p])
    record['zipSha256']=digest((output/bundleNames[index]).read_bytes());manifest['members'].append(record)
fullName='SSMS_S1-S6_complete_source.zip'
with zipfile.ZipFile(output/fullName,'w',zipfile.ZIP_DEFLATED) as z:
    for p in sorted(source):z.writestr(p,source[p])
reconstructed=dict(baselineFiles);seen=set()
for filename in bundleNames:
    with zipfile.ZipFile(output/filename) as z:
        assert z.testzip() is None
        for p in z.namelist():
            assert not p.startswith('/') and '..' not in Path(p).parts and ':' not in p
            assert not any(part in ('handoff','_handoff') for part in Path(p).parts),p
            assert p not in seen,p
            seen.add(p);reconstructed[p]=z.read(p)
assert seen==set(changed)
assert reconstructed==source,'Overlay differs from the complete source release'
with zipfile.ZipFile(output/fullName) as z:
    assert z.testzip() is None
    assert {p:z.read(p) for p in z.namelist()}==source
manifest['completeSourceZip']=fullName;manifest['completeSourceSha256']=digest((output/fullName).read_bytes())
manifest['verification']={'nonOverlappingOwnership':True,'allChangesCovered':True,'byteExactBaselineOverlay':True,'zipIntegrity':True,'excluded':['.git','.local-runtime','target','__pycache__']}
(output/'manifest.json').write_text(json.dumps(manifest,indent=2)+'\n',encoding='utf-8')
with zipfile.ZipFile(output/'SSMS_12_member_bundles.zip','w',zipfile.ZIP_DEFLATED) as z:
    for filename in [*bundleNames,'MEMBER_UPLOAD_INSTRUCTIONS.txt','manifest.json']:
        z.write(output/filename,filename)
    z.write(root/'database/evaluator-import.csv','evaluator-import.csv')
print(f'PASS: {len(changed)} changed files, {len(source)} source files, 12 non-overlapping member bundles; exact baseline reconstruction and ZIP integrity verified.')
for index,member in enumerate(members):print(f'{bundleNames[index]}: {len(groups[index])} files')
