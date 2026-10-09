let branches = [];
let inventoryVehicles = [];
let inventoryPage = 1;
const inventoryPageSize = 20;
let inventoryTotal = 0;
let inventorySearchTimer;
let inventoryRequest = 0;

window.onload = async () => {
    document.getElementById('inventorySearch').addEventListener('input', () => {
        inventoryPage = 1;
        clearTimeout(inventorySearchTimer);
        inventorySearchTimer = setTimeout(fetchInventory, 250);
    });
    document.getElementById('inventoryPrevious').addEventListener('click', () => {
        inventoryPage--;
        fetchInventory();
    });
    document.getElementById('inventoryNext').addEventListener('click', () => {
        inventoryPage++;
        fetchInventory();
    });
    const response = await fetch('/api/branches/all');
    if (response.ok) branches = await response.json();
    await fetchInventory();
};

async function fetchInventory() {
    const tableBody = document.getElementById('inventoryBody');
    const request = ++inventoryRequest;
    try {
        const params = new URLSearchParams({page:String(inventoryPage - 1),size:String(inventoryPageSize),
            query:document.getElementById('inventorySearch').value.trim()});
        const response = await fetch('/api/vehicles/inventory-page?' + params, {cache:'no-store'});
        if (!response.ok) throw new Error('Could not load inventory.');
        const data = await response.json();
        if (request !== inventoryRequest) return;
        inventoryVehicles = data.vehicles;
        inventoryTotal = data.total;
        renderInventory();
    } catch (error) {
        if (request !== inventoryRequest) return;
        console.error(error);
        tableBody.innerHTML = '<tr><td colspan="10" class="empty-table-cell">Could not load inventory.</td></tr>';
    }
}

function renderInventory() {
    const tableBody = document.getElementById('inventoryBody');
    const pages = Math.max(1, Math.ceil(inventoryTotal / inventoryPageSize));
    if (inventoryPage > pages) { inventoryPage = pages; fetchInventory(); return; }
    inventoryPage = Math.max(1, Math.min(inventoryPage, pages));
    const start = (inventoryPage - 1) * inventoryPageSize;
    tableBody.replaceChildren();
    inventoryVehicles.forEach(vehicle => tableBody.appendChild(createVehicleRow(vehicle)));
    if (!inventoryTotal) tableBody.innerHTML = '<tr><td colspan="10" class="empty-table-cell">No vehicles match this search.</td></tr>';
    document.getElementById('inventoryCount').textContent = `${inventoryTotal} vehicles`;
    document.getElementById('inventoryPageSummary').textContent = inventoryTotal
        ? `Showing ${start + 1}–${Math.min(start + inventoryPageSize, inventoryTotal)} of ${inventoryTotal}`
        : 'Showing 0 vehicles';
    document.getElementById('inventoryPageNumber').textContent = `${inventoryPage} / ${pages}`;
    document.getElementById('inventoryPrevious').disabled = inventoryPage === 1;
    document.getElementById('inventoryNext').disabled = inventoryPage === pages;
}

function createVehicleRow(vehicle) {
    const row = document.createElement('tr');
    const status = String(vehicle.operationalStatus || (vehicle.available ? 'AVAILABLE' : 'RENTED')).toUpperCase();
    const currentBranch = branches.find(branch => branch.branchId === vehicle.currentBranch);
    const canRelocate = status === 'AVAILABLE' || status === 'BRANCH CLOSED';
    const branchOptions = branches
        .filter(branch => branch.open || branch.branchId === vehicle.currentBranch)
        .map(branch => `<option value="${escapeHtml(branch.branchId)}"
            ${branch.branchId === vehicle.currentBranch ? 'selected' : ''}
            ${!branch.open ? 'disabled' : ''}>${escapeHtml(branch.name)}${!branch.open ? ' (Closed)' : ''}</option>`)
        .join('');
    const imageName = String(vehicle.vehicleImageFileName || '');
    const imagePath = /^\/images\/[\w./-]+$/.test(imageName) && !imageName.includes('..')
        ? imageName
        : /^[\w.-]+$/.test(imageName)
            ? `/images/${encodeURIComponent(imageName)}`
            : '/images/vehicle-placeholder.svg';

    row.innerHTML = `
        <td class="inventory-image-cell"><div class="img-thumbnail-container"><img class="img-thumbnail" src="${escapeHtml(imagePath)}" alt="${escapeHtml(vehicle.make)} ${escapeHtml(vehicle.model)}" loading="lazy" onerror="this.onerror=null;this.src='/images/vehicle-placeholder.svg'"></div></td>
        <td class="mono-cell">${escapeHtml(vehicle.vehicleId)}</td>
        <td><div class="primary-cell">${escapeHtml(vehicle.make)} ${escapeHtml(vehicle.model)}</div>
            <div class="secondary-cell">${escapeHtml(vehicle.year)}</div></td>
        <td>${escapeHtml(formatVehicleType(vehicle.type))}</td>
        <td>${escapeHtml(formatCategory(vehicle.usageCategory))}</td>
        <td><select id="branch-${escapeHtml(vehicle.vehicleId)}" class="form-control branch-select">
            ${branchOptions || `<option selected>${escapeHtml(vehicle.currentBranch)}</option>`}
        </select><div class="secondary-cell">${escapeHtml(currentBranch?.branchId || vehicle.currentBranch)}</div></td>
        <td class="rate-cell">Rs. ${Number(vehicle.rentalRate).toLocaleString()}</td>
        <td class="mono-cell">${Number(vehicle.mileage || 0).toLocaleString('en-LK')} km</td>
        <td><span class="badge ${statusClass(status)}">${escapeHtml(status)}</span></td>
        <td><div class="action-buttons">
            <button class="inventory-action edit" type="button" title="Edit ${escapeHtml(vehicle.vehicleId)}" aria-label="Edit ${escapeHtml(vehicle.vehicleId)}" onclick="editVehicle('${escapeJs(vehicle.vehicleId)}')" ${status === 'RETIRED' ? 'disabled' : ''}><span class="material-symbols-outlined" aria-hidden="true">edit</span></button>
            <button class="inventory-action" type="button" title="Relocate ${escapeHtml(vehicle.vehicleId)}" aria-label="Relocate ${escapeHtml(vehicle.vehicleId)}" onclick="relocateVehicle('${escapeJs(vehicle.vehicleId)}')" ${canRelocate ? '' : 'disabled'}><span class="material-symbols-outlined" aria-hidden="true">move_item</span></button>
            <a class="inventory-action" title="History for ${escapeHtml(vehicle.vehicleId)}" aria-label="History for ${escapeHtml(vehicle.vehicleId)}" href="/admin/vehicles/${encodeURIComponent(vehicle.vehicleId)}/history"><span class="material-symbols-outlined" aria-hidden="true">history</span></a>
            <button class="inventory-action retire" type="button" title="Retire ${escapeHtml(vehicle.vehicleId)}" aria-label="Retire ${escapeHtml(vehicle.vehicleId)}" onclick="deleteVehicle('${escapeJs(vehicle.vehicleId)}')" ${status === 'AVAILABLE' || status === 'BRANCH CLOSED' ? '' : 'disabled'}><span class="material-symbols-outlined" aria-hidden="true">archive</span></button>
        </div></td>`;
    return row;
}

function formatVehicleType(type) {
    const value = String(type || '').toUpperCase();
    if (value === 'MOTORCYCLE') return 'Bike';
    if (value === 'SUV') return 'SUV';
    if (value === 'VAN') return 'Van';
    if (value === 'CAR') return 'Car';
    return value || 'Vehicle';
}

function formatCategory(category) {
    return String(category || 'DAILY').toLowerCase().replace(/\b\w/g, letter => letter.toUpperCase());
}

function statusClass(status) {
    if (status === 'AVAILABLE') return 'approved';
    if (status === 'BOOKED') return 'booked';
    if (status === 'MAINTENANCE') return 'maintenance';
    if (status === 'BRANCH CLOSED') return 'closed';
    return 'rejected';
}

function editVehicle(id) {
    window.location.href = `/vehicleForm?id=${encodeURIComponent(id)}`;
}

async function deleteVehicle(id) {
    if (!confirm('Retire this vehicle? Historical records will be preserved.')) return;
    const response = await fetch(`/api/vehicles/delete/${encodeURIComponent(id)}`, {method: 'DELETE'});
    if (response.ok) await fetchInventory();
    else alert(await response.text() || 'Failed to delete vehicle.');
}

async function relocateVehicle(id) {
    const branch = document.getElementById(`branch-${id}`).value;
    const response = await fetch(`/api/vehicles/${encodeURIComponent(id)}/relocate`, {
        method: 'POST',
        headers: {'Content-Type': 'application/x-www-form-urlencoded'},
        body: new URLSearchParams({branch})
    });
    const message = await response.text();
    if (!response.ok) return alert(message);
    alert(message);
    await fetchInventory();
}

function escapeHtml(value) {
    return String(value ?? '').replace(/[&<>"']/g, char => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[char]));
}

function escapeJs(value) {
    return String(value ?? '').replace(/\\/g, '\\\\').replace(/'/g, "\\'");
}
