let timetableExams=[];
const timetableNode=id=>document.getElementById(id);
let dateMode=AppUI.params.get('view')==='date';
function renderExams(rows,filtered) {
    timetableNode('timetableRows').innerHTML=rows.map(e=>`<tr><td>${esc(e.academicYear)}</td><td>${esc(AppUI.date(e.examDate))}</td><td><strong>${esc(e.courseCode)}</strong><br>${esc(e.courseName)}</td><td>${esc(AppUI.time(e.startTime))} – ${esc(AppUI.time(e.endTime))}</td><td>${e.rooms.map(r=>esc(r.roomNumber)).join(', ')}</td><td>${AppUI.badge(e.status)}</td></tr>`).join('')||'<tr><td colspan="6"><div class="empty-state">No matching published examinations. Change the search or academic year.</div></td></tr>';
    const grouped=new Map();
    for(const {value:e} of filtered){const key=`${e.academicYear} / ${AppUI.date(e.examDate)}`;if(!grouped.has(key))grouped.set(key,[]);grouped.get(key).push(e);}
    timetableNode('calendarView').innerHTML=[...grouped].map(([date,exams])=>`<article class="card"><h3>${esc(date)}</h3>${exams.map(e=>`<p><strong>${esc(e.courseCode)}</strong> — ${esc(e.courseName)}<br>${esc(AppUI.time(e.startTime))} – ${esc(AppUI.time(e.endTime))}<br>Rooms ${e.rooms.map(r=>esc(r.roomNumber)).join(', ')}</p>`).join('')}</article>`).join('')||'<div class="empty-state full">No matching published examinations.</div>';
}
const timetableList=AppUI.list({search:timetableNode('recordSearch'),size:timetableNode('pageSize'),previous:timetableNode('previousPage'),next:timetableNode('nextPage'),count:timetableNode('recordCount'),render:renderExams});
function filterYear() {
    const year=Number(timetableNode('academicYear').value);
    timetableList.set(timetableExams.filter(e=>!year||e.academicYearId===year).map(value=>({value,text:`${value.courseCode} ${value.courseName} ${value.academicYear} ${value.examDate} ${AppUI.date(value.examDate)} ${value.rooms.map(r=>r.roomNumber).join(' ')}`})));
}
function setView(value) {
    dateMode=value;timetableNode('listView').hidden=dateMode;timetableNode('calendarView').hidden=!dateMode;
    timetableNode('listButton').setAttribute('aria-pressed',String(!dateMode));timetableNode('calendarButton').setAttribute('aria-pressed',String(dateMode));
    timetableNode('previousPage').parentElement.hidden=dateMode;timetableNode('pageSize').closest('label').hidden=dateMode;
    AppUI.remember({view:dateMode?'date':''});
}
timetableNode('academicYear').addEventListener('change',()=>{AppUI.remember({academicYearId:timetableNode('academicYear').value});filterYear();});
timetableNode('listButton').addEventListener('click',()=>setView(false));
timetableNode('calendarButton').addEventListener('click',()=>setView(true));
timetableNode('printTimetable').addEventListener('click',()=>window.print());
(async()=>{
    try {
        const data=await apiRequest(AppUI.url('api/timetable'));timetableExams=data.exams;
        timetableNode('academicYear').innerHTML='<option value="">All academic years</option>'+data.years.map(y=>`<option value="${y.id}">${esc(y.label)}</option>`).join('');
        if(data.years.some(y=>String(y.id)===AppUI.params.get('academicYearId')))timetableNode('academicYear').value=AppUI.params.get('academicYearId');
        filterYear();setView(dateMode);
    } catch(error){AppUI.message(timetableNode('message'),error.message);}
})();
