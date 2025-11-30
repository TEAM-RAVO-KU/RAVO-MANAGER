function updateSystemEvents(events) {
    const container = document.getElementById('system-events');
    container.innerHTML = '';

    if (!events || events.length === 0) {
        container.innerHTML = '<div class="no-events">현재 이벤트가 없습니다.</div>';
        return;
    }

    events.forEach(event => {
        container.appendChild(createSystemEventElement(event));
    });
}

function createSystemEventElement(event) {

    const severityClass = event.severity?.toLowerCase() || "info";
    const typeClass = event.eventType?.toLowerCase() || "system";
    const typeText = convertTypeBadgeText(event.eventType);

    const div = document.createElement('div');
    div.className = `system-event ${severityClass}`;

    div.innerHTML = `
        <div class="event-icon ${severityClass}"></div>
        <div class="event-content">
            <div class="event-title">${event.title}</div>
            ${event.description ? `<div class="event-description">${event.description}</div>` : ''}
            <div class="event-timestamp">${formatTimestamp(event.timestamp)}</div>
        </div>
        <span class="event-type-badge ${typeClass}">
            ${typeText}
        </span>
    `;

    return div;
}


function formatTimestamp(ts) {
    if (!ts) return '-';
    return new Date(ts).toLocaleString('ko-KR');
}

function convertTypeBadgeText(type) {
    const map = {
        SYNC: '동기화',
        CONNECTION: '연결',
        PERFORMANCE: '성능',
        RECOVERY: '복구',
        BACKUP: '백업'
    };

    return map[type] || '시스템';
}
