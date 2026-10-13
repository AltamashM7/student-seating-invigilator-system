let publishedExams = [];
const studentNode = id => document.getElementById(id);
function renderPreview() {
    if (!studentNode('upcomingExams')) return;
    const year = Number(studentNode('academicYear').value);
    const query = studentNode('previewSearch').value.trim().toLowerCase();
    const matches = publishedExams.filter(e => e.academicYearId === year && `${e.courseCode} ${e.courseName} ${e.examDate} ${e.rooms.map(r=>r.roomNumber).join(' ')}`.toLowerCase().includes(query));
    studentNode('previewCount').textContent = `${matches.length} published examinations${matches.length>6?' · Showing the first 6; open the full timetable for more.':''}`;
    studentNode('upcomingExams').innerHTML = matches.slice(0,6).map(e => `<article class="card"><p class="eyebrow">${esc(AppUI.date(e.examDate))}</p><h3>${esc(e.courseCode)}</h3><p>${esc(e.courseName)}</p><p>${esc(AppUI.time(e.startTime))} – ${esc(AppUI.time(e.endTime))}</p><p>Rooms ${e.rooms.map(r=>esc(r.roomNumber)).join(', ')}</p></article>`).join('') || '<div class="empty-state full">No matching published examinations. Try another course or academic year.</div>';
}
function clearResults() { studentNode('studentResults').hidden=true; AppUI.message(studentNode('message'),''); }
async function loadStudentPortal() {
    try {
        const data=await apiRequest(AppUI.url('api/timetable')); publishedExams=data.exams;
        studentNode('academicYear').innerHTML=data.years.map(y=>`<option value="${y.id}">${esc(y.label)}</option>`).join('');
        const selected=Number(AppUI.params.get('academicYearId')) || data.years.find(y=>y.isCurrent)?.id || data.years[0]?.id;
        if (data.years.some(y=>y.id===selected)) studentNode('academicYear').value=selected;
        renderPreview();
    } catch(error) {AppUI.message(studentNode('message'),'The schedule could not be loaded. Refresh this page to try again.');}
}
document.querySelectorAll('[data-timetable-link]').forEach(a=>a.href=AppUI.url('pages/timetable.html'));
studentNode('academicYear').addEventListener('change',()=>{AppUI.remember({academicYearId:studentNode('academicYear').value});clearResults();renderPreview();});
studentNode('previewSearch')?.addEventListener('input',renderPreview);
studentNode('rollNumber').addEventListener('input',clearResults);
studentNode('printResults').addEventListener('click',()=>window.print());
studentNode('searchForm').addEventListener('submit',event=>{
    event.preventDefault(); AppUI.busy(studentNode('searchButton'),async()=>{
        try {
            const query=new URLSearchParams({academicYearId:studentNode('academicYear').value,rollNumber:studentNode('rollNumber').value.trim()});
            const data=await apiRequest(AppUI.url('api/seating/search?'+query));
            const results=data.results;
            studentNode('studentResults').hidden=false;
            studentNode('studentName').textContent=results.length?`${results[0].name} · ${results[0].rollNumber}`:'No published seating found';
            studentNode('printResults').hidden=!results.length;
            studentNode('results').innerHTML=results.map(r=>`<article class="seat-result"><p class="eyebrow">${esc(r.academicYear)} · ${esc(AppUI.date(r.examDate))}</p><h3>${esc(r.courseCode)} — ${esc(r.courseName)}</h3><p class="muted">${esc(AppUI.time(r.startTime))} – ${esc(AppUI.time(r.endTime))}</p><div class="location-pair"><div><small>Room</small><strong>${esc(r.roomNumber)}</strong></div><div><small>Seat</small><strong>${esc(r.seatNumber)}</strong></div></div></article>`).join('') || '<div class="empty-state">No published seating for the selected roll number and academic year. Seating details are available after publication by the examination administrator.</div>';
            AppUI.message(studentNode('message'),results.length?`${results.length} published seating result${results.length===1?'':'s'} found.`:'No published result for this roll number and year.',!!results.length);
            studentNode('studentResults').scrollIntoView({behavior:'smooth',block:'start'});
        } catch(error) {clearResults();AppUI.message(studentNode('message'),error.message);}
    },'Searching seating…');
});
loadStudentPortal();
