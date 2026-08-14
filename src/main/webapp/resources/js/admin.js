'use strict';

(function () {
    const SIDEBAR_STORAGE_KEY = 'spendolive.admin.sidebar.collapsed';
    const BOARD_AREA_ID = 'adminBoardArea';
    const BOARD_URL_PATTERNS = ['/admin/inquiry/list.do', '/admin/faq/list.do'];

    function appShell() {
        return document.getElementById('adminAppShell');
    }

    function pageRoot() {
        return document.querySelector('.admin-main[data-admin-page]');
    }

    function currentPage() {
        const root = pageRoot();
        return root ? root.dataset.adminPage : '';
    }

    function setSidebarCollapsed(collapsed) {
        const shell = appShell();
        if (!shell) return;
        shell.classList.toggle('is-sidebar-collapsed', collapsed);
        const toggle = document.querySelector('[data-admin-sidebar-toggle]');
        if (toggle) {
            toggle.setAttribute('aria-expanded', String(!collapsed));
            toggle.setAttribute('aria-label', collapsed ? '사이드바 펼치기' : '사이드바 접기');
        }
        try {
            localStorage.setItem(SIDEBAR_STORAGE_KEY, String(collapsed));
        } catch (ignore) {
            // 저장소를 사용할 수 없는 환경에서는 현재 화면에서만 상태를 유지
        }
    }

    function closeMobileSidebar() {
        const shell = appShell();
        if (!shell) return;
        shell.classList.remove('is-mobile-sidebar-open');
    }

    function toggleMobileSidebar() {
        const shell = appShell();
        if (!shell) return;
        shell.classList.toggle('is-mobile-sidebar-open');
    }

    function setSubmenu(groupName, open) {
        const group = document.querySelector('[data-admin-group="' + groupName + '"]');
        if (!group) return;
        group.classList.toggle('is-open', open);
        const toggle = group.querySelector('[data-admin-submenu-toggle]');
        if (toggle) toggle.setAttribute('aria-expanded', String(open));
    }

    function initSidebar() {
        const shell = appShell();
        if (!shell) return;

        const page = currentPage();
        document.querySelectorAll('[data-admin-nav]').forEach(function (link) {
            const matchesCustomer = link.dataset.adminNav === 'inquiry' && ['inquiry', 'faq', 'notice'].includes(page);
            link.classList.toggle('active', link.dataset.adminNav === page || matchesCustomer);
        });
        document.querySelectorAll('[data-admin-nav-child]').forEach(function (link) {
            link.classList.toggle('active', link.dataset.adminNavChild === page);
        });

        const groupName = ['inquiry', 'faq', 'notice'].includes(page) ? 'customer' : page;
        if (groupName) setSubmenu(groupName, true);

        let collapsed = false;
        try {
            collapsed = localStorage.getItem(SIDEBAR_STORAGE_KEY) === 'true';
        } catch (ignore) {
            collapsed = false;
        }
        if (!window.matchMedia('(max-width: 900px)').matches) {
            setSidebarCollapsed(collapsed);
        }

        updateSidebarSubmenuActive();
    }

    function updateSidebarSubmenuActive() {
        const hash = window.location.hash.replace('#', '');
        const currentPath = window.location.pathname;
        document.querySelectorAll('.admin-sidebar-submenu a').forEach(function (link) {
            const linkUrl = new URL(link.href, window.location.origin);
            const samePath = linkUrl.pathname === currentPath;
            const sameHash = linkUrl.hash ? linkUrl.hash.replace('#', '') === hash : !hash;
            const pageMatch = link.dataset.adminNavChild === currentPage();
            link.classList.toggle('active', pageMatch || (samePath && sameHash));
        });
    }

    function showSection(sectionName, updateHash, animate) {
        const root = pageRoot();
        if (!root) return false;
        const selected = root.querySelector('[data-admin-section="' + sectionName + '"]');
        if (!selected) return false;

        root.querySelectorAll('[data-admin-section]').forEach(function (section) {
            const active = section === selected;
            section.classList.remove('admin-motion-block', 'admin-motion-section');
            section.style.removeProperty('--admin-motion-delay');
            section.hidden = !active;
            section.classList.toggle('is-active', active);
        });
        root.querySelectorAll('[data-admin-section-target]').forEach(function (button) {
            button.classList.toggle('active', button.dataset.adminSectionTarget === sectionName);
        });

        if (updateHash) {
            history.replaceState(null, '', window.location.pathname + window.location.search + '#' + sectionName);
        }
        if (animate) replayMotion(selected, 'admin-motion-section');
        updateSidebarSubmenuActive();
        return true;
    }

    function initSections() {
        const root = pageRoot();
        if (!root) return;
        const sections = root.querySelectorAll('[data-admin-section]');
        if (!sections.length) return;

        const hash = window.location.hash.replace('#', '');
        const defaultSection = root.dataset.adminDefaultSection || sections[0].dataset.adminSection;
        showSection(root.querySelector('[data-admin-section="' + hash + '"]') ? hash : defaultSection, false, false);
    }

    function rowMatchesFilter(row, filter) {
        if (!filter || filter === 'all') return true;
        const status = (row.dataset.rowStatus || '').toLowerCase().trim();
        const warningCount = Number(row.dataset.warningCount || 0);
        if (filter === 'active') return status === 'active';
        if (filter === 'warning') return warningCount > 0 || ['warning', 'blocked', 'suspended'].includes(status);
        if (filter === 'leave') return ['leave', 'withdrawn', 'deleted'].includes(status);
        if (filter === 'wait') return ['wait', 'ready', 'unpaid', 'yet'].includes(status);
        if (filter === 'complete') return ['complete', 'done', 'paid', 'released', 'refunded'].includes(status);
        return status === filter;
    }

    function applyTableView(table, animate) {
        if (!table) return;
        const filter = table.dataset.currentFilter || 'all';
        const search = table.dataset.searchInput ? document.getElementById(table.dataset.searchInput) : null;
        const keyword = search ? search.value.trim().toLowerCase() : '';
        let visibleCount = 0;

        table.querySelectorAll('tbody tr[data-row-status]').forEach(function (row) {
            const visible = rowMatchesFilter(row, filter) && (!keyword || row.textContent.toLowerCase().includes(keyword));
            row.hidden = !visible;
            if (visible) visibleCount += 1;
        });

        if (table.dataset.emptyTarget) {
            const empty = document.querySelector(table.dataset.emptyTarget);
            if (empty) empty.hidden = visibleCount > 0;
        }
        if (table.dataset.countTarget) {
            const count = document.querySelector(table.dataset.countTarget);
            if (count) count.textContent = String(visibleCount);
        }
        const wrap = table.closest('.table-wrap');
        if (animate && wrap) replayMotion(wrap, 'admin-motion-table');
    }

    function applyHashFilter(animate) {
        const hash = window.location.hash.replace('#', '') || 'all';
        const button = document.querySelector('[data-admin-row-filter="' + hash + '"]');
        if (!button) return;
        const table = document.querySelector(button.dataset.filterTarget);
        if (!table) return;
        table.dataset.currentFilter = hash;
        document.querySelectorAll('[data-admin-row-filter][data-filter-target="' + button.dataset.filterTarget + '"]').forEach(function (item) {
            item.classList.toggle('active', item === button);
        });
        applyTableView(table, animate);
    }

    function initTableFilters() {
        document.querySelectorAll('table[data-admin-filter-table]').forEach(function (table) {
            table.dataset.currentFilter = 'all';
            applyTableView(table, false);
        });
        applyHashFilter(false);
    }

    function replayMotion(element, className) {
        if (!element || window.matchMedia('(prefers-reduced-motion: reduce)').matches) return;

        // 숨겨져 있던 탭에 최초 진입 애니메이션 클래스가 남아 있으면
        // 탭 전환 애니메이션과 겹쳐 두 번 깜빡이는 것처럼 보일 수 있다
        element.classList.remove('admin-motion-block', 'admin-motion-section', 'admin-motion-table', 'admin-motion-panel');
        element.style.removeProperty('--admin-motion-delay');
        void element.offsetWidth;
        element.classList.add(className);
        element.addEventListener('animationend', function cleanup() {
            element.classList.remove(className);
            element.removeEventListener('animationend', cleanup);
        }, { once: true });
    }

    function animateInitialBlocks() {
        const root = pageRoot();
        if (!root || window.matchMedia('(prefers-reduced-motion: reduce)').matches) return;
        const blocks = Array.from(root.children).filter(function (element) {
            // hidden 상태의 탭에는 최초 애니메이션을 걸지 않는다
            // 숨겨진 탭이 나중에 열릴 때 section 애니메이션과 중복되는 문제를 막는다
            return !element.hidden && element.matches('.hero,.stat-grid,.content-grid,.panel,.admin-local-tabs,.admin-page-section,#adminBoardArea,.flash-ok,.flash-err');
        }).slice(0, 8);
        blocks.forEach(function (element, index) {
            element.style.setProperty('--admin-motion-delay', String(index * 35) + 'ms');
            element.classList.add('admin-motion-block');
        });
    }

    function showToast(message) {
        const toast = document.querySelector('.toast');
        if (!toast || !message) return;
        toast.textContent = message;
        toast.classList.add('show');
        window.clearTimeout(window.__adminToastTimer);
        window.__adminToastTimer = window.setTimeout(function () {
            toast.classList.remove('show');
        }, 1800);
    }

    function isBoardLink(href) {
        return href && BOARD_URL_PATTERNS.some(function (pattern) { return href.includes(pattern); });
    }

    function swapBoardArea(html, pushUrl) {
        const documentFromResponse = new DOMParser().parseFromString(html, 'text/html');
        const nextArea = documentFromResponse.getElementById(BOARD_AREA_ID);
        const currentArea = document.getElementById(BOARD_AREA_ID);
        if (!nextArea || !currentArea) {
            window.location.href = pushUrl;
            return;
        }
        currentArea.innerHTML = nextArea.innerHTML;
        replayMotion(currentArea, 'admin-motion-section');

        // #adminBoardArea 안쪽만 교체되고 바깥의 .admin-main[data-admin-page]는 그대로 남아있어서,
        // 문의관리 ↔ FAQ관리 탭 전환 후에도 사이드바 활성 표시가 안 바뀌는 문제가 있었음.
        // 새로 받아온 응답에서 data-admin-page 값을 읽어와 현재 .admin-main에도 반영한다.
        const nextRoot = documentFromResponse.querySelector('.admin-main[data-admin-page]');
        const currentRoot = document.querySelector('.admin-main[data-admin-page]');
        if (nextRoot && currentRoot) {
            currentRoot.dataset.adminPage = nextRoot.dataset.adminPage;
        }

        // pushState로 주소를 먼저 갱신한 뒤 initSidebar()를 호출해야 함.
        // updateSidebarSubmenuActive()가 window.location.pathname을 읽어서 "같은 주소인지"도
        // 같이 판단하는데, 순서가 바뀌면 아직 안 바뀐 옛 주소 때문에 이전 메뉴도 활성으로
        // 착각해서 두 메뉴가 동시에 활성 표시되는 문제가 있었음.
        if (pushUrl) history.pushState({ boardUrl: pushUrl }, '', pushUrl);
        initSidebar();
    }

    function loadBoard(url, push) {
        fetch(url, { credentials: 'same-origin' })
            .then(function (response) {
                if (!response.ok) throw new Error('board fetch failed');
                return response.text();
            })
            .then(function (html) { swapBoardArea(html, push ? url : null); })
            .catch(function () { window.location.href = url; });
    }

    function openReportProcess(button) {
        const panel = document.getElementById('commentArea');
        if (!panel) return;
        const reason = document.getElementById('selectedReportReason');
        const reportId = document.getElementById('formReportId');
        const memberId = document.getElementById('formReportMemberId');
        if (reportId) reportId.value = button.dataset.reportId || '';
        if (memberId) memberId.value = button.dataset.reportedMemberId || '';
        if (reason) reason.textContent = '선택한 신고 사유: ' + (button.dataset.reportReason || '');
        panel.hidden = false;
        replayMotion(panel, 'admin-motion-panel');
        panel.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }

    document.addEventListener('click', function (event) {
        const sidebarToggle = event.target.closest('[data-admin-sidebar-toggle]');
        if (sidebarToggle) {
            const shell = appShell();
            setSidebarCollapsed(!shell.classList.contains('is-sidebar-collapsed'));
            return;
        }

        if (event.target.closest('[data-admin-mobile-menu]')) {
            toggleMobileSidebar();
            return;
        }

        if (event.target.id === 'adminSidebarBackdrop') {
            closeMobileSidebar();
            return;
        }

        const submenuToggle = event.target.closest('[data-admin-submenu-toggle]');
        if (submenuToggle) {
            const groupName = submenuToggle.dataset.adminSubmenuToggle;
            const group = document.querySelector('[data-admin-group="' + groupName + '"]');
            setSubmenu(groupName, !group.classList.contains('is-open'));
            return;
        }

        const sectionButton = event.target.closest('[data-admin-section-target]');
        if (sectionButton && pageRoot()) {
            event.preventDefault();
            showSection(sectionButton.dataset.adminSectionTarget, true, true);
            return;
        }

        const filterButton = event.target.closest('[data-admin-row-filter]');
        if (filterButton) {
            const table = document.querySelector(filterButton.dataset.filterTarget);
            if (!table) return;
            table.dataset.currentFilter = filterButton.dataset.adminRowFilter;
            document.querySelectorAll('[data-admin-row-filter][data-filter-target="' + filterButton.dataset.filterTarget + '"]').forEach(function (item) {
                item.classList.toggle('active', item === filterButton);
            });
            history.replaceState(null, '', window.location.pathname + window.location.search + '#' + filterButton.dataset.adminRowFilter);
            applyTableView(table, true);
            updateSidebarSubmenuActive();
            return;
        }

        const reportButton = event.target.closest('[data-report-process]');
        if (reportButton) {
            openReportProcess(reportButton);
            return;
        }

        if (event.target.closest('[data-report-process-cancel]')) {
            const panel = document.getElementById('commentArea');
            if (panel) panel.hidden = true;
            return;
        }

        const toastButton = event.target.closest('[data-toast]');
        if (toastButton) {
            showToast(toastButton.dataset.toast);
            return;
        }

        // 기존 프로젝트에 이미 있던 문의관리 ↔ FAQ관리 목록 AJAX만 유지
        const boardArea = document.getElementById(BOARD_AREA_ID);
        const link = event.target.closest('a');
        if (boardArea && link && boardArea.contains(link) && isBoardLink(link.getAttribute('href'))) {
            event.preventDefault();
            loadBoard(link.href, true);
        }
    });

    document.addEventListener('input', function (event) {
        const input = event.target.closest('[data-search-table]');
        if (!input) return;
        const table = document.querySelector(input.dataset.searchTable);
        if (!table) return;
        window.clearTimeout(input.__adminSearchTimer);
        input.__adminSearchTimer = window.setTimeout(function () {
            applyTableView(table, false);
        }, 90);
    });

    window.addEventListener('hashchange', function () {
        if (!showSection(window.location.hash.replace('#', ''), false, true)) applyHashFilter(true);
        updateSidebarSubmenuActive();
    });

    window.addEventListener('popstate', function (event) {
        if (event.state && event.state.boardUrl) loadBoard(event.state.boardUrl, false);
    });

    document.addEventListener('DOMContentLoaded', function () {
        initSidebar();
        initSections();
        initTableFilters();
        animateInitialBlocks();
    });

    window.bindCheckboxAsHidden = function (formId, checkboxId, hiddenName) {
        const form = document.getElementById(formId);
        const checkbox = document.getElementById(checkboxId);
        if (!form || !checkbox) return;
        form.addEventListener('submit', function () {
            checkbox.name = '';
            let hidden = form.querySelector('input[type="hidden"][name="' + hiddenName + '"]');
            if (!hidden) {
                hidden = document.createElement('input');
                hidden.type = 'hidden';
                hidden.name = hiddenName;
                form.appendChild(hidden);
            }
            hidden.value = checkbox.checked ? 'Y' : 'N';
        });
    };

    window.openAdminInquiryModal = function (inquiryId) {
        const template = document.getElementById('adminInqDetailTpl' + inquiryId);
        const body = document.getElementById('adminInqDetailBody');
        const modal = document.getElementById('adminInqDetailModal');
        if (!template || !body || !modal) return;
        body.innerHTML = template.innerHTML;
        modal.classList.add('show');
    };

    window.closeAdminInquiryModal = function () {
        const modal = document.getElementById('adminInqDetailModal');
        if (modal) modal.classList.remove('show');
    };
})();