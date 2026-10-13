"""Generate small HTML shells; shared behavior stays in app-ui.js and page controllers."""
from pathlib import Path

web=Path(__file__).resolve().parents[1]/'src/main/webapp'
def page(path,title,body,controller='',shell='public',resource='',student=''):
    target=web/path
    depth=len(Path(path).parts)-1
    prefix='../'*depth
    if depth:
        body=body.replace('href="pages/timetable.html"', f'href="{prefix}pages/timetable.html"')
    shared=f'{prefix}assets/'
    version='?v=20261013-s7r2' if path in ('pages/admin/seating.html','pages/admin/students.html') else '?v=20261004-ui3'
    staff=shell in ('admin','faculty')
    html=f'''<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
  <meta name="theme-color" content="#203646"><title>{title} | Exam Planner</title>
  <link rel="stylesheet" href="{shared}css/main.css{version}"><link rel="stylesheet" href="{shared}css/ui.css{version}">
</head>
<body data-shell="{shell}" data-resource="{resource}" data-student-page="{student}">
  <a class="skip-link" href="#main">Skip to content</a>
  <header data-app-header></header>
  {'<div class="workspace"><aside data-app-nav></aside>' if staff else ''}
  <main id="main" class="{'content' if staff else 'public-content'}" tabindex="-1">
{body}
  </main>
  {'</div>' if staff else '<footer class="public-footer">Exam Planner · Published examination information</footer>'}
  <script src="{shared}js/api.js{version}"></script><script src="{shared}js/app-ui.js?v=20261013-s7r2"></script>
  {'<script src="'+shared+'js/guard.js'+version+'"></script>' if staff else ''}
  {'<script src="'+shared+'js/'+controller+version+'"></script>' if controller else ''}
</body>
</html>
'''
    target.parent.mkdir(parents=True,exist_ok=True);target.write_text(html,encoding='utf-8')

def heading(title,description,section='Administration'):
    return f'<div class="breadcrumbs">{section} / {title}</div><div class="page-heading"><div><h1 id="pageTitle">{title}</h1><p>{description}</p></div></div>'
def searchbar(placeholder='Search records…',extra=''):
    return f'''<div class="list-toolbar"><label class="search-field">Search<input type="search" id="recordSearch" placeholder="{placeholder}" autocomplete="off"></label>{extra}<label class="page-size">Per page<select id="pageSize"><option>25</option><option>50</option><option>100</option></select></label></div>'''
def pager():
    return '<div class="list-meta"><span id="recordCount" role="status">Loading records…</span><div class="pager"><button id="previousPage" class="secondary" type="button">Previous</button><button id="nextPage" class="secondary" type="button">Next</button></div></div>'

managers={
 'years':('Academic Years','Manage calendar years and choose the year currently in use.','academic year'),
 'departments':('Departments','Keep department names and codes consistent across the planner.','department'),
 'divisions':('Divisions','Organize each department into year-of-study groups and divisions.','division'),
 'students':('Students','Manage student details and their placement in the selected academic year.','student'),
 'enrollments':('Course Enrollments','Enroll students into courses offered in the selected academic year.','enrollment'),
 'faculty':('Faculty','Manage faculty profiles and their staff login accounts.','faculty member'),
 'rooms':('Rooms','Manage physical capacity, seat layout and accessible entry.','room'),
 'courses':('Courses','Maintain the course catalog and academic-year offerings.','course')
}
for resource,(title,description,singular) in managers.items():
    cohort=resource in ('students','divisions','courses')
    extra='<label>Department<select id="departmentFilter"><option value="">All departments</option></select></label><label>Study year<select id="studyFilter"><option value="">All years</option>'+''.join(f'<option value="{i}">{["FY","SY","TY","BE"][i-1]}</option>' for i in range(1,5))+'</select></label>' if cohort else ''
    extra+='<label>Sort by<select id="sortBy"></select></label>'
    body=heading(title,description)+f'''
    <div class="toolbar"><label>Academic year<select id="academicYear" name="academicYear"></select></label><button id="newRecord" type="button">Add {singular}</button></div>
    <p id="notice" role="status" hidden></p>
    <details class="editor" id="editor"><summary id="editorTitle">Add {singular}</summary><div class="editor-body"><p class="helper-text" id="editorHelp"></p><form id="recordForm"><div id="fields" class="grid-form"></div><div class="toolbar form-actions"><button id="saveButton" type="submit">Save {singular}</button><button id="clearButton" type="button" class="secondary">Cancel editing</button></div></form></div></details>
    <section class="panel"><div class="panel-heading"><h2>{title} list</h2><button id="refreshButton" type="button" class="secondary">Refresh</button></div>
    {searchbar('Search names, codes or details…',extra)}
    <div class="table-wrap"><table><thead><tr id="tableHead"></tr></thead><tbody id="tableRows"></tbody></table></div>{pager()}</section>
    '''
    if resource=='rooms':
        body+='''<details class="editor" id="accessibility"><summary>Configure individual accessible seats</summary><div class="editor-body"><p class="helper-text">Configure before assigning the room to an examination. Both room entry and the seat must be accessible.</p><form id="accessibilityForm" class="grid-form"><label>Room<select id="accessibilityRoom"></select></label><label>Seat<select id="seatId" required></select></label><label class="check-label"><input id="seatAccessible" type="checkbox">Accessible seat</label><button type="submit">Save seat accessibility</button></form><p id="seatNotice" role="status" hidden></p></div></details>'''
    page(f'pages/admin/{resource}.html',title,body,'admin.js','admin',resource)

body=heading('Dashboard','Academic records, student counts and examination administration.')+'''
<div class="toolbar"><label>Academic year<select id="academicYear"></select></label></div><p id="notice" role="status" hidden></p>
<section class="cards" id="summary" aria-label="Academic totals"></section>
<nav class="quick-links" aria-label="Quick actions"><a href="students.html?new=1">Add a student</a><a href="exams.html?new=1">Create an exam</a><a href="seating.html">Review seating</a><a href="import-students.html">Import a CSV</a></nav>
<section class="panel"><div class="panel-heading"><h2>Student counts</h2><span class="muted">Student counts by academic-year placement</span></div><label class="search-field">Search department or division<input id="dashboardSearch" type="search" placeholder="Search CSE, SY or division A…" autocomplete="off"></label><p class="helper-text" id="countSummary" role="status"></p><h3>By department and study year</h3><div class="table-wrap"><table><thead><tr><th scope="col">Department / study year</th><th scope="col">Students</th></tr></thead><tbody id="yearCounts"></tbody></table></div>
<details class="editor" open><summary>Division breakdown</summary><div class="editor-body table-wrap"><table><thead><tr><th>Department</th><th>Study year</th><th>Division</th><th>Students</th></tr></thead><tbody id="divisionCounts"></tbody></table></div></details></section>'''
page('pages/admin/dashboard.html','Dashboard',body,'admin.js','admin','dashboard')

body=heading('Exam Scheduling','Plan examinations, review conflicts and open the associated seating chart.')+'''
<div class="toolbar"><button id="newExam" type="button">Create exam</button><a href="seating.html" class="button secondary">Open seating</a></div><p id="message" role="status" hidden></p>
<details id="examEditor" class="editor"><summary id="examEditorTitle">Create an exam</summary><div class="editor-body"><p class="helper-text">Draft inputs are editable before seating history exists. Published examinations retain their recorded details.</p>
<form id="examForm" class="grid-form"><input id="id" type="hidden"><label>Academic year<select id="academicYearId" required></select></label><label>Course offering<select id="courseId" required></select></label><label>Date<input id="examDate" type="date" required></label><label>Start time<input id="startTime" type="time" required></label><label>End time<input id="endTime" type="time" required></label><label>Status<select id="status"><option value="DRAFT">Draft</option><option value="SCHEDULED">Scheduled</option></select></label>
<fieldset class="full panel"><legend>Rooms</legend><label>Find a room<input id="roomSearch" type="search" placeholder="Search room number…" autocomplete="off"></label><p id="roomSelection" class="helper-text" role="status">Select one or more rooms.</p><div id="roomChoices" class="room-picker"></div></fieldset><label class="check-label full"><input id="spacingRequired" type="checkbox">Leave every alternate column empty for horizontal spacing</label><p class="helper-text full">Spacing reduces usable capacity. Accessible-seat availability is checked when generating the seating plan.</p>
<div class="full toolbar"><button id="saveExam" type="submit">Save exam</button><button type="button" id="clearButton" class="secondary">Cancel editing</button></div></form></div></details>
<section class="panel"><div class="panel-heading"><h2>Examinations</h2><button id="refreshExams" type="button" class="secondary">Refresh</button></div>'''+searchbar('Search course, date or room…','<label>Academic year<select id="examYearFilter"><option value="">All academic years</option></select></label><label>Status<select id="statusFilter"><option value="">All statuses</option><option>DRAFT</option><option>SCHEDULED</option><option>COMPLETED</option><option>CANCELLED</option></select></label>')+'''<div class="table-wrap"><table><thead><tr><th>Course</th><th>Academic year</th><th>Date / time</th><th>Rooms</th><th>Status</th><th>Actions</th></tr></thead><tbody id="examRows"></tbody></table></div>'''+pager()+'</section>'
page('pages/admin/exams.html','Exam Scheduling',body,'exams.js','admin')

body='''<div class="breadcrumbs">Administration / Seating Arrangements</div>
<div class="page-heading"><div><h1 id="pageTitle">Seating Arrangements</h1><p>Allocate seats, manage accommodation requirements and record manual changes.</p></div></div>
<p id="message" role="status" hidden></p>
<p id="operationProgress" class="helper-text" role="status" hidden></p>
<section class="panel">
  <label>Examination<select id="examId"></select></label>
  <p id="summary" class="helper-text"></p><p id="accommodationSummary" class="helper-text"></p>
  <div class="toolbar"><button id="generateButton" type="button">Generate seating</button><button id="openOverride" type="button" class="secondary">Manual seating changes</button><button id="openAccommodation" type="button" class="secondary">Accommodation requirements</button><a href="exams.html">Manage exams</a></div>
  <p class="helper-text">Accessible seating requirements take priority. Regeneration preserves manual assignments by default.</p>
  <details><summary>Advanced regeneration options</summary><div class="toolbar form-actions"><label class="check-label"><input id="replaceManual" type="checkbox">Replace manual assignments too</label><button id="clearButton" type="button" class="danger">Clear seating</button></div></details>
</section>
<details class="editor" id="accommodationEditor"><summary>Accommodation requirements</summary><div class="editor-body">
  <p class="helper-text">Record only the seating requirement. This applies to the student's active examinations across academic years. Generated plans are updated together; ungenerated plans remain ungenerated.</p>
  <form id="accommodationForm" class="grid-form">
    <label>Student<select id="accommodationStudentId" required></select></label>
    <label class="check-label"><input id="requiresAccessible" type="checkbox">Accessible seating required</label>
    <label class="full">Reason<input id="accommodationReason" maxlength="250" required autocomplete="off"></label>
    <p class="helper-text full">Other students' manual assignments are preserved. An incompatible manual seat for this student is released for accessible allocation. Insufficient accessible capacity leaves the requirement and all seating unchanged.</p>
    <button id="accommodationButton" type="submit">Review accommodation change</button>
  </form>
</div></details>
<details class="editor" id="manualEditor"><summary>Manual seating changes</summary><div class="editor-body">
  <form id="overrideForm" class="grid-form">
    <label>Student<select id="studentId" required></select></label>
    <label>Action<select id="action"><option value="reassign">Move to empty seat</option><option value="swap">Swap occupied seats</option></select></label>
    <label>Target seat<select id="seatId" required></select></label>
    <label>Reason<input id="reason" maxlength="250" required autocomplete="off"></label>
    <p id="overrideHelp" class="helper-text full"></p><p id="selectionSummary" class="helper-text full" role="status"></p>
    <button id="overrideButton" type="submit">Review manual change</button>
  </form>
</div></details>
<section class="panel">
  <div class="list-toolbar"><label class="search-field">Find a student or seat<input id="seatSearch" type="search" placeholder="Search roll number, name or seat…" autocomplete="off"></label><label>Room<select id="chartRoom"><option value="">All rooms</option></select></label></div>
  <p id="seatSearchCount" class="helper-text" role="status"></p>
  <p class="helper-text">Click an occupied seat to select a student, then click the destination. Choose another source student from the manual-change form.</p>
  <div class="legend"><span>Assigned</span><span class="accessible-key">Accessible seat</span><span class="spacing-key">Reserved for spacing</span><span class="manual-key">Manual assignment</span><span class="requirement-key">Accommodation required</span></div>
</section>
<div id="charts"></div>
<details class="editor" id="historyEditor"><summary>Change history</summary><div class="editor-body"><p class="helper-text">Actor, reason and previous/new seats are retained. Shows the latest 200 events.</p><label>Search history<input id="historySearch" type="search" placeholder="Search student, action or reason…" autocomplete="off"></label><div class="table-wrap"><table><thead><tr><th>When</th><th>By</th><th>Student</th><th>Action</th><th>From</th><th>To</th><th>Reason</th></tr></thead><tbody id="history"></tbody></table></div><div class="list-meta"><span id="historyCount" role="status"></span><div class="pager"><button id="historyPrevious" type="button" class="secondary">Previous</button><button id="historyNext" type="button" class="secondary">Next</button></div></div></div></details>
<dialog id="operationDialog" aria-labelledby="operationTitle">
  <form id="confirmationForm">
    <h2 id="operationTitle"></h2><div id="operationDetails"></div>
    <label>Reason<input id="operationReason" maxlength="250" required autocomplete="off"></label>
    <label id="replacementAcknowledgement" class="check-label" hidden><input id="confirmReplacement" type="checkbox">I confirm that manual assignments will be replaced.</label>
    <div class="toolbar form-actions"><button id="confirmOperation" type="submit">Confirm</button><button id="cancelOperation" type="button" class="secondary">Cancel</button></div>
  </form>
</dialog>'''
page('pages/admin/seating.html','Seating Arrangements',body,'seating.js','admin')

body=heading('Import Students','Import student records and course enrollments from a CSV file.')+'''
<section class="import-steps"><article class="card"><small>01 · Prepare</small><h3>Sample CSV</h3><p>12 new students, 2 workshop courses and 24 enrollment rows.</p><p><a href="../../assets/data/evaluator-import.csv" download>Download sample CSV</a></p></article><article class="card"><small>02 · Review</small><h3>Required records</h3><p>Sample requirements: academic year 2026-27, CSE FY divisions A/B and course offerings CSE103/CSE104.</p></article><article class="card"><small>03 · Import</small><h3>Import summary</h3><p>New students, added enrollments, duplicates and row errors are reported separately.</p></article></section>
<section class="panel"><h2>CSV upload</h2><form id="importForm" class="stack-form"><label>CSV file<input id="file" name="file" type="file" accept=".csv,text/csv" required></label><p id="fileSummary" class="helper-text" role="status">Choose a file to preview its first rows.</p><div id="preview" class="table-wrap" hidden></div><button id="importButton" type="submit">Import students</button></form></section><section id="result" class="panel" role="status" hidden></section>
<details class="editor"><summary>File format and import rules</summary><div class="editor-body"><p><a href="../../assets/data/student_import_template.csv" download>Download format template</a></p><p class="helper-text">One row per student/course enrollment. Reimporting the sample reports duplicates. Use unpublished courses without generated seating for new enrollments.</p><div class="table-wrap"><code>roll_number,name,email,department_code,semester,course_code,academic_year,division,requires_accessible</code></div><p class="helper-text">Requires accessible: true or false. Plain comma-separated fields only; quoted commas are unsupported. Academic year, division and course offering must already exist.</p></div></details>'''
page('pages/admin/import-students.html','Import Students',body,'import-students.js','admin')

body=heading('Administrator Accounts','Create administrator accounts and reset account passwords.')+'''
<p class="helper-text">Faculty profiles and login accounts are managed in <a href="faculty.html">Faculty</a>.</p>
<details class="editor" open><summary>Create an administrator</summary><div class="editor-body"><form id="userForm" class="grid-form"><label>Name<input id="name" autocomplete="name" required maxlength="100"></label><label>Email<input id="email" type="email" autocomplete="email" required></label><label>Username<input id="username" autocomplete="off" spellcheck="false" required maxlength="50"></label><label>Initial password<input id="password" type="password" autocomplete="new-password" required minlength="8"></label><p class="helper-text full">Use 8 or more characters with uppercase, lowercase and a number.</p><p id="message" class="full" role="status" hidden></p><button type="submit">Create account</button></form></div></details>
<details class="editor" id="resetAccount"><summary>Reset an account password</summary><div class="editor-body"><p class="helper-text">Match the existing account username and registered email. This action requires an administrator.</p><form id="adminResetForm" class="grid-form"><label>Account username<input id="resetUsername" autocomplete="off" required></label><label>Registered email<input id="resetEmail" type="email" autocomplete="off" required></label><label>New password<input id="newPassword" type="password" autocomplete="new-password" required minlength="8"></label><button type="submit">Reset account password</button><p id="resetMessage" class="full" role="status" hidden></p></form></div></details>'''
page('pages/admin/create-user.html','Administrator Accounts',body,'accounts.js','admin')

def student_body(home=True):
    finder='''<section class="panel finder"><h2>Seat lookup</h2><p class="helper-text">Search published seating by roll number and academic year.</p><form id="searchForm" class="stack-form"><label>Academic year<select id="academicYear" required></select></label><label>Roll number<input id="rollNumber" name="rollNumber" required maxlength="30" autocomplete="off" spellcheck="false" placeholder="For example, CSE26Y1A001…"></label><button id="searchButton" type="submit">Search seating</button></form><p id="message" role="status" hidden></p><p class="helper-text">Only published seating arrangements are displayed.</p></section>'''
    hero=f'''<section class="hero"><div class="hero-copy"><p class="eyebrow">Student examination portal</p><h1>Examination Seating</h1><p>View examination schedules and published seating arrangements.</p><a href="{'pages/timetable.html' if home else '../timetable.html'}" class="button secondary" data-timetable-link>Examination timetable</a></div>{finder}</section>'''
    results='''<section id="studentResults" hidden><div class="result-header"><div><p class="eyebrow">Published seating</p><h2 id="studentName">Examination seating</h2></div><button id="printResults" class="secondary" type="button">Print details</button></div><div id="results" class="result-grid"></div></section>'''
    preview='''<section id="examPreview"><div class="section-heading"><div><h2>Examination schedule</h2></div><a data-timetable-link href="pages/timetable.html">View full timetable</a></div><div class="list-toolbar"><label class="search-field">Search published exams<input id="previewSearch" type="search" placeholder="Search course or room…" autocomplete="off"></label></div><p id="previewCount" class="helper-text" role="status"></p><div id="upcomingExams" class="exam-preview"></div></section>''' if home else ''
    return hero+results+preview
page('index.html','Student Portal',student_body(),'student-home.js',student='home')
page('pages/student/dashboard.html','Student Portal',student_body(),'student-home.js',student='home')
page('pages/student/seating.html','Seat Lookup',student_body(False),'student-home.js',student='seating')

body=heading('Examination Timetable','Search published examinations by course or room, and check the selected academic year.','Student portal')+'''
<p id="message" role="status" hidden></p><div class="list-toolbar"><label>Academic year<select id="academicYear"></select></label><label class="search-field">Search examinations<input id="recordSearch" type="search" placeholder="Search course, date or room…" autocomplete="off"></label><label class="page-size">Per page<select id="pageSize"><option>25</option><option>50</option><option>100</option></select></label></div><div class="toolbar"><div class="segmented" aria-label="Timetable view"><button id="listButton" type="button" aria-pressed="true">List</button><button id="calendarButton" type="button" aria-pressed="false">By date</button></div><button id="printTimetable" type="button" class="secondary">Print timetable</button></div>
<section id="listView" class="panel table-wrap"><table><thead><tr><th>Academic year</th><th>Date</th><th>Course</th><th>Time</th><th>Rooms</th><th>Status</th></tr></thead><tbody id="timetableRows"></tbody></table></section><section id="calendarView" class="cards" hidden></section>'''+pager()
page('pages/timetable.html','Examination Timetable',body,'timetable.js')

body=heading('Faculty Dashboard','Published examination information for faculty.','Faculty')+'''<section class="cards"><article class="card"><h3>Examination timetable</h3><p>Search courses, rooms and dates in the published schedule.</p><p><a href="../timetable.html">View timetable</a></p></article></section><p class="helper-text">Invigilation duty allocation is currently unavailable.</p>'''
page('pages/faculty/dashboard.html','Faculty Dashboard',body,'faculty.js','faculty')

body='''<div class="auth-shell"><section class="auth-card"><p class="eyebrow">Staff workspace</p><h1>Staff Login</h1><p class="muted">Sign in to manage academic records and examinations.</p><form id="loginForm" class="stack-form"><label>Username<input id="username" name="username" autocomplete="username" spellcheck="false" required></label><label>Password<div class="password-control"><input id="password" name="password" aria-label="Password" type="password" autocomplete="current-password" required><button id="togglePassword" class="secondary" type="button" aria-pressed="false">Show</button></div></label><p id="message" role="status" hidden></p><button id="loginButton" type="submit">Sign in</button></form><div class="auth-links"><a href="pages/auth/forgot-password.html">Password recovery</a><a href="index.html">Return to student portal</a></div></section></div>'''
page('login.html','Staff Login',body,'login.js')
page('pages/auth/forgot-password.html','Password Help','''<section class="auth-card"><p class="eyebrow">Staff account help</p><h1>Password help</h1><p class="muted">Contact the examination administrator for account recovery. Provide the account username and registered email.</p><a href="../../login.html" class="button secondary">Back to sign in</a></section>''')
page('pages/auth/reset-password.html','Account Recovery','''<section class="auth-card"><h1>Account recovery</h1><p class="muted">An administrator can reset a matching account from the staff workspace.</p><a href="../admin/create-user.html#resetAccount" class="button">Open account recovery</a><p><a href="../../login.html">Staff sign in</a></p></section>''')
print('Generated shared-shell student, staff and management pages.')
