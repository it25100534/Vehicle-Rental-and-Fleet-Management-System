window.onload = () => {
    setupFilterControls();
    loadCatalog();
};

let allVehicles = [];

let viewMode = "TOP";
let activeType = "CAR";
let activeUse = "FAMILY";

let visibleCount = 12;
const LOAD_MORE_STEP = 9;

/*
    Metadata will be filled properly after we finalize the 45 vehicles.
    For now, missing vehicles fall back to type/rate based rules.
*/
const vehicleMeta = {
    "CAR-0001": { displayCategory: "CAR", useCategory: "DAILY", topChoice: true },
    "CAR-0002": { displayCategory: "CAR", useCategory: "DAILY", topChoice: true },
    "CAR-0003": { displayCategory: "CAR", useCategory: "BUSINESS", topChoice: true },
    "CAR-0004": { displayCategory: "CAR", useCategory: "FAMILY", topChoice: false },
    "CAR-0005": { displayCategory: "CAR", useCategory: "DAILY", topChoice: false },
    "CAR-0006": { displayCategory: "CAR", useCategory: "BUSINESS", topChoice: false },
    "CAR-0007": { displayCategory: "CAR", useCategory: "FAMILY", topChoice: false },
    "CAR-0008": { displayCategory: "CAR", useCategory: "BUSINESS", topChoice: false },
    "CAR-0009": { displayCategory: "CAR", useCategory: "WEDDING", topChoice: true },

    "SUV-0001": { displayCategory: "SUV", useCategory: "FAMILY", topChoice: true },
    "SUV-0002": { displayCategory: "SUV", useCategory: "FAMILY", topChoice: true },
    "SUV-0003": { displayCategory: "SUV", useCategory: "ADVENTURE", topChoice: false },
    "SUV-0004": { displayCategory: "SUV", useCategory: "ADVENTURE", topChoice: true },
    "SUV-0005": { displayCategory: "SUV", useCategory: "ADVENTURE", topChoice: false },
    "SUV-0006": { displayCategory: "SUV", useCategory: "BUSINESS", topChoice: false },
    "SUV-0007": { displayCategory: "SUV", useCategory: "FAMILY", topChoice: false },
    "SUV-0008": { displayCategory: "SUV", useCategory: "TRANSPORT", topChoice: true },
    "SUV-0009": { displayCategory: "SUV", useCategory: "DAILY", topChoice: false },

    "VAN-0001": { displayCategory: "VAN", useCategory: "TRANSPORT", topChoice: true },
    "VAN-0002": { displayCategory: "VAN", useCategory: "TRANSPORT", topChoice: false },
    "VAN-0003": { displayCategory: "VAN", useCategory: "TRANSPORT", topChoice: false },
    "VAN-0004": { displayCategory: "VAN", useCategory: "FAMILY", topChoice: true },
    "VAN-0005": { displayCategory: "VAN", useCategory: "FAMILY", topChoice: false },
    "VAN-0006": { displayCategory: "VAN", useCategory: "FAMILY", topChoice: false },
    "VAN-0007": { displayCategory: "VAN", useCategory: "BUSINESS", topChoice: true },
    "VAN-0008": { displayCategory: "VAN", useCategory: "TRANSPORT", topChoice: false },
    "VAN-0009": { displayCategory: "VAN", useCategory: "TRANSPORT", topChoice: false },

    "MOT-0001": { displayCategory: "MOTORCYCLE", useCategory: "DAILY", topChoice: true },
    "MOT-0002": { displayCategory: "MOTORCYCLE", useCategory: "DAILY", topChoice: false },
    "MOT-0003": { displayCategory: "MOTORCYCLE", useCategory: "DAILY", topChoice: false },
    "MOT-0004": { displayCategory: "MOTORCYCLE", useCategory: "ADVENTURE", topChoice: true },
    "MOT-0005": { displayCategory: "MOTORCYCLE", useCategory: "DAILY", topChoice: false },
    "MOT-0006": { displayCategory: "MOTORCYCLE", useCategory: "ADVENTURE", topChoice: true },
    "MOT-0007": { displayCategory: "MOTORCYCLE", useCategory: "DAILY", topChoice: false },
    "MOT-0008": { displayCategory: "MOTORCYCLE", useCategory: "ADVENTURE", topChoice: false },
    "MOT-0009": { displayCategory: "MOTORCYCLE", useCategory: "WEDDING", topChoice: true },

    "PRE-0001": { displayCategory: "PREMIUM", useCategory: "WEDDING", topChoice: true },
    "PRE-0002": { displayCategory: "PREMIUM", useCategory: "BUSINESS", topChoice: true },
    "PRE-0003": { displayCategory: "PREMIUM", useCategory: "BUSINESS", topChoice: false },
    "PRE-0004": { displayCategory: "PREMIUM", useCategory: "WEDDING", topChoice: true },
    "PRE-0005": { displayCategory: "PREMIUM", useCategory: "WEDDING", topChoice: true },
    "PRE-0006": { displayCategory: "PREMIUM", useCategory: "BUSINESS", topChoice: false },
    "PRE-0007": { displayCategory: "PREMIUM", useCategory: "BUSINESS", topChoice: true },
    "PRE-0008": { displayCategory: "PREMIUM", useCategory: "WEDDING", topChoice: true },
    "PRE-0009": { displayCategory: "PREMIUM", useCategory: "BUSINESS", topChoice: true }
};

function setupFilterControls() {
    const modeTabs = document.querySelectorAll(".filter-mode-tab");
    const typeTabs = document.querySelectorAll("#typeFilterTabs .category-tab");
    const useTabs = document.querySelectorAll("#useFilterTabs .category-tab");

    const typeFilterTabs = document.getElementById("typeFilterTabs");
    const useFilterTabs = document.getElementById("useFilterTabs");
    const loadMoreBtn = document.getElementById("loadMoreBtn");

    modeTabs.forEach(tab => {
        tab.addEventListener("click", () => {
            modeTabs.forEach(item => item.classList.remove("active"));
            tab.classList.add("active");

            viewMode = tab.dataset.mode;
            visibleCount = 12;

            if (typeFilterTabs) {
                typeFilterTabs.hidden = viewMode !== "TYPE";
            }

            if (useFilterTabs) {
                useFilterTabs.hidden = viewMode !== "USE";
            }

            renderVehicles();
        });
    });

    typeTabs.forEach(tab => {
        tab.addEventListener("click", () => {
            typeTabs.forEach(item => item.classList.remove("active"));
            tab.classList.add("active");

            activeType = tab.dataset.type;
            visibleCount = 12;
            renderVehicles();
        });
    });

    useTabs.forEach(tab => {
        tab.addEventListener("click", () => {
            useTabs.forEach(item => item.classList.remove("active"));
            tab.classList.add("active");

            activeUse = tab.dataset.use;
            visibleCount = 12;
            renderVehicles();
        });
    });

        if (loadMoreBtn) {
            loadMoreBtn.addEventListener("click", () => {
                visibleCount += LOAD_MORE_STEP;
                renderVehicles();
            });
        }

        applyCatalogUrlFilters();
    }

    function applyCatalogUrlFilters() {
        const params = new URLSearchParams(window.location.search);

        const requestedMode = String(params.get("mode") || "").toUpperCase();
        const requestedUse = String(params.get("use") || "").toUpperCase();
        const requestedType = String(params.get("type") || "").toUpperCase();

        const validModes = ["TOP", "ALL", "TYPE", "USE"];
        const validUses = ["FAMILY", "BUSINESS", "WEDDING", "ADVENTURE", "TRANSPORT", "DAILY"];
        const validTypes = ["CAR", "SUV", "VAN", "MOTORCYCLE", "PREMIUM"];

        if (validModes.includes(requestedMode)) {
            viewMode = requestedMode;
        }

        if (viewMode === "USE" && validUses.includes(requestedUse)) {
            activeUse = requestedUse;
        }

        if (viewMode === "TYPE" && validTypes.includes(requestedType)) {
            activeType = requestedType;
        }

        visibleCount = 12;
        syncFilterUi();
    }

    function syncFilterUi() {
        const modeTabs = document.querySelectorAll(".filter-mode-tab");
        const typeTabs = document.querySelectorAll("#typeFilterTabs .category-tab");
        const useTabs = document.querySelectorAll("#useFilterTabs .category-tab");

        const typeFilterTabs = document.getElementById("typeFilterTabs");
        const useFilterTabs = document.getElementById("useFilterTabs");

        modeTabs.forEach(tab => {
            tab.classList.toggle("active", tab.dataset.mode === viewMode);
        });

        typeTabs.forEach(tab => {
            tab.classList.toggle("active", tab.dataset.type === activeType);
        });

        useTabs.forEach(tab => {
            tab.classList.toggle("active", tab.dataset.use === activeUse);
        });

        if (typeFilterTabs) {
            typeFilterTabs.hidden = viewMode !== "TYPE";
        }

        if (useFilterTabs) {
            useFilterTabs.hidden = viewMode !== "USE";
        }
    }

async function loadCatalog() {
    const grid = document.getElementById("catalogGrid");

    try {
        grid.innerHTML = `
            <div class="empty-state">
                Loading vehicles...
            </div>
        `;

        const response = await fetch("/api/vehicles");

        if (!response.ok) {
            throw new Error("Could not fetch /api/vehicles");
        }

        const data = await response.json();

        allVehicles = Array.isArray(data)
            ? data
            : data.vehicles || data.data || data.content || [];

        renderVehicles();

    } catch (error) {
        console.error(error);

        grid.innerHTML = `
            <div class="empty-state">
                <h3>Could not load vehicles</h3>
                <p>Check if your backend API <strong>/api/vehicles</strong> is running.</p>
            </div>
        `;
    }
}

function renderVehicles() {
    const grid = document.getElementById("catalogGrid");
    const loadMoreWrap = document.getElementById("loadMoreWrap");

    const filteredVehicles = getFilteredVehicles();

    const vehiclesToShow = viewMode === "ALL"
        ? filteredVehicles.slice(0, visibleCount)
        : filteredVehicles;

    grid.innerHTML = "";

    if (filteredVehicles.length === 0) {
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
            ? `/images/${imageFile}`
            : "https://via.placeholder.com/900x560?text=DriveEase+Vehicle";

        const title = `${vehicle.make || ""} ${vehicle.model || ""}`.trim() || "DriveEase Vehicle";
        const type = getVehicleType(vehicle);
        const useCategory = getUseCategory(vehicle);
        const topChoice = isTopChoice(vehicle);

        const seats = vehicle.numberOfSeats || vehicle.seats || vehicle.seatCount || vehicle.capacity || getDefaultSeats(type);
        const transmission = vehicle.transmission || vehicle.transmissionType || getDefaultTransmission(type);
        const fuel = vehicle.fuel || vehicle.fuelType || "Fuel";

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
                    onerror="this.src='https://via.placeholder.com/900x560?text=DriveEase+Vehicle'"
                >
            </div>

            <div class="car-card-body">
                <h3>${escapeHtml(title)}</h3>

                <div class="car-tags">
                    <span class="car-tag">${escapeHtml(formatType(type))}</span>
                    <span class="car-tag">${escapeHtml(formatUse(useCategory))}</span>
                    ${topChoiceTag}
                </div>

                <div class="car-divider"></div>

                <div class="car-bottom">
                    <div class="car-specs">
                        <span class="spec-pill">
                            <span class="spec-icon">▣</span>
                            ${escapeHtml(formatType(type))}
                        </span>

                        <span class="spec-pill">
                            <span class="spec-icon">⚙</span>
                            ${escapeHtml(transmission)}
                        </span>

                        <span class="spec-pill">
                            <span class="spec-icon">♙</span>
                            ${escapeHtml(seats)} seats
                        </span>

                        <span class="spec-pill">
                            <span class="spec-icon">⛽</span>
                            ${escapeHtml(fuel)}
                        </span>
                    </div>

                    <div class="car-price">
                        <strong>Rs. ${escapeHtml(rate)}</strong>
                        <span>/Day</span>
                    </div>
                </div>

                <button class="book-now-btn" type="button">
                    Book Now
                </button>
            </div>
        `;

        const bookButton = card.querySelector(".book-now-btn");

        bookButton.addEventListener("click", () => {
            if (!vehicleId) {
                alert("Vehicle ID missing. Please check vehicle data.");
                return;
            }

            window.location.href = `/bookVehicle?id=${encodeURIComponent(vehicleId)}`;
        });

        grid.appendChild(card);
    });

    if (loadMoreWrap) {
        loadMoreWrap.hidden = !(viewMode === "ALL" && filteredVehicles.length > visibleCount);
    }
}
function getFilteredVehicles() {
    return allVehicles.filter(vehicle => {
        if (viewMode === "TOP") {
            return isTopChoice(vehicle);
        }

        if (viewMode === "ALL") {
            return true;
        }

        if (viewMode === "TYPE") {
            if (activeType === "PREMIUM") {
                return isPremium(vehicle);
            }

            return getVehicleType(vehicle) === activeType;
        }

        if (viewMode === "USE") {
            return getUseCategory(vehicle) === activeUse;
        }

        return true;
    });
}

function getVehicleId(vehicle) {
    return vehicle.vehicleId || vehicle.id || "";
}

function getVehicleMeta(vehicle) {
    const vehicleId = getVehicleId(vehicle);
    return vehicleMeta[vehicleId] || {};
}

function getVehicleType(vehicle) {
    const meta = getVehicleMeta(vehicle);

    if (meta.displayCategory) {
        return meta.displayCategory;
    }

    return String(
        vehicle.type ||
        vehicle.vehicleType ||
        vehicle.category ||
        ""
    ).toUpperCase();
}

function getUseCategory(vehicle) {
    const meta = getVehicleMeta(vehicle);

    if (meta.useCategory) {
        return meta.useCategory;
    }

    const type = getVehicleType(vehicle);

    if (type === "VAN") return "TRANSPORT";
    if (type === "SUV") return "FAMILY";
    if (type === "MOTORCYCLE") return "DAILY";

    return "DAILY";
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
    if (value === "MOTORCYCLE") return "Motorcycle";

    return type;
}

function getDefaultSeats(type) {
    const value = String(type).toUpperCase();

    if (value === "MOTORCYCLE") return "2";
    if (value === "VAN") return "8";
    if (value === "SUV") return "5";

    return "5";
}

function getDefaultTransmission(type) {
    const value = String(type).toUpperCase();

    if (value === "MOTORCYCLE") return "Manual";

    return "Automatic";
}

function escapeHtml(value) {
    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}