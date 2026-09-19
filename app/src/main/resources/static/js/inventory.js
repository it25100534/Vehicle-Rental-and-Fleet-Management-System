// Load inventory as soon as the page opens
window.onload = fetchInventory;

async function fetchInventory() {
    try {
        const response = await fetch('/api/vehicles');
        const vehicles = await response.json();

        const tableBody = document.getElementById('inventoryBody');
        tableBody.innerHTML = ''; // Clear existing rows

        // Show a nice message if the inventory is empty
        if (vehicles.length === 0) {
            tableBody.innerHTML = `
                <tr>
                    <td colspan="7" style="text-align: center; color: var(--text-secondary); padding: 3rem;">
                        <span class="material-symbols-outlined" style="font-size: 2rem; opacity: 0.5; display: block; margin-bottom: 8px;">directions_car</span>
                        No vehicles currently in inventory.
                    </td>
                </tr>`;
            return;
        }

        vehicles.forEach(vehicle => {
            const row = document.createElement('tr');

            // Reuse the badge classes we made for the dashboard
            const isAvailable = String(vehicle.available).toLowerCase() === 'true';
            const badgeClass = isAvailable ? 'badge approved' : 'badge rejected';
            const badgeText = isAvailable ? 'AVAILABLE' : 'RENTED';

            row.innerHTML = `
                <td>
                    <div class="img-thumbnail-container">
                        <img src="/images/${vehicle.vehicleImageFileName}"
                             onerror="this.src='https://via.placeholder.com/80'"
                             class="img-thumbnail">
                    </div>
                </td>

                <td style="font-family: monospace; color: var(--text-secondary); white-space: nowrap;">
                    ${vehicle.vehicleId}
                </td>

                <td>
                    <div style="font-weight: 600; color: var(--text-primary);">${vehicle.make} ${vehicle.model}</div>
                    <div style="font-size: 0.85rem; color: var(--text-secondary);">${vehicle.year}</div>
                </td>

                <td style="color: var(--text-secondary);">
                    ${vehicle.type || 'Vehicle'}
                </td>

                <td style="font-weight: 600; color: var(--text-secondary);">
                    Rs. ${vehicle.rentalRate}
                </td>

                <td>
                    <span class="${badgeClass}">${badgeText}</span>
                </td>

                <td style="text-align: right;">
                    <div class="action-buttons">
                        <button class="btn btn-ghost-yellow" onclick="editVehicle('${vehicle.vehicleId}')" style="padding: 6px 12px; gap: 4px;">
                            <span class="material-symbols-outlined" style="font-size: 1rem;">edit</span> Edit
                        </button>
                        <button class="btn btn-danger" onclick="deleteVehicle('${vehicle.vehicleId}')" style="padding: 6px 12px; gap: 4px;">
                            <span class="material-symbols-outlined" style="font-size: 1rem;">delete</span> Delete
                        </button>
                    </div>
                </td>
            `;
            tableBody.appendChild(row);
        });
    } catch (error) {
        console.error("Error loading inventory:", error);
    }
}

function editVehicle(id) {
    window.location.href = `/vehicleForm?id=${id}`;
}

async function deleteVehicle(id) {
    if (confirm("Are you sure you want to delete this vehicle? This cannot be undone.")) {
        const response = await fetch(`/api/vehicles/delete/${id}`, {
            method: 'DELETE'
        });

        if (response.ok) {
            fetchInventory(); // Instantly refresh the table without reloading the page!
        } else {
            alert("Failed to delete vehicle.");
        }
    }
}