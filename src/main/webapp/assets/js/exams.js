requireRole('ADMIN');
let exams = [];
let courses = [];
let rooms = [];

async function loadOptions() {
    const [courseData, roomData] = await Promise.all([
        apiRequest('../../api/admin/courses'),
        apiRequest('../../api/admin/rooms')
    ]);
    courses = courseData.courses;
    rooms = roomData.rooms;
    document.getElementById('courseId').innerHTML = courses.map(c => `<option value="${c.id}">${c.code} - ${c.name}</option>`).join('');
    document.getElementById('roomIds').innerHTML = rooms.map(r => `<option value="${r.id}">${r.roomNumber} (${r.capacity})</option>`).join('');
}

async function loadExams() {
    const data = await apiRequest('../../api/admin/exams');
    exams = data.exams;
    document.getElementById('examRows').innerHTML = exams.map(exam => `<tr><td>${exam.courseCode}</td><td>${exam.examDate}</td><td>${exam.startTime.slice(0,5)} - ${exam.endTime.slice(0,5)}</td><td>${exam.rooms}</td><td><span class="badge">${exam.status}</span></td><td>${actions(exam)}</td></tr>`).join('');
}

function actions(exam) {
    if (exam.status === 'DRAFT') return `<button class="secondary" onclick="editExam(${exam.id})">Edit</button> <button onclick="changeStatus(${exam.id}, 'SCHEDULED')">Schedule</button> <button class="danger" onclick="deleteExam(${exam.id})">Delete</button>`;
    if (exam.status === 'SCHEDULED') return `<button onclick="changeStatus(${exam.id}, 'COMPLETED')">Complete</button> <button class="danger" onclick="changeStatus(${exam.id}, 'CANCELLED')">Cancel</button>`;
    if (exam.status === 'CANCELLED') return `<button class="danger" onclick="deleteExam(${exam.id})">Delete</button>`;
    return '-';
}

function editExam(id) {
    const exam = exams.find(e => e.id === id);
    document.getElementById('examId').value = exam.id;
    document.getElementById('courseId').value = exam.courseId;
    document.getElementById('examDate').value = exam.examDate;
    document.getElementById('startTime').value = exam.startTime.slice(0,5);
    document.getElementById('endTime').value = exam.endTime.slice(0,5);
    document.getElementById('status').value = exam.status;
    const selectedNames = exam.rooms.split(',').map(v => v.trim());
    [...document.getElementById('roomIds').options].forEach(option => {
        const room = rooms.find(r => String(r.id) === option.value);
        option.selected = room && selectedNames.includes(room.roomNumber);
    });
}
function resetExamForm() { document.getElementById('examForm').reset(); document.getElementById('examId').value = ''; document.getElementById('conflicts').innerHTML = ''; }

async function changeStatus(id, status) {
    try { await apiRequest('../../api/admin/exams', { method: 'POST', body: formBody({ action: 'status', id, status }) }); await loadExams(); }
    catch (error) { alert(error.message); }
}
async function deleteExam(id) {
    if (!confirm('Delete this exam?')) return;
    try { await apiRequest('../../api/admin/exams', { method: 'POST', body: formBody({ action: 'delete', id }) }); await loadExams(); }
    catch (error) { alert(error.message); }
}

document.getElementById('examForm').addEventListener('submit', async (event) => {
    event.preventDefault();
    const id = document.getElementById('examId').value;
    const body = formBody({ action: id ? 'update' : 'create', id, courseId: document.getElementById('courseId').value, examDate: document.getElementById('examDate').value, startTime: document.getElementById('startTime').value, endTime: document.getElementById('endTime').value, status: document.getElementById('status').value });
    [...document.getElementById('roomIds').selectedOptions].forEach(option => body.append('roomId', option.value));
    const conflictBox = document.getElementById('conflicts');
    conflictBox.innerHTML = '';
    try {
        await apiRequest('../../api/admin/exams', { method: 'POST', body });
        resetExamForm(); await loadExams();
    } catch (error) {
        if (error.status === 409 && error.data.conflicts) {
            conflictBox.innerHTML = `<div class="error-box"><strong>Cannot save:</strong><ul>${error.data.conflicts.map(c => `<li>${c}</li>`).join('')}</ul></div>`;
        } else conflictBox.innerHTML = `<div class="error-box">${error.message}</div>`;
    }
});
(async () => { await loadOptions(); await loadExams(); })();
