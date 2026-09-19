// Run this exactly once when the page loads
window.onload = function() {
    toggleFields(); // Set up the dropdowns initially

    // Check if there is an ID in the URL
    const urlParams = new URLSearchParams(window.location.search);
    const vehicleId = urlParams.get('id');

    if (vehicleId) {
        console.log("Update Mode detected for ID:", vehicleId);
        loadVehicleData(vehicleId);
    }
};

function toggleFields() {
    const type = document.getElementById("type").value;
    const seatsGroup = document.getElementById("seatsGroup");
    const driveGroup = document.getElementById("driveTrainGroup");
    const motoGroup = document.getElementById("motoGroup");

    if (type === "MOTORCYCLE") {
        seatsGroup.style.display = "none";
        driveGroup.style.display = "none";
        motoGroup.style.display = "block";
    } else if (type === "CAR") {
        seatsGroup.style.display = "block";
        driveGroup.style.display = "none";
        motoGroup.style.display = "none";
    } else {
        seatsGroup.style.display = "block";
        driveGroup.style.display = "block";
        motoGroup.style.display = "none";
    }
}

async function loadVehicleData(id) {
    try {
        const response = await fetch(`/api/vehicles/${id}`);
        if (!response.ok) {
            alert("Vehicle not found!");
            return;
        }

        const v = await response.json();

        // Fill the input fields with the data from the backend
        document.getElementById("formTitle").innerText = "Update Vehicle Details";
        document.getElementById("make").value = v.make;
        document.getElementById("model").value = v.model;
        document.getElementById("year").value = v.year;
        document.getElementById("fuel").value = v.fuelType;
        document.getElementById("rate").value = v.rentalRate;
        document.getElementById("mileage").value = v.mileage;
        document.getElementById("type").value = v.type;

        // Prevent changing the type during an update
        document.getElementById("type").disabled = true;

        // Show/Fill fields specific to the vehicle type
        toggleFields();
        if (v.type === "MOTORCYCLE") {
            document.getElementById("motoType").value = v.motorcycleType || v.motorcycleType; // Catch whichever name Jackson uses
        } else {
            document.getElementById("seats").value = v.numberOfSeats;
            if (v.type !== "CAR") {
                document.getElementById("driveTrain").value = v.driveTrain;
            }
        }

        // Save the ID so we know we are updating when we hit Save
        document.getElementById("vehicleForm").dataset.editId = id;

        // Optional: Let the user know they don't HAVE to upload a new image
        document.getElementById("imageUpload").required = false;

    } catch (error) {
        console.error("Error fetching vehicle details:", error);
    }
}

async function submitVehicleForm() {
    const fileInput = document.getElementById('imageUpload');
    const formElement = document.getElementById("vehicleForm");
    const isUpdate = formElement.dataset.editId !== undefined;

    // Require image only if it's a NEW vehicle
    if (!isUpdate && !fileInput.files[0]) {
        alert("Please select a vehicle image.");
        return;
    }

    const formData = new FormData();
    const type = document.getElementById("type").value;

    formData.append("type", type);
    formData.append("make", document.getElementById("make").value);
    formData.append("model", document.getElementById("model").value);
    formData.append("year", document.getElementById("year").value);
    formData.append("fuel", document.getElementById("fuel").value);
    formData.append("rate", document.getElementById("rate").value);
    formData.append("mileage", document.getElementById("mileage").value);

    // Add the image if they uploaded one (required for new, optional for update)
    if (fileInput.files[0]) {
        formData.append("image", fileInput.files[0]);
    }

    if (type === "MOTORCYCLE") {
        formData.append("motoType", document.getElementById("motoType").value);
    } else {
        formData.append("seats", document.getElementById("seats").value);
        if (type !== "CAR") {
            formData.append("driveTrain", document.getElementById("driveTrain").value);
        }
    }

    // --- STEP 3: THE UPDATE ROUTE ---
    let endpoint = '/api/vehicles/add';
    if (isUpdate) {
        // If we are updating, attach the ID to the form data
        formData.append("id", formElement.dataset.editId);
        // We will send it to a special update endpoint we will build next
        endpoint = '/api/vehicles/update';
    }

    try {
        const response = await fetch(endpoint, {
            method: 'POST', // POST works fine for forms with files
            body: formData
        });

        if (response.ok) {
            alert(isUpdate ? "Vehicle updated successfully!" : "Vehicle added successfully!");
            window.location.href = 'inventory';
        } else {
            const errorMsg = await response.text();
            alert("Failed to save: " + errorMsg);
        }
    } catch (error) {
        console.error("Error during save:", error);
    }
}
