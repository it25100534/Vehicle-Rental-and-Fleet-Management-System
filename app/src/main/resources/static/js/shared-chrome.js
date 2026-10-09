(() => {
  'use strict';
  const menu=document.querySelector('.driveease-header .menu-toggle');
  const nav=document.querySelector('.driveease-header nav');
  if(menu&&nav){
    const close=()=>{menu.setAttribute('aria-expanded','false');nav.classList.remove('open');};
    menu.addEventListener('click',()=>{const open=menu.getAttribute('aria-expanded')!=='true';menu.setAttribute('aria-expanded',String(open));nav.classList.toggle('open',open);});
    document.addEventListener('keydown',event=>{if(event.key==='Escape')close();});
    nav.addEventListener('click',event=>{if(event.target.closest('a'))close();});
  }
  const year=document.getElementById('currentYear');if(year)year.textContent=new Date().getFullYear();
  const badge=document.getElementById('customerNotificationBadge');
  if(badge){
    const bell=badge.closest('a');
    const refresh=async()=>{
      try{
        const response=await fetch('/api/customer/notifications/unread-count',{credentials:'same-origin',cache:'no-store'});
        if(!response.ok)return;
        const count=Math.max(0,Number((await response.json()).count)||0);
        badge.hidden=count===0;
        badge.textContent=count>99?'99+':String(count);
        bell.setAttribute('aria-label',count?`Notifications, ${count} unread`:'Notifications');
      }catch(error){/* Keep the header usable if notifications are temporarily unavailable. */}
    };
    refresh();
    setInterval(()=>{if(!document.hidden)refresh();},15000);
    window.addEventListener('focus',refresh);
    document.addEventListener('visibilitychange',()=>{if(!document.hidden)refresh();});
  }
})();
