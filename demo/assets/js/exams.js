let exams=[],examData,clearExamDirty;
const examNode=id=>document.getElementById(id);
const examNotice=(text,ok=false)=>AppUI.message(examNode('message'),text,ok);
const examList=AppUI.list({search:examNode('recordSearch'),size:examNode('pageSize'),previous:examNode('previousPage'),next:examNode('nextPage'),count:examNode('recordCount'),render:renderExamRows});
function filterExams() {
    const year=Number(examNode('examYearFilter').value),status=examNode('statusFilter').value;
    examList.set(exams.filter(e=>(!year||e.academicYearId===year)&&(!status||e.status===status)).map(value=>({value,text:`${value.courseCode} ${value.courseName} ${value.academicYear} ${value.examDate} ${AppUI.date(value.examDate)} ${value.rooms.map(r=>r.roomNumber).join(' ')} ${value.status}`})));
}
function renderExamRows(rows) {
    examNode('examRows').innerHTML=rows.map(e=>`<tr><td><strong>${esc(e.courseCode)}</strong><br>${esc(e.courseName)}</td><td>${esc(e.academicYear)}</td><td>${esc(AppUI.date(e.examDate))}<br>${esc(AppUI.time(e.startTime))} – ${esc(AppUI.time(e.endTime))}</td><td>${e.rooms.map(r=>esc(r.roomNumber)).join(', ')}</td><td>${AppUI.badge(e.status)}</td><td><a href="seating.html?examId=${e.id}">View seating</a><details class="row-actions"><summary>Actions</summary><div class="action-links">${e.status==='DRAFT'?`<button type="button" data-edit="${e.id}" class="secondary">Edit</button><button type="button" data-status="SCHEDULED" data-id="${e.id}">Schedule</button>`:e.status==='SCHEDULED'?`<button type="button" data-status="COMPLETED" data-id="${e.id}" class="secondary">Complete</button>`:''}${['DRAFT','SCHEDULED'].includes(e.status)?`<button type="button" data-status="CANCELLED" data-id="${e.id}" class="danger">Cancel exam</button>`:''}${['DRAFT','CANCELLED'].includes(e.status)?`<button type="button" data-delete="${e.id}" class="danger">Delete</button>`:''}</div></details></td></tr>`).join('')||'<tr><td colspan="6"><div class="empty-state">No matching exams. Change your filters or create an examination.</div></td></tr>';
}
async function loadExams() {exams=(await apiRequest(AppUI.url('api/admin/exams'))).exams;filterExams();}
async function loadCoursesForYear() {
    const data=await apiRequest(AppUI.url(`api/admin/data?academicYearId=${examNode('academicYearId').value}`));
    examNode('courseId').innerHTML=data.courses.filter(c=>c.offered).map(c=>`<option value="${c.id}">${esc(c.code)} — ${esc(c.name)} / ${AppUI.studyYear(c.studyYear)}</option>`).join('');
    AppUI.searchableSelect(examNode('courseId'),'Course offering');
}
function updateRoomSummary() {
    const chosen=[...examNode('roomChoices').querySelectorAll('input:checked')].map(input=>examData.rooms.find(r=>r.id===Number(input.value)));
    examNode('roomSelection').textContent=chosen.length?`${chosen.length} room${chosen.length===1?'':'s'} selected · ${chosen.reduce((sum,r)=>sum+r.capacity,0)} physical seats${examNode('spacingRequired').checked?' (usable capacity is lower with spacing)':''}`:'Select one or more rooms.';
}
function filterRooms() {
    const query=examNode('roomSearch').value.toLowerCase();
    examNode('roomChoices').querySelectorAll('label').forEach(label=>label.hidden=!label.textContent.toLowerCase().includes(query));
}
async function openEditor(id=0) {
    examNode('examForm').reset();clearExamDirty();examNode('id').value=id||'';
    examNode('academicYearId').value=examData.selectedYearId;await loadCoursesForYear();
    examNode('examEditorTitle').textContent=id?'Edit draft examination':'Create an exam';
    if(id) {
        const e=exams.find(e=>e.id===id);examNode('academicYearId').value=e.academicYearId;await loadCoursesForYear();
        for(const key of ['courseId','examDate','startTime','endTime','status'])examNode(key).value=e[key];
        examNode('spacingRequired').checked=!!e.spacingRequired;
        const selected=new Set(e.rooms.map(r=>r.id));examNode('roomChoices').querySelectorAll('input').forEach(input=>input.checked=selected.has(Number(input.value)));
    }
    filterRooms();updateRoomSummary();examNode('examEditor').open=true;
    examNode('examEditor').scrollIntoView({behavior:'smooth',block:'start'});examNode('academicYearId').focus();
}
async function mutate(values,button) {
    await AppUI.busy(button,async()=>{try{await apiRequest(AppUI.url('api/admin/exams'),{method:'POST',body:formBody(values)});await loadExams();examNotice('Examination updated.',true);}catch(error){examNotice(error.message);}});
}
examNode('academicYearId').addEventListener('change',()=>loadCoursesForYear().catch(e=>examNotice(e.message)));
examNode('examYearFilter').addEventListener('change',()=>{AppUI.remember({year:examNode('examYearFilter').value});filterExams();});
examNode('statusFilter').addEventListener('change',()=>{AppUI.remember({status:examNode('statusFilter').value});filterExams();});
examNode('roomSearch').addEventListener('input',filterRooms);examNode('roomChoices').addEventListener('change',updateRoomSummary);examNode('spacingRequired').addEventListener('change',updateRoomSummary);
examNode('newExam').addEventListener('click',()=>openEditor().catch(e=>examNotice(e.message)));
examNode('refreshExams').addEventListener('click',()=>AppUI.busy(examNode('refreshExams'),()=>loadExams().catch(e=>examNotice(e.message)),'Refreshing…'));
examNode('clearButton').addEventListener('click',()=>{examNode('examForm').reset();clearExamDirty();examNode('id').value='';examNode('examEditor').open=false;examNotice('');});
examNode('examRows').addEventListener('click',e=>{
    const button=e.target.closest('button');if(!button)return;
    if(button.dataset.edit){openEditor(Number(button.dataset.edit)).catch(e=>examNotice(e.message));return;}
    if(button.dataset.delete){if(confirm('Delete this exam? Exams with seating history are retained.'))mutate({action:'delete',id:button.dataset.delete},button);return;}
    if(button.dataset.status){if(confirm(`Mark this examination as ${button.dataset.status.toLowerCase()}?`))mutate({action:'status',id:button.dataset.id,status:button.dataset.status},button);}
});
examNode('examForm').addEventListener('submit',e=>{
    e.preventDefault();AppUI.busy(examNode('saveExam'),async()=>{
        const chosen=[...examNode('roomChoices').querySelectorAll('input:checked')];
        if(!chosen.length){examNotice('Select at least one room for this examination.');examNode('roomSearch').focus();return;}
        if(examNode('startTime').value>=examNode('endTime').value){examNotice('End time must be after the start time.');examNode('endTime').focus();return;}
        const id=examNode('id').value,values={action:id?'update':'create',id,spacingRequired:examNode('spacingRequired').checked};
        for(const key of ['academicYearId','courseId','examDate','startTime','endTime','status'])values[key]=examNode(key).value;
        const body=formBody(values);chosen.forEach(input=>body.append('roomId',input.value));
        try{await apiRequest(AppUI.url('api/admin/exams'),{method:'POST',body});clearExamDirty();await loadExams();examNode('examEditor').open=false;examNotice('Examination saved.',true);}catch(error){examNotice(error.message);}
    });
});
(async()=>{
    if(!await requireRole('ADMIN'))return;
    clearExamDirty=AppUI.dirty(examNode('examForm'));
    try{
        examData=await apiRequest(AppUI.url('api/admin/data'));
        const years=examData.years.map(y=>`<option value="${y.id}">${esc(y.label)}</option>`).join('');
        examNode('academicYearId').innerHTML=years;examNode('academicYearId').value=examData.selectedYearId;
        examNode('examYearFilter').innerHTML='<option value="">All academic years</option>'+years;
        examNode('examYearFilter').value=AppUI.params.get('year')||'';examNode('statusFilter').value=AppUI.params.get('status')||'';
        examNode('roomChoices').innerHTML=examData.rooms.map(r=>`<label class="room-option"><input type="checkbox" name="roomId" value="${r.id}"><span>${esc(r.roomNumber)}<small>${r.capacity} seats${r.isAccessible?' · accessible entry':''}</small></span></label>`).join('');
        await loadCoursesForYear();await loadExams();if(AppUI.params.has('new'))await openEditor();
    }catch(error){examNotice(error.message);}
})();
