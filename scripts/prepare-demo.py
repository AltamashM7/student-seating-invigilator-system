"""Generate/publish the known fictional demo through the real application APIs."""
import argparse, http.cookiejar, json, urllib.parse, urllib.request
from pathlib import Path

parser=argparse.ArgumentParser()
parser.add_argument('--confirm-demo',action='store_true',required=True)
args=parser.parse_args()
base='http://127.0.0.1:33080/exam-seating'
opener=urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))
def request(path,values=None):
    data=None if values is None else urllib.parse.urlencode(values).encode()
    with opener.open(base+path,data=data,timeout=30) as response: return json.load(response)
request('/api/auth/login',dict(username='admin',password='Admin@123'))
data=request('/api/admin/data?academicYearId=1')
exams=request('/api/admin/exams')['exams']
if len(data['students'])!=240 or len(exams)!=24 or any(e['status']!='DRAFT' for e in exams):
    raise SystemExit('Requires a fresh reset-local-demo database with 240 students and 24 drafts.')
total=0
for exam in exams:
    result=request('/api/admin/seating',dict(action='generate',examId=exam['id'],reason='Initial fictional demonstration allocation'))
    total+=result['assigned']
    if exam['courseCode']!='CSE102':
        request('/api/admin/exams',dict(action='status',id=exam['id'],status='SCHEDULED'))
charts=[request(f"/api/admin/seating?examId={e['id']}") for e in exams]
assigned=[s for c in charts for s in c['seats'] if s['studentId']]
evidence={
    'students':len(data['students']),'courseEnrollments':len(data['enrollments']),
    'divisions':len(data['counts']),'exams':len(exams),'scheduledExams':23,'draftExams':1,
    'assignments':total,'accessibleAssignments':sum(bool(s['requiresAccessible']) for s in assigned),
    'allAccessibleNeedsMet':all(s['isAccessible'] for s in assigned if s['requiresAccessible']),
    'uniqueSeatsPerExam':all(len([s['id'] for s in c['seats'] if s['studentId']])==len(set(s['id'] for s in c['seats'] if s['studentId'])) for c in charts),
    'uniqueStudentsPerExam':all(len([s['studentId'] for s in c['seats'] if s['studentId']])==len(set(s['studentId'] for s in c['seats'] if s['studentId'])) for c in charts),
    'allStudentsAssigned':all(sum(bool(s['studentId']) for s in c['seats'])==len(c['students']) for c in charts),
    'spacingSatisfied':all(not c['exam']['spacingRequired'] or all(s['columnNumber']%2==1 for s in c['seats'] if s['studentId']) for c in charts)
}
if not all(evidence[k] for k in ('allAccessibleNeedsMet','uniqueSeatsPerExam','uniqueStudentsPerExam','allStudentsAssigned','spacingSatisfied')): raise AssertionError(evidence)
output=Path(__file__).resolve().parents[1]/'docs/testing/evidence/demo-results.json'
output.write_text(json.dumps(evidence,indent=2)+'\n',encoding='utf-8')
print(json.dumps(evidence,indent=2))
