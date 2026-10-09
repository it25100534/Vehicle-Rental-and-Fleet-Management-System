document.addEventListener('DOMContentLoaded', () => {
    const modal = document.getElementById('licence-review-modal');
    if (!modal) return;
    const form = document.getElementById('licence-review-form');
    const reasonWrap = modal.querySelector('.licence-reject-reason');
    const reason = document.getElementById('licence-review-reason');
    const reject = modal.querySelector('.licence-review-reject');
    const confirmReject = modal.querySelector('.licence-review-confirm-reject');
    const approve = modal.querySelector('.licence-review-approve');
    const name = document.getElementById('licence-review-name');
    const username = document.getElementById('licence-review-username');
    const status = document.getElementById('licence-review-status');
    const front = document.getElementById('licence-review-front');
    const back = document.getElementById('licence-review-back');
    let trigger = null;

    const close = () => {
        modal.hidden = true;
        document.body.classList.remove('licence-review-opened');
        front.removeAttribute('src');
        back.removeAttribute('src');
        trigger?.focus();
    };

    document.querySelectorAll('.licence-review-open').forEach(button => button.addEventListener('click', () => {
        trigger = button;
        name.textContent = button.dataset.name || button.dataset.username;
        username.textContent = `@${button.dataset.username || ''}`;
        status.textContent = button.dataset.status === 'APPROVED' ? 'Verified' : button.dataset.status === 'REJECTED' ? 'Needs resubmission' : 'Awaiting review';
        front.src = button.dataset.frontUrl;
        back.src = button.dataset.backUrl;
        form.action = button.dataset.reviewUrl;
        reason.value = '';
        reason.disabled = true;
        reason.required = false;
        reasonWrap.hidden = true;
        confirmReject.hidden = true;
        const alreadyApproved = button.dataset.status === 'APPROVED';
        reject.hidden = alreadyApproved;
        approve.hidden = alreadyApproved;
        modal.hidden = false;
        document.body.classList.add('licence-review-opened');
        modal.querySelector('.licence-review-close').focus();
    }));

    reject.addEventListener('click', () => {
        reasonWrap.hidden = false;
        reason.disabled = false;
        reason.required = true;
        confirmReject.hidden = false;
        reject.hidden = true;
        approve.hidden = true;
        reason.focus();
    });

    modal.querySelectorAll('.licence-review-close, .licence-review-cancel').forEach(button => button.addEventListener('click', close));
    modal.addEventListener('click', event => { if (event.target === modal) close(); });
    document.addEventListener('keydown', event => { if (event.key === 'Escape' && !modal.hidden) close(); });
});
