const configuredApiBase =
    document.querySelector('meta[name="api-base-url"]')?.content?.trim()
    || window.__API_BASE_URL__
    || '';
const API_URL = `${configuredApiBase.replace(/\/$/, '')}/api/decision`;

const personalCodeInput = document.getElementById('personalCode');
const loanAmountSlider  = document.getElementById('loanAmount');
const loanPeriodSlider  = document.getElementById('loanPeriod');
const amountDisplay     = document.getElementById('amountDisplay');
const periodDisplay     = document.getElementById('periodDisplay');
const submitBtn         = document.getElementById('submitBtn');
const btnText           = submitBtn.querySelector('.btn-text');
const btnSpinner        = submitBtn.querySelector('.btn-spinner');
const resultArea        = document.getElementById('resultArea');
const resultBadge       = document.getElementById('resultBadge');
const resultAmountRow   = document.getElementById('resultAmountRow');
const resultPeriodRow   = document.getElementById('resultPeriodRow');
const resultAmount      = document.getElementById('resultAmount');
const resultPeriod      = document.getElementById('resultPeriod');
const resultMessage     = document.getElementById('resultMessage');
const errorArea         = document.getElementById('errorArea');
const errorMessage      = document.getElementById('errorMessage');

loanAmountSlider.addEventListener('input', () => {
    amountDisplay.textContent = `€ ${Number(loanAmountSlider.value).toLocaleString('en')}`;
});

loanPeriodSlider.addEventListener('input', () => {
    periodDisplay.textContent = `${loanPeriodSlider.value} months`;
});

document.querySelectorAll('.chip').forEach(chip => {
    chip.addEventListener('click', () => {
        personalCodeInput.value = chip.dataset.code;
        clearResults();
    });
});

submitBtn.addEventListener('click', async () => {
    clearResults();

    const personalCode = personalCodeInput.value.trim();
    if (!personalCode) { showError('Please enter a personal code.'); return; }

    setLoading(true);

    try {
        const response = await fetch(API_URL, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                personalCode,
                loanAmount: Number(loanAmountSlider.value),
                loanPeriod: Number(loanPeriodSlider.value),
            }),
        });

        const payload = await response.text();
        let data = {};
        if (payload) {
            try {
                data = JSON.parse(payload);
            } catch (_err) {
                data = {};
            }
        }

        if (!response.ok) {
            showError(data.message || `Request failed with status ${response.status}.`);
            return;
        }

        showResult(data);
    } catch (err) {
        showError('Could not reach the backend. Make sure it is running on port 8080.');
    } finally {
        setLoading(false);
    }
});

function setLoading(on) {
    submitBtn.disabled = on;
    btnText.hidden = on;
    btnSpinner.hidden = !on;
}

function clearResults() {
    resultArea.hidden = true;
    errorArea.hidden  = true;
}

function showResult(data) {
    const isPositive = data.decision === 'POSITIVE';
    resultBadge.textContent = isPositive ? '✓ Positive Decision' : '✗ Negative Decision';
    resultBadge.className   = `result-badge ${isPositive ? 'positive' : 'negative'}`;

    if (isPositive) {
        resultAmount.textContent = `€ ${Number(data.approvedAmount).toLocaleString('en')}`;
        resultPeriod.textContent = `${data.approvedPeriod} months`;
        resultAmountRow.hidden   = false;
        resultPeriodRow.hidden   = false;
    } else {
        resultAmountRow.hidden = true;
        resultPeriodRow.hidden = true;
    }

    resultMessage.textContent = data.message;
    resultArea.hidden = false;
}

function showError(msg) {
    errorMessage.textContent = msg;
    errorArea.hidden = false;
}
