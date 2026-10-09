document.addEventListener("DOMContentLoaded", function () {
    loadBookings();
    const dialog = document.getElementById('rentalReviewDialog');
    const form = document.getElementById('rentalReviewForm');
    document.getElementById('closeRentalReview')?.addEventListener('click', () => dialog.close());
    document.getElementById('bookingTableBody')?.addEventListener('click', event => {
        const button = event.target.closest('[data-review-booking]');
        if (!button) return;
        form.reset();
        document.getElementById('reviewBookingId').value = button.dataset.reviewBooking;
        document.getElementById('rentalReviewError').textContent = '';
        dialog.showModal();
    });
    form?.addEventListener('submit', async event => {
        event.preventDefault();
        const submit = form.querySelector('button[type="submit"]');
        submit.disabled = true;
        try {
            const response = await fetch('/api/customer/reviews', {method:'POST', body:new FormData(form), credentials:'same-origin'});
            if (!response.ok) throw new Error(response.status === 400 ? 'This rental cannot be reviewed again, or the rating is invalid.' : 'Could not save your review. Please try again.');
            dialog.close();
            loadBookings();
        } catch (error) {
            document.getElementById('rentalReviewError').textContent = error.message;
        } finally { submit.disabled = false; }
    });
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
    if (normalized === "cancelled") return "rejected";
    if (normalized === "active" || normalized === "returned" || normalized === "completed") return "approved";

    return "pending";
}

function buildActionHtml(booking, reviewsById) {
    const transactionId = escapeHtml(booking.transactionId);
    const status = String(booking.bookingStatus ?? "");

    if (status === "Pending") {
        return `
            <div class="history-actions">
                <span class="history-action-note gold">⏳ Awaiting approval</span>
                <a href="/editBooking?id=${transactionId}" class="history-action-btn light">Edit</a>
                <button onclick="submitCancel('${transactionId}')" class="history-action-btn red">Cancel</button>
            </div>
        `;
    }

    if (status === "Approved") {
        return `
            <div class="history-actions">
                <a href="/checkout?transactionId=${transactionId}" class="history-action-btn green">Pay Now</a>
                <button onclick="submitCancel('${transactionId}')" class="history-action-btn red">Cancel</button>
            </div>
        `;
    }

    if (status === "Paid") {
        return `<div class="history-actions"><span class="history-action-note green">✅ Payment complete</span><button onclick="submitCancel('${transactionId}')" class="history-action-btn red">Cancel</button></div>`;
    }

    if (status === "Rejected") {
        return `<span class="history-action-note red">✕ Booking declined</span>`;
    }

    if (status === "Returned" || status === "Completed") {
        const review = reviewsById.get(booking.transactionId);
        if (review) return `<span class="history-review-stars" aria-label="${review.stars} out of 5 stars">${'★'.repeat(review.stars)}${'☆'.repeat(5-review.stars)}</span>`;
        return `<button type="button" class="history-action-btn dark" data-review-booking="${transactionId}">Review rental</button>`;
    }

    return `<strong>${escapeHtml(status)}</strong>`;
}

async function loadBookings() {
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

    try {
        const [bookingsResponse, reviewsResponse] = await Promise.all([
            fetch(`/api/bookings?customer=${encodeURIComponent(currentUser)}`),
            fetch('/api/customer/reviews')
        ]);
        if (!bookingsResponse.ok) throw new Error('Bookings unavailable');
        const data = await bookingsResponse.json();
        const reviews = reviewsResponse.ok ? await reviewsResponse.json() : [];
        const reviewsById = new Map(reviews.map(review => [review.bookingId, review]));
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
                        <td>${buildActionHtml(booking, reviewsById)}</td>
                    </tr>
                `;

                tableBody.innerHTML += row;
            });
    } catch (error) {
            console.error("Error fetching the bookings:", error);
            tableBody.innerHTML = `
                <tr>
                    <td colspan="6" class="history-empty">Error loading bookings. Please try again later.</td>
                </tr>
            `;
    }
}

function submitCancel(transactionId) {
    const reason = prompt("Why are you cancelling this booking?");
    if (reason && confirm("Confirm cancellation? A Rs. 2,500 late fee applies within 48 hours of pickup. Paid bookings are refunded after any fee.")) {
        const form = document.createElement("form");
        form.method = "POST";
        form.action = `/bookings/${encodeURIComponent(transactionId)}/cancel`;

        const input = document.createElement("input");
        input.type = "hidden";
        input.name = "reason";
        input.value = reason;

        form.appendChild(input);
        document.body.appendChild(form);
        form.submit();
    }
}
