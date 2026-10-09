document.addEventListener("DOMContentLoaded", () => {
    const summary = document.getElementById("invoice-summary");
    const body = document.getElementById("invoice-table-body");
    const search = document.getElementById("invoice-search");
    const pagination = document.getElementById("invoice-pagination");
    const pageSize = 20;
    let invoices = [];
    let page = 1;

    const amount = value => Number.isFinite(Number(value)) ? Number(value) : 0;
    const money = value => `Rs. ${amount(value).toLocaleString("en-LK", {minimumFractionDigits: 2, maximumFractionDigits: 2})}`;
    const adjustment = item => amount(item.lateFee) + amount(item.damageFee) + amount(item.fuelCharge)
        + amount(item.mileageCharge) + amount(item.alternateDropoffCharge)
        + amount(item.depositAmount) - amount(item.refundAmount);

    function cell(row, value, className) {
        const td = document.createElement("td");
        td.textContent = value == null ? "" : String(value);
        if (className) td.className = className;
        row.appendChild(td);
        return td;
    }

    function showEmpty(message) {
        const row = document.createElement("tr");
        cell(row, message, "empty-table-cell").colSpan = 8;
        body.replaceChildren(row);
        pagination.hidden = true;
    }

    function pageButton(label, target, disabled) {
        const button = document.createElement("button");
        button.type = "button";
        button.className = "btn btn-secondary";
        button.textContent = label;
        button.disabled = disabled;
        button.addEventListener("click", () => { page = target; render(); });
        return button;
    }

    function render() {
        const query = search.value.trim().toLocaleLowerCase();
        const matches = invoices.filter(item =>
            [item.invoiceId, item.id, item.customerName, item.customer, item.vehicleId, item.vehicle]
                .some(value => String(value ?? "").toLocaleLowerCase().includes(query)));
        if (!matches.length) {
            showEmpty(invoices.length ? "No invoices match this search." : "No invoices found. Complete a checkout to generate billing records.");
            return;
        }
        const pages = Math.ceil(matches.length / pageSize);
        page = Math.min(page, pages);
        const start = (page - 1) * pageSize;
        const visible = matches.slice(start, start + pageSize);
        body.replaceChildren(...visible.map(item => {
            const row = document.createElement("tr");
            cell(row, item.invoiceId || item.id || "N/A", "cell-code");
            cell(row, item.customerName || item.customer || "N/A");
            cell(row, item.vehicleId || item.vehicle || "N/A", "cell-code");
            cell(row, money(item.totalAmount));
            cell(row, money(adjustment(item)));
            const badgeCell = document.createElement("td");
            const badge = document.createElement("span");
            badge.className = "badge " + (/paid|settled|complete/i.test(item.status || "") ? "approved" : "pending");
            badge.textContent = item.status || "N/A";
            badgeCell.appendChild(badge);
            row.appendChild(badgeCell);
            const method = typeof item.paymentMethod === "object" ? item.paymentMethod?.type : item.paymentMethod;
            const methods = {creditcard: "Credit Card", paypal: "PayPal", cash: "Cash"};
            cell(row, methods[String(method || "").toLowerCase()] || method || "N/A", "cell-muted");
            const actionCell = document.createElement("td");
            const actions = document.createElement("div");
            actions.className = "row-actions";
            const id = encodeURIComponent(item.id || item.invoiceId || "");
            [["View", ""], ["Edit", "/edit"], ["Print", "/print"]].forEach(([label, suffix]) => {
                const link = document.createElement("a");
                link.className = "btn btn-secondary";
                link.href = `/admin/invoices/${id}${suffix}`;
                link.textContent = label;
                if (label === "Print") { link.target = "_blank"; link.rel = "noopener"; }
                actions.appendChild(link);
            });
            actionCell.appendChild(actions);
            row.appendChild(actionCell);
            return row;
        }));
        pagination.hidden = pages <= 1;
        if (pages > 1) {
            const info = document.createElement("span");
            info.textContent = `Showing ${start + 1}–${start + visible.length} of ${matches.length}`;
            const links = document.createElement("div");
            links.className = "page-links";
            links.append(pageButton("Previous", page - 1, page === 1));
            const current = document.createElement("span");
            current.className = "page-current";
            current.textContent = `${page} / ${pages}`;
            links.append(current, pageButton("Next", page + 1, page === pages));
            pagination.replaceChildren(info, links);
        }
    }

    search.addEventListener("input", () => { page = 1; render(); });
    fetch("/api/invoices")
        .then(response => { if (!response.ok) throw new Error("Failed to load invoices"); return response.json(); })
        .then(data => {
            invoices = Array.isArray(data) ? data : [];
            const billed = invoices.reduce((sum, item) => sum + amount(item.totalAmount), 0);
            const adjusted = invoices.reduce((sum, item) => sum + adjustment(item), 0);
            summary.textContent = invoices.length
                ? `Total invoices: ${invoices.length} · Billed: ${money(billed)} · Net return adjustments: ${money(adjusted)}`
                : "No invoice records found yet.";
            render();
        })
        .catch(error => {
            console.error(error);
            summary.textContent = "Could not load invoice data.";
            showEmpty("Failed to load invoices. Check the backend API.");
        });
});
