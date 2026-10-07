let chart=null;
const seatingNode=id=>document.getElementById(id);
const seatingNotice=(text,ok=false)=>AppUI.message(seatingNode('message'),text,ok);
const historyList=AppUI.list({search:seatingNode('historySearch'),previous:seatingNode('historyPrevious'),next:seatingNode('historyNext'),count:seatingNode('historyCount'),render:rows=>{
    seatingNode('history').innerHTML=rows.map(h=>`<tr><td>${esc(h.createdAt)}</td><td>${esc(h.actor)}</td><td>${esc(chart.students.find(s=>s.id===h.studentId)?.rollNumber||'—')}</td><td>${esc(h.action)}</td><td>${esc(h.oldSeat||'—')}</td><td>${esc(h.newSeat||'—')}</td><td>${esc(h.reason)}</td></tr>`).join('')||'<tr><td colspan="7">No matching changes recorded.</td></tr>';
}});
function updateTargets() {
    if(!chart)return;
    const student=chart.students.find(s=>s.id===Number(seatingNode('studentId').value));
    const current=chart.seats.find(s=>s.studentId===student?.id),swap=seatingNode('action').value==='swap';
    const chosen=seatingNode('seatId').value;
    const options=chart.seats.map(seat=>{
        let blocked='';
        if(!student||!current)blocked='No allocated student selected';
        else if(seat.id===current.id)blocked='Current seat';
        else if(chart.exam.spacingRequired&&seat.columnNumber%2===0)blocked='Reserved for spacing';
        else if(student.requiresAccessible&&!seat.isAccessible)blocked='Accessible seat needed';
        else if(swap&&!seat.studentId)blocked='Empty — choose move instead';
        else if(!swap&&seat.studentId)blocked='Occupied — choose swap instead';
        else if(swap&&seat.requiresAccessible&&!current.isAccessible)blocked='Other student needs accessibility';
        return {seat,blocked};
    });
    seatingNode('seatId').innerHTML=options.map(({seat,blocked})=>`<option value="${seat.id}" ${blocked?'disabled':''}>${esc(seat.roomNumber)} / ${esc(seat.seatNumber)} — ${esc(blocked||(seat.studentId?seat.rollNumber:'Empty'))}${seat.isAccessible?' · accessible':''}</option>`).join('');
    if(options.some(o=>String(o.seat.id)===chosen&&!o.blocked))seatingNode('seatId').value=chosen;
    else seatingNode('seatId').value=options.find(o=>!o.blocked)?.seat.id||'';
    AppUI.searchableSelect(seatingNode('seatId'),'Target seat');
    const editable=['DRAFT','SCHEDULED'].includes(chart.exam.status);
    seatingNode('overrideButton').disabled=!editable||!options.some(o=>!o.blocked);
    seatingNode('overrideHelp').textContent=!options.some(o=>!o.blocked)?(swap?'No eligible swap target. Choose another student.':'No eligible empty seat for this student. Choose swap, another student or review the accessible-seat layout.'):swap?'Both students keep an eligible seat. Unavailable targets are disabled; the server validates again before saving.':'Choose an empty, eligible target seat and enter a reason. Unavailable targets are disabled.';
}
function filterChart() {
    const query=seatingNode('seatSearch').value.trim().toLowerCase(),room=seatingNode('chartRoom').value;
    let count=0;
    document.querySelectorAll('[data-chart-room]').forEach(panel=>panel.hidden=!!room&&panel.dataset.chartRoom!==room);
    document.querySelectorAll('[data-seat-id]').forEach(button=>{
        const seat=chart.seats.find(s=>s.id===Number(button.dataset.seatId));
        const match=(!room||String(seat.roomId)===room)&&`${seat.roomNumber} ${seat.seatNumber} ${seat.rollNumber||''} ${seat.name||''}`.toLowerCase().includes(query);
        button.classList.toggle('muted-seat',!!query&&!match);if(match)count++;
    });
    seatingNode('seatSearchCount').textContent=query?`${count} matching seats. Nonmatching seats stay visible to preserve the room layout.`:`${count} physical seats${room?' in this room':''}. Reserved columns are unavailable when spacing is enabled.`;
}
async function loadChart() {
    if(!seatingNode('examId').value)return;
    chart=await apiRequest(AppUI.url(`api/admin/seating?examId=${seatingNode('examId').value}`));
    const assigned=chart.seats.filter(s=>s.studentId);
    seatingNode('summary').textContent=`${chart.exam.courseCode} · ${chart.exam.academicYear} · ${chart.exam.status.toLowerCase()} · ${assigned.length} of ${chart.students.length} students assigned · ${chart.exam.spacingRequired?'alternate columns reserved':'full-capacity layout'}`;
    const selectedStudent=seatingNode('studentId').value;
    seatingNode('studentId').innerHTML=assigned.map(s=>`<option value="${s.studentId}">${esc(s.rollNumber)} — ${esc(s.name)}${s.requiresAccessible?' · accessible seat required':''}</option>`).join('');
    if(assigned.some(s=>String(s.studentId)===selectedStudent))seatingNode('studentId').value=selectedStudent;
    AppUI.searchableSelect(seatingNode('studentId'),'Student');
    const rooms=new Map();chart.seats.forEach(s=>{if(!rooms.has(s.roomId))rooms.set(s.roomId,[]);rooms.get(s.roomId).push(s);});
    const selectedRoom=seatingNode('chartRoom').value;
    seatingNode('chartRoom').innerHTML='<option value="">All rooms</option>'+[...rooms.values()].map(seats=>`<option value="${seats[0].roomId}">${esc(seats[0].roomNumber)}</option>`).join('');
    if(rooms.has(Number(selectedRoom)))seatingNode('chartRoom').value=selectedRoom;
    seatingNode('charts').innerHTML=[...rooms.values()].map(seats=>`<section class="panel" data-chart-room="${seats[0].roomId}"><div class="panel-heading"><h2>Room ${esc(seats[0].roomNumber)}</h2><span class="helper-text">${seats.filter(s=>s.studentId).length} assigned · ${seats.length} physical seats</span></div><div class="seat-grid" style="--columns:${seats[0].seatColumns}">${seats.map(s=>`<button type="button" data-seat-id="${s.id}" class="seat ${s.studentId?'occupied':''} ${s.isAccessible?'accessible':''} ${chart.exam.spacingRequired&&s.columnNumber%2===0?'blocked':''}" aria-label="${esc(`${s.roomNumber}, ${s.seatNumber}, ${s.studentId?s.rollNumber+' '+s.name:'empty'}${s.isAccessible?', accessible':''}${s.isManual?', manual assignment':''}`)}"><strong>${esc(s.seatNumber)}</strong><span>${s.studentId?esc(s.rollNumber):chart.exam.spacingRequired&&s.columnNumber%2===0?'Spacing':'Empty'}</span>${s.studentId?`<span>${esc(s.name)}</span>`:''}${s.isAccessible?'<small>Accessible</small>':''}${s.isManual?'<span class="seat-manual">Manual assignment</span>':''}</button>`).join('')}</div></section>`).join('');
    historyList.set(chart.history.map(value=>({value,text:`${value.actor} ${value.action} ${value.oldSeat||''} ${value.newSeat||''} ${value.reason} ${chart.students.find(s=>s.id===value.studentId)?.rollNumber||''}`})));
    seatingNode('historyEditor').querySelector('summary').textContent=`Change history (${chart.history.length} recent events)`;
    const editable=['DRAFT','SCHEDULED'].includes(chart.exam.status);
    seatingNode('generateButton').disabled=!editable;
    seatingNode('openOverride').disabled=!editable||!assigned.length;
    seatingNode('clearButton').disabled=!['DRAFT','CANCELLED'].includes(chart.exam.status);
    updateTargets();filterChart();
}
async function seatingAction(action,values={}) {return apiRequest(AppUI.url('api/admin/seating'),{method:'POST',body:formBody({examId:seatingNode('examId').value,action,...values})});}
seatingNode('examId').addEventListener('change',()=>{AppUI.remember({examId:seatingNode('examId').value});seatingNotice('');loadChart().catch(e=>seatingNotice(e.message));});
seatingNode('studentId').addEventListener('change',updateTargets);seatingNode('action').addEventListener('change',updateTargets);
seatingNode('seatSearch').addEventListener('input',filterChart);seatingNode('chartRoom').addEventListener('change',filterChart);
seatingNode('openOverride').addEventListener('click',()=>{seatingNode('manualEditor').open=true;seatingNode('manualEditor').scrollIntoView({behavior:'smooth',block:'start'});seatingNode('studentId').focus();});
seatingNode('charts').addEventListener('click',e=>{
    const button=e.target.closest('[data-seat-id]');if(!button)return;
    const seat=chart.seats.find(s=>s.id===Number(button.dataset.seatId));
    if(!seat.studentId){seatingNotice(`${seat.roomNumber} / ${seat.seatNumber}: ${chart.exam.spacingRequired&&seat.columnNumber%2===0?'reserved for spacing':'unassigned seat'}.` ,true);return;}
    // A chart click selects a student only. Saving still requires a target and reason.
    seatingNode('studentId')._searchInput&&(seatingNode('studentId')._searchInput.value='');
    seatingNode('studentId').innerHTML=chart.seats.filter(s=>s.studentId).map(s=>`<option value="${s.studentId}">${esc(s.rollNumber)} — ${esc(s.name)}</option>`).join('');
    seatingNode('studentId').value=seat.studentId;updateTargets();seatingNode('manualEditor').open=true;
    seatingNode('manualEditor').scrollIntoView({behavior:'smooth',block:'start'});seatingNode('reason').focus();
});
seatingNode('generateButton').addEventListener('click',()=>{
    const replaceManual=seatingNode('replaceManual').checked;
    if(!confirm(replaceManual?'Replace all seating, including manual assignments? Every change is recorded.':'Generate seating while keeping existing manual assignments?'))return;
    AppUI.busy(seatingNode('generateButton'),async()=>{try{const result=await seatingAction('generate',{replaceManual,reason:replaceManual?'Administrator confirmed full regeneration':'Regenerated while preserving manual assignments'});await loadChart();seatingNotice(`${result.assigned} students assigned successfully.`,true);}catch(error){seatingNotice(error.message);}},'Generating seating…');
});
seatingNode('overrideForm').addEventListener('submit',e=>{
    e.preventDefault();AppUI.busy(seatingNode('overrideButton'),async()=>{try{await seatingAction(seatingNode('action').value,{studentId:seatingNode('studentId').value,seatId:seatingNode('seatId').value,reason:seatingNode('reason').value});seatingNode('reason').value='';await loadChart();seatingNotice('Manual change saved and recorded.',true);}catch(error){seatingNotice(error.message);}});
});
seatingNode('clearButton').addEventListener('click',()=>{
    if(!confirm('Clear this draft or cancelled seating plan? Recorded history is retained.'))return;
    AppUI.busy(seatingNode('clearButton'),async()=>{try{await seatingAction('clear',{reason:'Administrator confirmed clearing seating'});await loadChart();seatingNotice('Seating cleared. History retained.',true);}catch(error){seatingNotice(error.message);}},'Clearing seating…');
});
(async()=>{
    if(!await requireRole('ADMIN'))return;
    try{
        const exams=(await apiRequest(AppUI.url('api/admin/exams'))).exams;
        seatingNode('examId').innerHTML=exams.map(e=>`<option value="${e.id}">${esc(e.courseCode)} / ${esc(e.academicYear)} / ${esc(AppUI.date(e.examDate))} / ${esc(e.status.toLowerCase())}</option>`).join('');
        if(exams.some(e=>String(e.id)===AppUI.params.get('examId')))seatingNode('examId').value=AppUI.params.get('examId');
        AppUI.searchableSelect(seatingNode('examId'),'Examination');
        if(exams.length)await loadChart();else{seatingNotice('Create an examination first, then generate its seating plan.');['generateButton','openOverride','clearButton','overrideButton'].forEach(id=>seatingNode(id).disabled=true);}
    }catch(error){seatingNotice(error.message);}
})();
