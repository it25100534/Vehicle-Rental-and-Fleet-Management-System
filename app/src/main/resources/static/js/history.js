document.addEventListener("DOMContentLoaded", function () {
    loadBookings();
});

function escapeHtml(value) {
    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

function shortId(value) {
    const text = String(value ?? "");
    if (text.length <= 14) {
        return text;
    }
    return `${text.slice(0, 8)}...${text.slice(-4)}`;
}

function statusClass(status) {
    const normalized = String(status ?? "").toLowerCase();

    if (normalized === "pending") return "pending";
    if (normalized === "approved") return "approved";
    if (normalized === "paid") return "paid";
    if (normalized === "rejected") return "rejected";

    return "pending";
}

function buildActionHtml(booking) {
    const transactionId = escapeHtml(booking.transactionId);
    const status = String(booking.bookingStatus ?? "");

    if (status === "Pending") {
        return `
            <div class="history-actions">
                <span class="history-action-note gold">⏳ Awaiting approval</span>
                <a href="/editBooking?id=${transactionId}" class="history-action-btn light">Edit</a>
                <button onclick="submitDelete('${transactionId}')" class="history-action-btn red">Delete</button>
            </div>
        `;
    }

    if (status === "Approved") {
        return `
            <div class="history-actions">
                <a href="/checkout?transactionId=${transactionId}" class="history-action-btn green">Pay Now</a>
            </div>
        `;
    }

    if (status === "Paid") {
        return `<span class="history-action-note green">✅ Payment complete</span>`;
    }

    if (status === "Rejected") {
        return `<span class="history-action-note red">✕ Booking declined</span>`;
    }

    return `<strong>${escapeHtml(status)}</strong>`;
}

function loadBookings() {
    const currentUserElement = document.getElementById("loggedInUsername");
    const tableBody = document.getElementById("bookingTableBody");

    if (!currentUserElement || !tableBody) {
        console.error("Booking history page elements are missing.");
        return;
    }

    const currentUser = currentUserElement.value;

    if (!currentUser) {
        tableBody.innerHTML = `
            <tr>
                <td colspan="6" class="history-empty">Please log in to view your booking history.</td>
            </tr>
        `;
        return;
    }

    fetch(`/api/bookings?customer=${encodeURIComponent(currentUser)}`)
        .then(response => response.json())
        .then(data => {
            tableBody.innerHTML = "";

            if (!data.length) {
                tableBody.innerHTML = `
                    <tr>
                        <td colspan="7" class="history-empty">
                            No bookings found yet. Choose a vehicle from the catalog to start your first rental.
                        </td>
                    </tr>
                `;
                return;
            }

            data.forEach(booking => {
                const status = escapeHtml(booking.bookingStatus);

                const row = `
                    <tr>
                        <td>${escapeHtml(booking.customerName)}</td>
                        <td>${escapeHtml(booking.vehicleId)}</td>
                        <td>${escapeHtml(booking.startDate)}</td>
                        <td>${escapeHtml(booking.returnDate)}</td>
                        <td>
                            <span class="history-status ${statusClass(booking.bookingStatus)}">${status}</span>
                        </td>
                        <td>${buildActionHtml(booking)}</td>
                    </tr>
                `;

                tableBody.innerHTML += row;
            });
        })
        .catch(error => {
            console.error("Error fetching the bookings:", error);
            tableBody.innerHTML = `
                <tr>
                    <td colspan="6" class="history-empty">Error loading bookings. Please try again later.</td>
                </tr>
            `;
        });
}

function submitDelete(transactionId) {
    if (confirm("Are you sure you want to cancel this booking?")) {
        const form = document.createElement("form");
        form.method = "POST";
        form.action = "/deleteBooking";

        const input = document.createElement("input");
        input.type = "hidden";
        input.name = "transactionId";
        input.value = transactionId;

        form.appendChild(input);
        document.body.appendChild(form);
        form.submit();
    }
}