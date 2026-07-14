// Dark mode toggle
function toggleDarkMode(){
  document.body.classList.toggle('dark');
  localStorage.setItem('ben-dark', document.body.classList.contains('dark'));
}
(function(){
  if(localStorage.getItem('ben-dark')==='true') document.body.classList.add('dark');
})();

// File preview helper
function previewFiles(input, containerId){
  const container = document.getElementById(containerId);
  container.innerHTML='';
  const files = input.files;
  for(let i=0;i<files.length;i++){
    const f=files[i];
    const reader=new FileReader();
    reader.onload=function(e){
      const img=document.createElement('img');
      img.src=e.target.result; img.alt=f.name;
      container.appendChild(img);
    };
    if(f.type.startsWith('image/')) reader.readAsDataURL(f);
    else{
      const div=document.createElement('div'); div.textContent=f.name; div.className='small-note'; container.appendChild(div);
    }
  }
}
