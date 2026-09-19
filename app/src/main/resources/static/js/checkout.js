const paymentDetails = document.getElementById("payment-details");
const paymentType = document.getElementById("paymentType");
const checkoutForm = document.getElementById("checkout-form");
const messageDiv = document.getElementById("message");
const subtotalDisplay = document.getElementById("subtotalDisplay");
const totalDisplay = document.getElementById("totalDisplay");

console.log("checkout.js version 5 loaded");

function formatCurrency(amount) {
    return `Rs. ${Number(amount || 0).toLocaleString("en-LK", {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
    })}`;
}

function getRentalTotal() {
    const rentalDays = Number(document.getElementById("rentalDays").value);
    const amountPerDay = Number(document.getElementById("amountPerDay").value);

    if (!Number.isFinite(rentalDays) || !Number.isFinite(amountPerDay)) {
        return 0;
    }

    return rentalDays * amountPerDay;
}

function updatePriceSummary() {
    const total = getRentalTotal();

    if (subtotalDisplay) {
        subtotalDisplay.textContent = formatCurrency(total);
    }

    if (totalDisplay) {
        totalDisplay.textContent = formatCurrency(total);
    }
}

function renderPaymentFields(type) {
    let html = "";

    if (type === "creditCard") {
        html = `
            <div class="checkout-field">
                <label for="cardNumber">Card Number</label>
                <input id="cardNumber" name="cardNumber" type="text" inputmode="numeric" maxlength="19" placeholder="13 to 19 digits" required>
            </div>

            <div class="checkout-field">
                <label for="cardHolder">Card Holder</label>
                <input id="cardHolder" name="cardHolder" type="text" placeholder="Name on card" required>
            </div>

            <div class="checkout-grid">
                <div class="checkout-field">
                    <label for="expiryDate">Expiry Date (MM/YY)</label>
                    <input id="expiryDate" name="expiryDate" type="text" placeholder="MM/YY" maxlength="5" required>
                </div>

                <div class="checkout-field">
                    <label for="cvv">CVV</label>
                    <input id="cvv" name="cvv" type="text" inputmode="numeric" maxlength="4" placeholder="3 or 4 digits" required>
                </div>
            </div>
        `;
    } else if (type === "paypal") {
        html = `
            <div class="checkout-field">
                <label for="paypalEmail">PayPal Email</label>
                <input id="paypalEmail" name="paypalEmail" type="email" placeholder="you@example.com" required>
            </div>
        `;
    } else {
        html = `
            <div class="checkout-field">
                <label for="receiptNumber">Receipt Number</label>
                <input id="receiptNumber" name="receiptNumber" type="text" placeholder="Enter cash receipt number" required>
            </div>
        `;
    }

    paymentDetails.innerHTML = html;
}

function showMessage(text, type = "success") {
    messageDiv.textContent = text;
    messageDiv.className = `message visible ${type}`;
}

function buildPaymentMethod(type) {
    if (type === "creditCard") {
        return {
            type: "creditCard",
            cardNumber: document.getElementById("cardNumber").value.trim(),
            cardHolder: document.getElementById("cardHolder").value.trim(),
            expiryDate: document.getElementById("expiryDate").value.trim(),
            cvv: document.getElementById("cvv").value.trim()
        };
    }

    if (type === "paypal") {
        return {
            type: "paypal",
            paypalEmail: document.getElementById("paypalEmail").value.trim()
        };
    }

    return {
        type: "cash",
        receiptNumber: document.getElementById("receiptNumber").value.trim()
    };
}

function validateForm() {
    const rentalDays = Number(document.getElementById("rentalDays").value);
    const amountPerDay = Number(document.getElementById("amountPerDay").value);
    const customerName = document.getElementById("customerName").value.trim();
    const vehicleId = document.getElementById("vehicleId").value.trim();
    const vehicleName = document.getElementById("vehicleName").value.trim();
    const termsAccepted = document.getElementById("termsAccepted").checked;

    if (!customerName || !vehicleId || !vehicleName) {
        throw new Error("Customer name, vehicle ID, and vehicle model are required.");
    }

    if (!Number.isInteger(rentalDays) || rentalDays <= 0) {
        throw new Error("Rental days must be a whole number greater than 0.");
    }

    if (amountPerDay <= 0) {
        throw new Error("Amount per day must be greater than 0.");
    }

    if (!termsAccepted) {
        throw new Error("Please agree to the rental terms and payment conditions before continuing.");
    }

    const type = paymentType.value;
    const paymentMethod = buildPaymentMethod(type);

    if (type === "creditCard") {
        const cleanCardNumber = paymentMethod.cardNumber.replace(/\s+/g, "");

        if (!/^\d{13,19}$/.test(cleanCardNumber)) {
            throw new Error("Credit card number must be 13 to 19 digits.");
        }

        paymentMethod.cardNumber = cleanCardNumber;

        if (!paymentMethod.cardHolder) {
            throw new Error("Card holder name is required.");
        }

        if (!/^(0[1-9]|1[0-2])\/\d{2}$/.test(paymentMethod.expiryDate)) {
            throw new Error("Expiry date must use MM/YY format.");
        }

        if (!/^\d{3,4}$/.test(paymentMethod.cvv)) {
            throw new Error("CVV must be 3 or 4 digits.");
        }
    }

    if (type === "paypal") {
        if (!/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(paymentMethod.paypalEmail)) {
            throw new Error("Enter a valid PayPal email.");
        }
    }

    if (type === "cash" && !paymentMethod.receiptNumber) {
        throw new Error("Receipt number is required for cash payments.");
    }

    return paymentMethod;
}

paymentType.addEventListener("change", () => renderPaymentFields(paymentType.value));

window.addEventListener("load", () => {
    renderPaymentFields(paymentType.value);
    updatePriceSummary();
});

checkoutForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    messageDiv.className = "message";

    try {
        const paymentMethod = validateForm();

        const body = {
            customerName: document.getElementById("customerName").value.trim(),
            vehicleId: document.getElementById("vehicleId").value.trim(),
            vehicleName: document.getElementById("vehicleName").value.trim(),
            rentalDays: Number(document.getElementById("rentalDays").value),
            amountPerDay: Number(document.getElementById("amountPerDay").value),

            // Hidden from customer UI, kept for backend invoice compatibility.
            discount: 0,
            lateFee: 0,

            paymentMethod
        };

        console.log("Sending invoice payload:", body);

        const response = await fetch("/api/invoices", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(body)
        });

        if (!response.ok) {
            const errorText = await readErrorMessage(response);
            throw new Error(errorText || "Unable to create invoice.");
        }

        const invoice = await response.json();

        const transactionId = document.getElementById("transactionId").value;

        if (transactionId && transactionId !== "Unknown") {
            await fetch(`/markAsPaid?transactionId=${encodeURIComponent(transactionId)}`, { method: "POST" });
        }

        showMessage(`Payment successful! Invoice #${invoice.id} created. Redirecting...`, "success");

        setTimeout(() => {
            window.location.href = "/reservationHistory";
        }, 2000);

    } catch (error) {
        showMessage(error.message || "Validation failed.", "error");
    }
});

async function readErrorMessage(response) {
    const text = await response.text();

    try {
        const error = JSON.parse(text);

        if (Array.isArray(error.errors) && error.errors.length > 0) {
            return error.errors.map(item => `${item.field}: ${item.message}`).join(", ");
        }

        return error.message || text;
    } catch {
        return text;
    }
}