window.onload = async () => {
    await loadCatalogMetadata();
    await loadBranchFilters();
    setupFilterControls();
    await loadCatalog();
    setInterval(checkBranchChanges, 10000);
    document.addEventListener('visibilitychange', () => { if (!document.hidden) checkBranchChanges(); });
    window.addEventListener('focus', checkBranchChanges);
};

let allVehicles = [];
let catalogTotal = 0;
let catalogRequest = 0;
let catalogLoading = false;
let modalPreviewCount = 0;
let catalogFacets = {styles:[],transmissions:[],fuels:[],seats:[]};

let catalogBranches = [];
let selectedStartDate = "";
let selectedReturnDate = "";
let draftStartDate = "";
let draftReturnDate = "";
let modalPreviewRequest = 0;
let modalPreviewLoading = false;
let modalPreviewError = false;

const LOAD_MORE_STEP = 9;

function toDateInputValue(date) {
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
}

/*
    Metadata for customer-facing models. Physical copies inherit their
    model's category and presentation details.
*/
let vehicleMeta = {};

// Catalogue presentation details for the fleet models. The persisted
// vehicle record has no transmission field, so these values describe the listed
// rental vehicles rather than inferring a gearbox from a broad vehicle type.
let vehicleSpecs = {};

async function loadCatalogMetadata() {
    const response = await fetch('/data/catalog-metadata.json');
    if (!response.ok) throw new Error('Catalog metadata unavailable');
    const metadata = await response.json();
    vehicleMeta = metadata;
    vehicleSpecs = metadata;
}

const specIcons = {
    body: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M3 16h18l-1.5-5a2 2 0 0 0-1.9-1.4H6.4A2 2 0 0 0 4.5 11L3 16Z"/><path d="M5 16v2m14-2v2M7 9.6l1.3-3h7.4l1.3 3"/><circle cx="6.5" cy="16" r="1"/><circle cx="17.5" cy="16" r="1"/></svg>',
    transmission: '<svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="6" cy="5" r="1.5"/><circle cx="18" cy="5" r="1.5"/><circle cx="6" cy="19" r="1.5"/><circle cx="18" cy="19" r="1.5"/><path d="M6 6.5v11M18 6.5v11M6 12h12M12 12V5"/></svg>',
    seats: '<svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="9" cy="4" r="2"/><path d="M6 8v7h9l3 6M9 8v5h7M3 13v7h11"/></svg>',
    fuel: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4 21V4h10v17M2 21h14M6 7h6v4H6zM14 8h3l3 4v6a2 2 0 0 1-4 0v-4M17 6l3 3"/></svg>'
};

const useOptions = [['FAMILY','Family'],['BUSINESS','Business'],['WEDDING','Wedding'],['ADVENTURE','Adventure'],['TRANSPORT','Transport'],['DAILY','Daily use']];
const typeOptions = [['CAR','Cars'],['SUV','SUVs'],['VAN','Vans'],['MOTORCYCLE','Bikes'],['PREMIUM','Premium']];
let quickGroup = 'USE';
let searchQuery = '';
let appliedFilters = emptyFilterState();
let draftFilters = emptyFilterState();
let filterTrigger = null;

function emptyFilterState() {
    return {types:[],styles:[],uses:[],branches:[],transmissions:[],fuels:[],seats:[],minPrice:null,maxPrice:null,topOnly:false};
}
function copyFilterState(state) {
    return {types:[...state.types],styles:[...state.styles],uses:[...state.uses],branches:[...state.branches],transmissions:[...state.transmissions],fuels:[...state.fuels],seats:[...state.seats],minPrice:state.minPrice,maxPrice:state.maxPrice,topOnly:state.topOnly};
}
function setupFilterControls() {
    const searchInput = document.getElementById('catalogNameSearch');
    let searchTimer;
    searchInput.addEventListener('input', () => {
        clearTimeout(searchTimer);
        searchTimer = setTimeout(() => {
            searchQuery = searchInput.value.trim();
            saveCatalogState();
            loadCatalog();
        }, 250);
    });
    const start = document.getElementById('availabilityStart');
    const end = document.getElementById('availabilityReturn');
    const branch = document.getElementById('availabilityBranch');
    const status = document.getElementById('availabilityStatus');
    start.min = toDateInputValue(new Date());
    start.addEventListener('change', () => {
        draftStartDate = start.value;
        if (draftStartDate) {
            const next = new Date(draftStartDate + 'T12:00:00');
            next.setDate(next.getDate() + 1);
            end.min = toDateInputValue(next);
            if (end.value && end.value < end.min) end.value = '';
        } else end.min = '';
        draftReturnDate = end.value;
        refreshModalPreview();
    });
    end.addEventListener('change', () => {
        draftReturnDate = end.value;
        refreshModalPreview();
    });
    branch.addEventListener('change', () => {
        draftFilters.branches = branch.value ? [branch.value] : [];
        refreshModalPreview();
    });
    document.querySelectorAll('.catalog-switch-option').forEach(button => button.addEventListener('click', () => {
        if (quickGroup === button.dataset.group) return;
        quickGroup = button.dataset.group;
        if (quickGroup === 'USE') appliedFilters.types = [];
        else appliedFilters.uses = [];
        syncFilterUi();
        saveCatalogState();
        loadCatalog();
    }));
    document.getElementById('quickFilters').addEventListener('click', event => {
        const button = event.target.closest('[data-quick-value]');
        if (!button) return;
        const key = quickGroup === 'USE' ? 'uses' : 'types';
        const other = quickGroup === 'USE' ? 'types' : 'uses';
        appliedFilters[key] = button.dataset.quickValue ? [button.dataset.quickValue] : [];
        appliedFilters[other] = [];
        syncFilterUi();
        saveCatalogState();
        loadCatalog();
    });
    const modal = document.getElementById('catalogFilterModal');
    document.getElementById('openFilters').addEventListener('click', () => {
        filterTrigger = document.activeElement;
        draftFilters = copyFilterState(appliedFilters);
        draftStartDate = selectedStartDate;
        draftReturnDate = selectedReturnDate;
        start.value = draftStartDate;
        end.value = draftReturnDate;
        branch.value = draftFilters.branches.length === 1 ? draftFilters.branches[0] : '';
        if (draftStartDate) {
            const next = new Date(draftStartDate + 'T12:00:00');
            next.setDate(next.getDate() + 1);
            end.min = toDateInputValue(next);
        } else end.min = '';
        status.textContent = '';
        modalPreviewLoading = false;
        modalPreviewError = false;
        renderModalOptions();
        refreshModalPreview();
        modal.hidden = false;
        document.body.classList.add('catalog-modal-open');
        document.getElementById('closeFilters').focus();
    });
    document.getElementById('closeFilters').addEventListener('click', closeFilterModal);
    modal.addEventListener('click', event => { if (event.target === modal) closeFilterModal(); });
    document.addEventListener('keydown', event => {
        if (!modal.hidden && event.key === 'Escape') closeFilterModal();
        if (!modal.hidden && event.key === 'Tab') {
            const focusable = [...modal.querySelectorAll('button:not([disabled]),input:not([disabled])')].filter(el => el.offsetParent !== null);
            if (!focusable.length) return;
            if (event.shiftKey && document.activeElement === focusable[0]) {event.preventDefault();focusable.at(-1).focus();}
            else if (!event.shiftKey && document.activeElement === focusable.at(-1)) {event.preventDefault();focusable[0].focus();}
        }
    });
    modal.querySelector('.catalog-modal-content').addEventListener('click', event => {
        const button = event.target.closest('.catalog-modal-option');
        if (!button) return;
        const key = button.dataset.filter;
        const value = button.dataset.value;
        if (key === 'topOnly') draftFilters.topOnly = !draftFilters.topOnly;
        else {
            const choices = draftFilters[key];
            draftFilters[key] = choices.includes(value) ? choices.filter(item => item !== value) : [...choices,value];
        }
        renderModalOptions();
        refreshModalPreview();
    });
    for (const [id,key] of [['modalMinPrice','minPrice'],['modalMaxPrice','maxPrice']]) {
        document.getElementById(id).addEventListener('input', event => {
            draftFilters[key] = event.target.value === '' ? null : Math.max(0,Number(event.target.value));
            refreshModalPreview();
        });
    }
    document.getElementById('clearModalFilters').addEventListener('click', () => {
        draftFilters = emptyFilterState();
        draftStartDate = '';
        draftReturnDate = '';
        start.value = '';
        end.value = '';
        end.min = '';
        branch.value = '';
        refreshModalPreview();
        renderModalOptions();
    });
    document.getElementById('applyModalFilters').addEventListener('click', async () => {
        if (modalPreviewLoading || modalPreviewError || Boolean(draftStartDate) !== Boolean(draftReturnDate)) return;
        appliedFilters = copyFilterState(draftFilters);
        selectedStartDate = draftStartDate;
        selectedReturnDate = draftReturnDate;
        syncFilterUi();
        closeFilterModal();
        saveCatalogState();
        await loadCatalog();
    });
    document.getElementById('loadMoreBtn').addEventListener('click', () => loadCatalogPage(true));
    applyCatalogUrlFilters();
}
function closeFilterModal() {
    modalPreviewRequest++;
    document.getElementById('catalogFilterModal').hidden = true;
    document.body.classList.remove('catalog-modal-open');
    if (filterTrigger) filterTrigger.focus();
}
function applyCatalogUrlFilters() {
    const params = new URLSearchParams(window.location.search);
    if (params.has('state')) {
        try {
            const stored = JSON.parse(params.get('state'));
            const clean = emptyFilterState();
            for (const key of ['types','styles','uses','branches','transmissions','fuels','seats'])
                if (Array.isArray(stored.filters?.[key])) clean[key] = stored.filters[key].filter(value => typeof value === 'string');
            clean.topOnly = Boolean(stored.filters?.topOnly);
            clean.minPrice = Number.isFinite(stored.filters?.minPrice) ? stored.filters.minPrice : null;
            clean.maxPrice = Number.isFinite(stored.filters?.maxPrice) ? stored.filters.maxPrice : null;
            appliedFilters = clean;
            quickGroup = stored.group === 'TYPE' ? 'TYPE' : 'USE';
            selectedStartDate = stored.startDate || '';
            selectedReturnDate = stored.returnDate || '';
            searchQuery = typeof stored.search === 'string' ? stored.search.slice(0, 80) : '';
            document.getElementById('catalogNameSearch').value = searchQuery;
            syncFilterUi();
            return;
        } catch (error) { console.warn('Could not restore catalog filters', error); }
    }
    const mode = String(params.get('mode')||'').toUpperCase();
    const use = String(params.get('use')||'').toUpperCase();
    const type = String(params.get('type')||'').toUpperCase();
    const branch = String(params.get('branch')||'').toUpperCase();
    searchQuery = String(params.get('search')||'').slice(0, 80);
    document.getElementById('catalogNameSearch').value = searchQuery;
    if (mode === 'TOP') appliedFilters.topOnly = true;
    if (useOptions.some(([value]) => value === use)) { appliedFilters.uses = [use]; quickGroup = 'USE'; }
    if (typeOptions.some(([value]) => value === type)) { appliedFilters.types = [type]; quickGroup = 'TYPE'; }
    if (catalogBranches.some(item => item.branchId === branch)) appliedFilters.branches = [branch];
    syncFilterUi();
}

function saveCatalogState() {
    const params = new URLSearchParams();
    params.set('state', JSON.stringify({filters:appliedFilters,group:quickGroup,
        startDate:selectedStartDate,returnDate:selectedReturnDate,search:searchQuery}));
    history.replaceState(null, '', `${location.pathname}?${params}${location.hash}`);
}
function syncFilterUi() {
    document.querySelectorAll('.catalog-switch-option').forEach(button => {
        const selected = button.dataset.group === quickGroup;
        button.classList.toggle('active',selected);
        button.setAttribute('aria-pressed',String(selected));
    });
    renderQuickFilters();
    const count = ['types','styles','uses','branches','transmissions','fuels','seats'].reduce((total,key)=>total+appliedFilters[key].length,0)
        + Number(appliedFilters.topOnly) + Number(appliedFilters.minPrice !== null) + Number(appliedFilters.maxPrice !== null);
    const trigger = document.getElementById('openFilters');
    const active = count > 0 || Boolean(selectedStartDate && selectedReturnDate);
    trigger.classList.toggle('has-filters', active);
    trigger.setAttribute('aria-label', active ? 'Filters active' : 'Filters');
}
function renderQuickFilters() {
    const key = quickGroup === 'USE' ? 'uses' : 'types';
    const options = quickGroup === 'USE' ? useOptions : typeOptions;
    const list = [['','All'],...options];
    document.getElementById('quickFilters').innerHTML = list.map(([value,label]) => {
        const selected = value ? appliedFilters[key].length === 1 && appliedFilters[key][0] === value : appliedFilters[key].length === 0;
        return `<button class="catalog-quick-option ${selected?'active':''}" type="button" data-quick-value="${value}" aria-pressed="${selected}">${label}</button>`;
    }).join('');
}
function modalOption(key,value,label) {
    const selected = key === 'topOnly' ? draftFilters.topOnly : draftFilters[key].includes(value);
    return `<button class="catalog-modal-option ${selected?'active':''}" type="button" data-filter="${key}" data-value="${escapeHtml(value)}" aria-pressed="${selected}"><span class="catalog-modal-option-dot" aria-hidden="true">✓</span>${escapeHtml(label)}</button>`;
}
function renderModalOptions() {
    const kinds = [
        ['modalTypeOptions','types',typeOptions],
        ['modalStyleOptions','styles',catalogFacets.styles.map(value=>[value,value])],
        ['modalUseOptions','uses',useOptions],
        ['modalTransmissionOptions','transmissions',catalogFacets.transmissions.map(value=>[value,value])],
        ['modalFuelOptions','fuels',catalogFacets.fuels.map(value=>[value,value])],
        ['modalSeatsOptions','seats',catalogFacets.seats.map(value=>[value,`${value} seats`])],
        ['modalFeaturedOptions','topOnly',[['true','Top picks only']]]
    ];
    for (const [id,key,options] of kinds) {
        const container = document.getElementById(id);
        container.innerHTML = options.map(([value,label])=>modalOption(key,value,label)).join('');
        container.closest('fieldset').hidden = options.length === 0;
    }
    document.getElementById('modalMinPrice').value = draftFilters.minPrice ?? '';
    document.getElementById('modalMaxPrice').value = draftFilters.maxPrice ?? '';
    updateModalResultCount();
}
function updateModalResultCount() {
    const incomplete = Boolean(draftStartDate) !== Boolean(draftReturnDate);
    const invalid = draftStartDate && draftReturnDate && draftReturnDate <= draftStartDate;
    const busy = modalPreviewLoading || modalPreviewError || incomplete || invalid;
    const count = busy ? '…' : modalPreviewCount;
    document.getElementById('modalResultCount').textContent = String(count);
    document.getElementById('applyModalFilters').disabled = Boolean(busy);
    const status = document.getElementById('availabilityStatus');
    if (incomplete) status.textContent = 'Select both dates to check availability, or leave both empty.';
    else if (invalid) status.textContent = 'Return date must be after the start date.';
    else if (modalPreviewLoading) status.textContent = 'Checking availability for your dates…';
    else if (modalPreviewError) status.textContent = 'Availability could not be checked. Try another date or try again.';
    else status.textContent = '';
}
async function refreshModalPreview() {
    const request = ++modalPreviewRequest;
    const incomplete = Boolean(draftStartDate) !== Boolean(draftReturnDate);
    const invalid = draftStartDate && draftReturnDate && draftReturnDate <= draftStartDate;
    if (incomplete || invalid) {
        modalPreviewLoading = false;
        modalPreviewError = false;
        updateModalResultCount();
        return;
    }
    modalPreviewLoading = true;
    modalPreviewError = false;
    updateModalResultCount();
    try {
        const params = catalogParams(draftFilters, draftStartDate, draftReturnDate);
        params.set('limit', '0');
        const response = await fetch('/api/vehicles/catalog-page?' + params, {cache:'no-store'});
        if (!response.ok) throw new Error('Availability request failed');
        const data = await response.json();
        if (request !== modalPreviewRequest) return;
        modalPreviewCount = data.total;
        modalPreviewLoading = false;
        updateModalResultCount();
    } catch (error) {
        if (request !== modalPreviewRequest) return;
        console.error(error);
        modalPreviewLoading = false;
        modalPreviewError = true;
        updateModalResultCount();
    }
}

function catalogParams(filters, startDate, returnDate) {
    const params = new URLSearchParams();
    for (const key of ['types','styles','uses','branches','transmissions','fuels','seats'])
        if (filters[key].length) params.set(key, filters[key].join(','));
    if (filters.minPrice !== null) params.set('minPrice', filters.minPrice);
    if (filters.maxPrice !== null) params.set('maxPrice', filters.maxPrice);
    if (filters.topOnly) params.set('topOnly', 'true');
    if (searchQuery) params.set('search', searchQuery);
    if (startDate && returnDate) { params.set('startDate', startDate); params.set('returnDate', returnDate); }
    return params;
}
async function loadCatalog() {
    catalogRequest++;
    catalogLoading = false;
    allVehicles = [];
    catalogTotal = 0;
    await loadCatalogPage(false);
}

async function loadCatalogPage(append) {
    const grid = document.getElementById("catalogGrid");
    if (catalogLoading) return;
    catalogLoading = true;
    const request = ++catalogRequest;
    const button = document.getElementById('loadMoreBtn');
    button.disabled = true;
    try {
        if (!append) grid.innerHTML = '<div class="empty-state">Loading vehicles...</div>';
        const params = catalogParams(appliedFilters, selectedStartDate, selectedReturnDate);
        params.set('offset', append ? String(allVehicles.length) : '0');
        params.set('limit', append ? String(LOAD_MORE_STEP) : '12');
        const response = await fetch('/api/vehicles/catalog-page?' + params, {cache:'no-store'});
        if (!response.ok) throw new Error('Could not load catalog page');
        const data = await response.json();
        if (request !== catalogRequest) return;
        allVehicles = append ? [...allVehicles, ...data.vehicles] : data.vehicles;
        catalogTotal = data.total;
        catalogFacets = data.facets || catalogFacets;
        renderVehicles();
    } catch (error) {
        if (request !== catalogRequest) return;
        console.error(error);
        if (append) return;
        document.getElementById("resultCount").textContent = "";
        document.getElementById("catalogFilterSummary").textContent = "We could not load the fleet right now.";
        document.getElementById("loadMoreWrap").hidden = true;
        if (selectedStartDate && selectedReturnDate) {
            document.getElementById("availabilityStatus").textContent = "Availability could not be checked. Please try again.";
        }

        grid.innerHTML = `
            <div class="empty-state">
                <h3>Could not load vehicles</h3>
                <p>Please try again shortly or contact us for assistance.</p>
            </div>
        `;
    } finally {
        if (request === catalogRequest) { catalogLoading = false; button.disabled = false; }
    }
}

function renderVehicles() {
    const grid = document.getElementById("catalogGrid");
    const loadMoreWrap = document.getElementById("loadMoreWrap");

    const vehiclesToShow = allVehicles;
    document.getElementById("resultCount").textContent = `${catalogTotal} ${catalogTotal === 1 ? 'vehicle' : 'vehicles'}`;
    const summaryParts = [];
    if (searchQuery) summaryParts.push(`Search: “${searchQuery}”`);
    if (appliedFilters.topOnly) summaryParts.push("Top picks");
    if (appliedFilters.types.length) summaryParts.push(appliedFilters.types.map(value => value === 'PREMIUM' ? 'Premium' : formatType(value)).join(', '));
    if (appliedFilters.uses.length) summaryParts.push(appliedFilters.uses.map(formatUse).join(', '));
    if (appliedFilters.branches.length) summaryParts.push(appliedFilters.branches.map(value => catalogBranches.find(item => item.branchId === value)?.name || value).join(', '));
    if (appliedFilters.styles.length || appliedFilters.transmissions.length || appliedFilters.fuels.length || appliedFilters.seats.length || appliedFilters.minPrice !== null || appliedFilters.maxPrice !== null) summaryParts.push('More filters applied');
    if (selectedStartDate && selectedReturnDate) summaryParts.push("available for your dates");
    document.getElementById("catalogFilterSummary").textContent = summaryParts.length
        ? summaryParts.join(" · ")
        : "Showing all vehicles";

    grid.innerHTML = "";

    if (catalogTotal === 0) {
        grid.innerHTML = `
            <div class="empty-state">
                <h3>No vehicles found</h3>
                <p>No vehicles are available in this filter right now.</p>
            </div>
        `;

        if (loadMoreWrap) {
            loadMoreWrap.hidden = true;
        }

        return;
    }

    vehiclesToShow.forEach(vehicle => {
        const card = document.createElement("article");
        card.className = "car-card";

        const vehicleId = getVehicleId(vehicle);
        const imageFile = vehicle.vehicleImageFileName || vehicle.imageFileName || vehicle.image || "";

        const imageSrc = imageFile
            ? `/images/${encodeURIComponent(imageFile)}`
            : "/images/vehicle-placeholder.svg";

        const title = `${vehicle.make || ""} ${vehicle.model || ""}`.trim() || "DriveEase Vehicle";
        const useCategory = getUseCategory(vehicle);
        const topChoice = isTopChoice(vehicle);
        const operationalStatus = String(vehicle.operationalStatus || "AVAILABLE").toUpperCase();

        const seats = getVehicleSeats(vehicle);
        const bodyStyle = getBodyStyle(vehicle);
        const transmission = getTransmission(vehicle);
        const fuel = getFuelType(vehicle) || "Fuel";

        const rateValue = Number(vehicle.rentalRate || vehicle.rate || vehicle.price || 0);
        const rate = Number.isFinite(rateValue)
            ? rateValue.toLocaleString("en-LK")
            : "0";

        const topChoiceTag = topChoice
            ? `<span class="car-tag dark">Top Choice</span>`
            : "";

        card.innerHTML = `
            <div class="car-image-area">
                <img
                    src="${imageSrc}"
                    alt="${escapeHtml(title)}"
                    onerror="this.onerror=null;this.src='/images/vehicle-placeholder.svg'"
                >
            </div>

            <div class="car-card-body">
                <h3>${escapeHtml(title)}</h3>

                <div class="car-tags">
                    <span class="car-tag">${escapeHtml(formatUse(useCategory))}</span>
                    ${topChoiceTag}
                </div>

                <div class="car-divider"></div>

                <div class="car-bottom">
                    <div class="car-specs">
                        <span class="spec-pill">
                            <span class="spec-icon">${specIcons.body}</span>
                            ${escapeHtml(bodyStyle)}
                        </span>

                        <span class="spec-pill">
                            <span class="spec-icon">${specIcons.transmission}</span>
                            ${escapeHtml(transmission)}
                        </span>

                        <span class="spec-pill">
                            <span class="spec-icon">${specIcons.seats}</span>
                            ${escapeHtml(seats)} seats
                        </span>

                        <span class="spec-pill">
                            <span class="spec-icon">${specIcons.fuel}</span>
                            ${escapeHtml(fuel)}
                        </span>
                    </div>

                    <div class="car-price">
                        <strong>Rs. ${escapeHtml(rate)}</strong>
                        <span>/Day</span>
                    </div>
                </div>

                <button class="book-now-btn" type="button" ${operationalStatus === "AVAILABLE" ? "" : "disabled"}>
                    ${operationalStatus === "AVAILABLE" ? "Book Now" : escapeHtml(operationalStatus)}
                </button>
            </div>
        `;

        const bookButton = card.querySelector(".book-now-btn");

        bookButton.addEventListener("click", () => {
            if (!vehicleId) {
                alert("Vehicle ID missing. Please check vehicle data.");
                return;
            }

            const dates = selectedStartDate && selectedReturnDate ? `&startDate=${encodeURIComponent(selectedStartDate)}&returnDate=${encodeURIComponent(selectedReturnDate)}` : "";
            window.location.href = `/bookVehicle?id=${encodeURIComponent(vehicleId)}${dates}`;
        });

        grid.appendChild(card);
    });

    if (loadMoreWrap) {
        loadMoreWrap.hidden = allVehicles.length >= catalogTotal;
    }
}
function getModelId(vehicle) {
    return getVehicleId(vehicle).replace(/-U[23]$/, '');
}

function getVehicleId(vehicle) {
    return vehicle.vehicleId || vehicle.id || "";
}

function getVehicleMeta(vehicle) {
    const vehicleId = getModelId(vehicle);
    return vehicleMeta[vehicleId] || {};
}

function getVehicleType(vehicle) {
    return String(
        vehicle.type ||
        vehicle.vehicleType ||
        vehicle.category ||
        ""
    ).toUpperCase();
}

function getBodyStyle(vehicle) {
    const listed = vehicleSpecs[getModelId(vehicle)];
    if (listed) return listed.body;
    const subtype = String(vehicle.carType || vehicle.motorcycleType || vehicle.vehicleSubtype || '').trim();
    if (subtype && !/^(2WD|4WD|AWD)$/i.test(subtype)) return subtype;
    const type = getVehicleType(vehicle);
    if (type === 'SUV') return subtype ? `${subtype.toUpperCase()} SUV` : 'SUV';
    if (type === 'VAN') return 'Passenger van';
    return type === 'MOTORCYCLE' ? 'Motorcycle' : 'Car';
}

function getTransmission(vehicle) {
    const recorded = String(vehicle.transmission || vehicle.transmissionType || '').trim().toUpperCase();
    if (recorded) return /AUTO|CVT|AMT|DSG|DCT/.test(recorded) ? 'Automatic' : 'Manual';
    const listed = vehicleSpecs[getModelId(vehicle)];
    if (listed) return listed.transmission;
    if (getVehicleType(vehicle) === 'MOTORCYCLE')
        return /SCOOTER/i.test(getBodyStyle(vehicle)) ? 'Automatic' : 'Manual';
    return 'Automatic';
}

function getVehicleSeats(vehicle) {
    const seats = Number(vehicle.numberOfSeats || vehicle.seats || vehicle.seatCount || vehicle.capacity);
    return Number.isFinite(seats) && seats > 0 ? seats : Number(getDefaultSeats(getVehicleType(vehicle)));
}

function getFuelType(vehicle) {
    const fuel = String(vehicle.fuel || vehicle.fuelType || '').trim();
    return fuel ? fuel[0].toUpperCase() + fuel.slice(1).toLowerCase() : '';
}

function getUseCategory(vehicle) {
    const meta = getVehicleMeta(vehicle);
    if (meta.useCategory) return meta.useCategory;
    if (vehicle.usageCategory) return String(vehicle.usageCategory).toUpperCase();

    const type = getVehicleType(vehicle);

    if (type === "VAN") return "TRANSPORT";
    if (type === "SUV") return "FAMILY";
    if (type === "MOTORCYCLE") return "DAILY";

    return "DAILY";
}

function getFilterType(vehicle) {
    const meta = getVehicleMeta(vehicle);
    return meta.displayCategory || (isPremium(vehicle) ? 'PREMIUM' : getVehicleType(vehicle));
}

function isPremium(vehicle) {
    const meta = getVehicleMeta(vehicle);

    if (meta.displayCategory === "PREMIUM") {
        return true;
    }

    const rate = Number(vehicle.rentalRate || vehicle.rate || vehicle.price || 0);
    return rate >= 25000;
}

function isTopChoice(vehicle) {
    const meta = getVehicleMeta(vehicle);

    if (typeof meta.topChoice === "boolean") {
        return meta.topChoice;
    }

    const rate = Number(vehicle.rentalRate || vehicle.rate || vehicle.price || 0);
    return rate >= 10000;
}

function formatUse(useCategory) {
    const value = String(useCategory).toUpperCase();

    if (value === "FAMILY") return "Family";
    if (value === "BUSINESS") return "Business";
    if (value === "WEDDING") return "Wedding";
    if (value === "ADVENTURE") return "Adventure";
    if (value === "TRANSPORT") return "Transport";
    if (value === "DAILY") return "Daily Use";

    return useCategory;
}
function formatType(type) {
    const value = String(type).toUpperCase();

    if (value === "CAR") return "Car";
    if (value === "SUV") return "SUV";
    if (value === "VAN") return "Van";
    if (value === "MOTORCYCLE") return "Bike";
    if (value === "PREMIUM") return "Premium";

    return type;
}

function getDefaultSeats(type) {
    const value = String(type).toUpperCase();

    if (value === "MOTORCYCLE") return "2";
    if (value === "VAN") return "8";
    if (value === "SUV") return "5";

    return "5";
}

function escapeHtml(value) {
    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

async function loadBranchFilters() {
    try {
        const response = await fetch('/api/branches', {cache:'no-store'});
        if (!response.ok) return;
        const data = await response.json();
        catalogBranches = Array.isArray(data) ? data : [];
        branchSignature = JSON.stringify(catalogBranches.map(branch => [branch.branchId, branch.name, branch.open]));
        document.getElementById('availabilityBranch').innerHTML = '<option value="">Any open branch</option>'
            + catalogBranches.map(branch => `<option value="${escapeHtml(branch.branchId)}">${escapeHtml(branch.name)}</option>`).join('')
            + (appliedFilters.branches.length && !catalogBranches.some(branch => branch.branchId === appliedFilters.branches[0])
                ? `<option value="${escapeHtml(appliedFilters.branches[0])}">${escapeHtml(appliedFilters.branches[0])} (closed)</option>` : '');
    } catch (error) {
        console.error('Could not load branches', error);
    }
}

let branchSignature = '';
async function checkBranchChanges() {
    if (document.hidden) return;
    try {
        const response = await fetch('/api/branches', {cache:'no-store'});
        if (!response.ok) return;
        const branches = await response.json();
        const signature = JSON.stringify(branches.map(branch => [branch.branchId, branch.name, branch.open]));
        if (!branchSignature) { branchSignature = signature; return; }
        if (signature !== branchSignature) {
            branchSignature = signature;
            await loadBranchFilters();
            await loadCatalog();
        }
    } catch (error) { console.warn('Could not refresh branch status', error); }
}
