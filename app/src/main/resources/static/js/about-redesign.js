(() => {
  'use strict';
  const window=document.querySelector('.team-window');
  if(window){
    const reduced=matchMedia('(prefers-reduced-motion: reduce)');
    const track=window.querySelector('.team-track');
    const originals=[...track.children];
    originals.forEach(card=>{const copy=card.cloneNode(true);copy.setAttribute('aria-hidden','true');copy.inert=true;track.append(copy);});
    let paused=false,last=0,offset=0,visible=false;
    new IntersectionObserver(entries=>{visible=entries[0].isIntersecting;}).observe(window);
    window.addEventListener('focusin',()=>{paused=true;});
    window.addEventListener('focusout',()=>{paused=false;});
    window.addEventListener('touchstart',()=>{paused=true;},{passive:true});
    window.addEventListener('touchend',()=>{offset=window.scrollLeft;paused=false;},{passive:true});
    window.addEventListener('keydown',event=>{if(event.key==='ArrowRight'||event.key==='ArrowLeft'){event.preventDefault();window.scrollBy({left:(event.key==='ArrowRight'?1:-1)*300,behavior:reduced.matches?'instant':'smooth'});}});
    window.addEventListener('focusout',()=>{offset=window.scrollLeft;});
    function move(now){const elapsed=last?Math.min(now-last,50):0;last=now;if(visible&&!paused&&!reduced.matches&&!document.hidden){const distance=track.children[originals.length].offsetLeft-originals[0].offsetLeft;if(distance>0){offset=(offset+elapsed*.035)%distance;window.scrollLeft=offset;}}requestAnimationFrame(move);}
    requestAnimationFrame(move);
  }
})();
