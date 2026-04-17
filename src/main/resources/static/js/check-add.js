function isUPCAlreadySelected(upc, currentRow) {
    const selects = document.querySelectorAll('#salesRows select[name="upc"]');
    for (let select of selects) {
        if (select !== currentRow && select.value === upc && upc !== "") {
            return true;
        }
    }
    return false;
}

function updateStockInfo(selectEl) {
    const selectedOption = selectEl.options[selectEl.selectedIndex];
    const stock = selectedOption.getAttribute('data-stock') || 0;
    const stockSpan = selectEl.closest('td').querySelector('.stock-info');
    if (stockSpan) {
        stockSpan.textContent = `In stock: ${stock}`;
        stockSpan.style.color = stock > 0 ? 'var(--text-light)' : 'var(--primary-dark)';
    }
    const row = selectEl.closest('tr');
    const qtyInput = row.querySelector('.quantity-input');
    validateQuantity(qtyInput, stock);
}

function validateQuantity(qtyInput, maxStock) {
    const val = parseInt(qtyInput.value);
    if (isNaN(val)) return;
    if (val > maxStock) {
        qtyInput.setCustomValidity(`Only ${maxStock} available`);
        qtyInput.style.borderColor = 'var(--primary-dark)';
    } else {
        qtyInput.setCustomValidity('');
        qtyInput.style.borderColor = '';
    }
}

function addRow() {
    const tbody = document.getElementById('salesRows');
    const firstSelect = tbody.querySelector('select[name="upc"]');
    if (!firstSelect) return;
    const optionsHTML = Array.from(firstSelect.options).map(opt => opt.outerHTML).join('');
    const newRow = document.createElement('tr');
    newRow.innerHTML = `
            <td>
                <div class="select-wrapper">
                    <select name="upc" class="category-select form-control product-select" style="min-width: 220px;" required>
                        <option value="">Select product</option>
                        ${optionsHTML}
                    </select>
                    <span class="select-arrow">&#8964;</span>
                </div>
                <span class="stock-info"></span>
            </td>
            <td><input type="number" name="quantity" class="form-control quantity-input" min="1" value="1" required></td>
            <td><button type="button" class="remove-row-btn" onclick="removeRow(this)">✖</button></td>
        `;
    tbody.appendChild(newRow);
    const newSelect = newRow.querySelector('select[name="upc"]');
    const newQty = newRow.querySelector('.quantity-input');
    newSelect.addEventListener('change', function() {
        if (isUPCAlreadySelected(this.value, this)) {
            alert("This product is already added. Please change quantity in the existing row.");
            this.value = "";
            updateStockInfo(this);
            return;
        }
        updateStockInfo(this);
    });
    newQty.addEventListener('input', function() {
        const select = this.closest('tr').querySelector('select[name="upc"]');
        const selectedOption = select.options[select.selectedIndex];
        const stock = selectedOption.getAttribute('data-stock') || 0;
        validateQuantity(this, stock);
    });
    updateStockInfo(newSelect);
}

function removeRow(btn) {
    const row = btn.closest('tr');
    if (document.querySelectorAll('#salesRows tr').length > 1) {
        row.remove();
    } else {
        alert("You must keep at least one product row");
    }
}

document.addEventListener('DOMContentLoaded', function() {
    const selects = document.querySelectorAll('#salesRows select[name="upc"]');
    selects.forEach(select => {
        select.addEventListener('change', function() {
            if (isUPCAlreadySelected(this.value, this)) {
                alert("This product is already added. Please change quantity in the existing row.");
                this.value = "";
                updateStockInfo(this);
                return;
            }
            updateStockInfo(this);
        });
        updateStockInfo(select);
    });
    const quantities = document.querySelectorAll('#salesRows .quantity-input');
    quantities.forEach(qty => {
        qty.addEventListener('input', function() {
            const select = this.closest('tr').querySelector('select[name="upc"]');
            const selectedOption = select.options[select.selectedIndex];
            const stock = selectedOption.getAttribute('data-stock') || 0;
            validateQuantity(this, stock);
        });
    });
});