let chart = null, loading = 0, working = false, sourceSelected = false, pendingOperation = null;
const seatingNode = id => document.getElementById(id);
const seatingNotice = (text, ok = false) => AppUI.message(seatingNode('message'), text, ok);
const operationName = action => ({ACCOMMODATION_ON:'Accommodation required',ACCOMMODATION_OFF:'Accommodation removed',ACCOMMODATION_REGENERATE:'Accommodation seating update'}[action] || action.toLowerCase().replace(/_/g,' ').replace(/^./, c=>c.toUpperCase()));
const historyList = AppUI.list({
    search:seatingNode('historySearch'), previous:seatingNode('historyPrevious'), next:seatingNode('historyNext'), count:seatingNode('historyCount'),
    render:rows => { seatingNode('history').innerHTML = rows.map(h=>`<tr><td>${esc(h.createdAt)}</td><td>${esc(h.actor)}</td><td>${esc(h.rollNumber||'—')}</td><td>${esc(operationName(h.action))}</td><td>${esc(h.oldSeat||'—')}</td><td>${esc(h.newSeat||'—')}</td><td>${esc(h.reason)}</td></tr>`).join('') || '<tr><td colspan="7">No matching changes recorded.</td></tr>'; }
});
const editable = () => chart && ['DRAFT','SCHEDULED'].includes(chart.exam.status);
function updateControls() {
    const blocked = working || seatingNode('operationDialog').open;
    const assigned = chart?.seats.some(s=>s.studentId);
    seatingNode('examId').disabled = blocked;
    seatingNode('generateButton').disabled = blocked || !editable() || !chart?.students.length;
    seatingNode('generateButton').textContent = assigned ? 'Regenerate seating' : 'Generate seating';
    seatingNode('openOverride').disabled = blocked || !editable() || !assigned;
    seatingNode('openAccommodation').disabled = blocked || !editable() || !chart?.students.length;
    seatingNode('clearButton').disabled = blocked || !assigned || !['DRAFT','CANCELLED'].includes(chart?.exam.status);
    seatingNode('overrideButton').disabled = blocked || !editable() || !seatingNode('seatId').value || !!seatingNode('seatId').selectedOptions[0]?.disabled;
    const student = chart?.students.find(s=>s.id===Number(seatingNode('accommodationStudentId').value));
    seatingNode('accommodationButton').disabled = blocked || !editable() || !student || !!student.requiresAccessible===seatingNode('requiresAccessible').checked;
    ['studentId','action','seatId','accommodationStudentId','requiresAccessible','replaceManual'].forEach(id=>seatingNode(id).disabled=blocked||!editable());
}
function targetOptions() {
    if (!chart) return [];
    const student = chart.students.find(s=>s.id===Number(seatingNode('studentId').value));
    const current = chart.seats.find(s=>s.studentId===student?.id), swap = seatingNode('action').value==='swap';
    return chart.seats.map(seat=>{
        let blocked = '';
        if (!student || !current) blocked='No assigned student selected';
        else if (seat.id===current.id) blocked='Current seat';
        else if (chart.exam.spacingRequired && seat.columnNumber%2===0) blocked='Reserved for spacing';
        else if (student.requiresAccessible && !seat.isAccessible) blocked='Accessible seat required';
        else if (swap && !seat.studentId) blocked='Empty — choose move';
        else if (!swap && seat.studentId) blocked='Occupied — choose swap';
        else if (swap && seat.requiresAccessible && !current.isAccessible) blocked='Other student requires accessibility';
        return {seat,blocked};
    });
}
function updateTargets() {
    if (!chart) return;
    const chosen = seatingNode('seatId').value, options = targetOptions();
    seatingNode('seatId').innerHTML = options.map(({seat,blocked})=>`<option value="${seat.id}" ${blocked?'disabled':''}>${esc(seat.roomNumber)} / ${esc(seat.seatNumber)} — ${esc(blocked || (seat.studentId?seat.rollNumber:'Empty'))}${seat.isAccessible?' · accessible':''}</option>`).join('');
    seatingNode('seatId').value = options.some(o=>String(o.seat.id)===chosen && !o.blocked) ? chosen : options.find(o=>!o.blocked)?.seat.id || '';
    AppUI.searchableSelect(seatingNode('seatId'),'Target seat');
    seatingNode('overrideHelp').textContent = options.some(o=>!o.blocked)
        ? 'Select a destination from the chart or list. Both students must satisfy accessibility and spacing requirements.'
        : 'No eligible target for this action. Choose another student or review the room layout.';
    updateSelection(); updateControls();
}
function updateSelection() {
    if (!chart) return;
    const student = Number(seatingNode('studentId').value), target = Number(seatingNode('seatId').value);
    const current = chart.seats.find(s=>s.studentId===student), destination = chart.seats.find(s=>s.id===target);
    document.querySelectorAll('[data-seat-id]').forEach(button=>{
        button.classList.toggle('source-seat',sourceSelected && Number(button.dataset.seatId)===current?.id);
        button.classList.toggle('target-seat',sourceSelected && Number(button.dataset.seatId)===target);
        button.setAttribute('aria-pressed',String(sourceSelected && [current?.id,target].includes(Number(button.dataset.seatId))));
    });
    seatingNode('selectionSummary').textContent = current && destination
        ? `${current.rollNumber}: ${current.roomNumber} / ${current.seatNumber} → ${destination.roomNumber} / ${destination.seatNumber}${destination.studentId?' · '+destination.rollNumber+' moves to the source seat':''}`
        : '';
}
function updateAccommodation() {
    const student = chart?.students.find(s=>s.id===Number(seatingNode('accommodationStudentId').value));
    seatingNode('requiresAccessible').checked = !!student?.requiresAccessible; updateControls();
}
function filterChart() {
    if (!chart) return;
    const query = seatingNode('seatSearch').value.trim().toLowerCase(), room = seatingNode('chartRoom').value;
    let count = 0;
    document.querySelectorAll('[data-chart-room]').forEach(panel=>panel.hidden=!!room && panel.dataset.chartRoom!==room);
    document.querySelectorAll('[data-seat-id]').forEach(button=>{
        const seat = chart.seats.find(s=>s.id===Number(button.dataset.seatId));
        const match = (!room || String(seat.roomId)===room) && `${seat.roomNumber} ${seat.seatNumber} ${seat.rollNumber||''} ${seat.name||''}`.toLowerCase().includes(query);
        button.classList.toggle('muted-seat',!!query && !match); if (match) count++;
    });
    seatingNode('seatSearchCount').textContent = query ? `${count} matching seats. Other seats remain visible to preserve the layout.` : `${count} physical seats${room?' in this room':''}.`;
}
async function loadChart() {
    const examId = seatingNode('examId').value, request = ++loading;
    if (!examId) return;
    const data = await apiRequest(AppUI.url(`api/admin/seating?examId=${examId}`));
    if (request!==loading || examId!==seatingNode('examId').value) return;
    chart = data;
    const assigned = chart.seats.filter(s=>s.studentId);
    seatingNode('summary').textContent = `${chart.exam.courseCode} · ${chart.exam.academicYear} · ${chart.exam.status.toLowerCase()} · ${assigned.length} of ${chart.students.length} students assigned · ${chart.exam.spacingRequired?'alternate columns reserved':'full-capacity layout'}`;
    const required = chart.students.filter(s=>s.requiresAccessible).length;
    const qualifying = chart.seats.filter(s=>s.isAccessible && (!chart.exam.spacingRequired || s.columnNumber%2===1)).length;
    seatingNode('accommodationSummary').textContent = `${required} student${required===1?'':'s'} require accessible seating · ${qualifying} eligible accessible seats. Other students use regular seats first.`;
    const selectedStudent = seatingNode('studentId').value, selectedAccommodation = seatingNode('accommodationStudentId').value;
    const studentOption = s=>`<option value="${s.id}">${esc(s.rollNumber)} — ${esc(s.name)}${s.requiresAccessible?' · accommodation required':''}</option>`;
    seatingNode('studentId').innerHTML = chart.students.filter(s=>assigned.some(a=>a.studentId===s.id)).map(studentOption).join('');
    seatingNode('accommodationStudentId').innerHTML = chart.students.map(studentOption).join('');
    if (assigned.some(s=>String(s.studentId)===selectedStudent)) seatingNode('studentId').value=selectedStudent;
    if (chart.students.some(s=>String(s.id)===selectedAccommodation)) seatingNode('accommodationStudentId').value=selectedAccommodation;
    AppUI.searchableSelect(seatingNode('studentId'),'Student');
    AppUI.searchableSelect(seatingNode('accommodationStudentId'),'Accommodation student');
    const rooms = new Map(); chart.seats.forEach(s=>{if(!rooms.has(s.roomId))rooms.set(s.roomId,[]);rooms.get(s.roomId).push(s);});
    const selectedRoom = seatingNode('chartRoom').value;
    seatingNode('chartRoom').innerHTML = '<option value="">All rooms</option>' + [...rooms.values()].map(seats=>`<option value="${seats[0].roomId}">${esc(seats[0].roomNumber)}</option>`).join('');
    if (rooms.has(Number(selectedRoom))) seatingNode('chartRoom').value=selectedRoom;
    seatingNode('charts').innerHTML = [...rooms.values()].map(seats=>`<section class="panel" data-chart-room="${seats[0].roomId}"><div class="panel-heading"><h2>Room ${esc(seats[0].roomNumber)}</h2><span class="helper-text">${seats.filter(s=>s.studentId).length} assigned · ${seats.length} physical seats</span></div><div class="seat-grid" style="--columns:${seats[0].seatColumns}">${seats.map(s=>`<button type="button" data-seat-id="${s.id}" class="seat ${s.studentId?'occupied':''} ${s.isAccessible?'accessible':''} ${chart.exam.spacingRequired&&s.columnNumber%2===0?'blocked':''}" aria-label="${esc(`${s.roomNumber}, ${s.seatNumber}, ${s.studentId?s.rollNumber+' '+s.name:'empty'}${s.isAccessible?', accessible seat':''}${s.requiresAccessible?', accommodation required':''}${s.isManual?', manual assignment':''}`)}"><strong>${esc(s.seatNumber)}</strong><span>${s.studentId?esc(s.rollNumber):chart.exam.spacingRequired&&s.columnNumber%2===0?'Spacing':'Empty'}</span>${s.studentId?`<span>${esc(s.name)}</span>`:''}${s.isAccessible?'<small>Accessible seat</small>':''}${s.requiresAccessible?'<small class="seat-requirement">Accommodation required</small>':''}${s.isManual?'<span class="seat-manual">Manual assignment</span>':''}</button>`).join('')}</div></section>`).join('') || '<section class="panel empty-state">No rooms assigned to this examination.</section>';
    historyList.set(chart.history.map(value=>({value,text:`${value.actor} ${value.action} ${value.oldSeat||''} ${value.newSeat||''} ${value.reason} ${value.rollNumber||''}`})));
    seatingNode('historyEditor').querySelector('summary').textContent=`Change history (${chart.history.length} recent events)`;
    updateAccommodation(); updateTargets(); filterChart();
}
async function runWork(work, label='Validating seating…') {
    if(working)return;
    working=true; updateControls(); seatingNotice('');
    seatingNode('operationProgress').textContent=label;seatingNode('operationProgress').hidden=false;
    try { await work(); }
    catch(error) {
        if (error.status===409) { try { await loadChart(); } catch (_) {} }
        seatingNotice(error.message);
    } finally { working=false;seatingNode('operationProgress').hidden=true;updateControls(); }
}
function reviewOperation(title, details, reason, action, replacement=false) {
    pendingOperation=action;
    seatingNode('operationTitle').textContent=title;
    seatingNode('operationDetails').innerHTML=details;
    seatingNode('operationReason').value=reason;
    seatingNode('replacementAcknowledgement').hidden=!replacement;
    seatingNode('confirmReplacement').required=replacement;
    seatingNode('confirmReplacement').checked=false;
    seatingNode('operationDialog').showModal(); updateControls();
    seatingNode('operationReason').focus();
}
function closeOperation() {
    seatingNode('operationDialog').close(); pendingOperation=null;
    seatingNode('replaceManual').checked=false; updateControls();
}
async function seatingAction(action, values={}) {
    return apiRequest(AppUI.url('api/admin/seating'),{method:'POST',body:formBody({examId:chart.exam.id,expectedRevision:chart.revision,action,...values})});
}
seatingNode('confirmationForm').addEventListener('submit',e=>{
    e.preventDefault();
    if(working || !pendingOperation)return;
    const action=pendingOperation, reason=seatingNode('operationReason').value.trim();
    const button=seatingNode('confirmOperation'); button.disabled=true;button.textContent='Saving…';
    runWork(async()=>{
        try { await action(reason); closeOperation(); await loadChart(); }
        catch(error) { closeOperation(); throw error; }
        finally { button.disabled=false;button.textContent='Confirm'; }
    },'Saving seating changes…');
});
seatingNode('cancelOperation').addEventListener('click',closeOperation);
seatingNode('operationDialog').addEventListener('cancel',()=>{pendingOperation=null;seatingNode('replaceManual').checked=false;});
seatingNode('operationDialog').addEventListener('close',updateControls);
seatingNode('examId').addEventListener('change',()=>{
    sourceSelected=false; seatingNode('replaceManual').checked=false; AppUI.remember({examId:seatingNode('examId').value});
    loadChart().catch(e=>seatingNotice(e.message));
});
seatingNode('studentId').addEventListener('change',()=>{sourceSelected=true;updateTargets();});
seatingNode('action').addEventListener('change',updateTargets);
seatingNode('seatId').addEventListener('change',updateSelection);
seatingNode('accommodationStudentId').addEventListener('change',updateAccommodation);
seatingNode('requiresAccessible').addEventListener('change',updateControls);
seatingNode('seatSearch').addEventListener('input',filterChart); seatingNode('chartRoom').addEventListener('change',filterChart);
seatingNode('openOverride').addEventListener('click',()=>{sourceSelected=true;seatingNode('manualEditor').open=true;updateSelection();seatingNode('manualEditor').scrollIntoView({behavior:'smooth',block:'start'});seatingNode('studentId').focus();});
seatingNode('openAccommodation').addEventListener('click',()=>{seatingNode('accommodationEditor').open=true;seatingNode('accommodationEditor').scrollIntoView({behavior:'smooth',block:'start'});seatingNode('accommodationStudentId').focus();});
seatingNode('charts').addEventListener('click',e=>{
    const button=e.target.closest('[data-seat-id]'); if(!button || !editable() || working)return;
    const seat=chart.seats.find(s=>s.id===Number(button.dataset.seatId));
    if (!sourceSelected || !seatingNode('manualEditor').open) {
        if(!seat.studentId){seatingNotice('Select an occupied source seat first.');return;}
        seatingNode('studentId').innerHTML=chart.students.filter(s=>chart.seats.some(a=>a.studentId===s.id)).map(s=>`<option value="${s.id}">${esc(s.rollNumber)} — ${esc(s.name)}${s.requiresAccessible?' · accommodation required':''}</option>`).join('');
        seatingNode('studentId').value=seat.studentId; AppUI.searchableSelect(seatingNode('studentId'),'Student');
        sourceSelected=true;seatingNode('manualEditor').open=true;updateTargets();seatingNotice('Source selected. Choose the destination seat.',true);return;
    }
    seatingNode('action').value=seat.studentId?'swap':'reassign'; updateTargets();
    const option=targetOptions().find(o=>o.seat.id===seat.id);
    if(option.blocked){seatingNotice(option.blocked);return;}
    seatingNode('seatId').value=seat.id;updateSelection();seatingNotice('Destination selected. Enter a reason in the manual-change form.',true);
});
seatingNode('generateButton').addEventListener('click',()=>runWork(async()=>{
    const replaceManual=seatingNode('replaceManual').checked, examId=chart.exam.id;
    const preview=await apiRequest(AppUI.url(`api/admin/seating?examId=${examId}&action=preview&replaceManual=${replaceManual}`));
    const changes=preview.changes.length?`<div class="table-wrap preview-changes"><table><thead><tr><th>Student</th><th>From</th><th>To</th></tr></thead><tbody>${preview.changes.map(c=>`<tr><td>${esc(c.rollNumber)}</td><td>${esc(c.oldSeat||'Unassigned')}</td><td>${esc(c.newSeat)}</td></tr>`).join('')}</tbody></table></div>`:'<p>Seat positions remain the same.</p>';
    reviewOperation(chart.seats.some(s=>s.studentId)?'Review regeneration':'Review seating allocation',
        `<p>${preview.assigned} students will be assigned; ${preview.changed} seat position${preview.changed===1?'':'s'} will change.</p><p>${replaceManual?preview.replacedManual+' manual assignment'+(preview.replacedManual===1?'':'s')+' will be replaced.':preview.preservedManual+' manual assignment'+(preview.preservedManual===1?'':'s')+' will be preserved.'}</p>${changes}`,
        chart.seats.some(s=>s.studentId)?'Seating regeneration':'Initial seating allocation',
        async reason=>{const result=await seatingAction('generate',{examId,replaceManual,confirmReplaceManual:replaceManual,expectedRevision:preview.revision,reason});seatingNotice(`${result.assigned} students assigned. Seating history updated.`,true);},
        replaceManual);
}));
seatingNode('overrideForm').addEventListener('submit',e=>{
    e.preventDefault();
    const action=seatingNode('action').value, studentId=Number(seatingNode('studentId').value), seatId=Number(seatingNode('seatId').value), revision=chart.revision;
    const source=chart.seats.find(s=>s.studentId===studentId), target=chart.seats.find(s=>s.id===seatId);
    reviewOperation(action==='swap'?'Confirm seat swap':'Confirm seat reassignment',
        `<p>${esc(source.rollNumber)}: ${esc(source.roomNumber)} / ${esc(source.seatNumber)} → ${esc(target.roomNumber)} / ${esc(target.seatNumber)}</p>${action==='swap'?`<p>${esc(target.rollNumber)}: ${esc(target.roomNumber)} / ${esc(target.seatNumber)} → ${esc(source.roomNumber)} / ${esc(source.seatNumber)}</p>`:''}<p>The changed assignments will be marked manual and preserved during standard regeneration.</p>`,
        seatingNode('reason').value,
        async reason=>{await seatingAction(action,{studentId,seatId,expectedRevision:revision,reason});seatingNode('reason').value='';seatingNotice('Manual seating change saved.',true);});
});
seatingNode('accommodationForm').addEventListener('submit',e=>{
    e.preventDefault();runWork(async()=>{
        const studentId=Number(seatingNode('accommodationStudentId').value), required=seatingNode('requiresAccessible').checked, examId=chart.exam.id;
        const student=chart.students.find(s=>s.id===studentId);
        const preview=await apiRequest(AppUI.url(`api/admin/seating?examId=${examId}&action=previewAccommodation&studentId=${studentId}&requiresAccessible=${required}`));
        reviewOperation('Review accommodation requirement',
            `<p>${esc(student.rollNumber)} — ${esc(student.name)}: accessible seating ${required?'required':'not required'}.</p><p>${preview.affectedExams.length} generated active examination plan${preview.affectedExams.length===1?'':'s'} will be updated; ${preview.changed} seat position${preview.changed===1?'':'s'} will change. ${preview.releasedManual} incompatible manual assignment${preview.releasedManual===1?'':'s'} for this student will be released.</p>${preview.affectedExams.length?'<ul>'+preview.affectedExams.map(exam=>`<li>${esc(exam.courseCode)} / ${esc(exam.academicYear)}: ${exam.changed} seat change${exam.changed===1?'':'s'}</li>`).join('')+'</ul>':''}<p>Other valid manual assignments will be retained. Ungenerated and completed examinations will not be regenerated.</p>`,
            seatingNode('accommodationReason').value,
            async reason=>{const result=await seatingAction('accommodation',{examId,studentId,requiresAccessible:required,expectedRevision:preview.revision,reason});seatingNode('accommodationReason').value='';seatingNotice(`Accommodation requirement saved. ${result.affectedExams.length} seating plan${result.affectedExams.length===1?'':'s'} updated.`,true);});
    });
});
seatingNode('clearButton').addEventListener('click',()=>{
    const revision=chart.revision;
    reviewOperation('Confirm clearing seating','<p>All current assignments will be removed. The recorded history will be retained.</p>','Seating plan cleared',
        async reason=>{await seatingAction('clear',{expectedRevision:revision,reason});sourceSelected=false;seatingNotice('Seating cleared. History retained.',true);});
});
(async()=>{
    if(!await requireRole('ADMIN'))return;
    try {
        const exams=(await apiRequest(AppUI.url('api/admin/exams'))).exams;
        seatingNode('examId').innerHTML=exams.map(e=>`<option value="${e.id}">${esc(e.courseCode)} / ${esc(e.academicYear)} / ${esc(AppUI.date(e.examDate))} / ${esc(e.status.toLowerCase())}</option>`).join('');
        if(exams.some(e=>String(e.id)===AppUI.params.get('examId')))seatingNode('examId').value=AppUI.params.get('examId');
        AppUI.searchableSelect(seatingNode('examId'),'Examination');
        if(exams.length)await loadChart();else{seatingNotice('Create an examination before allocating seats.');updateControls();}
    } catch(error) {seatingNotice(error.message);updateControls();}
})();
