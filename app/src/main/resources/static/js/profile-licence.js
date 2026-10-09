document.addEventListener('DOMContentLoaded', () => {
    const form = document.querySelector('.licence-upload-form');
    if (!form) return;
    const inputs = [form.querySelector('#frontImage'), form.querySelector('#backImage')];
    const error = form.querySelector('.licence-upload-error');
    const maxBytes = 3 * 1024 * 1024;

    for (const input of inputs) {
        input.addEventListener('change', () => {
            const name = form.querySelector(`[data-file-name-for="${input.id}"]`);
            name.textContent = input.files?.[0]?.name || 'Choose a JPG or PNG image';
            error.hidden = true;
        });
    }

    form.addEventListener('submit', event => {
        error.hidden = true;
        for (const input of inputs) {
            const file = input.files?.[0];
            if (!file || !['image/jpeg', 'image/png'].includes(file.type) || file.size > maxBytes) {
                event.preventDefault();
                error.textContent = 'Choose a JPG or PNG image under 3 MB for both sides of your licence.';
                error.hidden = false;
                input.focus();
                return;
            }
        }
    });
});
