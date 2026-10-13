"""Test upgrade against an isolated original-schema fixture, retaining no test database."""
import argparse, json, os, subprocess
from pathlib import Path

parser=argparse.ArgumentParser()
parser.add_argument('--confirm-disposable',action='store_true',required=True)
parser.add_argument('--mysql-home',default=r'C:\Program Files\MySQL\MySQL Server 8.0')
args=parser.parse_args()
root=Path(__file__).resolve().parents[1]
client=[str(Path(args.mysql_home)/'bin/mysql.exe'),'--no-defaults','--host=127.0.0.1','--port=33307','--user=root','--batch','--skip-column-names','--default-character-set=utf8mb4']
def sql(text):
    result=subprocess.run(client,input=text,encoding='utf-8',capture_output=True)
    if result.returncode: raise RuntimeError(result.stderr)
    return result.stdout.strip()
actual=Path(sql('SELECT @@datadir;')).resolve()
expected=(root/'.local-runtime/mysql-data').resolve()
if os.path.normcase(str(actual))!=os.path.normcase(str(expected)): raise SystemExit('Refusing an unrelated database server.')
database='exam_upgrade_test'
try:
    sql(f'DROP DATABASE IF EXISTS {database};')
    original=(root/'scripts/fixtures/s1-s4-schema.sql').read_text(encoding='utf-8').replace('exam_seating',database)
    fixture=f"""
    USE {database};
    INSERT INTO departments(code,name) VALUES ('OLD','Original Department');
    INSERT INTO rooms(room_number,capacity) VALUES ('OLD-R1',2);
    INSERT INTO seats(room_id,seat_number) VALUES (1,'S001'),(1,'S002');
    INSERT INTO courses(course_code,course_name,department_id,semester) VALUES ('OLD301','Original Course',1,5);
    INSERT INTO students(roll_number,name,email,department_id,semester) VALUES ('OLD001','Original Student','original@example.test',1,5);
    INSERT INTO course_enrollments(student_id,course_id) VALUES (1,1);
    INSERT INTO examinations(course_id,exam_date,start_time,end_time) VALUES (1,'2026-10-15','09:00','10:00');
    INSERT INTO examination_rooms VALUES (1,1);
    """
    sql(original+fixture)
    migration=(root/'database/migrations/001_s1_s6_upgrade.sql').read_text(encoding='utf-8').replace('exam_seating',database)
    sql(migration)
    placement=sql(f"USE {database}; SELECT s.roll_number,c.course_code,c.study_year,d.code,a.semester,ce.academic_year_id,e.academic_year_id FROM students s JOIN student_academic_enrollments a ON a.student_id=s.id JOIN divisions d ON d.id=a.division_id JOIN course_enrollments ce ON ce.student_id=s.id JOIN courses c ON c.id=ce.course_id JOIN examinations e ON e.course_id=c.id;")
    assert placement=='OLD001\tOLD301\t3\tA\t5\t1\t1',placement
    seats=sql(f'USE {database}; SELECT seat_number,seat_row,seat_column,is_accessible FROM seats ORDER BY id;')
    assert seats=='S001\t1\t1\t1\nS002\t1\t2\t1',seats
    assert sql(f'USE {database}; SELECT COUNT(*) FROM examination_rooms;')=='1'
    assert sql(f'USE {database}; SELECT COUNT(*) FROM course_offerings;')=='1'
    result={'status':'PASS','preserved':['student','course','enrollment','exam','room','seats','exam-room link'],'annualPlacement':'2026-27 / study year 3 / division A / semester 5','courseOfferingCreated':True,'seatCoordinatesAndAccessibilityDerived':True,'placementQuery':placement,'seatQuery':seats}
    output=root/'docs/testing/evidence/migration-results.json'
    output.write_text(json.dumps(result,indent=2)+'\n',encoding='utf-8')
    print(json.dumps(result,indent=2))
finally:
    sql(f'DROP DATABASE IF EXISTS {database};')
