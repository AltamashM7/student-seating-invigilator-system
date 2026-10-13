"""Deterministic fictional records for an empty S1-S6 database, never real student data."""
from pathlib import Path
import hashlib, csv

def quote(value):
    return "'" + str(value).replace("'", "''") + "'"

sql = ['-- Fictional evaluation data. Run once after schema.sql and seed.sql on a fresh database.', 'USE exam_seating;', 'START TRANSACTION;']
first = ['Atharva','Shruti','Omkar','Sayali','Siddharth','Pooja','Harsh','Sakshi','Akash','Vaishnavi','Niraj','Sanika','Abhishek','Prajakta','Saurabh','Rutuja','Tejas','Mitali','Shreyas','Anushka','Pratik','Gauri','Rohit','Swarali']
last = ['Deshpande','Kulkarni','Joshi','Patil','Pawar','Shinde','More','Jadhav','Kadam','Nair','Iyer','Shah','Rao','Bhat','Singh','Verma']
subjects = {
    'CSE': [('Programming Fundamentals','Engineering Mathematics'),('Data Structures','Database Management Systems'),('Operating Systems','Computer Networks'),('Distributed Systems','Software Testing')],
    'IT': [('Introduction to Computing','Applied Mathematics'),('Web Technology','Object Oriented Programming'),('Information Security','Data Communication'),('Cloud Computing','Data Analytics')],
    'ENTC': [('Basic Electronics','Engineering Physics'),('Digital Electronics','Circuit Analysis'),('Signals and Systems','Microcontrollers'),('Wireless Communication','Embedded Systems')]
}
faculty_password=hashlib.sha256(b'Faculty@123').hexdigest()
for dept_index,(dept,years) in enumerate(subjects.items()):
    dep=f"(SELECT id FROM departments WHERE code={quote(dept)})"
    for year in range(1,5):
        for division in ['A','B']:
            sql.append(f"INSERT INTO divisions(academic_year_id,department_id,study_year,code) VALUES (1,{dep},{year},{quote(division)});")
        codes=[]
        for index,title in enumerate(years[year-1],1):
            code=f'{dept}{year}{index:02d}';codes.append(code)
            sql.append(f"INSERT INTO courses(course_code,course_name,department_id,semester,study_year) VALUES ({quote(code)},{quote(title)},{dep},{year*2-1},{year});")
            sql.append('SET @course=LAST_INSERT_ID(); INSERT INTO course_offerings VALUES (@course,1);')
        for div_index,division in enumerate(['A','B']):
            group=f"(SELECT id FROM divisions WHERE academic_year_id=1 AND department_id={dep} AND study_year={year} AND code={quote(division)})"
            for index in range(1,11):
                serial=dept_index*80+(year-1)*20+div_index*10+index
                name=f'{first[(serial-1)%len(first)]} {last[((serial-1)//len(first)+index)%len(last)]}'
                roll=f'{dept}26Y{year}{division}{index:03d}'
                sql.append(f"INSERT INTO students(roll_number,name,email,department_id,semester,requires_accessible) VALUES ({quote(roll)},{quote(name)},{quote(roll.lower()+'@example.test')},{dep},{year*2-1},{int(index==1)});")
                sql.append(f'SET @student=LAST_INSERT_ID(); INSERT INTO student_academic_enrollments VALUES (@student,1,{group},{year*2-1});')
                for code in codes:
                    sql.append(f"INSERT INTO course_enrollments(student_id,course_id,academic_year_id) SELECT @student,id,1 FROM courses WHERE course_code={quote(code)};")
        faculty_name=f'{first[(dept_index*4+year+10)%len(first)]} {last[(dept_index+year+3)%len(last)]}'
        user=f'{dept.lower()}.faculty{year}'
        sql.append(f"INSERT INTO users(username,password_hash,role) VALUES ({quote(user)},{quote(faculty_password)},'FACULTY');")
        sql.append(f"INSERT INTO faculty(user_id,employee_code,name,email,department_id) VALUES (LAST_INSERT_ID(),{quote(dept+'-F'+str(year))},{quote(faculty_name)},{quote(user+'@example.test')},{dep});")
# Unpublished workshop offerings remain editable for the evaluator CSV demonstration.
for code,title in [('CSE103','Computer Workshop'),('CSE104','Computational Thinking')]:
    sql.append(f"INSERT INTO courses(course_code,course_name,department_id,semester,study_year) SELECT {quote(code)},{quote(title)},id,1,1 FROM departments WHERE code='CSE'; SET @course=LAST_INSERT_ID(); INSERT INTO course_offerings VALUES (@course,1);")
for room in range(1,9):
    sql.append(f"INSERT INTO rooms(room_number,capacity,seat_columns,is_accessible) VALUES ('R10{room}',30,6,TRUE); SET @room=LAST_INSERT_ID();")
    for seat in range(1,31):
        sql.append(f"INSERT INTO seats(room_id,seat_number,seat_row,seat_column,is_accessible) VALUES (@room,'S{seat:03d}',{(seat-1)//6+1},{(seat-1)%6+1},{int(seat<=2)});")
for dep_index,dept in enumerate(subjects):
    for year in range(1,5):
        date=f'2026-10-{12+dep_index*4+year-1:02d}'
        for index,start,end in [(1,'09:00:00','10:30:00'),(2,'11:00:00','12:30:00')]:
            sql.append(f"INSERT INTO examinations(course_id,academic_year_id,exam_date,start_time,end_time,status,spacing_required) SELECT id,1,'{date}','{start}','{end}','DRAFT',{int(index==2)} FROM courses WHERE course_code='{dept}{year}{index:02d}'; SET @exam=LAST_INSERT_ID();")
            sql.append("INSERT INTO examination_rooms SELECT @exam,id FROM rooms WHERE room_number IN ('R101','R102');")
sql.extend(['COMMIT;','-- 240 students, 480 enrollments, 24 divisions, 26 courses (2 CSV workshops), 24 exams, 12 faculty, 8 rooms/240 seats.'])
Path('database/demo-data.sql').write_text('\n'.join(sql)+'\n',encoding='utf-8')
headers=['roll_number','name','email','department_code','semester','course_code','academic_year','division','requires_accessible']
with Path('database/evaluator-import.csv').open('w',newline='',encoding='utf-8') as file:
    writer=csv.writer(file);writer.writerow(headers)
    for i in range(1,13):
        roll=f'DEMO26CSE{i:03d}'
        name=f'{first[(i+5)%len(first)]} {last[(i+2)%len(last)]}'
        for course in ['CSE103','CSE104']:
            writer.writerow([roll,name,roll.lower()+'@example.test','CSE',1,course,'2026-27','A' if i<=6 else 'B','true' if i in (1,7) else 'false'])
assets=Path('src/main/webapp/assets/data');assets.mkdir(exist_ok=True)
(assets/'evaluator-import.csv').write_bytes(Path('database/evaluator-import.csv').read_bytes())
(assets/'student_import_template.csv').write_bytes(Path('database/student_import_template.csv').read_bytes())
print('Created database/demo-data.sql: 240 fictional students with linked academic records.')
print('Created evaluator-import.csv: 24 rows, 12 new students, 2 available workshop offerings.')
