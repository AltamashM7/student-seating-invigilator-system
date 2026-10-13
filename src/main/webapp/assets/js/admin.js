const fieldSets = {
    years: [['startYear','Starting year','number',2000,2100],['isCurrent','Current academic year','checkbox']],
    departments: [['code','Department code'],['name','Department name']],
    divisions: [['departmentId','Department','departments'],['studyYear','Year of study','studyYears'],['code','Division']],
    courses: [['code','Course code'],['name','Course name'],['departmentId','Department','departments'],['studyYear','Year of study','studyYears'],['semester','Semester','number',1,8]],
    rooms: [['roomNumber','Room number'],['capacity','Capacity','number',1,500],['seatColumns','Columns per row','number',1,30],['isAccessible','Accessible room entry','checkbox']],
    students: [['rollNumber','Roll number'],['name','Student name'],['email','Email (optional)','email'],['divisionId','Department / year / division','divisions'],['semester','Semester','number',1,8],['requiresAccessible','Accessible seating required','checkbox']],
    faculty: [['employeeCode','Employee code'],['name','Faculty name'],['email','Email','email'],['departmentId','Department','departments'],['username','Login username'],['password','Password (blank keeps current password)','password']],
    enrollments: [['studentId','Student','students'],['courseId','Course','courses']]
};
const resource = document.body.dataset.resource;
const singular = {years:'academic year',departments:'department',divisions:'division',courses:'course',rooms:'room',students:'student',faculty:'faculty member',enrollments:'enrollment'};
let snapshot, academicYearId = Number(AppUI.params.get('academicYearId')) || 0, editingId = 0, listView, clearDirty;
const node = id => document.getElementById(id);
const notice = (text, ok=false) => AppUI.message(node('notice'),text,ok);
function optionsFor(type) {
    if (type === 'studyYears') return [1,2,3,4].map(id=>({id,label:AppUI.studyYear(id)}));
    if (type === 'departments') return snapshot.departments.map(d=>({id:d.id,label:`${d.code} — ${d.name}`}));
    if (type === 'divisions') return snapshot.divisions.filter(d=>d.academicYearId===academicYearId).map(d=>({id:d.id,label:`${d.departmentCode} / ${AppUI.studyYear(d.studyYear)} / ${d.code}`}));
    if (type === 'students') return snapshot.students.filter(s=>s.divisionId).map(s=>({id:s.id,label:`${s.rollNumber} — ${s.name}`}));
    if (type === 'courses') return snapshot.courses.filter(c=>c.offered).map(c=>({id:c.id,label:`${c.code} — ${c.name}`}));
    return [];
}
function labelValue(row, key, type) {
    if (type==='checkbox') return row[key] ? 'Yes' : 'No';
    if (['departments','divisions','students','courses','studyYears'].includes(type)) return optionsFor(type).find(o=>o.id===row[key])?.label || 'Not placed this year';
    return row[key] ?? '';
}
async function refresh() {
    snapshot = await apiRequest(AppUI.url(`api/admin/data${academicYearId?`?academicYearId=${academicYearId}`:''}`));
    academicYearId = snapshot.selectedYearId;
    node('academicYear').innerHTML = snapshot.years.map(y=>`<option value="${y.id}">${esc(y.label)}${y.isCurrent?' (current)':''}</option>`).join('');
    node('academicYear').value = academicYearId;
    if (resource==='dashboard') { renderDashboard(); return; }
    if (node('departmentFilter')) {
        const selected=node('departmentFilter').value;
        node('departmentFilter').innerHTML='<option value="">All departments</option>'+snapshot.departments.map(d=>`<option value="${d.id}">${esc(d.code)}</option>`).join('');
        node('departmentFilter').value=selected;
    }
    renderFields(); drawRecords();
    if (resource==='rooms') renderAccessibility();
}
function renderDashboard() {
    node('summary').innerHTML = [['Students',snapshot.students.filter(s=>s.divisionId).length],['Departments',snapshot.departments.length],['Divisions',snapshot.counts.length],['Course offerings',snapshot.courses.filter(c=>c.offered).length],['Rooms',snapshot.rooms.length]].map(([title,count])=>`<article class="card"><p>${title}</p><h2>${count}</h2></article>`).join('');
    const query=node('dashboardSearch').value.trim().toLowerCase();
    const years=snapshot.yearCounts.filter(r=>`${r.department} ${AppUI.studyYear(r.studyYear)}`.toLowerCase().includes(query));
    const divisions=snapshot.counts.filter(r=>`${r.department} ${AppUI.studyYear(r.studyYear)} division ${r.division}`.toLowerCase().includes(query));
    node('yearCounts').innerHTML=years.map(r=>`<tr><td>${esc(r.department)} / ${AppUI.studyYear(r.studyYear)}</td><td>${r.studentCount}</td></tr>`).join('')||'<tr><td colspan="2">No matching department-year groups.</td></tr>';
    node('divisionCounts').innerHTML=divisions.map(r=>`<tr><td>${esc(r.department)}</td><td>${AppUI.studyYear(r.studyYear)}</td><td>${esc(r.division)}</td><td>${r.studentCount}</td></tr>`).join('')||'<tr><td colspan="4">No matching divisions.</td></tr>';
    node('countSummary').textContent=`${years.length} department-year groups · ${divisions.length} divisions`;
}
function renderFields() {
    editingId=0;
    node('fields').innerHTML=fieldSets[resource].map(([key,label,type,min,max])=>{
        const required=(key==='email'&&resource==='students')||key==='password'?'':'required';
        if (type==='checkbox') return `<label class="check-label"><input id="${key}" name="${key}" type="checkbox">${label}</label>`;
        if (['departments','studyYears','divisions','students','courses'].includes(type)) return `<label>${label}<select id="${key}" name="${key}" aria-label="${label}" required>${optionsFor(type).map(o=>`<option value="${o.id}">${esc(o.label)}</option>`).join('')}</select></label>`;
        return `<label>${label}<input id="${key}" name="${key}" type="${type||'text'}" ${required} ${type==='number'?`min="${min}" max="${max}"`:''} maxlength="${key==='name'?100:120}" autocomplete="${type==='password'?'new-password':'off'}" ${['code','rollNumber','employeeCode','username','email'].includes(key)?'spellcheck="false"':''}></label>`;
    }).join('');
    if (resource==='rooms') {node('seatColumns').value=6;node('isAccessible').checked=true;}
    if (resource==='courses') {
        node('semester').value=1;
        node('studyYear').addEventListener('change',()=>node('semester').value=Number(node('studyYear').value)*2-1);
    }
    if (resource==='students') {
        const selectSemester=()=>{const division=snapshot.divisions.find(d=>d.id===Number(node('divisionId').value));if(division)node('semester').value=division.studyYear*2-1;};
        node('divisionId').addEventListener('change',selectSemester);selectSemester();
    }
    for (const [key,label,type] of fieldSets[resource]) if (['divisions','students','courses'].includes(type)) AppUI.searchableSelect(node(key),label);
    node('saveButton').textContent=`Save ${singular[resource]}`;
    node('editorTitle').textContent=`Add ${singular[resource]}`;
    node('editorHelp').textContent=resource==='courses'?`The course will be offered in ${snapshot.years.find(y=>y.id===academicYearId)?.label}. Semesters must match the year of study.`:resource==='students'?'Placement applies to the selected academic year. Record only the accessible seating requirement. To change it after allocation, use Accommodation requirements on the Seating page.':resource==='faculty'?'Set an initial password for new accounts. Leave it blank when editing to keep the existing password.':'Referenced records may be protected from structural changes or deletion.';
    node('tableHead').innerHTML=fieldSets[resource].filter(f=>f[0]!=='password').map(f=>`<th scope="col">${esc(f[1])}</th>`).join('')+(resource==='courses'?'<th scope="col">Offered this year</th>':'')+'<th scope="col">Actions</th>';
    clearDirty?.();
}
function drawRecords() {
    if (!snapshot) return;
    const fields=fieldSets[resource];
    let rows=resource==='divisions'?snapshot.divisions.filter(d=>d.academicYearId===academicYearId):snapshot[resource];
    const department=node('departmentFilter')?.value,study=node('studyFilter')?.value;
    rows=rows.filter(r=>(!department||r.departmentId===Number(department))&&(!study||r.studyYear===Number(study)));
    const key=node('sortBy').value;
    rows=[...rows].sort((a,b)=>key==='studyYear'?a.studyYear-b.studyYear:String(labelValue(a,key,fields.find(f=>f[0]===key)?.[2])).localeCompare(String(labelValue(b,key,fields.find(f=>f[0]===key)?.[2])),undefined,{numeric:true}));
    listView.set(rows.map(value=>({value,text:fields.map(([key,,type])=>labelValue(value,key,type)).join(' ')})));
}
function renderRows(rows) {
    node('tableRows').innerHTML=rows.map(row=>`<tr>${fieldSets[resource].filter(f=>f[0]!=='password').map(([key,,type])=>`<td>${esc(labelValue(row,key,type))}</td>`).join('')}${resource==='courses'?`<td>${row.offered?AppUI.badge('Offered'):'Not offered'}</td>`:''}<td><details class="row-actions"><summary>Actions</summary><div class="action-links"><button type="button" class="secondary" data-edit="${row.id}">Edit</button><button type="button" class="danger" data-delete="${row.id}">Delete</button>${resource==='courses'?`<button type="button" class="secondary" data-offer="${row.id}" data-action="${row.offered?'unoffer':'offer'}">${row.offered?'Remove offering':'Offer this year'}</button>`:''}</div></details></td></tr>`).join('')||`<tr><td colspan="${fieldSets[resource].length+2}"><div class="empty-state">No matching records. Change the filters or add a ${singular[resource]}.</div></td></tr>`;
}
function editRecord(id) {
    const row=snapshot[resource].find(r=>r.id===id);renderFields();editingId=id;
    for (const [key,,type] of fieldSets[resource]) {const input=node(key);if(type==='checkbox')input.checked=!!row[key];else input.value=key==='password'?'':row[key]??'';}
    node('editorTitle').textContent=`Edit ${singular[resource]}`;node('editor').open=true;
    node('editor').scrollIntoView({behavior:'smooth',block:'start'});node(fieldSets[resource][0][0]).focus();notice('');
}
async function saveData(values) {return apiRequest(AppUI.url('api/admin/data'),{method:'POST',body:formBody({resource,academicYearId,...values})});}
function renderAccessibility() {
    const selected=node('accessibilityRoom').value;
    node('accessibilityRoom').innerHTML=snapshot.rooms.map(r=>`<option value="${r.id}">${esc(r.roomNumber)}</option>`).join('');
    if(selected)node('accessibilityRoom').value=selected;
    updateSeatOptions();
}
function updateSeatOptions() {
    const room=Number(node('accessibilityRoom').value);
    node('seatId').innerHTML=snapshot.seats.filter(s=>s.roomId===room).map(s=>`<option value="${s.id}">${esc(s.seatNumber)}${s.isAccessible?' (accessible)':''}</option>`).join('');updateSeatFlag();
}
function updateSeatFlag() {node('seatAccessible').checked=!!snapshot.seats.find(s=>s.id===Number(node('seatId').value))?.isAccessible;}
(async()=>{
    if (!await requireRole('ADMIN')) return;
    node('academicYear').addEventListener('change',async e=>{academicYearId=Number(e.target.value);AppUI.remember({academicYearId});clearDirty?.();try{await refresh();notice('');}catch(error){notice(error.message);}});
    if (resource==='dashboard') node('dashboardSearch').addEventListener('input',renderDashboard);
    else {
        clearDirty=AppUI.dirty(node('recordForm'));
        listView=AppUI.list({search:node('recordSearch'),size:node('pageSize'),previous:node('previousPage'),next:node('nextPage'),count:node('recordCount'),render:renderRows});
        node('sortBy').innerHTML=fieldSets[resource].filter(f=>!['password','isCurrent','requiresAccessible','isAccessible'].includes(f[0])).map(f=>`<option value="${f[0]}">${esc(f[1])}</option>`).join('');
        ['departmentFilter','studyFilter','sortBy'].forEach(id=>node(id)?.addEventListener('change',drawRecords));
        node('refreshButton').addEventListener('click',()=>AppUI.busy(node('refreshButton'),()=>refresh().catch(e=>notice(e.message)),'Refreshing…'));
        node('newRecord').addEventListener('click',()=>{renderFields();node('editor').open=true;node(fieldSets[resource][0][0]).focus();});
        node('clearButton').addEventListener('click',()=>{renderFields();node('editor').open=false;notice('');});
        node('recordForm').addEventListener('submit',e=>{e.preventDefault();AppUI.busy(node('saveButton'),async()=>{
            const values={};for(const [key,,type] of fieldSets[resource])values[key]=type==='checkbox'?node(key).checked:node(key).value;
            try{await saveData({...values,action:editingId?'update':'create',id:editingId||undefined});clearDirty();await refresh();node('editor').open=false;notice('Record saved successfully.',true);}catch(error){notice(error.message);}
        });});
        node('tableRows').addEventListener('click',async e=>{
            const button=e.target.closest('button');if(!button)return;
            if(button.dataset.edit){editRecord(Number(button.dataset.edit));return;}
            if(button.dataset.delete&&!confirm(`Delete this ${singular[resource]}? Referenced records are protected.`))return;
            AppUI.busy(button,async()=>{try{await saveData({id:button.dataset.delete||button.dataset.offer,action:button.dataset.delete?'delete':button.dataset.action});await refresh();notice(button.dataset.delete?'Record deleted.':'Offering updated.',true);}catch(error){notice(error.message);}});
        });
        if (resource==='rooms') {
            node('accessibilityRoom').addEventListener('change',updateSeatOptions);node('seatId').addEventListener('change',updateSeatFlag);
            node('accessibilityForm').addEventListener('submit',e=>{e.preventDefault();AppUI.busy(e.submitter,async()=>{try{await apiRequest(AppUI.url('api/admin/data'),{method:'POST',body:formBody({resource:'seats',action:'update',id:node('seatId').value,isAccessible:node('seatAccessible').checked})});await refresh();AppUI.message(node('seatNotice'),'Seat accessibility saved.',true);}catch(error){AppUI.message(node('seatNotice'),error.message);}});});
        }
    }
    try {await refresh();if(resource!=='dashboard'&&AppUI.params.has('new'))node('editor').open=true;} catch(e){notice(e.message);}
})();
