/* Browser-only training model. Java/Tomcat/MySQL remain the real implementation. */
(function (global) {
  'use strict';
  const copy = value => JSON.parse(JSON.stringify(value));
  const fail = (message, status = 409) => { const e = new Error(message); e.status = status; throw e; };
  const same = (a, b) => String(a).toLowerCase() === String(b).toLowerCase();
  const flag = value => value === true || value === 1 || value === '1' || value === 'true';
  const active = exam => ['DRAFT', 'SCHEDULED'].includes(exam.status);
  const published = exam => ['SCHEDULED', 'COMPLETED'].includes(exam.status);
  const email = value => !value || /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value);
  const password = value => typeof value === 'string' && value.length >= 8 && /[A-Z]/.test(value) && /[a-z]/.test(value) && /\d/.test(value);
  const order = (a, b) => String(a).toLowerCase() < String(b).toLowerCase() ? -1 : String(a).toLowerCase() > String(b).toLowerCase() ? 1 : 0;
  const number = (value, low = 1, high = Number.MAX_SAFE_INTEGER) => {
    const n = Number(value);
    if (!Number.isInteger(n) || n < low || n > high) fail('Invalid number or linked record');
    return n;
  };
  const text = (value, max = 120) => {
    const s = String(value ?? '').trim();
    if (!s || s.length > max) fail(`A value of 1–${max} characters is required`);
    return s;
  };
  class DemoEngine {
    constructor(seed) { this.state = copy(seed); }
    exportState() { return copy(this.state); }
    next(table) { return Math.max(0, ...this.state[table].map(r => r.id || 0)) + 1; }
    find(table, id) { return this.state[table].find(r => r.id === Number(id)) || fail('Record not found'); }
    user(id) { return this.state.users.find(u => u.id === id); }
    year(id) { return id ? this.find('years', id).id : (this.state.years.find(y => flag(y.isCurrent)) || this.state.years[0]).id; }
    unique(table, key, value, id = 0) {
      if (this.state[table].some(r => r.id !== id && r[key] != null && same(r[key], value))) fail(`Duplicate ${key}`);
    }
    roster(e) {
      const ids = new Set(this.state.enrollments.filter(r => r.courseId === e.courseId && r.academicYearId === e.academicYearId).map(r => r.studentId));
      return this.state.students.filter(s => ids.has(s.id)).sort((a, b) => order(a.rollNumber, b.rollNumber) || a.id - b.id);
    }
    assignments(id) { return this.state.assignments.filter(a => a.examId === id); }
    examView(e) {
      const c = this.find('courses', e.courseId), y = this.find('years', e.academicYearId);
      return {...copy(e), courseCode:c.code, courseName:c.name, academicYear:y.label,
        rooms:e.roomIds.map(id => this.find('rooms', id)).sort((a,b)=>order(a.roomNumber,b.roomNumber)).map(r=>({id:r.id,roomNumber:r.roomNumber,capacity:r.capacity}))};
    }
    examList(publicOnly = false, year = 0) {
      return this.state.exams.filter(e => (!publicOnly || published(e)) && (!year || e.academicYearId === year))
        .map(e => this.examView(e)).sort((a,b)=>order(a.examDate+a.startTime+a.courseCode,b.examDate+b.startTime+b.courseCode));
    }
    snapshot(year) {
      const d = this.state, y = this.year(year);
      const divisions = d.divisions.map(v => ({...v,departmentCode:this.find('departments',v.departmentId).code}));
      const students = d.students.map(s => {
        const p = d.placements.find(p => p.studentId === s.id && p.academicYearId === y);
        const v = p && this.find('divisions', p.divisionId);
        return {...s,divisionId:p?.divisionId ?? null,semester:p?.semester ?? null,departmentId:v?.departmentId ?? null,
          studyYear:v?.studyYear ?? null,divisionCode:v?.code ?? null,departmentCode:v ? this.find('departments',v.departmentId).code : null};
      }).sort((a,b)=>order(a.rollNumber,b.rollNumber));
      const courses = d.courses.map(c=>({...c,departmentCode:this.find('departments',c.departmentId).code,
        offered:d.offerings.some(o=>o.courseId===c.id && o.academicYearId===y)})).sort((a,b)=>order(a.code,b.code));
      const counts = divisions.filter(v=>v.academicYearId===y).map(v=>({id:v.id,departmentId:v.departmentId,
        department:v.departmentCode,studyYear:v.studyYear,division:v.code,studentCount:students.filter(s=>s.divisionId===v.id).length}))
        .sort((a,b)=>order(a.department,b.department)||a.studyYear-b.studyYear||order(a.division,b.division));
      const yearCounts = d.departments.flatMap(p=>[1,2,3,4].map(studyYear=>({department:p.code,studyYear,
        studentCount:students.filter(s=>s.departmentId===p.id && s.studyYear===studyYear).length}))).sort((a,b)=>order(a.department,b.department)||a.studyYear-b.studyYear);
      return copy({years:[...d.years].sort((a,b)=>b.startYear-a.startYear),selectedYearId:y,departments:d.departments,
        divisions,courses,rooms:d.rooms,seats:d.seats,students,enrollments:d.enrollments.filter(e=>e.academicYearId===y),faculty:d.faculty,counts,yearCounts});
    }
    revision() { return `${this.state.epoch || 'seed'}:${this.state.revision}`; }
    chart(id) {
      const e = this.find('exams',id), students = this.roster(e), assigned = this.assignments(e.id);
      const seats = this.state.seats.filter(s=>e.roomIds.includes(s.roomId)).map(s=>{
        const r=this.find('rooms',s.roomId), a=assigned.find(a=>a.seatId===s.id), student=a && this.find('students',a.studentId);
        return {...s,roomNumber:r.roomNumber,seatColumns:r.seatColumns,isAccessible:flag(s.isAccessible)&&flag(r.isAccessible),
          studentId:student?.id ?? null,isManual:!!a?.isManual,rollNumber:student?.rollNumber ?? null,name:student?.name ?? null,requiresAccessible:!!student?.requiresAccessible};
      }).sort((a,b)=>order(a.roomNumber,b.roomNumber)||a.roomId-b.roomId||a.rowNumber-b.rowNumber||a.columnNumber-b.columnNumber||a.id-b.id);
      return copy({exam:this.examView(e),students,seats,revision:this.revision(),history:this.state.history.filter(h=>h.examId===e.id).slice(-200).reverse()});
    }
    editable(e) { if (!active(e)) fail('Completed or cancelled exams cannot be allocated, tagged or overridden'); }
    eligible(s, seat, spaced) { return (!spaced || seat.columnNumber % 2 === 1) && (!flag(s.requiresAccessible) || flag(seat.isAccessible)); }
    allocate(chart, pins = new Map()) {
      const out = new Map(), used = new Set(), seats = chart.seats;
      for (const [id, seatId] of pins) {
        const s = chart.students.find(s=>s.id===id), seat = seats.find(s=>s.id===seatId);
        if (!s || !seat || used.has(seatId) || !this.eligible(s,seat,flag(chart.exam.spacingRequired))) fail('Preserved manual assignment is invalid');
        out.set(id,seatId);used.add(seatId);
      }
      const students = [...chart.students].sort((a,b)=>Number(flag(b.requiresAccessible))-Number(flag(a.requiresAccessible)) || order(a.rollNumber,b.rollNumber)||a.id-b.id);
      const pick = (s, accessible) => seats.find(seat=>!used.has(seat.id) && this.eligible(s,seat,flag(chart.exam.spacingRequired)) && flag(seat.isAccessible)===accessible);
      for (const s of students.filter(s=>flag(s.requiresAccessible))) {
        if (out.has(s.id)) continue;
        const seat=pick(s,true);if(!seat)fail('Insufficient accessible seats; no changes saved');out.set(s.id,seat.id);used.add(seat.id);
      }
      for (const accessible of [false,true]) for (const s of students.filter(s=>!flag(s.requiresAccessible))) {
        if (out.has(s.id)) continue;
        const seat=pick(s,accessible);
        if(seat){out.set(s.id,seat.id);used.add(seat.id);}else if(accessible)fail('Insufficient usable seat capacity; no changes saved');
      }
      return out;
    }
    pins(chart) { return new Map(chart.seats.filter(s=>s.studentId && s.isManual).map(s=>[s.studentId,s.id])); }
    label(chart,id) { const s=chart.seats.find(s=>s.id===id);return s ? `${s.roomNumber} / ${s.seatNumber}` : null; }
    generation(id,replace) {
      const chart=this.chart(id);this.editable(chart.exam);
      if(!chart.students.length)fail('No students enrolled for this course and academic year');
      const pins=replace ? new Map() : this.pins(chart), allocation=this.allocate(chart,pins);
      return {chart,pins,allocation};
    }
    summary(plan, replace) {
      const {chart,pins,allocation}=plan, changes=[];
      for(const s of chart.students) {
        const old=chart.seats.find(seat=>seat.studentId===s.id)?.id, seat=allocation.get(s.id);
        if(old!==seat)changes.push({rollNumber:s.rollNumber,oldSeat:this.label(chart,old),newSeat:this.label(chart,seat)});
      }
      const manuals=this.pins(chart).size;
      return {revision:this.revision(),assigned:allocation.size,changed:changes.length,manualAssignments:manuals,
        preservedManual:pins.size,replacedManual:manuals-pins.size,requiresAccessible:chart.students.filter(s=>flag(s.requiresAccessible)).length,replaceManual:replace,changes};
    }
    audit(chart,studentId,action,old,newSeat,reason,user) {
      const s=this.state.students.find(s=>s.id===studentId);
      this.state.history.push({id:this.next('history'),examId:chart.exam.id,studentId,rollNumber:s?.rollNumber ?? null,name:s?.name ?? null,
        action,oldSeat:this.label(chart,old),newSeat:this.label(chart,newSeat),reason,createdAt:new Date().toISOString(),actor:user.username});
    }
    persist(plan,reason,user,action) {
      const {chart,allocation,pins}=plan;
      this.state.assignments=this.state.assignments.filter(a=>a.examId!==chart.exam.id);
      for(const [studentId,seatId] of allocation) {
        this.state.assignments.push({examId:chart.exam.id,studentId,seatId,isManual:pins.has(studentId)});
        this.audit(chart,studentId,action,chart.seats.find(s=>s.studentId===studentId)?.id,seatId,reason,user);
      }
    }
    accommodation(id,studentId,required) {
      const current=this.chart(id);this.editable(current.exam);
      const selected=current.students.find(s=>s.id===studentId) || fail('Student is not enrolled in this examination');
      if(flag(selected.requiresAccessible)===required)fail('The accommodation requirement is already set to this value');
      const plans=[];let releasedManual=0;
      for(const e of this.state.exams.filter(e=>active(e)&&this.roster(e).some(s=>s.id===studentId)).sort((a,b)=>a.id-b.id)) {
        const chart=this.chart(e.id);if(!this.assignments(e.id).length)continue;
        chart.students=chart.students.map(s=>s.id===studentId?{...s,requiresAccessible:required}:s);
        const pins=this.pins(chart), pin=pins.get(studentId);
        if(pin && !this.eligible({...selected,requiresAccessible:required},chart.seats.find(s=>s.id===pin),flag(e.spacingRequired))){pins.delete(studentId);releasedManual++;}
        try{plans.push({chart,pins,allocation:this.allocate(chart,pins)});}catch(error){fail(`${chart.exam.courseCode} / ${chart.exam.academicYear}: ${error.message}`);}
      }
      const affectedExams=plans.map(p=>({id:p.chart.exam.id,courseCode:p.chart.exam.courseCode,academicYear:p.chart.exam.academicYear,changed:this.summary(p,false).changed}));
      return {current,plans,summary:{revision:this.revision(),studentId,requiresAccessible:required,affectedExams,
        changed:affectedExams.reduce((sum,e)=>sum+e.changed,0),releasedManual}};
    }
    seating(params,write,user) {
      const id=number(params.examId), action=params.action;
      if(!write){if(!action)return this.chart(id);if(action==='preview')return this.summary(this.generation(id,flag(params.replaceManual)),flag(params.replaceManual));
        if(action==='previewAccommodation')return this.accommodation(id,number(params.studentId),flag(params.requiresAccessible)).summary;fail('Unknown read action',400);}
      const reason=text(params.reason,250);
      if(params.expectedRevision && params.expectedRevision!==this.revision())fail('Seating or accommodation data changed; review the operation again');
      if(['generate','regenerate'].includes(action)) {
        const replace=flag(params.replaceManual);
        if(replace && !flag(params.confirmReplaceManual))fail('Confirm replacement of manual assignments before regenerating');
        const plan=this.generation(id,replace);this.persist(plan,reason,user,this.assignments(id).length?'REGENERATE':'GENERATE');return {success:true,assigned:plan.allocation.size};
      }
      const chart=this.chart(id);
      if(action==='clear') {
        if(!['DRAFT','CANCELLED'].includes(chart.exam.status))fail('Only draft or cancelled seating can be cleared');
        for(const a of this.assignments(id))this.audit(chart,a.studentId,'CLEAR',a.seatId,null,reason,user);
        this.state.assignments=this.state.assignments.filter(a=>a.examId!==id);return {success:true};
      }
      this.editable(chart.exam);
      if(action==='accommodation') {
        if(params.expectedRevision!==this.revision())fail('Preview the accommodation requirement again');
        const studentId=number(params.studentId), change=this.accommodation(id,studentId,flag(params.requiresAccessible));
        const before=chart.seats.find(s=>s.studentId===studentId)?.id;
        this.find('students',studentId).requiresAccessible=flag(params.requiresAccessible);
        for(const plan of change.plans)this.persist(plan,reason,user,'ACCOMMODATION_REGENERATE');
        const after=this.assignments(id).find(a=>a.studentId===studentId)?.seatId;
        this.audit(chart,studentId,flag(params.requiresAccessible)?'ACCOMMODATION_ON':'ACCOMMODATION_OFF',before,after,reason,user);
        return {success:true,...change.summary};
      }
      if(!['swap','reassign'].includes(action))fail('Unknown seating action');
      const studentId=number(params.studentId), target=chart.seats.find(s=>s.id===number(params.seatId)) || fail('Target seat is outside selected rooms');
      const student=chart.students.find(s=>s.id===studentId) || fail('Student is not enrolled'), source=chart.seats.find(s=>s.studentId===studentId) || fail('Student has no assigned seat');
      if(source.id===target.id)fail('Choose a different target seat');
      if(!this.eligible(student,target,flag(chart.exam.spacingRequired)))fail('Target does not satisfy accessibility or spacing');
      if(action==='reassign' && target.studentId)fail('Target occupied; choose swap');
      if(action==='swap' && !target.studentId)fail('Target is empty; choose move');
      const changes=[[studentId,target.id]];
      if(action==='swap') {
        const other=chart.students.find(s=>s.id===target.studentId) || fail('Other student is not enrolled');
        if(!this.eligible(other,source,flag(chart.exam.spacingRequired)))fail('Swap would violate the other student’s accessibility or spacing');
        changes.push([other.id,source.id]);
      }
      for(const [sid,seatId] of changes){const assignment=this.assignments(id).find(a=>a.studentId===sid);this.audit(chart,sid,action==='swap'?'SWAP':'REASSIGN',assignment.seatId,seatId,reason,user);assignment.seatId=seatId;assignment.isManual=true;}
      return {success:true};
    }
    overlapping(a,b) { return a.id!==b.id && a.examDate===b.examDate && a.startTime.slice(0,5)<b.endTime.slice(0,5) && a.endTime.slice(0,5)>b.startTime.slice(0,5) && b.status!=='CANCELLED'; }
    conflicts(e) {
      const roster=new Set(this.roster(e).map(s=>s.id));
      for(const other of this.state.exams.filter(o=>this.overlapping(e,o))) {
        if(e.roomIds.some(id=>other.roomIds.includes(id)))fail('Timetable conflict: selected room overlaps another exam');
        if(this.roster(other).some(s=>roster.has(s.id)))fail('Timetable conflict: shared student overlaps another exam');
      }
    }
    protectEnrollment(course,year) {
      if(this.state.exams.some(e=>e.courseId===course&&e.academicYearId===year&&(published(e)||this.assignments(e.id).length)))fail('Enrollments are protected after seating generation or exam publication');
    }
    enroll(studentId,courseId,year,id=0) {
      this.protectEnrollment(courseId,year);
      const course=this.find('courses',courseId), placement=this.state.placements.find(p=>p.studentId===studentId&&p.academicYearId===year) || fail('Student has no placement in this year');
      if(!this.state.offerings.some(o=>o.courseId===courseId&&o.academicYearId===year))fail('Course is not offered this year');
      if(placement.semester!==course.semester)fail('Student and course semesters must agree');
      if(this.state.enrollments.some(e=>e.id!==id&&e.studentId===studentId&&e.courseId===courseId&&e.academicYearId===year))fail('Duplicate enrollment');
      for(const e of this.state.exams.filter(e=>e.courseId===courseId&&e.academicYearId===year&&e.status!=='CANCELLED'))
        if(this.state.exams.some(other=>this.overlapping(e,other)&&this.roster(other).some(s=>s.id===studentId)))fail('Enrollment would introduce overlapping exams for this student');
      if(id){const old=this.find('enrollments',id);this.protectEnrollment(old.courseId,old.academicYearId);Object.assign(old,{studentId,courseId,academicYearId:year});}
      else{id=this.next('enrollments');this.state.enrollments.push({id,studentId,courseId,academicYearId:year});}
      return id;
    }
    saveExam(p) {
      const d=this.state, action=p.action, id=action==='create'?0:number(p.id);
      if(action==='delete') {
        const e=this.find('exams',id);if(!['DRAFT','CANCELLED'].includes(e.status))fail('Only draft or cancelled exams can be deleted');
        if(d.history.some(h=>h.examId===id))fail('Exam has seating history; cancel and retain its audit record');
        d.exams=d.exams.filter(e=>e.id!==id);return {success:true,id};
      }
      if(action==='status') {
        const e=this.find('exams',id), next=p.status;
        if(e.status!==next && !({DRAFT:['SCHEDULED','CANCELLED'],SCHEDULED:['COMPLETED','CANCELLED']}[e.status]||[]).includes(next))fail(`Invalid state transition from ${e.status} to ${next}`);
        if(next==='SCHEDULED')this.conflicts(e);e.status=next;return {success:true,id};
      }
      if(!['create','update'].includes(action))fail('Unknown exam action');
      const courseId=number(p.courseId), academicYearId=this.year(number(p.academicYearId));this.find('courses',courseId);
      if(!d.offerings.some(o=>o.courseId===courseId&&o.academicYearId===academicYearId))fail('Course is not offered this year');
      const examDate=text(p.examDate,10), startTime=text(p.startTime,8), endTime=text(p.endTime,8);
      if(!/^\d{4}-\d{2}-\d{2}$/.test(examDate)||new Date(examDate+'T00:00:00Z').toISOString().slice(0,10)!==examDate||![startTime,endTime].every(t=>/^([01]\d|2[0-3]):[0-5]\d(:[0-5]\d)?$/.test(t)))fail('Invalid date or time');
      if(endTime<=startTime || !['DRAFT','SCHEDULED'].includes(p.status))fail('End time must be later than start time; select draft or scheduled');
      const roomIds=(Array.isArray(p.roomId)?p.roomId:[p.roomId]).map(r=>this.find('rooms',number(r)).id);
      if(!roomIds.length || new Set(roomIds).size!==roomIds.length)fail('Select distinct rooms');
      if(id){const old=this.find('exams',id);if(old.status!=='DRAFT')fail('Only draft exams can be edited');if(d.history.some(h=>h.examId===id))fail('Exam has seating history; retain it and create a new exam');}
      const e={id:id||this.next('exams'),courseId,academicYearId,examDate,startTime,endTime,status:p.status,spacingRequired:flag(p.spacingRequired),roomIds};
      this.conflicts(e);if(id)Object.assign(this.find('exams',id),e);else d.exams.push(e);return {success:true,id:e.id};
    }
    protectStudent(id,year) {
      if(this.state.exams.some(e=>e.academicYearId===year&&((active(e)&&this.assignments(e.id).some(a=>a.studentId===id))||(published(e)&&this.roster(e).some(s=>s.id===id)))))fail('Student has active seating or published exams; placement is protected');
    }
    saveData(p) {
      const d=this.state, resource=p.resource, action=p.action, id=action==='create'?0:number(p.id), year=()=>this.year(number(p.academicYearId));
      if(!['create','update','delete','offer','unoffer'].includes(action))fail('Unknown action');
      if(action==='delete')return this.deleteData(resource,id);
      if(['offer','unoffer'].includes(action) && resource!=='courses')fail('Unknown action for this resource');
      let row;
      switch(resource) {
        case 'years': {
          const startYear=number(p.startYear,2000,2100);this.unique('years','startYear',startYear,id);
          if(id&&flag(this.find('years',id).isCurrent)&&!flag(p.isCurrent))fail('Select another current year before clearing this one');
          if(flag(p.isCurrent))d.years.forEach(y=>y.isCurrent=false);
          row={startYear,label:`${startYear}-${String((startYear+1)%100).padStart(2,'0')}`,isCurrent:flag(p.isCurrent)};break;
        }
        case 'departments': {const code=text(p.code,20),name=text(p.name,100);this.unique(resource,'code',code,id);row={code,name};break;}
        case 'divisions': {
          const departmentId=this.find('departments',number(p.departmentId)).id, studyYear=number(p.studyYear,1,4), academicYearId=year(), code=text(p.code,20);
          if(d.divisions.some(v=>v.id!==id&&v.academicYearId===academicYearId&&v.departmentId===departmentId&&v.studyYear===studyYear&&same(v.code,code)))fail('Duplicate division');
          if(id){const old=this.find(resource,id);if((old.academicYearId!==academicYearId||old.departmentId!==departmentId||old.studyYear!==studyYear)&&d.placements.some(a=>a.divisionId===id))fail('Division placement fields are protected while students are enrolled');}
          row={departmentId,studyYear,academicYearId,code};break;
        }
        case 'courses': {
          const academicYearId=year();
          if(action==='unoffer') {if(d.enrollments.some(e=>e.courseId===id&&e.academicYearId===academicYearId)||d.exams.some(e=>e.courseId===id&&e.academicYearId===academicYearId))fail('Course offering is referenced');d.offerings=d.offerings.filter(o=>o.courseId!==id||o.academicYearId!==academicYearId);return {success:true,id};}
          if(action==='offer'){this.find(resource,id);if(!d.offerings.some(o=>o.courseId===id&&o.academicYearId===academicYearId))d.offerings.push({courseId:id,academicYearId});return {success:true,id};}
          const code=text(p.code,30),name=text(p.name,120),departmentId=this.find('departments',number(p.departmentId)).id,semester=number(p.semester,1,8),studyYear=number(p.studyYear,1,4);
          if(Math.ceil(semester/2)!==studyYear)fail('Study year and semester must agree');this.unique(resource,'code',code,id);
          if(id){const old=this.find(resource,id);if((old.departmentId!==departmentId||old.semester!==semester)&&(d.enrollments.some(e=>e.courseId===id)||d.exams.some(e=>e.courseId===id&&(published(e)||d.history.some(h=>h.examId===e.id)))))fail('Course placement is protected while referenced');}
          const courseId=id||this.next(resource);if(!d.offerings.some(o=>o.courseId===courseId&&o.academicYearId===academicYearId))d.offerings.push({courseId,academicYearId});
          row={code,name,departmentId,semester,studyYear};break;
        }
        case 'rooms': {
          const roomNumber=text(p.roomNumber,30),capacity=number(p.capacity,1,500),seatColumns=number(p.seatColumns,1,30);this.unique(resource,'roomNumber',roomNumber,id);
          if(id && d.exams.some(e=>e.roomIds.includes(id)))fail('Room is already assigned; layout and accessibility are protected');
          const roomId=id||this.next(resource);d.seats=d.seats.filter(s=>s.roomId!==id);let seatId=this.next('seats');
          for(let i=1;i<=capacity;i++)d.seats.push({id:seatId++,roomId,seatNumber:`S${String(i).padStart(3,'0')}`,rowNumber:Math.floor((i-1)/seatColumns)+1,columnNumber:(i-1)%seatColumns+1,isAccessible:flag(p.isAccessible)&&i<=2});
          row={roomNumber,capacity,seatColumns,isAccessible:flag(p.isAccessible)};break;
        }
        case 'seats': {
          const seat=this.find('seats',id);if(d.exams.some(e=>e.roomIds.includes(seat.roomId)))fail('Room is already assigned; accessibility is protected');seat.isAccessible=flag(p.isAccessible);return {success:true,id};
        }
        case 'students': {
          const divisionId=number(p.divisionId),v=this.find('divisions',divisionId),academicYearId=year(),semester=number(p.semester,1,8);
          if(v.academicYearId!==academicYearId||v.studyYear!==Math.ceil(semester/2))fail('Division, academic year and semester must agree');
          const rollNumber=text(p.rollNumber,30),name=text(p.name,100),mail=String(p.email||'').trim();if(!email(mail)||mail.length>120)fail('Invalid email');this.unique(resource,'rollNumber',rollNumber,id);if(mail)this.unique(resource,'email',mail,id);
          if(id){this.protectStudent(id,academicYearId);const old=this.find(resource,id);if(flag(old.requiresAccessible)!==flag(p.requiresAccessible)&&d.exams.some(e=>active(e)&&this.assignments(e.id).some(a=>a.studentId===id)))fail('Use accommodation requirements on the Seating page');
            if(d.enrollments.some(e=>e.studentId===id&&e.academicYearId===academicYearId&&this.find('courses',e.courseId).semester!==semester))fail('Remove incompatible course enrollments before changing semester');}
          const sid=id||this.next(resource), placement=d.placements.find(a=>a.studentId===sid&&a.academicYearId===academicYearId);
          if(placement)Object.assign(placement,{divisionId,semester});else d.placements.push({studentId:sid,academicYearId,divisionId,semester});
          row={rollNumber,name,email:mail||null,requiresAccessible:flag(p.requiresAccessible)};break;
        }
        case 'enrollments': return {success:true,id:this.enroll(number(p.studentId),number(p.courseId),year(),id)};
        case 'faculty': {
          const employeeCode=text(p.employeeCode,30),name=text(p.name,100),mail=text(p.email,120),username=text(p.username,50),departmentId=this.find('departments',number(p.departmentId)).id;
          if(!email(mail))fail('Invalid email');this.unique(resource,'employeeCode',employeeCode,id);this.unique(resource,'email',mail,id);
          const userId=id?this.find(resource,id).userId:this.next('users');this.unique('users','username',username,userId);
          if((!id||p.password)&&!password(p.password))fail('Password needs 8 characters, uppercase, lowercase and a digit');
          if(id)Object.assign(this.find('users',userId),{username,name,email:mail,...(p.password?{password:p.password}:{})});else d.users.push({id:userId,username,name,email:mail,password:p.password,role:'FACULTY'});
          row={employeeCode,name,email:mail,username,departmentId,userId};break;
        }
        default: fail('Unknown resource');
      }
      const savedId=id||this.next(resource);if(id)Object.assign(this.find(resource,id),row);else d[resource].push({id:savedId,...row});return {success:true,id:savedId};
    }
    deleteData(resource,id) {
      const d=this.state, row=this.find(resource,id), reject=()=>fail('Referenced records cannot be deleted');
      switch(resource) {
        case 'years': if(flag(row.isCurrent))fail('Cannot delete the current academic year');if(d.divisions.some(v=>v.academicYearId===id)||d.offerings.some(o=>o.academicYearId===id)||d.exams.some(e=>e.academicYearId===id)||d.placements.some(p=>p.academicYearId===id))reject();break;
        case 'departments': if(d.divisions.some(v=>v.departmentId===id)||d.courses.some(c=>c.departmentId===id)||d.faculty.some(f=>f.departmentId===id))reject();break;
        case 'divisions': if(d.placements.some(p=>p.divisionId===id))reject();break;
        case 'rooms': if(d.exams.some(e=>e.roomIds.includes(id)))reject();d.seats=d.seats.filter(s=>s.roomId!==id);break;
        case 'courses': if(d.enrollments.some(e=>e.courseId===id)||d.exams.some(e=>e.courseId===id))reject();d.offerings=d.offerings.filter(o=>o.courseId!==id);break;
        case 'enrollments': this.protectEnrollment(row.courseId,row.academicYearId);break;
        case 'students': if(d.history.some(h=>h.studentId===id)||d.assignments.some(a=>a.studentId===id))reject();for(const e of d.enrollments.filter(e=>e.studentId===id))this.protectEnrollment(e.courseId,e.academicYearId);d.enrollments=d.enrollments.filter(e=>e.studentId!==id);d.placements=d.placements.filter(p=>p.studentId!==id);break;
        case 'faculty': d.users=d.users.filter(u=>u.id!==row.userId);break;
        default:fail('Unknown resource');
      }
      d[resource]=d[resource].filter(r=>r.id!==id);return {success:true,id};
    }
    importCsv(csv) {
      const lines=String(csv).replace(/^\uFEFF/,'').split(/\r?\n/), header=lines.shift()?.trim().toLowerCase();
      const full='roll_number,name,email,department_code,semester,course_code,academic_year,division,requires_accessible',legacy=header==='roll_number,name,email,department_code,semester,course_code';
      if(header!==full && !legacy)fail('CSV header does not match the supported format',400);
      const result={imported:0,enrolled:0,duplicates:0,invalid:0,errors:[]};
      lines.forEach((line,index)=>{
        if(!line.trim())return;const before=this.exportState();
        try {
          const v=line.split(',').map(s=>s.trim());if(v.length!==(legacy?6:9)||line.includes('"'))fail('Quoted fields are unsupported; check the number of columns');
          const [rollNumber,name,mail,departmentCode,rawSemester,courseCode]=v,semester=number(rawSemester,1,8);
          text(rollNumber,30);text(name,100);if(!email(mail)||mail.length>120)fail('Invalid email');
          if(!legacy&&!['true','false','1','0'].includes(v[8].toLowerCase()))fail('requires_accessible must be true or false');
          const required=legacy?false:flag(v[8].toLowerCase());
          const department=this.state.departments.find(d=>same(d.code,departmentCode)) || fail('Unknown department');
          const year=legacy?this.find('years',this.year()):this.state.years.find(y=>y.label===v[6]) || fail('Unknown academic year');
          const course=this.state.courses.find(c=>same(c.code,courseCode)&&this.state.offerings.some(o=>o.courseId===c.id&&o.academicYearId===year.id)) || fail('Unknown course offering');
          if(course.semester!==semester)fail('Course and student semesters differ');this.protectEnrollment(course.id,year.id);
          const division=this.state.divisions.find(d=>d.departmentId===department.id&&d.academicYearId===year.id&&d.studyYear===Math.ceil(semester/2)&&same(d.code,legacy?'A':v[7])) || fail('Unknown division');
          let student=this.state.students.find(s=>same(s.rollNumber,rollNumber)), created=!student;
          if(!student){if(mail)this.unique('students','email',mail);student={id:this.next('students'),rollNumber,name,email:mail||null,requiresAccessible:required};this.state.students.push(student);}
          else if(student.name!==name||(student.email||'')!==mail||(!legacy&&flag(student.requiresAccessible)!==required))fail('Existing student details differ; edit the student explicitly');
          const placement=this.state.placements.find(p=>p.studentId===student.id&&p.academicYearId===year.id);
          if(placement&&(placement.divisionId!==division.id||placement.semester!==semester))fail('Existing annual placement differs');
          if(!placement)this.state.placements.push({studentId:student.id,academicYearId:year.id,divisionId:division.id,semester});
          if(this.state.enrollments.some(e=>e.studentId===student.id&&e.courseId===course.id&&e.academicYearId===year.id)){result.duplicates++;return;}
          this.enroll(student.id,course.id,year.id);result[created?'imported':'enrolled']++;
        }catch(error){this.state=before;result.invalid++;result.errors.push(`Line ${index+2}: ${error.message}`);}
      });return result;
    }
    auth(action,p,user) {
      if(action==='login') {
        const found=this.state.users.find(u=>same(u.username,String(p.username||'').trim())&&u.password===p.password) || fail('Invalid username or password',401);
        return {success:true,userId:found.id,role:found.role,name:found.name};
      }
      if(action==='session')return user?{authenticated:true,userId:user.id,username:user.username,role:user.role,name:user.name}:{authenticated:false};
      if(action==='logout')return {success:true};
      if(user?.role!=='ADMIN')fail('Admin access required',403);
      if(!password(p.password))fail('Password needs 8 characters, uppercase, lowercase and a digit',400);
      if(action==='register') {
        const username=text(p.username,50),name=text(p.name,100),mail=text(p.email,120);if(!email(mail))fail('Invalid email',400);this.unique('users','username',username);this.unique('users','email',mail);
        if(p.role!=='ADMIN')fail('Use faculty management for faculty accounts');
        this.state.users.push({id:this.next('users'),username,name,email:mail,password:p.password,role:'ADMIN'});return {success:true};
      }
      if(action==='reset-password') {
        const found=this.state.users.find(u=>same(u.username,p.username)&&same(u.email,p.email)) || fail('Matching account not found');found.password=p.password;return {success:true};
      }
      fail('Unknown authentication action',400);
    }
    handle(path,method='GET',p={},userId=0) {
      const before=this.exportState(), user=this.user(userId), write=method!=='GET';
      try {
        if(path.startsWith('admin/')&&user?.role!=='ADMIN')fail(user?'Admin access required':'Login required',user?403:401);
        let result;
        if(path.startsWith('auth/'))result=this.auth(path.slice(5),p,user);
        else if(path==='admin/data')result=write?this.saveData(p):this.snapshot(Number(p.academicYearId)||0);
        else if(path==='admin/exams')result=write?this.saveExam(p):{exams:this.examList()};
        else if(path==='admin/seating')result=this.seating(p,write,user);
        else if(path==='admin/import/students'&&write)result=this.importCsv(p.csv);
        else if(path==='timetable'&&!write)result={years:copy(this.state.years),exams:this.examList(true)};
        else if(path==='seating/search'&&!write) {
          const student=this.state.students.find(s=>same(s.rollNumber,String(p.rollNumber||'').trim())), results=[];
          if(student)for(const e of this.examList(true,Number(p.academicYearId))) {
            const a=this.assignments(e.id).find(a=>a.studentId===student.id);if(!a)continue;const s=this.find('seats',a.seatId),r=this.find('rooms',s.roomId);
            results.push({...e,rollNumber:student.rollNumber,name:student.name,roomNumber:r.roomNumber,seatNumber:s.seatNumber});
          }result={results};
        } else fail('This route is unavailable in the team demo',404);
        if(write&&!['auth/login','auth/logout'].includes(path))this.state.revision++;
        return copy(result);
      } catch(error) {this.state=before;throw error;}
    }
    initialize() {
      if(this.state.assignments.length)return;
      const user=this.state.users[0];
      for(const e of this.state.exams)this.persist(this.generation(e.id,false),'Initial fictional demonstration allocation',user,'GENERATE');
      this.state.revision=0;
    }
  }
  global.DemoEngine=DemoEngine;
  if(typeof module==='object'&&module.exports)module.exports=DemoEngine;
})(typeof window==='object'?window:globalThis);
