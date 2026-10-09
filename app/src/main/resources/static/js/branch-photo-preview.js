(() => {
    const input = document.getElementById('image');
    const preview = document.getElementById('branchPhotoPreview');
    if (!input || !preview) return;
    const original = preview.getAttribute('src');
    let url;
    input.addEventListener('change', () => {
        if (url) URL.revokeObjectURL(url);
        const file = input.files[0];
        input.setCustomValidity('');
        if (file && (!['image/jpeg', 'image/png'].includes(file.type) || file.size > 5 * 1024 * 1024)) {
            input.setCustomValidity('Choose a JPEG or PNG image up to 5 MB.');
            input.reportValidity();
            return;
        }
        url = file ? URL.createObjectURL(file) : original;
        preview.hidden = !url;
        preview.style.display = url ? 'block' : 'none';
        if (url) preview.src = url;
    });
})();
