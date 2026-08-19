/**
 * Standardized Pagination Component for CivicConnect UI
 * Handles page numbers windowing, jump-to-page, size changes, and event callbacks
 */
class PaginationComponent {
  /**
   * @param {Object} config
   * @param {HTMLElement} config.container - Target container element for pagination controls
   * @param {HTMLElement} config.infoContainer - Target container element for info text
   * @param {Function} config.onPageChange - Callback when page changes (0-indexed)
   * @param {Function} config.onSizeChange - Callback when page size changes
   */
  constructor(config) {
    this.container = config.container;
    this.infoContainer = config.infoContainer;
    this.onPageChange = config.onPageChange || (() => {});
    this.onSizeChange = config.onSizeChange || (() => {});
    
    this.state = {
      pageNumber: 0,
      pageSize: 10,
      totalElements: 0,
      totalPages: 0,
      hasNext: false,
      hasPrevious: false
    };
  }

  /**
   * Update component with standard PaginationResponse data
   * @param {Object} paginationResponse - Standard pagination object from backend
   */
  update(paginationResponse) {
    if (!paginationResponse) return;
    this.state = {
      pageNumber: paginationResponse.pageNumber || 0,
      pageSize: paginationResponse.pageSize || 10,
      totalElements: paginationResponse.totalElements || 0,
      totalPages: paginationResponse.totalPages || 0,
      hasNext: Boolean(paginationResponse.hasNext),
      hasPrevious: Boolean(paginationResponse.hasPrevious)
    };
    this.render();
  }

  render() {
    this.renderInfo();
    this.renderControls();
  }

  renderInfo() {
    if (!this.infoContainer) return;
    const { pageNumber, pageSize, totalElements, totalPages } = this.state;
    if (totalElements === 0) {
      this.infoContainer.innerHTML = `<span>No complaints found</span>`;
      return;
    }
    const start = pageNumber * pageSize + 1;
    const end = Math.min((pageNumber + 1) * pageSize, totalElements);
    this.infoContainer.innerHTML = `
      <span>Showing <strong>${start}–${end}</strong> of <strong>${totalElements}</strong> complaints</span>
      <span style="color: var(--text-muted); margin-left: 0.5rem;">(Page ${pageNumber + 1} of ${Math.max(1, totalPages)})</span>
    `;
  }

  renderControls() {
    if (!this.container) return;
    const { pageNumber, totalPages, hasNext, hasPrevious } = this.state;

    if (totalPages <= 1) {
      this.container.innerHTML = '';
      return;
    }

    let html = `
      <button class="page-btn" id="btn-first" title="First Page" ${!hasPrevious ? 'disabled' : ''}>
        &laquo;
      </button>
      <button class="page-btn" id="btn-prev" title="Previous Page" ${!hasPrevious ? 'disabled' : ''}>
        &lsaquo;
      </button>
    `;

    // Dynamic Page Number Window (e.g. 1 2 3 ... 10)
    const pageWindow = this.getPageNumbers(pageNumber, totalPages);
    pageWindow.forEach(p => {
      if (p === '...') {
        html += `<span style="padding: 0 0.4rem; color: var(--text-muted);">...</span>`;
      } else {
        const isCurrent = p === pageNumber;
        html += `
          <button class="page-btn ${isCurrent ? 'active' : ''}" data-page="${p}">
            ${p + 1}
          </button>
        `;
      }
    });

    html += `
      <button class="page-btn" id="btn-next" title="Next Page" ${!hasNext ? 'disabled' : ''}>
        &rsaquo;
      </button>
      <button class="page-btn" id="btn-last" title="Last Page" ${!hasNext ? 'disabled' : ''}>
        &raquo;
      </button>
    `;

    this.container.innerHTML = html;
    this.attachEventListeners();
  }

  getPageNumbers(current, total) {
    if (total <= 7) {
      return Array.from({ length: total }, (_, i) => i);
    }
    const pages = [];
    pages.push(0);

    let start = Math.max(1, current - 2);
    let end = Math.min(total - 2, current + 2);

    if (start > 1) pages.push('...');
    for (let i = start; i <= end; i++) {
      pages.push(i);
    }
    if (end < total - 2) pages.push('...');

    pages.push(total - 1);
    return pages;
  }

  attachEventListeners() {
    const { pageNumber, totalPages } = this.state;

    const firstBtn = this.container.querySelector('#btn-first');
    if (firstBtn) firstBtn.onclick = () => this.onPageChange(0);

    const prevBtn = this.container.querySelector('#btn-prev');
    if (prevBtn) prevBtn.onclick = () => this.onPageChange(pageNumber - 1);

    const nextBtn = this.container.querySelector('#btn-next');
    if (nextBtn) nextBtn.onclick = () => this.onPageChange(pageNumber + 1);

    const lastBtn = this.container.querySelector('#btn-last');
    if (lastBtn) lastBtn.onclick = () => this.onPageChange(totalPages - 1);

    this.container.querySelectorAll('[data-page]').forEach(btn => {
      btn.onclick = () => {
        const targetPage = parseInt(btn.getAttribute('data-page'), 10);
        if (targetPage !== pageNumber) {
          this.onPageChange(targetPage);
        }
      };
    });
  }
}
