"""Run only against the disposable evaluation instance with fresh demo data.
The test changes records. Never point it at a live or shared college database.
"""
import argparse, concurrent.futures, http.cookiejar, json, time, urllib.error, urllib.parse, urllib.request
from pathlib import Path

parser=argparse.ArgumentParser()
parser.add_argument('--base-url',default='http://127.0.0.1:33080/exam-seating')
parser.add_argument('--confirm-disposable',action='store_true',required=True)
args=parser.parse_args()
base=args.base_url.rstrip('/')
checks=[]

class Client:
    def __init__(self): self.opener=urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))
    def request(self,path,values=None,expected=200,body=None,headers=None):
        if values is not None: body=urllib.parse.urlencode(values,doseq=True).encode()
        req=urllib.request.Request(base+path,data=body,headers=headers or {})
        try:
            response=self.opener.open(req,timeout=30);status=response.status;raw=response.read().decode()
        except urllib.error.HTTPError as e: status=e.code;raw=e.read().decode()
        try: data=json.loads(raw)
        except json.JSONDecodeError: data={'raw':raw[:300]}
        if status!=expected: raise AssertionError(f'{path}: expected {expected}, got {status}: {data}')
        return data

def check(name,condition=True):
    if not condition: raise AssertionError(name)
    checks.append({'name':name,'status':'PASS'});print('PASS',name,flush=True)
admin=Client();public=Client();faculty=Client()
def snapshot(): return admin.request('/api/admin/data?academicYearId=1')
def save(resource,action='create',**values): return admin.request('/api/admin/data',dict(resource=resource,action=action,academicYearId=1,**values))['id']
def reject(resource,expected=400,**values): return admin.request('/api/admin/data',dict(resource=resource,action='create',academicYearId=1,**values),expected=expected)
def exam(**values): return admin.request('/api/admin/exams',dict(action='create',academicYearId=1,examDate='2027-01-15',startTime='09:00',endTime='10:00',status='DRAFT',**values))['id']
def seating(examId,action='generate',expected=200,**values): return admin.request('/api/admin/seating',dict(examId=examId,action=action,reason='KPI integration verification',**values),expected=expected)
def chart(examId): return admin.request(f'/api/admin/seating?examId={examId}')
def upload(text,expected=200):
    boundary='KPI_BOUNDARY_26'
    body=(f'--{boundary}\r\nContent-Disposition: form-data; name="file"; filename="students.csv"\r\nContent-Type: text/csv\r\n\r\n'+text+f'\r\n--{boundary}--\r\n').encode()
    return admin.request('/api/admin/import/students',expected=expected,body=body,headers={'Content-Type':'multipart/form-data; boundary='+boundary})

try:
    public.request('/api/admin/data',expected=401);check('AUTH: logged-out admin API rejected')
    public.request('/api/admin/seating?examId=1',expected=401);check('AUTH: logged-out seating API rejected')
    public.request('/api/auth/reset-password',{'username':'admin','email':'admin@example.com','password':'Changed@123'},expected=403);check('AUTH: public password reset rejected')
    public.request('/api/auth/login',{'username':'admin','password':'wrong'},expected=401);check('AUTH: invalid password rejected')
    public.request('/api/auth/login',{'username':'','password':'Admin@123'},expected=400);check('AUTH: missing username rejected')
    admin.request('/api/auth/login',{'username':'admin','password':'Admin@123'});check('AUTH: admin login succeeds')
    faculty.request('/api/auth/login',{'username':'cse.faculty1','password':'Faculty@123'});check('AUTH: faculty login succeeds')
    faculty.request('/api/admin/data',expected=403);check('AUTH: faculty admin access rejected')
    faculty.request('/api/admin/seating?examId=1',expected=403);check('AUTH: faculty seating mutation access rejected')
    admin.request('/api/faculty/duties',expected=403);check('AUTH: admin cannot bypass faculty-role filter')
    admin.request('/api/auth/register',{'username':'weak','password':'abc','role':'ADMIN','name':'Test','email':'weak@example.test'},expected=400);check('AUTH: weak account password rejected')
    admin.request('/api/auth/register',{'username':'admin','password':'Admin@123','role':'ADMIN','name':'Test','email':'kpi@example.test'},expected=409);check('AUTH: duplicate username rejected')
    admin.request('/api/auth/register',{'username':'kpi.admin','password':'KpiAdmin@123','role':'ADMIN','name':'KPI Administrator','email':'kpi.admin@example.test'},expected=201);check('AUTH: authorized admin account registration')
    admin.request('/api/auth/reset-password',{'username':'kpi.admin','email':'wrong@example.test','password':'Changed@123'},expected=404);check('AUTH: reset requires matching account email')
    admin.request('/api/auth/reset-password',{'username':'kpi.admin','email':'kpi.admin@example.test','password':'weak'},expected=400);check('AUTH: reset password policy enforced')
    admin.request('/api/auth/reset-password',{'username':'kpi.admin','email':'kpi.admin@example.test','password':'Changed@123'});check('AUTH: administrator password reset succeeds')
    resetClient=Client();resetClient.request('/api/auth/login',{'username':'kpi.admin','password':'KpiAdmin@123'},expected=401);check('AUTH: old password no longer works after reset')
    resetClient.request('/api/auth/login',{'username':'kpi.admin','password':'Changed@123'});check('AUTH: replacement password logs in')
    data=snapshot();check('DATA: 240 students',len(data['students'])==240)
    check('DATA: 480 multi-course enrollments',len(data['enrollments'])==480)
    check('DATA: 24 divisions, all counts correct',len(data['counts'])==24 and all(r['studentCount']==10 for r in data['counts']))
    check('DATA: year counts do not double-count enrollments',sum(r['studentCount'] for r in data['counts'])==240)
    check('DATA: every department and study year appears',len(data['yearCounts'])==12 and all(r['studentCount']==20 for r in data['yearCounts']))
    check('DATA: 12 faculty and 26 courses',len(data['faculty'])==12 and len(data['courses'])==26)
    check('DATA: refreshed fictional names exclude the requested surname',all('mujawar' not in r['name'].lower() for r in data['students']+data['faculty']))
    check('DATA: two workshop offerings available for demonstration import',len([c for c in data['courses'] if c['code'] in ('CSE103','CSE104') and c['offered']])==2)
    dep=data['departments'][0]['id']; group=next(d for d in data['divisions'] if d['departmentCode']=='CSE' and d['studyYear']==1 and d['code']=='A')['id']
    year=save('years',startYear=2027,isCurrent='false');check('CRUD: academic year create')
    save('years','update',id=year,startYear=2028,isCurrent='false');check('CRUD: academic year update')
    save('years','delete',id=year);check('CRUD: academic year delete')
    admin.request('/api/admin/data',{'resource':'years','action':'delete','id':1},expected=400);check('CRUD: current year deletion prevented')
    department=save('departments',code='KPI',name='Verification Department');check('CRUD: department create')
    check('DATA: empty department appears with zero counts',len([r for r in snapshot()['yearCounts'] if r['department']=='KPI' and r['studentCount']==0])==4)
    save('departments','update',id=department,code='KPI',name='Updated Department');check('CRUD: department update')
    division=save('divisions',departmentId=department,studyYear=1,code='A');check('CRUD: division create')
    save('divisions','update',id=division,departmentId=department,studyYear=1,code='B');check('CRUD: division update')
    save('divisions','delete',id=division);save('departments','delete',id=department);check('CRUD: division and department delete')
    reject('courses',code='BAD',name='Bad year',departmentId=dep,semester=5,studyYear=1);check('CRUD: incompatible year/semester rejected')
    course=save('courses',code='KPI101',name='Verification Course',departmentId=dep,semester=1,studyYear=1);check('CRUD: course and annual offering create')
    save('courses','update',id=course,code='KPI101',name='Verification Course Updated',departmentId=dep,semester=1,studyYear=1);check('CRUD: course update')
    room=save('rooms',roomNumber='KPI-R1',capacity=5,seatColumns=3,isAccessible='true');check('CRUD: room create')
    check('ROOM: capacity generates 5 seats',len([s for s in snapshot()['seats'] if s['roomId']==room])==5)
    save('rooms','update',id=room,roomNumber='KPI-R1',capacity=8,seatColumns=4,isAccessible='true');check('ROOM: capacity increase generates 8 seats',len([s for s in snapshot()['seats'] if s['roomId']==room])==8)
    save('rooms','update',id=room,roomNumber='KPI-R1',capacity=3,seatColumns=3,isAccessible='true');check('ROOM: capacity decrease removes excess seats',len([s for s in snapshot()['seats'] if s['roomId']==room])==3)
    faculty_id=save('faculty',employeeCode='KPI-F',name='Test Faculty',email='kpi.faculty@example.test',departmentId=dep,username='kpi.faculty',password='Faculty@123');check('CRUD: faculty create')
    save('faculty','update',id=faculty_id,employeeCode='KPI-F',name='Test Faculty Updated',email='kpi.faculty@example.test',departmentId=dep,username='kpi.faculty',password='');check('CRUD: faculty update without password change')
    save('faculty','delete',id=faculty_id);check('CRUD: faculty and associated account delete')
    student=save('students',rollNumber='KPI001',name='Verification Student',email='kpi001@example.test',divisionId=group,semester=1,requiresAccessible='false');check('CRUD: student and annual placement create')
    save('students','update',id=student,rollNumber='KPI001',name='Verification Student Updated',email='kpi001@example.test',divisionId=group,semester=1,requiresAccessible='false');check('CRUD: student update')
    enrollment=save('enrollments',studentId=student,courseId=course);check('CRUD: course enrollment create')
    save('enrollments','update',id=enrollment,studentId=student,courseId=course);check('CRUD: enrollment update')
    save('enrollments','delete',id=enrollment);save('students','delete',id=student);check('CRUD: enrollment and student delete')
    upload('wrong,header\nX,Y',expected=400);check('CSV: invalid header rejected')
    header='roll_number,name,email,department_code,semester,course_code,academic_year,division,requires_accessible\n'
    csv=header+''.join(f'KPIB{i:03d},Test Student {i},kpib{i:03d}@example.test,CSE,1,KPI101,2026-27,A,false\n' for i in range(1,121))
    start=time.perf_counter();result=upload(csv);elapsed=time.perf_counter()-start
    check(f'CSV: 120 valid rows imported (100%; {elapsed:.2f}s)',result['imported']==120 and result['invalid']==0)
    result=upload(csv);check('CSV: 120 duplicate enrollments detected',result['duplicates']==120)
    course2=save('courses',code='KPI102',name='Second Verification Course',departmentId=dep,semester=1,studyYear=1)
    result=upload(csv.replace('KPI101','KPI102'));check('CSV: existing students gain 120 second-course enrollments',result['enrolled']==120 and result['imported']==0)
    result=upload(header+'\nKPIBAD,Invalid,bad-email,CSE,1,KPI101,2026-27,A,false\n');check('CSV: physical line numbers retained through blank lines',result['invalid']==1 and result['errors'][0].startswith('Line 3:'))
    result=upload(header+'KPIBAD,Invalid,bad@example.test,UNKNOWN,1,KPI101,2026-27,A,false\n');check('CSV: unknown department reported',result['invalid']==1)
    demonstration=(Path(__file__).resolve().parents[1]/'database/evaluator-import.csv').read_text(encoding='utf-8')
    result=upload(demonstration);check('CSV: evaluator file imports 12 students and 12 additional enrollments',result['imported']==12 and result['enrolled']==12 and result['invalid']==0)
    result=upload(demonstration);check('CSV: evaluator file reimport identifies 24 duplicates',result['duplicates']==24 and result['invalid']==0)
    admin.request('/api/admin/exams',dict(action='create',academicYearId=1,courseId=course,examDate='2027-01-15',startTime='10:00',endTime='09:00',status='DRAFT',roomId=room),expected=409);check('EXAM: invalid interval rejected')
    e1=exam(courseId=course,roomId=room);check('EXAM: nonconflicting draft created')
    admin.request('/api/admin/exams',dict(action='create',academicYearId=1,courseId=course2,examDate='2027-01-15',startTime='09:30',endTime='10:30',status='DRAFT',roomId=room),expected=409);check('EXAM: overlapping room rejected')
    other_room=next(r['id'] for r in data['rooms'] if r['roomNumber']=='R108')
    admin.request('/api/admin/exams',dict(action='create',academicYearId=1,courseId=course2,examDate='2027-01-15',startTime='09:30',endTime='10:30',status='DRAFT',roomId=other_room),expected=409);check('EXAM: shared-student conflict rejected across different rooms')
    e2=admin.request('/api/admin/exams',dict(action='create',academicYearId=1,courseId=course2,examDate='2027-01-15',startTime='10:00',endTime='11:00',status='DRAFT',roomId=room))['id'];check('EXAM: touching time boundary accepted')
    admin.request('/api/admin/exams',dict(action='update',id=e1,academicYearId=1,courseId=course,examDate='2027-01-15',startTime='08:00',endTime='09:00',status='DRAFT',roomId=room));check('EXAM: draft inputs updated')
    admin.request('/api/admin/exams',dict(action='status',id=e1,status='SCHEDULED'));check('EXAM: draft to scheduled')
    admin.request('/api/admin/exams',dict(action='status',id=e1,status='DRAFT'),expected=409);check('EXAM: scheduled to draft rejected')
    admin.request('/api/admin/exams',dict(action='status',id=e1,status='COMPLETED'));check('EXAM: scheduled to completed')
    admin.request('/api/admin/exams',dict(action='status',id=e1,status='CANCELLED'),expected=409);check('EXAM: completed transition rejected')
    admin.request('/api/admin/exams',dict(action='status',id=e2,status='CANCELLED'));check('EXAM: cancellation')
    admin.request('/api/admin/exams',dict(action='delete',id=e2));check('EXAM: cancelled deletion')
    seating(e1,expected=409);check('SEATING: completed allocation rejected')
    # A separate full-capacity failure uses a live draft with 120 enrolled students and 3 seats.
    capacity_exam=admin.request('/api/admin/exams',dict(action='create',academicYearId=1,courseId=course2,examDate='2027-01-16',startTime='09:00',endTime='10:00',status='DRAFT',roomId=room))['id']
    seating(capacity_exam,expected=409);check('SEATING: capacity failure leaves no partial rows',not any(s['studentId'] for s in chart(capacity_exam)['seats']))
    admin.request('/api/admin/exams',dict(action='delete',id=capacity_exam))
    baseline=admin.request('/api/admin/exams')['exams'];standard=next(e for e in baseline if e['courseCode']=='CSE101')['id'];spaced=next(e for e in baseline if e['courseCode']=='CSE102')['id']
    result=seating(standard);check('SEATING: 20 students assigned',result['assigned']==20)
    seats=chart(standard)['seats'];assigned=[s for s in seats if s['studentId']]
    check('SEATING: duplicate student and seat prevention',len({s['studentId'] for s in assigned})==20 and len({s['id'] for s in assigned})==20)
    check('ACCOMMODATION: all flagged students get accessible seats',all(s['isAccessible'] for s in assigned if s['requiresAccessible']))
    pwd=next(s for s in assigned if s['requiresAccessible']);nonaccessible=next(s for s in seats if not s['studentId'] and not s['isAccessible'])
    nextYear=save('years',startYear=2027,isCurrent='false')
    nextDivision=admin.request('/api/admin/data',dict(resource='divisions',action='create',academicYearId=nextYear,departmentId=dep,studyYear=2,code='A'))['id']
    profile=next(s for s in snapshot()['students'] if s['id']==pwd['studentId'])
    nextPlacement=dict(resource='students',action='update',id=profile['id'],academicYearId=nextYear,divisionId=nextDivision,semester=3,rollNumber=profile['rollNumber'],name=profile['name'],email=profile['email'],requiresAccessible='false')
    admin.request('/api/admin/data',nextPlacement,expected=400);check('YEAR: another academic year cannot bypass active accessibility protection')
    nextPlacement['requiresAccessible']='true';admin.request('/api/admin/data',nextPlacement)
    annual=admin.request(f'/api/admin/data?academicYearId={nextYear}')
    check('YEAR: promotion retains both annual placements',next(s for s in annual['students'] if s['id']==profile['id'])['semester']==3 and next(s for s in snapshot()['students'] if s['id']==profile['id'])['semester']==1)
    seating(standard,'reassign',expected=409,studentId=pwd['studentId'],seatId=nonaccessible['id']);check('OVERRIDE: invalid accommodation move rejected')
    regular=[s for s in assigned if not s['requiresAccessible']];empty=next(s for s in seats if not s['studentId'])
    seating(standard,'swap',expected=409,studentId=regular[0]['studentId'],seatId=pwd['id']);check('OVERRIDE: swap protects both students accommodation')
    seating(standard,'reassign',studentId=regular[0]['studentId'],seatId=empty['id']);check('OVERRIDE: valid move to empty seat')
    seating(standard,'reassign',expected=409,studentId=regular[1]['studentId'],seatId=empty['id']);check('OVERRIDE: occupied-seat reassignment rejected')
    seating(standard,'swap',studentId=regular[1]['studentId'],seatId=empty['id']);check('OVERRIDE: valid occupied-seat swap')
    before=chart(standard);pinned={s['studentId']:s['id'] for s in before['seats'] if s['isManual']}
    seating(standard);after=chart(standard);check('REGENERATION: manual assignments preserved',all(any(s['studentId']==student and s['id']==seat and s['isManual'] for s in after['seats']) for student,seat in pinned.items()))
    check('AUDIT: manual changes contain actor, time and reason',any(h['action']=='SWAP' and h['actor']=='admin' and h['createdAt'] and h['reason'] for h in after['history']))
    seating(standard,replaceManual='true',confirmReplaceManual='true');check('REGENERATION: explicit full replacement clears manual flags',not any(s['isManual'] for s in chart(standard)['seats']))
    seating(spaced);spaced_chart=chart(spaced);check('SPACING: only odd columns occupied',sum(bool(s['studentId']) for s in spaced_chart['seats'])==20 and all(s['columnNumber']%2==1 for s in spaced_chart['seats'] if s['studentId']))
    seating(spaced,'reassign',expected=409,studentId=next(s['studentId'] for s in spaced_chart['seats'] if s['studentId']),seatId=next(s['id'] for s in spaced_chart['seats'] if s['columnNumber']%2==0));check('SPACING: manual move to blocked column rejected')
    admin.request('/api/admin/exams',dict(action='status',id=standard,status='SCHEDULED'));check('PUBLIC: publish exam')
    roll=assigned[0]['rollNumber'];results=public.request('/api/seating/search?'+urllib.parse.urlencode({'academicYearId':1,'rollNumber':roll}))['results'];check('PUBLIC: roll-number search returns published seat',any(r['courseCode']=='CSE101' for r in results))
    check('PUBLIC: draft seating excluded',not any(r['courseCode']=='CSE102' for r in results))
    check('PUBLIC: timetable excludes drafts and cancelled exams',all(e['status'] in ['SCHEDULED','COMPLETED'] for e in public.request('/api/timetable')['exams']))
    seating(spaced,'clear');check('CLEAR: draft seating cleared and history retained',not any(s['studentId'] for s in chart(spaced)['seats']) and len(chart(spaced)['history'])>0)
    seating(spaced);check('CLEAR: subsequent regeneration works')
    # Remove bulk-import students/enrollments after cancelling the temporary completed exam.
    # A completed exam is deliberately retained; temporary enrollments cannot be silently removed.
    check('ENROLLMENT: published course enrollment changes protected',admin.request('/api/admin/data',dict(resource='enrollments',action='create',academicYearId=1,studentId=data['students'][0]['id'],courseId=course),expected=400)['error'])
    faculty.request('/api/auth/logout',{});check('AUTH: logout invalidates session',not faculty.request('/api/auth/session')['authenticated'])
    def concurrent_create(code):
        local=Client();local.request('/api/auth/login',{'username':'admin','password':'Admin@123'})
        try: result=local.request('/api/admin/exams',dict(action='create',academicYearId=1,courseId=code,examDate='2027-02-20',startTime='09:00',endTime='10:00',status='DRAFT',roomId=room));return 'created'
        except AssertionError as error:
            if 'got 409' in str(error): return 'conflict'
            raise
    with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool: outcomes=list(pool.map(concurrent_create,[course,course2]))
    check('CONCURRENCY: competing exam saves produce one winner',sorted(outcomes)==['conflict','created'])
    public.request('/api/admin/exams',expected=401);check('AUTH: exam endpoint remains protected')
    # Retain test fixtures in disposable DB; final demo reset is handled separately.
except Exception as exc:
    checks.append({'name':str(exc),'status':'FAIL'});print('FAIL',exc,flush=True)
    raise
finally:
    output=Path('docs/testing/evidence');output.mkdir(parents=True,exist_ok=True)
    (output/'integration-results.json').write_text(json.dumps({'baseUrl':base,'checks':checks,'passed':sum(c['status']=='PASS' for c in checks),'failed':sum(c['status']=='FAIL' for c in checks)},indent=2)+'\n',encoding='utf-8')
    print('RESULT',sum(c['status']=='PASS' for c in checks),'passed;',sum(c['status']=='FAIL' for c in checks),'failed',flush=True)
