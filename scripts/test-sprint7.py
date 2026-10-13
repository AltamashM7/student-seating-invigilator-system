"""Sprint 7 API checks. Run only against a separate disposable test instance."""
import argparse, http.cookiejar, json, urllib.error, urllib.parse, urllib.request
from pathlib import Path

parser=argparse.ArgumentParser()
parser.add_argument('--base-url',default='http://127.0.0.1:33090/exam-seating')
parser.add_argument('--confirm-disposable',action='store_true',required=True)
args=parser.parse_args()
base=args.base_url.rstrip('/')
checks=[]
class Client:
    def __init__(self):
        self.opener=urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))
    def request(self,path,values=None,expected=200):
        body=urllib.parse.urlencode(values).encode() if values is not None else None
        try:
            with self.opener.open(urllib.request.Request(base+path,data=body),timeout=20) as response:
                status=response.status;data=json.load(response)
        except urllib.error.HTTPError as error:
            status=error.code;data=json.load(error)
        if status!=expected: raise AssertionError(f'{path}: expected {expected}, got {status}: {data}')
        return data
admin=Client();public=Client();faculty=Client()
def check(name,condition=True):
    if not condition: raise AssertionError(name)
    checks.append(dict(name=name,status='PASS'));print('PASS',name,flush=True)
def save(resource,**values):
    return admin.request('/api/admin/data',dict(resource=resource,academicYearId=1,action='create',**values))['id']
def chart(exam):return admin.request(f'/api/admin/seating?examId={exam}')
def preview(exam,replace=False):
    return admin.request(f'/api/admin/seating?examId={exam}&action=preview&replaceManual={str(replace).lower()}')
def change(exam,action='generate',expected=200,**values):
    return admin.request('/api/admin/seating',dict(examId=exam,action=action,reason='Sprint 7 isolated API verification',**values),expected)
def generate(exam,replace=False,confirm=False):
    p=preview(exam,replace)
    return change(exam,replaceManual=str(replace).lower(),confirmReplaceManual=str(confirm).lower(),expectedRevision=p['revision'])
def assigned(exam):
    return {s['studentId']:s for s in chart(exam)['seats'] if s['studentId']}
def snapshot(exam):return chart(exam)['revision']
def tagpreview(exam,student,required,expected=200):
    return admin.request(f'/api/admin/seating?examId={exam}&action=previewAccommodation&studentId={student}&requiresAccessible={str(required).lower()}',expected=expected)
def tag(exam,student,required):
    p=tagpreview(exam,student,required)
    result=change(exam,'accommodation',studentId=student,requiresAccessible=str(required).lower(),expectedRevision=p['revision'])
    return p,result
def studentflag(student):
    return next(s['requiresAccessible'] for s in admin.request('/api/admin/data?academicYearId=1')['students'] if s['id']==student)
def create_exam(course,room,date='2028-01-15',start='09:00',end='10:00',spaced=False):
    return admin.request('/api/admin/exams',dict(action='create',academicYearId=1,courseId=course,roomId=room,examDate=date,startTime=start,endTime=end,status='DRAFT',spacingRequired=str(spaced).lower()))['id']

try:
    public.request('/api/admin/seating?examId=1',expected=401)
    check('unauthenticated seating request rejected')
    admin.request('/api/auth/login',dict(username='admin',password='Admin@123'))
    faculty.request('/api/auth/login',dict(username='cse.faculty1',password='Faculty@123'))
    faculty.request('/api/admin/seating',dict(examId=1,action='generate',reason='unauthorized'),expected=403)
    check('faculty cannot mutate admin seating')
    dep=save('departments',code='S7',name='Sprint 7 verification')
    division=save('divisions',departmentId=dep,studyYear=1,code='A')
    courses=[save('courses',code=f'S7{letter}',name=f'Sprint 7 {letter}',departmentId=dep,studyYear=1,semester=1) for letter in 'ABCD']
    room=save('rooms',roomNumber='S7-R101',capacity=12,seatColumns=4,isAccessible='true')
    inaccessible=save('rooms',roomNumber='S7-R102',capacity=6,seatColumns=3,isAccessible='false')
    spacedroom=save('rooms',roomNumber='S7-R103',capacity=8,seatColumns=4,isAccessible='true')
    data=admin.request('/api/admin/data?academicYearId=1')
    accessible_third=next(s['id'] for s in data['seats'] if s['roomId']==room and s['seatNumber']=='S003')
    admin.request('/api/admin/data',dict(resource='seats',action='update',id=accessible_third,isAccessible='true'))
    # An individually marked seat in a room without accessible entry still cannot qualify.
    inaccessible_seat=next(s['id'] for s in data['seats'] if s['roomId']==inaccessible and s['seatNumber']=='S001')
    admin.request('/api/admin/data',dict(resource='seats',action='update',id=inaccessible_seat,isAccessible='true'))
    students=[save('students',rollNumber=f'S7-00{i+1}',name=name,email=f's7-{i+1}@example.test',divisionId=division,semester=1,requiresAccessible='false') for i,name in enumerate(['Ananya Shah','Dev Patel','Isha Joshi','Rohan Desai','Meera Nair'])]
    for course in courses[:2]:
        for student in students:save('enrollments',studentId=student,courseId=course)
    first=create_exam(courses[0],room)
    second=create_exam(courses[1],room,start='11:00',end='12:00')
    tag(first,students[4],True)
    check('tag before allocation saves flag without generating any examination',studentflag(students[4]) and not assigned(first) and not assigned(second))
    p=preview(first)
    check('generation preview makes no saved assignments',p['assigned']==5 and len(p['changes'])==5 and not assigned(first))
    change(first,'reassign',409,studentId=students[0],seatId=chart(first)['seats'][0]['id'])
    check('manual API rejects enrolled student without an assignment',not assigned(first))
    generate(first);generate(second)
    admin.request('/api/admin/exams',dict(action='status',id=second,status='SCHEDULED'))
    check('published generated examination remains eligible for recorded seating operations',chart(second)['exam']['status']=='SCHEDULED')
    allseats=chart(first)['seats'];a=assigned(first)
    check('priority allocation reserves qualifying accessible seats',a[students[4]]['isAccessible'] and all(not a[s]['isAccessible'] for s in students[:4]))
    pwd=a[students[4]];regular=a[students[0]];other=a[students[1]]
    ordinary_empty=next(s for s in allseats if not s['studentId'] and not s['isAccessible'])
    change(first,'reassign',409,studentId=students[4],seatId=ordinary_empty['id'])
    check('accommodation move into an inaccessible seat rejected')
    old=snapshot(first)
    change(first,'swap',409,studentId=students[0],seatId=pwd['id'])
    check('invalid swap protects both students and leaves plan unchanged',snapshot(first)==old)
    outside=next(s['id'] for s in data['seats'] if s['roomId']==inaccessible)
    change(first,'reassign',409,studentId=students[0],seatId=outside)
    check('manual target outside examination rooms rejected')
    change(first,'reassign',409,studentId=students[0],seatId=other['id'])
    change(first,'swap',409,studentId=students[0],seatId=ordinary_empty['id'])
    change(first,'reassign',409,studentId=students[0],seatId=regular['id'])
    check('occupied move, empty swap and no-op rejected')
    change(first,'reassign',studentId=students[0],seatId=ordinary_empty['id'],expectedRevision=old)
    check('valid reassign marks changed seat manual',assigned(first)[students[0]]['id']==ordinary_empty['id'] and assigned(first)[students[0]]['isManual'])
    stale=snapshot(first)
    change(first,'swap',studentId=students[0],seatId=other['id'],expectedRevision=stale)
    a=assigned(first)
    check('valid swap changes and pins both assignments',a[students[0]]['id']==other['id'] and a[students[1]]['id']==ordinary_empty['id'] and a[students[0]]['isManual'] and a[students[1]]['isManual'])
    change(first,'reassign',409,studentId=students[2],seatId=ordinary_empty['id'],expectedRevision=stale)
    check('stale manual request rejected before applying changes')
    pins={s:r['id'] for s,r in a.items() if r['isManual']}
    p=preview(first);generate(first)
    check('regeneration preview and save preserve manual assignments',p['preservedManual']==2 and all(assigned(first)[s]['id']==seat and assigned(first)[s]['isManual'] for s,seat in pins.items()))
    history=chart(first)['history']
    check('swap history records both sides, actor, time and reason',sum(h['action']=='SWAP' for h in history)==2 and all(h['actor']=='admin' and h['createdAt'] and h['reason'] and h['oldSeat'] and h['newSeat'] for h in history if h['action']=='SWAP'))
    p=preview(first,True);before=snapshot(first)
    change(first,expected=409,replaceManual='true',expectedRevision=p['revision'])
    check('manual replacement requires server confirmation',snapshot(first)==before)
    generate(first,True,True)
    check('confirmed full replacement clears all manual pins',not any(s['isManual'] for s in assigned(first).values()))
    p=preview(first);ordinary_empty=next(s for s in chart(first)['seats'] if not s['studentId'] and not s['isAccessible'])
    change(first,'reassign',studentId=students[2],seatId=ordinary_empty['id'])
    change(first,expected=409,expectedRevision=p['revision'])
    check('stale regeneration preview cannot overwrite a newer manual change')
    # Pin a future accommodation student to an ordinary seat and preserve a different manual pin.
    target=next(s for s in chart(first)['seats'] if not s['studentId'] and not s['isAccessible'])
    change(first,'reassign',studentId=students[0],seatId=target['id'])
    retained=assigned(first)[students[2]]['id']
    p,result=tag(first,students[0],True)
    check('tagging regenerates both active generated examinations',len(result['affectedExams'])==2 and assigned(first)[students[0]]['isAccessible'] and assigned(second)[students[0]]['isAccessible'] and studentflag(students[0]))
    check('incompatible pin released only for tagged student',p['releasedManual']==1 and not assigned(first)[students[0]]['isManual'] and assigned(first)[students[2]]['id']==retained and assigned(first)[students[2]]['isManual'])
    check('accommodation change is audited',any(h['action']=='ACCOMMODATION_ON' and h['studentId']==students[0] for h in chart(first)['history']))
    p=tagpreview(first,students[0],False)
    target=next(s for s in chart(second)['seats'] if not s['studentId'] and not s['isAccessible'])
    change(second,'reassign',studentId=students[2],seatId=target['id'])
    before=snapshot(first)
    change(first,'accommodation',409,studentId=students[0],requiresAccessible='false',expectedRevision=p['revision'])
    check('accommodation preview detects changes in another affected examination',studentflag(students[0]) and snapshot(first)==before)
    tag(first,students[0],False)
    check('removing accommodation requirement rebalances ordinary seats',not studentflag(students[0]) and not assigned(first)[students[0]]['isAccessible'])
    save('enrollments',studentId=students[0],courseId=courses[2])
    third=create_exam(courses[2],inaccessible,date='2028-01-16')
    generate(third)
    before=[snapshot(e) for e in (first,second,third)]
    tagpreview(first,students[0],True,400)
    change(first,'accommodation',409,studentId=students[0],requiresAccessible='true',expectedRevision='invalid')
    check('insufficient accessible capacity rolls back tag and every affected plan',not studentflag(students[0]) and before==[snapshot(e) for e in (first,second,third)])
    check('accessible seat needs accessible room entry',not any(s['isAccessible'] for s in chart(third)['seats']))
    admin.request('/api/admin/exams',dict(action='status',id=third,status='CANCELLED'))
    tag(first,students[0],True)
    check('cancelled examination excluded from accommodation regeneration',assigned(first)[students[0]]['isAccessible'] and not assigned(third)[students[0]]['isAccessible'])
    for student in (students[1],students[4]):save('enrollments',studentId=student,courseId=courses[3])
    fourth=create_exam(courses[3],spacedroom,date='2028-01-17',spaced=True)
    generate(fourth)
    spacedchart=chart(fourth)
    check('spacing priority respects usable accessible capacity',all(s['columnNumber']%2==1 for s in spacedchart['seats'] if s['studentId']) and assigned(fourth)[students[4]]['isAccessible'])
    blocked=next(s for s in spacedchart['seats'] if s['columnNumber']%2==0)
    change(fourth,'reassign',409,studentId=students[1],seatId=blocked['id'])
    check('spacing remains enforced in manual operations')
    saved=snapshot(first)
    admin.request('/api/admin/seating',dict(examId=first,action='clear',reason=' '),expected=409)
    check('blank audit reason rejected with no seating changes',snapshot(first)==saved)
    admin.request('/api/admin/exams',dict(action='status',id=first,status='SCHEDULED'))
    admin.request('/api/admin/exams',dict(action='status',id=first,status='COMPLETED'))
    change(first,expected=409)
    change(first,'reassign',409,studentId=students[2],seatId=target['id'])
    tagpreview(first,students[1],True,400)
    check('completed examinations reject generation, manual edits and tagging')
    oldhistory=len(chart(third)['history'])
    change(third,'clear')
    check('clearing cancelled seating retains history',not assigned(third) and len(chart(third)['history'])>oldhistory)
    # Leave the second draft available for the bounded browser workflow check.
    Path('.local-runtime/s7-check/browser-fixture.json').write_text(json.dumps(dict(examId=second,studentId=students[3],baseUrl=base))+'\n')
except Exception as error:
    checks.append(dict(name=str(error),status='FAIL'))
    raise
finally:
    evidence=Path('docs/testing/evidence/sprint7-api-results.json')
    evidence.write_text(json.dumps(dict(baseUrl=base,checks=checks,passed=sum(c['status']=='PASS' for c in checks),failed=sum(c['status']=='FAIL' for c in checks)),indent=2)+'\n',encoding='utf-8')
    print('RESULT',sum(c['status']=='PASS' for c in checks),'passed;',sum(c['status']=='FAIL' for c in checks),'failed')
