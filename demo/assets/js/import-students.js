const importNode=id=>document.getElementById(id);
requireRole('ADMIN');
importNode('file').addEventListener('change',async()=>{
    const file=importNode('file').files[0];importNode('preview').hidden=true;importNode('result').hidden=true;
    if(!file){importNode('fileSummary').textContent='Choose a file to preview its first rows.';return;}
    if(file.size>2*1024*1024){importNode('fileSummary').textContent='Choose a CSV under 2 MB.';return;}
    const lines=(await file.text()).replace(/^\uFEFF/,'').split(/\r?\n/).filter(line=>line.trim());
    const header=lines[0]?.split(',')||[];
    importNode('fileSummary').textContent=`${file.name} · ${Math.max(0,lines.length-1)} non-empty data rows · ${header.length} columns. Preview shows the first 6 rows.`;
    importNode('preview').innerHTML=`<table><thead><tr>${header.map(h=>`<th>${esc(h)}</th>`).join('')}</tr></thead><tbody>${lines.slice(1,7).map(line=>`<tr>${line.split(',').map(cell=>`<td>${esc(cell)}</td>`).join('')}</tr>`).join('')}</tbody></table>`;
    importNode('preview').hidden=false;
});
importNode('importForm').addEventListener('submit',event=>{
    event.preventDefault();AppUI.busy(importNode('importButton'),async()=>{
        const result=importNode('result');const body=new FormData();body.append('file',importNode('file').files[0]);
        try{
            const data=await apiRequest(AppUI.url('api/admin/import/students'),{method:'POST',body});result.hidden=false;
            result.innerHTML=`<h2>Import complete</h2><section class="cards">${[['New students',data.imported],['Additional enrollments',data.enrolled],['Duplicates',data.duplicates],['Invalid rows',data.invalid]].map(([label,count])=>`<article class="card"><p>${label}</p><h2>${count}</h2></article>`).join('')}</section>${data.errors.length?`<div class="error-box"><h3>Rows to review</h3><ul>${data.errors.map(error=>`<li>${esc(error)}</li>`).join('')}</ul></div>`:'<div class="success-box">No row errors. Existing matching enrollments were kept unchanged.</div>'}<div class="quick-links"><a href="students.html">Review students</a><a href="enrollments.html">Review enrollments</a></div>`;
            result.scrollIntoView({behavior:'smooth',block:'start'});
        }catch(error){result.hidden=false;result.innerHTML=`<div class="error-box">${esc(error.message)}</div>`;}
    },'Importing students…');
});
