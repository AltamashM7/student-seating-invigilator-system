"""Read only the known fictional cohort from the local application for the Pages demo."""
import http.cookiejar, json, re, urllib.request, urllib.parse
from pathlib import Path

root=Path(__file__).resolve().parents[1]
opener=urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))
def get(path, values=None):
    body=None if values is None else urllib.parse.urlencode(values).encode()
    with opener.open('http://127.0.0.1:33080/exam-seating'+path,body,timeout=20) as response:
        return json.load(response)
get('/api/auth/login',{'username':'admin','password':'Admin@123'})
data=get('/api/admin/data?academicYearId=1')
rolls=set(re.findall(r"'((?:CSE|IT|ENTC)26Y[1-4][AB]\d{3})'",(root/'database/demo-data.sql').read_text(encoding='utf-8')))
students=[s for s in data['students'] if s['rollNumber'] in rolls]
assert len(students)==240, 'Only the documented fictional cohort may be exported'
student_ids={s['id'] for s in students}
exams=get('/api/admin/exams')['exams']
exams=[e for e in exams if 1<=e['id']<=24]
assert len(data['years'])==1 and len(data['departments'])==3 and len(data['courses'])==26 and len(data['rooms'])==8 and len(exams)==24
seed={k:data[k] for k in ['years','departments','divisions','courses','rooms','seats','faculty']}
seed['students']=[{k:s[k] for k in ['id','rollNumber','name','email','requiresAccessible']} for s in students]
seed['placements']=[{'studentId':s['id'],'academicYearId':1,'divisionId':s['divisionId'],'semester':s['semester']} for s in students]
seed['offerings']=[{'courseId':c['id'],'academicYearId':1} for c in data['courses'] if c['offered']]
seed['enrollments']=[dict(e,academicYearId=1) for e in data['enrollments'] if e['studentId'] in student_ids]
for seat in seed['seats']:
    room=next(r for r in seed['rooms'] if r['id']==seat['roomId'])
    i=int(seat['seatNumber'][1:])-1
    seat.update(rowNumber=i//room['seatColumns']+1,columnNumber=i%room['seatColumns']+1)
seed['exams']=[{**{k:e[k] for k in ['id','courseId','academicYearId','examDate','startTime','endTime','spacingRequired']},'status':'DRAFT' if e['courseCode']=='CSE102' else 'SCHEDULED','roomIds':[r['id'] for r in e['rooms']]} for e in exams]
seed['users']=[{'id':1,'username':'admin','password':'Admin@123','role':'ADMIN','name':'System Admin','email':'admin@example.test'}]
for f in seed['faculty']:
    seed['users'].append({'id':f['userId'],'username':f['username'],'password':'Faculty@123','role':'FACULTY','name':f['name'],'email':f['email']})
seed.update(assignments=[],history=[],revision=0,schemaVersion=1)
assert len(seed['enrollments'])==480
assert all(not s.get('email') or s['email'].endswith('@example.test') for s in seed['students'])
out=root/'demo/assets/data/demo-seed.json'
out.parent.mkdir(parents=True,exist_ok=True)
out.write_text(json.dumps(seed,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
get('/api/auth/logout',{})
print('Exported 240 fictional students, 480 enrollments, 24 exams; no local data changed.')
