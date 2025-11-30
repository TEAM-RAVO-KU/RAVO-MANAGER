const initialData = /*[[${initialData}]]*/ {};
let replicationChart = null;
let readChart = null;
let miniCharts = {};
let lastMetricsData = {
    active: {timestamp: 0, metrics: {}},
    standby: {timestamp: 0, metrics: {}}
};

document.addEventListener('DOMContentLoaded', () => {
    console.log('=== Dashboard Initialized ===');
    console.log('Initial data:', initialData);

    if (initialData) {
        updateDashboard(initialData);
    }
    initMiniCharts();
    initReadChart();
    initReplicationChart();

    // 즉시 한 번 실행
    fetchAndUpdate();

    // 3초마다 반복 - Dashboard 데이터
    setInterval(() => {
        console.log('=== Fetching dashboard data at', new Date().toLocaleTimeString(), '===');
        fetchAndUpdate();
    }, 3000);

    // 3초마다 반복 - 메트릭 데이터 (상세 정보용)
    setInterval(() => {
        console.log('=== Fetching metrics data at', new Date().toLocaleTimeString(), '===');
        fetchMetricsData();
    }, 3000);
});

async function fetchAndUpdate() {
    try {
        console.log('Fetching from /api/dashboard');
        const response = await fetch('/api/dashboard');
        console.log('Response status:', response.status, response.ok);

        if (!response.ok) {
            console.error('Response not OK:', response.status);
            return;
        }

        const data = await response.json();
        console.log('Received data:', data);
        updateDashboard(data);
        console.log('Dashboard updated successfully');
    } catch (error) {
        console.error('Failed to fetch dashboard data:', error);
    }
}

async function fetchMetricsData() {
    try {
        console.log('Fetching from /api/metrics');
        const response = await fetch('/api/metrics');

        if (!response.ok) {
            console.error('Metrics response not OK:', response.status);
            return;
        }

        const metricsData = await response.json();
        console.log('Received metrics data:', metricsData);

        // Active DB 상세 정보 업데이트
        if (metricsData.active) {
            updateDatabaseDetails('active', metricsData.active);
            updateMiniCharts('active', metricsData.active);
        }

        // Standby DB 상세 정보 업데이트
        if (metricsData.standby) {
            updateDatabaseDetails('standby', metricsData.standby);
            updateMiniCharts('standby', metricsData.standby);
        }

        console.log('Metrics updated successfully');
    } catch (error) {
        console.error('Failed to fetch metrics data:', error);
    }
}

function initMiniCharts() {
    const CHART_DATA_POINTS = 30;
    const commonLineOptions = {
        responsive: true,
        maintainAspectRatio: false,
        animation: false,
        plugins: {
            legend: {display: true, position: 'top', labels: {font: {size: 9}, usePointStyle: true, padding: 5}},
            tooltip: {enabled: true}
        },
        scales: {
            x: {display: false},
            y: {
                beginAtZero: true,
                ticks: {color: '#94a3b8', font: {size: 9}, precision: 0},
                grid: {color: 'rgba(226, 232, 240, 0.3)', drawBorder: false}
            }
        }
    };

    const commonDoughnutOptions = {
        responsive: true,
        maintainAspectRatio: false,
        animation: false,
        plugins: {
            legend: {display: true, position: 'bottom', labels: {font: {size: 8}, padding: 3}}
        }
    };

    ['active', 'standby'].forEach(type => {
        // CPU Chart
        const cpuCtx = document.getElementById(`${type}-cpu-mini-chart`).getContext('2d');
        miniCharts[`${type}-cpu`] = new Chart(cpuCtx, {
            type: 'line',
            data: {
                labels: Array(CHART_DATA_POINTS).fill(''),
                datasets: [{
                    label: 'cores/sec',
                    data: Array(CHART_DATA_POINTS).fill(0),
                    borderColor: '#8b5cf6',
                    backgroundColor: 'rgba(139, 92, 246, 0.1)',
                    fill: true,
                    tension: 0.3,
                    pointRadius: 0
                }]
            },
            options: {
                ...commonLineOptions,
                scales: {
                    x: {display: false},
                    y: {
                        beginAtZero: true,
                        ticks: {
                            color: '#94a3b8',
                            font: {size: 9},
                            precision: 3,
                            callback: function (value) {
                                return value.toFixed(3);
                            }
                        },
                        grid: {color: 'rgba(226, 232, 240, 0.3)', drawBorder: false},
                        afterDataLimits: (scale) => {
                            // 데이터의 최대값 찾기
                            const maxValue = Math.max(...scale.chart.data.datasets[0].data);

                            if (maxValue > 0 && maxValue < 0.1) {
                                // 값이 0.1보다 작으면 Y축 최대값을 동적 조정
                                scale.max = Math.max(0.1, maxValue * 1.5);
                            } else if (maxValue >= 0.1 && maxValue < 1) {
                                // 0.1~1 사이면 여유있게 설정
                                scale.max = Math.ceil(maxValue * 1.3 * 10) / 10;
                            }
                            // 1 이상이면 자동 스케일링 사용
                        }
                    }
                }
            }
        });

        // Network Chart
        const networkCtx = document.getElementById(`${type}-network-mini-chart`).getContext('2d');
        miniCharts[`${type}-network`] = new Chart(networkCtx, {
            type: 'line',
            data: {
                labels: Array(CHART_DATA_POINTS).fill(''),
                datasets: [
                    {
                        label: '전송',
                        data: Array(CHART_DATA_POINTS).fill(0),
                        borderColor: '#ef4444',
                        backgroundColor: 'rgba(239, 68, 68, 0.1)',
                        fill: true,
                        tension: 0.3,
                        pointRadius: 0
                    },
                    {
                        label: '수신',
                        data: Array(CHART_DATA_POINTS).fill(0),
                        borderColor: '#3b82f6',
                        backgroundColor: 'rgba(59, 130, 246, 0.1)',
                        fill: true,
                        tension: 0.3,
                        pointRadius: 0
                    }
                ]
            },
            options: commonLineOptions
        });

        // Memory Chart (Doughnut)
        const memoryCtx = document.getElementById(`${type}-memory-mini-chart`).getContext('2d');
        miniCharts[`${type}-memory`] = new Chart(memoryCtx, {
            type: 'doughnut',
            data: {
                labels: ['사용 중', '여유'],
                datasets: [{
                    data: [0, 1],
                    backgroundColor: ['#f59e0b', '#e5e7eb']
                }]
            },
            options: commonDoughnutOptions
        });
    });

    console.log('Mini charts initialized');
}

function calculateRate(type, key, currentValue) {
    const lastData = lastMetricsData[type];
    if (!lastData || !lastData.metrics || !lastData.timestamp) {
        console.log(`[${type}] No previous data for ${key}, initializing...`);
        return 0;
    }

    const lastValue = lastData.metrics[key] || 0;
    const timeDelta = (Date.now() - lastData.timestamp) / 1000;

    console.log(`[${type}] ${key} - Current: ${currentValue}, Last: ${lastValue}, TimeDelta: ${timeDelta}s`);

    if (timeDelta <= 0) {
        console.warn(`[${type}] Invalid time delta: ${timeDelta}`);
        return 0;
    }

    if (currentValue >= lastValue) {
        const rate = (currentValue - lastValue) / timeDelta;
        console.log(`[${type}] ${key} rate: ${rate}`);
        return rate;
    }

    console.warn(`[${type}] Current value (${currentValue}) < Last value (${lastValue}), returning 0`);
    return 0;
}

function updateTimeSeriesChart(chart, newValues) {
    chart.data.labels.push('');
    chart.data.labels.shift();
    chart.data.datasets.forEach((dataset, i) => {
        dataset.data.push(newValues[i] ?? 0);
        dataset.data.shift();
    });
    chart.update('none');
}

function updateMiniCharts(type, metricData) {
    const metrics = metricData.metrics || {};

    console.log(`[${type}] CPU metric:`, metrics.process_cpu_seconds_total);
    console.log(`[${type}] Bytes sent:`, metrics.mysql_global_status_bytes_sent);
    console.log(`[${type}] Bytes received:`, metrics.mysql_global_status_bytes_received);
    console.log(`[${type}] Heap in use:`, metrics.go_memstats_heap_inuse_bytes);
    console.log(`[${type}] Heap sys:`, metrics.go_memstats_heap_sys_bytes);

    // CPU 업데이트
    const cpuRate = calculateRate(type, 'process_cpu_seconds_total', metrics.process_cpu_seconds_total || 0);
    console.log(`[${type}] Calculated CPU rate:`, cpuRate);
    updateTimeSeriesChart(miniCharts[`${type}-cpu`], [cpuRate]);

    // Network 업데이트
    const bytesSent = calculateRate(type, 'mysql_global_status_bytes_sent', metrics.mysql_global_status_bytes_sent || 0);
    const bytesReceived = calculateRate(type, 'mysql_global_status_bytes_received', metrics.mysql_global_status_bytes_received || 0);
    console.log(`[${type}] Network rates - Sent:`, bytesSent, 'Received:', bytesReceived);
    updateTimeSeriesChart(miniCharts[`${type}-network`], [bytesSent, bytesReceived]);

    // Memory 업데이트
    const heapInUse = metrics.go_memstats_heap_inuse_bytes || 0;
    const heapSys = metrics.go_memstats_heap_sys_bytes || 0;
    const heapFree = Math.max(0, heapSys - heapInUse);
    console.log(`[${type}] Memory - InUse:`, heapInUse, 'Free:', heapFree);
    miniCharts[`${type}-memory`].data.datasets[0].data = [heapInUse, heapFree];
    miniCharts[`${type}-memory`].update('none');

    // 마지막 메트릭 데이터 저장
    lastMetricsData[type] = {timestamp: Date.now(), metrics: metrics};
}

function updateDatabaseDetails(type, metricData) {
    const metrics = metricData.metrics || {};
    const info = metricData.info || {};

    console.log(`Updating ${type} details:`, metricData);

    // QPS 계산 (실시간) - 소수점 유지
    const uptime = metrics.mysql_global_status_uptime || 1;
    const queries = metrics.mysql_global_status_queries || 0;
    const qps = queries / uptime;

    const qpsEl = document.getElementById(type + '-qps');
    if (qpsEl) {
        if (qps >= 1000) {
            // 1000 이상이면 쉼표 추가
            qpsEl.textContent = qps.toLocaleString('en-US', {
                minimumFractionDigits: 1,
                maximumFractionDigits: 1
            });
        } else if (qps >= 10) {
            // 10 이상이면 소수점 1자리
            qpsEl.textContent = qps.toFixed(1);
        } else {
            // 10 미만이면 소수점 2자리
            qpsEl.textContent = qps.toFixed(2);
        }
    }

    // Connections (더 정확한 값)
    const connections = metrics.mysql_global_status_threads_connected || 0;
    const connectionsEl = document.getElementById(type + '-connections');
    if (connectionsEl) {
        connectionsEl.textContent = Math.round(connections);
    }
}

function updateDashboard(data) {
    console.log('=== updateDashboard called ===');
    console.log('Full dashboard data:', JSON.stringify(data, null, 2));

    // Connection status 업데이트 확인
    const connectionStatus = data.isConnected ? '연결됨' : '연결 끊김';
    console.log('Setting connection status to:', connectionStatus);
    document.getElementById('connection-status').textContent = connectionStatus;

    if (data.activeDb) {
        console.log('Updating active DB:', data.activeDb);
        updateDatabaseCard('active', data.activeDb);
    }

    if (data.standbyDb) {
        console.log('Updating standby DB:', data.standbyDb);
        updateDatabaseCard('standby', data.standbyDb);
    }

    if (data.selectorStatus) {
        console.log('Updating service selector status:', data.selectorStatus);
        const targetEl = document.getElementById('k8s-target');
        const switchedEl = document.getElementById('k8s-switched');

        if (targetEl) {
            targetEl.textContent = data.selectorStatus.currentTarget || 'unknown';
        }
        if (switchedEl) {
            // lastSwitchedFormatted 필드 사용
            switchedEl.textContent = data.selectorStatus.lastSwitchedFormatted || '-';
        }
    }

    if (data.syncMetrics) {
        console.log('Updating sync metrics:', data.syncMetrics);
        const syncRate = data.syncMetrics.syncRate || 0;
        document.getElementById('sync-rate').textContent = syncRate.toFixed(2) + '%';
        document.getElementById('sync-bar-fill').style.width = syncRate + '%';
        document.getElementById('standby-data-transferred').textContent = data.syncMetrics.activeDataTransferred || '-';
        document.getElementById('active-data-transferred').textContent = data.syncMetrics.standbyDataTransferred || '-';
        document.getElementById('active-gtid').textContent = data.syncMetrics.activeGtid || '-';
        document.getElementById('standby-gtid').textContent = data.syncMetrics.standbyGtid || '-';
        document.getElementById('last-sync-time').textContent = data.syncMetrics.lastSyncTime || '-';
    }

    console.log('Checking writeActivity:', data.writeActivity ? 'EXISTS' : 'MISSING');
    if (data.writeActivity) {
        console.log('Calling updateWriteActivity with:', data.writeActivity);
        updateWriteActivity(data.writeActivity);
    } else {
        console.error('data.writeActivity is missing!');
    }

    console.log('Checking readActivity:', data.readActivity ? 'EXISTS' : 'MISSING');
    if (data.readActivity) {
        console.log('Calling updateReadActivity with:', data.readActivity);
        updateReadActivity(data.readActivity);
    } else {
        console.error('data.readActivity is missing!');
    }

    if (data.systemEvents) {
        console.log('Updating system events:', data.systemEvents);
        updateSystemEvents(data.systemEvents);
    }
    console.log('Dashboard update complete');
}

function updateDatabaseCard(type, dbData) {
    console.log(`Updating ${type} database card:`, dbData);

    // Uptime
    const uptimeEl = document.getElementById(type + '-uptime');
    if (uptimeEl) uptimeEl.textContent = dbData.uptime || '0d 0h 0m 0s';

    // Connections
    const connectionsEl = document.getElementById(type + '-connections');
    if (connectionsEl) connectionsEl.textContent = dbData.connections || '0';

    // QPS - 숫자 포맷팅 추가 (소수점 2자리)
    const qpsEl = document.getElementById(type + '-qps');
    if (qpsEl) {
        const qpsValue = dbData.qps || 0;
        if (qpsValue >= 1000) {
            // 1000 이상이면 쉼표 추가
            qpsEl.textContent = qpsValue.toLocaleString('en-US', {
                minimumFractionDigits: 1,
                maximumFractionDigits: 1
            });
        } else if (qpsValue >= 10) {
            // 10 이상이면 소수점 1자리
            qpsEl.textContent = qpsValue.toFixed(1);
        } else {
            // 10 미만이면 소수점 2자리
            qpsEl.textContent = qpsValue.toFixed(2);
        }
    }

    // Last Heartbeat
    const heartbeatEl = document.getElementById(type + '-heartbeat');
    if (heartbeatEl) heartbeatEl.textContent = dbData.lastHeartbeat || '-';

    // Status Badge (상단)
    const statusBadge = document.getElementById(type + '-status');
    if (statusBadge) {
        statusBadge.textContent = dbData.status;
        statusBadge.className = 'status-badge';

        if (dbData.status === 'Active') {
            statusBadge.classList.add('active');
        } else if (dbData.status === 'Standby') {
            statusBadge.classList.add('standby');
        } else {
            statusBadge.classList.add('down');
        }
    }

    // Status Card (메트릭 카드) 배경색 변경
    const statusCard = document.getElementById(type + '-status-card');
    const statusText = document.getElementById(type + '-status-text');

    if (statusCard && statusText) {
        // UP/DOWN 표시
        const isHealthy = dbData.isHealthy !== false && dbData.status !== 'Down';
        statusText.textContent = isHealthy ? '정상' : '중단';
        statusCard.className = 'metric-card';

        if (isHealthy) {
            statusCard.classList.add('status-up');
        } else {
            statusCard.classList.add('status-down');
        }
    }
}

function updateReadActivity(activityData) {
    console.log('Updating read activity:', activityData);

    if (!activityData || !activityData.activeDbReads || !activityData.standbyDbReads) {
        console.warn('Invalid read activity data, skipping update');
        return;
    }

    if (!Array.isArray(activityData.activeDbReads) || activityData.activeDbReads.length === 0) {
        console.warn('Active DB reads is empty or not an array');
        return;
    }

    if (!Array.isArray(activityData.standbyDbReads) || activityData.standbyDbReads.length === 0) {
        console.warn('Standby DB reads is empty or not an array');
        return;
    }

    try {
        // 고정된 X축 레이블 생성 (빈 문자열, 30개 고정)
        const dataLength = activityData.activeDbReads.length;
        const labels = new Array(dataLength).fill('');

        const activeData = activityData.activeDbReads.map(d => d.count || 0);
        const standbyData = activityData.standbyDbReads.map(d => d.count || 0);

        console.log('Read chart data points:', {
            labels: labels.length,
            activeData: activeData.length,
            standbyData: standbyData.length,
            sampleActive: activeData.slice(-5),
            sampleStandby: standbyData.slice(-5)
        });

        if (readChart && readChart.data) {
            readChart.data.labels = labels;
            readChart.data.datasets[0].data = activeData;
            readChart.data.datasets[1].data = standbyData;
            readChart.update('none');
            console.log('Read chart updated successfully');
        } else {
            console.error('readChart is null or invalid');
        }
    } catch (error) {
        console.error('Error updating read activity chart:', error);
    }
}

function updateWriteActivity(activityData) {
    console.log('=== updateWriteActivity called ===');
    console.log('Full activityData:', JSON.stringify(activityData, null, 2));

    if (!activityData) {
        console.error('activityData is null or undefined');
        return;
    }

    console.log('activityData.activeDbWrites:', activityData.activeDbWrites);
    console.log('activityData.standbyDbWrites:', activityData.standbyDbWrites);

    if (!activityData.activeDbWrites || !activityData.standbyDbWrites) {
        console.warn('Invalid activity data - missing activeDbWrites or standbyDbWrites');
        console.warn('Available keys:', Object.keys(activityData));
        return;
    }

    if (!Array.isArray(activityData.activeDbWrites) || activityData.activeDbWrites.length === 0) {
        console.warn('Active DB writes is empty or not an array, length:', activityData.activeDbWrites?.length);
        return;
    }

    if (!Array.isArray(activityData.standbyDbWrites) || activityData.standbyDbWrites.length === 0) {
        console.warn('Standby DB writes is empty or not an array, length:', activityData.standbyDbWrites?.length);
        return;
    }

    try {
        // 고정된 X축 레이블 생성 (빈 문자열, 30개 고정)
        const dataLength = activityData.activeDbWrites.length;
        const labels = new Array(dataLength).fill('');

        const activeData = activityData.activeDbWrites.map(d => d.count || 0);
        const standbyData = activityData.standbyDbWrites.map(d => d.count || 0);

        console.log('Write chart data points:', {
            labels: labels.length,
            activeData: activeData.length,
            standbyData: standbyData.length,
            sampleActive: activeData.slice(-5),
            sampleStandby: standbyData.slice(-5),
            allActiveData: activeData,
            allStandbyData: standbyData
        });

        if (replicationChart && replicationChart.data) {
            replicationChart.data.labels = labels;
            replicationChart.data.datasets[0].data = activeData;
            replicationChart.data.datasets[1].data = standbyData;
            replicationChart.update('none');
            console.log('Write chart updated successfully');
        } else {
            console.error('replicationChart is null or invalid');
        }
    } catch (error) {
        console.error('Error updating write activity chart:', error);
    }
}

function initReadChart() {
    const ctx = document.getElementById('read-chart').getContext('2d');

    readChart = new Chart(ctx, {
        type: 'line',
        data: {
            labels: [],
            datasets: [
                {
                    label: '활성 DB',
                    data: [],
                    borderColor: '#10b981',
                    backgroundColor: 'rgba(16, 185, 129, 0.1)',
                    fill: true,
                    tension: 0.4,
                    pointRadius: 3,
                    pointHoverRadius: 5
                },
                {
                    label: '대기 DB',
                    data: [],
                    borderColor: '#06b6d4',
                    backgroundColor: 'rgba(6, 182, 212, 0.1)',
                    fill: true,
                    tension: 0.4,
                    pointRadius: 3,
                    pointHoverRadius: 5
                }
            ]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            interaction: {
                mode: 'index',
                intersect: false,
            },
            plugins: {
                legend: {
                    display: true,
                    position: 'top',
                    labels: {
                        color: '#64748b',
                        usePointStyle: true,
                        padding: 15,
                        font: {
                            size: 12
                        }
                    }
                },
                tooltip: {
                    callbacks: {
                        title: function (context) {
                            return '데이터 지점: ' + (context[0].dataIndex + 1);
                        },
                        label: function (context) {
                            let label = context.dataset.label || '';
                            if (label) {
                                label += ': ';
                            }
                            label += context.parsed.y + ' 읽기';
                            return label;
                        }
                    }
                }
            },
            scales: {
                x: {
                    display: false  // X축 완전히 숨김
                },
                y: {
                    display: true,
                    title: {
                        display: false
                    },
                    beginAtZero: true,
                    ticks: {
                        color: '#94a3b8',
                        precision: 0,
                        font: {
                            size: 11
                        }
                    },
                    grid: {
                        color: 'rgba(226, 232, 240, 0.3)',
                        drawBorder: false
                    }
                }
            }
        }
    });

    console.log('Read chart initialized');
}

function initReplicationChart() {
    const ctx = document.getElementById('replication-chart').getContext('2d');

    replicationChart = new Chart(ctx, {
        type: 'line',
        data: {
            labels: [],
            datasets: [
                {
                    label: '활성 DB',
                    data: [],
                    borderColor: '#3b82f6',
                    backgroundColor: 'rgba(59, 130, 246, 0.1)',
                    fill: true,
                    tension: 0.4,
                    pointRadius: 3,
                    pointHoverRadius: 5
                },
                {
                    label: '대기 DB',
                    data: [],
                    borderColor: '#8b5cf6',
                    backgroundColor: 'rgba(139, 92, 246, 0.1)',
                    fill: true,
                    tension: 0.4,
                    pointRadius: 3,
                    pointHoverRadius: 5
                }
            ]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            interaction: {
                mode: 'index',
                intersect: false,
            },
            plugins: {
                legend: {
                    display: true,
                    position: 'top',
                    labels: {
                        color: '#64748b',
                        usePointStyle: true,
                        padding: 15,
                        font: {
                            size: 12
                        }
                    }
                },
                tooltip: {
                    callbacks: {
                        title: function (context) {
                            return '데이터 지점: ' + (context[0].dataIndex + 1);
                        },
                        label: function (context) {
                            let label = context.dataset.label || '';
                            if (label) {
                                label += ': ';
                            }
                            label += context.parsed.y + ' 쓰기';
                            return label;
                        }
                    }
                }
            },
            scales: {
                x: {
                    display: false  // X축 완전히 숨김
                },
                y: {
                    display: true,
                    title: {
                        display: false
                    },
                    beginAtZero: true,
                    ticks: {
                        color: '#94a3b8',
                        precision: 0,
                        font: {
                            size: 11
                        }
                    },
                    grid: {
                        color: 'rgba(226, 232, 240, 0.3)',
                        drawBorder: false
                    }
                }
            }
        }
    });

    console.log('Replication chart initialized');
}

function updateMockData() {
    // Mock data는 더 이상 사용하지 않음
    console.log('Mock data update skipped - using real data');
}