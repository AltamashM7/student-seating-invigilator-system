let timetable = [];
async function loadTimetable() {
    const data = await apiRequest('../api/timetable');
    timetable = data.exams;
    document.getElementById('timetableRows').innerHTML = timetable.map(exam => `<tr><td>${exam.examDate}</td><td>${exam.courseCode} - ${exam.courseName}</td><td>${exam.startTime.slice(0,5)} - ${exam.endTime.slice(0,5)}</td><td>${exam.rooms}</td><td><span class="badge">${exam.status}</span></td></tr>`).join('');

    const grouped = {};
    timetable.forEach(exam => (grouped[exam.examDate] ||= []).push(exam));
    document.getElementById('calendarView').innerHTML = Object.entries(grouped).map(([date, exams]) => `<article class="card"><h3>${date}</h3>${exams.map(exam => `<p><strong>${exam.courseCode}</strong><br>${exam.startTime.slice(0,5)} - ${exam.endTime.slice(0,5)}<br>${exam.rooms}</p>`).join('')}</article>`).join('');
}

document.getElementById('listButton').addEventListener('click', () => {
    document.getElementById('listView').hidden = false;
    document.getElementById('calendarView').hidden = true;
});
document.getElementById('calendarButton').addEventListener('click', () => {
    document.getElementById('listView').hidden = true;
    document.getElementById('calendarView').hidden = false;
});
loadTimetable();
