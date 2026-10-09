(() => {
    'use strict';
    const reducedMotion = matchMedia('(prefers-reduced-motion: reduce)');
    const asset = '/images/home-redesign/';
    const escape = value => String(value ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
    const imagePath = value => {
        const path = String(value || '');
        if (/^\/(images|branch-images)\/[\w./-]+$/.test(path) && !path.includes('..')) return path;
        if (/^[\w.-]+$/.test(path)) return '/images/' + path;
        return '/images/vehicle-placeholder.svg';
    };
    const arrow = '<svg aria-hidden="true"><use href="#i-arrow"/></svg>';
    const currency = value => 'Rs. ' + Number(value).toLocaleString('en-LK', {maximumFractionDigits:0});
    const menu = document.querySelector('.menu-toggle');
    const nav = document.getElementById('homeNavigation');
    // The shared chrome script manages navigation on every application page.
    document.getElementById('currentYear').textContent = new Date().getFullYear();
    // Optional replacement files never replace a usable supplied fallback until they load.
    fetch('/api/home/media').then(response => response.ok ? response.json() : []).then(files => {
        for (const element of document.querySelectorAll('[data-optional-image]')) {
            if (files.includes(element.dataset.optionalImage)) element.src = asset + element.dataset.optionalImage;
        }
        if (files.includes('founder-arosha-kamalhewa.webp')) {
            const founder = new Image(); founder.alt = '';
            founder.onload = () => document.getElementById('founderAvatar').replaceChildren(founder);
            founder.src = asset + 'founder-arosha-kamalhewa.webp';
        }
    }).catch(() => {});

    let motionPaused = reducedMotion.matches;
    const videoButton = document.querySelector('.video-toggle');
    function syncMotion() {
        document.body.classList.toggle('motion-paused', motionPaused);
        for (const video of document.querySelectorAll('.ambient-video')) {
            if (motionPaused) video.pause();
            else video.play().catch(() => {});
        }
        if (videoButton) {
            videoButton.textContent = motionPaused ? 'Play motion' : 'Pause motion';
            videoButton.setAttribute('aria-pressed', String(motionPaused));
        }
    }
    videoButton?.addEventListener('click', () => { motionPaused = !motionPaused; syncMotion(); });
    reducedMotion.addEventListener('change', () => { motionPaused = reducedMotion.matches; syncMotion(); updateFans(); });
    syncMotion();
    const fans = [...document.querySelectorAll('[data-scroll-fan]')];
    let scrollScheduled = false;
    function updateFans() {
        scrollScheduled = false;
        for (const fan of fans) {
            const top = fan.getBoundingClientRect().top;
            const progress = reducedMotion.matches ? 1 : Math.max(0, Math.min(1, (innerHeight * .84 - top) / (innerHeight * .58)));
            fan.style.setProperty('--spread', progress.toFixed(3));
        }
        for (const collage of document.querySelectorAll('[data-scroll-collage]')) {
            const rect = collage.getBoundingClientRect();
            const progress = reducedMotion.matches ? 1 : Math.max(0, Math.min(1, (innerHeight * .9 - rect.top) / (rect.height * .55)));
            collage.style.setProperty('--spread', progress.toFixed(3));
        }
    }
    function scheduleFans() { if (!scrollScheduled) { scrollScheduled = true; requestAnimationFrame(updateFans); } }
    addEventListener('scroll', scheduleFans, {passive:true});
    addEventListener('resize', scheduleFans);
    updateFans();
    const journeyImage = document.getElementById('journeyImage');
    const journeyOverlay = journeyImage.cloneNode(false);
    journeyOverlay.removeAttribute('id');
    journeyOverlay.className = 'journey-transition';
    journeyOverlay.alt = '';
    journeyOverlay.setAttribute('aria-hidden','true');
    journeyImage.after(journeyOverlay);
    let journeyGeneration = 0;
    function changeJourney(path, alt) {
        const generation = ++journeyGeneration;
        const loaded = new Image();
        loaded.onload = () => {
            if (generation !== journeyGeneration) return;
            journeyOverlay.src = journeyImage.src;
            journeyOverlay.style.opacity = '1';
            journeyImage.src = path;
            journeyImage.alt = alt;
            requestAnimationFrame(() => requestAnimationFrame(() => { journeyOverlay.style.opacity = '0'; }));
        };
        loaded.src = path;
    }
    for (const link of document.querySelectorAll('[data-use]')) {
        const update = () => {
                const use = link.dataset.use.toLowerCase();
            changeJourney(use === 'daily' ? '/images/CAR-0001_toyota-corolla-2020.png' : '/images/home-journey-' + use + '.png', link.firstChild.textContent.trim() + ' journey vehicles');
        };
        link.addEventListener('mouseenter', update);
        link.addEventListener('focus', update);
    }
    document.querySelector('.category-list').addEventListener('mouseleave', () => {
        changeJourney('/images/home-journey-default.png', 'DriveEase rental fleet');
    });
    const cards = [...document.querySelectorAll('.step-card')];
    const deck = document.getElementById('stepDeck');
    let step = 0;
    function showStep(next) {
        step = Math.max(0, Math.min(cards.length - 1, next));
        cards.forEach((card, index) => {
            card.classList.toggle('past', index < step);
            card.style.setProperty('--position', index - step);
            card.setAttribute('aria-hidden', String(index !== step));
            card.inert = index !== step;
        });
        document.getElementById('stepStatus').textContent = `Step ${step + 1} of ${cards.length}`;
        document.getElementById('previousStep').disabled = step === 0;
        document.getElementById('nextStep').disabled = step === cards.length - 1;
    }
    document.getElementById('nextStep').addEventListener('click', () => showStep(step + 1));
    document.getElementById('previousStep').addEventListener('click', () => showStep(step - 1));
    deck.addEventListener('click', event => { if (!event.target.closest('a')) showStep(step === cards.length - 1 ? 0 : step + 1); });
    deck.addEventListener('keydown', event => {
        if (['ArrowRight','ArrowLeft','Enter',' '].includes(event.key) && !event.target.closest('a')) {
            event.preventDefault(); showStep(step + (event.key === 'ArrowLeft' ? -1 : 1));
        }
    });
    let touchX;
    deck.addEventListener('touchstart', event => { touchX = event.changedTouches[0].clientX; }, {passive:true});
    deck.addEventListener('touchend', event => {
        const delta = event.changedTouches[0].clientX - touchX;
        if (Math.abs(delta) > 45) showStep(step + (delta < 0 ? 1 : -1));
    }, {passive:true});
    showStep(0);

    const storyWindow = document.getElementById('storiesWindow');
    const storyTrack = document.getElementById('storiesTrack');
    const originals = [...storyTrack.children];
    originals.forEach(card => { const clone = card.cloneNode(true); clone.setAttribute('aria-hidden','true'); clone.inert = true; storyTrack.append(clone); });
    let storyOffset = 0, storyPaused = false, storyHover = false, lastFrame = 0;
    function storyFrame(time) {
        const elapsed = Math.min((time - lastFrame) / 1000, .05); lastFrame = time;
        const period = (storyTrack.scrollWidth + 24) / 2;
        if (!motionPaused && !storyPaused && !storyHover && !document.hidden) storyOffset += elapsed * 28;
        storyOffset = ((storyOffset % period) + period) % period;
        storyTrack.style.transform = `translateX(${-storyOffset}px)`;
        requestAnimationFrame(storyFrame);
    }
    requestAnimationFrame(storyFrame);
    storyWindow.addEventListener('mouseenter', () => { storyHover = true; });
    storyWindow.addEventListener('mouseleave', () => { storyHover = false; });
    storyWindow.addEventListener('focusin', () => { storyHover = true; });
    storyWindow.addEventListener('focusout', () => { storyHover = false; });
    const pauseStories = document.getElementById('pauseStories');
    pauseStories?.addEventListener('click', () => {
        storyPaused = !storyPaused; pauseStories.textContent = storyPaused ? 'Play' : 'Pause';
        pauseStories.setAttribute('aria-pressed', String(storyPaused));
    });
    const shiftStory = direction => { storyOffset += direction * (originals[0].getBoundingClientRect().width + 24); };
    document.getElementById('previousStory')?.addEventListener('click', () => shiftStory(-1));
    document.getElementById('nextStory')?.addEventListener('click', () => shiftStory(1));
    storyWindow.addEventListener('keydown', event => { if (event.key === 'ArrowRight' || event.key === 'ArrowLeft') { event.preventDefault(); shiftStory(event.key === 'ArrowRight' ? 1 : -1); } });

    let statsSeen = false, totals, animationGeneration = 0;
    const statsStatus = document.getElementById('statsStatus');
    function renderStats() {
        if (!statsSeen || !totals) return;
        const satisfaction = document.querySelector('.satisfaction-value');
        if (satisfaction) {
            const reviewCount = Number(totals.reviewCount || 0);
            satisfaction.textContent = reviewCount ? `${Number(totals.satisfactionPercent)}%` : '—';
            satisfaction.setAttribute('aria-label', reviewCount
                ? `${Number(totals.satisfactionPercent)} percent from ${reviewCount} star ratings`
                : 'No reviews yet');
            const label = document.getElementById('satisfactionLabel');
            if (label) label.textContent = Number(totals.demoReviewCount || 0)
                ? 'Customer satisfaction · demo reviews included'
                : 'Customer satisfaction rate';
        }
        const generation = ++animationGeneration;
        for (const element of document.querySelectorAll('[data-stat]')) {
            const target = Number(totals[element.dataset.stat]);
            const start = performance.now();
            const from = Number(element.dataset.value || 0);
            element.dataset.value = target;
            function frame(time) {
                if (generation !== animationGeneration) return;
                const progress = motionPaused ? 1 : Math.min(1, (time - start) / 1100);
                element.textContent = Math.round(from + (target - from) * (1 - Math.pow(1 - progress, 3))).toLocaleString('en-LK');
                if (progress < 1) requestAnimationFrame(frame);
            }
            requestAnimationFrame(frame);
        }
    }
    new IntersectionObserver((entries, observer) => {
        if (entries.some(entry => entry.isIntersecting)) { statsSeen = true; renderStats(); observer.disconnect(); }
    }, {threshold:.2}).observe(document.getElementById('liveStats'));
    async function getJson(url) {
        const response = await fetch(url, {cache:'no-store'});
        if (!response.ok) throw new Error('Data unavailable');
        return response.json();
    }
    async function refreshStats() {
        try {
            totals = await getJson('/api/home/stats'); renderStats();
            statsStatus.textContent = 'Live database totals · Updated ' + new Date().toLocaleTimeString([], {hour:'2-digit',minute:'2-digit'});
        } catch (_) {
            totals = null; ++animationGeneration;
            document.querySelectorAll('[data-stat]').forEach(element => { element.textContent = '—'; });
            statsStatus.textContent = 'Current totals are unavailable. Please try again shortly.';
        }
    }
    const logoNames = {'toyota':'toyota','honda':'honda','suzuki':'suzuki','mazda':'mazda','bmw':'bmw','audi':'audi','mercedes-benz':'mercedes-benz','mercedes benz':'mercedes-benz','hyundai':'hyundai','jaguar':'jaguar','aston martin':'aston-martin','mini':'mini'};
    let branchRows = [];
    let branchSignature = '';
    async function refreshBranches() {
        const grid = document.getElementById('branchGrid');
        try {
            branchRows = await getJson('/api/branches');
            branchSignature = JSON.stringify(branchRows.map(row => [row.branchId,row.name,row.open]));
            const branchPhotos={'BR-001':asset+'branch-colombo.jpg','BR-002':asset+'branch-kandy.jpg','BR-003':asset+'branch-galle.jpg'};
            grid.innerHTML = branchRows.length ? branchRows.map(branch => `<article class="branch-card"><img src="${escape(imagePath(branch.imagePath || branchPhotos[branch.branchId] || asset+'branch-road.avif'))}" data-branch-fallback="${escape(branchPhotos[branch.branchId] || asset+'branch-road.avif')}" alt="${escape(branch.name)} branch" loading="lazy"><div><h3>${escape(branch.name)}</h3><p>${escape(branch.address)}</p><a class="branch-phone" href="tel:${escape(String(branch.phone).replace(/[^+\d]/g,''))}">${escape(branch.phone)}</a><a class="text-link" href="/catalog?mode=BRANCH&amp;branch=${encodeURIComponent(branch.branchId)}">Explore this branch ${arrow}</a></div></article>`).join('') : '<p class="data-state">No branches are currently open. Please contact us for assistance.</p>';
            grid.querySelectorAll('img').forEach(img=>img.addEventListener('error',()=>{if(img.dataset.branchFallback){const fallback=img.dataset.branchFallback;delete img.dataset.branchFallback;img.src=fallback;}},{once:true}));
            grid.querySelectorAll('img').forEach(image => image.addEventListener('error', () => { image.src = '/images/vehicle-placeholder.svg'; }, {once:true}));
        } catch (_) { grid.innerHTML = '<p class="data-state">Branch information is unavailable. <a href="/contact">Contact us for help →</a></p>'; }
    }
    async function refreshFleet() {
        const grid = document.getElementById('featuredFleet');
        try {
            const fleet = await getJson('/api/vehicles/home-featured');
            const picks = fleet.vehicles;
            grid.innerHTML = picks.length ? picks.map(vehicle => {
                const branch = branchRows.find(row => row.branchId === vehicle.currentBranch);
                const title = escape(vehicle.make + ' ' + vehicle.model);
                const seats = Number(vehicle.numberOfSeats);
                const seatIcon = '<svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="9" cy="4" r="2"/><path d="M6 8v7h9l3 6M9 8v5h7M3 13v7h11"/></svg>';
                const fuelIcon = '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4 21V4h10v17M2 21h14M6 7h6v4H6zM14 8h3l3 4v6a2 2 0 0 1-4 0v-4M17 6l3 3"/></svg>';
                return `<article class="vehicle-card"><a class="vehicle-photo" href="/bookVehicle?id=${encodeURIComponent(vehicle.vehicleId)}" aria-label="View ${title}"><img src="${escape(imagePath(vehicle.vehicleImageFileName))}" alt="${title}" loading="lazy"></a><div class="vehicle-info"><div class="vehicle-name-row"><h3>${title}</h3><a class="vehicle-view" href="/bookVehicle?id=${encodeURIComponent(vehicle.vehicleId)}" aria-label="Book ${title}">${arrow}</a></div><div class="vehicle-tags"><span>${escape(vehicle.usageCategory)}</span><span>·</span><span>${escape(branch?.name || vehicle.currentBranch)}</span></div><div class="vehicle-bottom"><div class="vehicle-specs">${seats > 0 ? `<span>${seatIcon}${seats}</span>` : ''}<span>${fuelIcon}${escape(vehicle.fuelType)}</span><span>${escape(vehicle.type === 'MOTORCYCLE' ? 'Bike' : vehicle.type)}</span></div><div><strong>${escape(currency(vehicle.rentalRate))}</strong><small> / day</small></div></div></div></article>`;
            }).join('') : '<p class="data-state">No featured vehicles are available right now. <a href="/catalog">Explore the fleet →</a></p>';
            grid.querySelectorAll('img').forEach(image => image.addEventListener('error', () => { image.src = '/images/vehicle-placeholder.svg'; }, {once:true}));
            document.querySelectorAll('[data-category-count]').forEach(element => {
                const count = fleet.categoryCounts[element.dataset.categoryCount] || 0;
                element.textContent = count + ' vehicles';
            });
            const brands = fleet.brands.filter(brand => logoNames[brand.toLowerCase()]);
            const logoHtml = brands.map(brand => `<span class="brand-item">${logoNames[brand.toLowerCase()] ? `<img src="${asset}brand-${logoNames[brand.toLowerCase()]}.svg" alt="${escape(brand)}" loading="lazy">` : escape(brand)}</span>`).join('');
            document.getElementById('brandTrack').innerHTML = `<div class="brand-set">${logoHtml}</div><div class="brand-set" aria-hidden="true">${logoHtml}</div>`;
        } catch (_) { grid.innerHTML = '<p class="data-state">The fleet could not be loaded. <a href="/catalog">Open the catalog →</a></p>'; }
    }
    async function refreshData() { await Promise.allSettled([refreshStats(), refreshBranches()]); await refreshFleet(); }
    async function checkBranches() {
        if (document.hidden) return;
        try {
            const rows = await getJson('/api/branches');
            const signature = JSON.stringify(rows.map(row => [row.branchId,row.name,row.open]));
            if (signature !== branchSignature) await refreshData();
        } catch (_) { /* The next poll or focus will retry. */ }
    }
    refreshData();
    setInterval(() => { if (!document.hidden) refreshData(); }, 60000);
    setInterval(checkBranches, 10000);
    document.addEventListener('visibilitychange', () => { if (!document.hidden) refreshData(); });
})();
