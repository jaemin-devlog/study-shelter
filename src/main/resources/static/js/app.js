const STORAGE_KEY = "ddanjit-session-token";
const HEARTBEAT_INTERVAL_MS = 10000;

const state = {
    sessionToken: localStorage.getItem(STORAGE_KEY),
    me: null,
    stompClient: null,
    connected: false,
    manualDisconnect: false,
    heartbeatTimerId: null,
    uiTickerId: null,
    presenceToastTimerId: null,
    toastTimerId: null,
    rankingItems: [],
    onlineCount: 0
};

function logRealtime(event, payload = {}) {
    console.info(`[realtime] ${event}`, payload);
}

const elements = {
    authSection: document.getElementById("authSection"),
    appSection: document.getElementById("appSection"),
    chatPanel: document.getElementById("chatPanel"),
    loginForm: document.getElementById("loginForm"),
    signupForm: document.getElementById("signupForm"),
    logoutButton: document.getElementById("logoutButton"),
    chatForm: document.getElementById("chatForm"),
    chatInput: document.getElementById("chatInput"),
    chatMessages: document.getElementById("chatMessages"),
    rankingList: document.getElementById("rankingList"),
    onlineUsersButton: document.getElementById("onlineUsersButton"),
    onlineUsersModal: document.getElementById("onlineUsersModal"),
    onlineUsersBackdrop: document.getElementById("onlineUsersBackdrop"),
    onlineUsersList: document.getElementById("onlineUsersList"),
    closeOnlineUsersModal: document.getElementById("closeOnlineUsersModal"),
    onlineCount: document.getElementById("onlineCount"),
    presenceToast: document.getElementById("presenceToast"),
    connectionStatus: document.getElementById("connectionStatus"),
    myNickname: document.getElementById("myNickname"),
    mySchool: document.getElementById("mySchool"),
    mySessionStatus: document.getElementById("mySessionStatus"),
    myElapsedTime: document.getElementById("myElapsedTime"),
    myRankText: document.getElementById("myRankText"),
    loginNickname: document.getElementById("loginNickname"),
    loginPassword: document.getElementById("loginPassword"),
    signupNickname: document.getElementById("signupNickname"),
    signupPassword: document.getElementById("signupPassword"),
    signupSchool: document.getElementById("signupSchool"),
    toast: document.getElementById("toast")
};

document.addEventListener("DOMContentLoaded", async () => {
    bindEvents();
    startUiTicker();
    renderEmptyChat();
    renderRanking([]);
    await loadPublicSnapshots();

    if (state.sessionToken) {
        await restoreSession();
        return;
    }

    showAuthSection();
});

function bindEvents() {
    elements.loginForm.addEventListener("submit", handleLogin);
    elements.signupForm.addEventListener("submit", handleSignup);
    elements.logoutButton.addEventListener("click", handleLogout);
    elements.chatForm.addEventListener("submit", handleSendChat);
    elements.onlineUsersButton?.addEventListener("click", openOnlineUsersModal);
    elements.closeOnlineUsersModal?.addEventListener("click", closeOnlineUsersModal);
    elements.onlineUsersBackdrop?.addEventListener("click", closeOnlineUsersModal);
    document.addEventListener("keydown", (event) => {
        if (event.key === "Escape") {
            closeOnlineUsersModal();
        }
    });
}

async function restoreSession() {
    try {
        const response = await apiRequest("/api/auth/me");
        state.me = response.data;
        renderMyInfo();
        showAppSection();
        await loadInitialSnapshots();
        connectWebSocket();
    } catch (error) {
        clearSessionState();
        await loadPublicSnapshots();
        showAuthSection();
    }
}

async function handleSignup(event) {
    event.preventDefault();

    const payload = {
        nickname: elements.signupNickname.value.trim(),
        password: elements.signupPassword.value.trim(),
        school: elements.signupSchool.value.trim()
    };

    try {
        await apiRequest("/api/auth/signup", {
            method: "POST",
            body: JSON.stringify(payload)
        });

        elements.loginNickname.value = payload.nickname;
        elements.signupForm.reset();
        showToast("회원가입이 완료되었습니다. 이제 로그인해 주세요.");
    } catch (error) {
        showToast(error.message);
    }
}

async function handleLogin(event) {
    event.preventDefault();

    const payload = {
        nickname: elements.loginNickname.value.trim(),
        password: elements.loginPassword.value.trim()
    };

    try {
        const response = await apiRequest("/api/auth/login", {
            method: "POST",
            body: JSON.stringify(payload)
        });

        state.sessionToken = response.data.sessionToken;
        localStorage.setItem(STORAGE_KEY, state.sessionToken);
        state.me = {
            sessionToken: response.data.sessionToken,
            nickname: response.data.nickname,
            school: response.data.school,
            connected: false,
            connectedAt: 0,
            lastHeartbeat: 0,
            totalConnectedSeconds: 0,
            rankingPosition: 0,
            totalUpdatedAt: Date.now()
        };

        elements.loginForm.reset();
        renderMyInfo();
        showAppSection();
        await loadInitialSnapshots();
        connectWebSocket();
        showToast("로그인되었습니다. 실시간 연결을 시작합니다.");
    } catch (error) {
        showToast(error.message);
    }
}

async function handleLogout() {
    state.manualDisconnect = true;

    try {
        await apiRequest("/api/auth/logout", {method: "POST"});
    } catch (error) {
        console.error(error);
    } finally {
        await disconnectSocket();
        clearSessionState();
        await loadPublicSnapshots();
        showAuthSection();
        showToast("로그아웃했습니다.");
        state.manualDisconnect = false;
    }
}

function handleSendChat(event) {
    event.preventDefault();

    const content = elements.chatInput.value.trim();
    if (!content) {
        return;
    }

    if (!state.connected || !state.stompClient || !state.stompClient.connected) {
        showToast("WebSocket 연결이 아직 준비되지 않았습니다. 잠시 후 다시 시도해 주세요.");
        return;
    }

    state.stompClient.publish({
        destination: "/app/chat.send",
        body: JSON.stringify({content})
    });

    elements.chatInput.value = "";
}

async function loadPublicSnapshots() {
    try {
        const [countResponse, rankingResponse, chatResponse] = await Promise.all([
            apiRequest("/api/presence/count"),
            apiRequest("/api/ranking/top10"),
            apiRequest("/api/chat/recent")
        ]);

        updateOnlineCount(countResponse.data.count);
        renderRanking(rankingResponse.data.rankings || [], rankingResponse.data.updatedAt);
        renderChatHistory(chatResponse.data.messages || []);
    } catch (error) {
        console.error(error);
    }
}

async function loadInitialSnapshots() {
    await loadPublicSnapshots();

    if (!state.sessionToken) {
        return;
    }

    try {
        const meResponse = await apiRequest("/api/auth/me");
        state.me = meResponse.data;
        renderMyInfo();
    } catch (error) {
        showToast(error.message);
    }
}

function connectWebSocket() {
    logRealtime("connect:start", {
        hasSessionToken: Boolean(state.sessionToken),
        host: window.location.host,
        protocol: window.location.protocol
    });
    connectWebSocketWithFallback(false);
}

function connectWebSocketWithFallback(useSockJsFallback) {
    if (!state.sessionToken || state.connected) {
        logRealtime("connect:skip", {
            hasSessionToken: Boolean(state.sessionToken),
            connected: state.connected,
            fallback: useSockJsFallback
        });
        return;
    }

    setConnectionStatus("연결 중", "connecting");
    logRealtime("connect:attempt", {
        fallback: useSockJsFallback,
        nativeUrl: useSockJsFallback ? null : buildNativeWsUrl(),
        sockJsUrl: useSockJsFallback ? `${window.location.origin}/ws` : null
    });

    const client = new StompJs.Client({
        connectHeaders: {
            sessionToken: state.sessionToken
        },
        reconnectDelay: 0,
        debug: () => {},
        onConnect: () => {
            logRealtime("connect:success", {
                fallback: useSockJsFallback
            });
            state.stompClient = client;
            state.connected = true;

            if (state.me && !state.me.connectedAt) {
                state.me.connectedAt = Date.now();
            }

            setConnectionStatus("연결됨", "online");
            updateSessionStateText("현재 접속 중");
            subscribeTopics(client);
            sendHeartbeat();
            startHeartbeat();
            updateMyElapsedTime();
            loadInitialSnapshots();
        },
        onStompError: (frame) => {
            logRealtime("connect:stomp-error", {
                fallback: useSockJsFallback,
                headers: frame.headers,
                body: frame.body
            });
            handleUnexpectedDisconnect(frame.headers.message || "실시간 연결 중 오류가 발생했습니다.");
        },
        onWebSocketClose: () => {
            logRealtime("connect:websocket-close", {
                fallback: useSockJsFallback,
                connected: state.connected,
                manualDisconnect: state.manualDisconnect
            });
            if (!state.connected && !state.manualDisconnect && !useSockJsFallback) {
                logRealtime("connect:fallback", {
                    from: "native",
                    to: "sockjs"
                });
                connectWebSocketWithFallback(true);
                return;
            }

            if (!state.manualDisconnect) {
                handleUnexpectedDisconnect("연결이 종료되어 세션을 정리했습니다. 다시 로그인해 주세요.");
            }
        },
        onWebSocketError: (event) => {
            logRealtime("connect:websocket-error", {
                fallback: useSockJsFallback,
                eventType: event?.type || null
            });
            setConnectionStatus("연결 오류", "offline");
        }
    });

    if (useSockJsFallback) {
        client.webSocketFactory = () => new SockJS("/ws");
    } else {
        client.brokerURL = buildNativeWsUrl();
    }

    state.stompClient = client;
    client.activate();
}

function buildNativeWsUrl() {
    const protocol = window.location.protocol === "https:" ? "wss" : "ws";
    return `${protocol}://${window.location.host}/ws-native`;
}

function subscribeTopics(client) {
    client.subscribe("/topic/chat", (message) => {
        appendChatMessage(JSON.parse(message.body));
    });

    client.subscribe("/topic/online-count", (message) => {
        const payload = JSON.parse(message.body);
        updateOnlineCount(payload.count);
        if (!elements.onlineUsersModal?.classList.contains("hidden")) {
            loadOnlineUsers();
        }
    });

    client.subscribe("/topic/ranking", (message) => {
        const payload = JSON.parse(message.body);
        renderRanking(payload.rankings || [], payload.updatedAt);
    });

    client.subscribe("/topic/presence-notice", (message) => {
        handlePresenceNotice(JSON.parse(message.body));
    });
}

function startHeartbeat() {
    stopHeartbeat();
    state.heartbeatTimerId = window.setInterval(sendHeartbeat, HEARTBEAT_INTERVAL_MS);
}

function stopHeartbeat() {
    if (!state.heartbeatTimerId) {
        return;
    }

    window.clearInterval(state.heartbeatTimerId);
    state.heartbeatTimerId = null;
}

function sendHeartbeat() {
    if (!state.connected || !state.stompClient || !state.stompClient.connected) {
        return;
    }

    state.stompClient.publish({
        destination: "/app/presence.heartbeat",
        body: JSON.stringify({clientTime: Date.now()})
    });
}

async function disconnectSocket() {
    stopHeartbeat();

    if (state.stompClient) {
        try {
            await state.stompClient.deactivate();
        } catch (error) {
            console.error(error);
        }
    }

    state.stompClient = null;
    state.connected = false;
    setConnectionStatus("연결 끊김", "offline");
}

async function handleUnexpectedDisconnect(message) {
    logRealtime("connect:unexpected-disconnect", {
        message
    });
    state.connected = false;
    clearSessionState();
    await loadPublicSnapshots();
    showAuthSection();
    showToast(message);
}

async function openOnlineUsersModal() {
    elements.onlineUsersModal?.classList.remove("hidden");
    await loadOnlineUsers();
}

function closeOnlineUsersModal() {
    elements.onlineUsersModal?.classList.add("hidden");
}

async function loadOnlineUsers() {
    try {
        const response = await apiRequest("/api/presence/users");
        renderOnlineUsers(response.data.users || []);
    } catch (error) {
        renderOnlineUsers([]);
        showToast(error.message);
    }
}

function renderOnlineUsers(users) {
    if (!elements.onlineUsersList) {
        return;
    }

    if (!Array.isArray(users) || users.length === 0) {
        elements.onlineUsersList.innerHTML = `
            <li class="empty-box">현재 접속 중인 사용자가 없습니다.</li>
        `;
        return;
    }

    const fragment = document.createDocumentFragment();

    users.forEach((user, index) => {
        const item = document.createElement("li");
        item.className = "online-user-item";
        item.innerHTML = `
            <span class="online-user-rank">${index + 1}</span>
            <div class="online-user-main">
                <strong>${escapeHtml(user.nickname)}</strong>
                <span>${escapeHtml(user.school)}</span>
            </div>
            <span class="online-user-time">${formatDurationFromSeconds(user.connectedSeconds)}</span>
        `;
        fragment.append(item);
    });

    elements.onlineUsersList.replaceChildren(fragment);
}

function renderMyInfo() {
    elements.myNickname.textContent = state.me?.nickname || "-";
    elements.mySchool.textContent = state.me?.school || "-";
    updateSessionStateText(state.connected ? "현재 접속 중" : "누적 기록 확인 가능");
    updateMyElapsedTime();
    updateMyRankText();
}

function updateSessionStateText(text) {
    elements.mySessionStatus.textContent = text;
}

function appendChatMessage(message) {
    if (!message || message.type === "SYSTEM") {
        return;
    }
    clearEmptyChatIfNeeded();
    elements.chatMessages.append(buildChatRow(message));
    elements.chatMessages.scrollTop = elements.chatMessages.scrollHeight;
}

function renderChatHistory(messages) {
    elements.chatMessages.innerHTML = "";

    const visibleMessages = Array.isArray(messages)
        ? messages.filter((message) => message.type !== "SYSTEM")
        : [];

    if (visibleMessages.length === 0) {
        renderEmptyChat();
        return;
    }

    const fragment = document.createDocumentFragment();
    visibleMessages.forEach((message) => {
        fragment.append(buildChatRow(message));
    });

    elements.chatMessages.append(fragment);
    elements.chatMessages.scrollTop = elements.chatMessages.scrollHeight;
}

function buildChatRow(message) {
    const isMine = state.me && message.nickname === state.me.nickname;
    const row = document.createElement("article");
    row.className = `chat-row ${isMine ? "me" : "other"}`;

    const wrap = document.createElement("div");
    wrap.className = "chat-bubble-wrap";

    const meta = document.createElement("div");
    meta.className = "chat-meta";

    const sender = document.createElement("span");
    sender.className = "chat-sender";
    sender.textContent = isMine ? "나" : message.nickname;

    const school = document.createElement("span");
    school.className = "chat-school";
    school.textContent = message.school || "";

    const time = document.createElement("span");
    time.className = "chat-time";
    time.textContent = formatClock(message.sentAt);

    meta.append(sender);
    meta.append(school);
    meta.append(time);

    const bubble = document.createElement("div");
    bubble.className = "chat-bubble";
    bubble.textContent = message.content;

    wrap.append(meta, bubble);
    row.append(wrap);

    return row;
}

function renderEmptyChat() {
    elements.chatMessages.innerHTML = `
        <div class="empty-box" id="emptyChatBox">
            아직 저장된 채팅이 없습니다. 첫 메시지를 남겨보세요.
        </div>
    `;
}

function clearEmptyChatIfNeeded() {
    const emptyBox = document.getElementById("emptyChatBox");
    if (emptyBox) {
        emptyBox.remove();
    }
}

function renderRanking(rankings, updatedAt = Date.now()) {
    const previousPositions = captureRankingPositions();
    state.rankingItems = Array.isArray(rankings) ? rankings.slice() : [];

    if (state.rankingItems.length === 0) {
        elements.rankingList.innerHTML = `
            <li class="empty-box">아직 집계된 순위가 없습니다. 첫 접속 기록을 남겨보세요.</li>
        `;
        updateMyRankText();
        return;
    }

    const fragment = document.createDocumentFragment();

    state.rankingItems.forEach((item) => {
        const isMine = state.me && item.nickname === state.me.nickname;
        const row = document.createElement("li");
        row.className = `ranking-item${isMine ? " mine" : ""}`;
        row.dataset.rankKey = createRankingKey(item);

        row.innerHTML = `
            <span class="rank-badge">${item.rank}</span>
            <div class="ranking-main">
                <div>
                    <strong class="ranking-name">${escapeHtml(item.nickname)}</strong>
                    <span class="ranking-school">
                        ${escapeHtml(item.school)} · ${item.online ? "접속 중" : "미접속"}
                    </span>
                </div>
                <span class="ranking-time">
                    ${formatDurationFromSeconds(item.totalConnectedSeconds)}
                </span>
            </div>
        `;
        row.dataset.baseSeconds = String(item.totalConnectedSeconds || 0);
        row.dataset.updatedAt = String(updatedAt || Date.now());
        row.dataset.online = String(Boolean(item.online));

        fragment.append(row);
    });

    if (state.me) {
        const myEntry = state.rankingItems.find((item) => item.nickname === state.me.nickname);
        if (myEntry) {
            state.me.totalConnectedSeconds = myEntry.totalConnectedSeconds;
            state.me.rankingPosition = myEntry.rank;
            state.me.totalUpdatedAt = updatedAt || Date.now();
        }
    }

    elements.rankingList.replaceChildren(fragment);
    animateRankingMovement(previousPositions);
    updateMyElapsedTime();
    updateMyRankText();
}

function captureRankingPositions() {
    const positions = new Map();
    elements.rankingList.querySelectorAll(".ranking-item").forEach((item) => {
        positions.set(item.dataset.rankKey, item.getBoundingClientRect().top);
    });
    return positions;
}

function animateRankingMovement(previousPositions) {
    requestAnimationFrame(() => {
        elements.rankingList.querySelectorAll(".ranking-item").forEach((item) => {
            const previousTop = previousPositions.get(item.dataset.rankKey);
            if (previousTop === undefined) {
                item.style.opacity = "0";
                item.style.transform = "translateY(12px)";
                requestAnimationFrame(() => {
                    item.style.transition = "transform 0.35s ease, opacity 0.35s ease";
                    item.style.opacity = "1";
                    item.style.transform = "translateY(0)";
                });
                return;
            }

            const currentTop = item.getBoundingClientRect().top;
            const deltaY = previousTop - currentTop;
            if (deltaY === 0) {
                return;
            }

            item.style.transition = "none";
            item.style.transform = `translateY(${deltaY}px)`;

            requestAnimationFrame(() => {
                item.style.transition = "transform 0.4s ease";
                item.style.transform = "translateY(0)";
            });
        });
    });
}

function updateMyRankText() {
    if (!state.me) {
        elements.myRankText.textContent = "집계 전";
        return;
    }

    const myRank = state.rankingItems.find((item) => item.nickname === state.me.nickname);
    if (myRank) {
        state.me.rankingPosition = myRank.rank;
        elements.myRankText.textContent = `전체 ${myRank.rank}위`;
        return;
    }

    if (state.me.rankingPosition > 0) {
        elements.myRankText.textContent = `전체 ${state.me.rankingPosition}위`;
        return;
    }

    elements.myRankText.textContent = "집계 전";
}

function startUiTicker() {
    if (state.uiTickerId) {
        return;
    }

    state.uiTickerId = window.setInterval(() => {
        updateMyElapsedTime();
        updateRankingDurations();
    }, 1000);
}

function updateRankingDurations() {
    elements.rankingList?.querySelectorAll(".ranking-item").forEach((row) => {
        const timeElement = row.querySelector(".ranking-time");
        if (!timeElement) {
            return;
        }

        const baseSeconds = Number(row.dataset.baseSeconds || 0);
        const updatedAt = Number(row.dataset.updatedAt || Date.now());
        const online = row.dataset.online === "true";
        const extraSeconds = online ? Math.max(0, Math.floor((Date.now() - updatedAt) / 1000)) : 0;
        timeElement.textContent = formatDurationFromSeconds(baseSeconds + extraSeconds);
    });
}

function updateMyElapsedTime() {
    if (!state.me) {
        elements.myElapsedTime.textContent = "00시간00분00초";
        return;
    }

    const baseSeconds = Math.max(0, Number(state.me.totalConnectedSeconds) || 0);
    const measuredAt = state.me.totalUpdatedAt || Date.now();
    const extraSeconds = state.connected
        ? Math.max(0, Math.floor((Date.now() - measuredAt) / 1000))
        : 0;

    elements.myElapsedTime.textContent = formatDurationFromSeconds(baseSeconds + extraSeconds);
}

function updateOnlineCount(count) {
    state.onlineCount = Number(count) || 0;
    elements.onlineCount.textContent = `${count}명`;
}

function handlePresenceNotice(payload) {
    if (!payload || !payload.nickname) {
        return;
    }

    if (payload.onlineCount > 10) {
        return;
    }

    if (elements.appSection?.classList.contains("hidden")) {
        return;
    }

    if (state.me && payload.nickname === state.me.nickname) {
        return;
    }

    showPresenceToast(`${payload.nickname}님이 대피소에 들어왔습니다.`);
}

function setConnectionStatus(text, mode) {
    if (!elements.connectionStatus) {
        return;
    }
    elements.connectionStatus.textContent = text;
    elements.connectionStatus.className = `status-pill ${mode}`;
}

function showAuthSection() {
    closeOnlineUsersModal();
    elements.authSection.classList.remove("hidden");
    elements.appSection.classList.add("hidden");
}

function showAppSection() {
    elements.authSection.classList.add("hidden");
    elements.appSection.classList.remove("hidden");
    focusChatPanelIfRequested();
}

function focusChatPanelIfRequested() {
    if (window.location.hash !== "#chat") {
        return;
    }

    window.requestAnimationFrame(() => {
        elements.chatPanel?.scrollIntoView({
            block: "center",
            behavior: "smooth"
        });
        elements.chatInput?.focus({preventScroll: true});
    });
}

function clearSessionState() {
    stopHeartbeat();
    localStorage.removeItem(STORAGE_KEY);
    state.sessionToken = null;
    state.me = null;
    state.connected = false;
    state.stompClient = null;

    setConnectionStatus("연결 끊김", "offline");
    elements.myNickname.textContent = "-";
    elements.mySchool.textContent = "-";
    elements.mySessionStatus.textContent = "대기 중";
    elements.myElapsedTime.textContent = "00시간 00분 00초";
    elements.myRankText.textContent = "집계 전";
}

async function apiRequest(url, options = {}) {
    const headers = {};

    if (options.body) {
        headers["Content-Type"] = "application/json";
    }

    if (state.sessionToken) {
        headers.Authorization = `Bearer ${state.sessionToken}`;
    }

    const response = await fetch(url, {
        method: options.method || "GET",
        headers: {
            ...headers,
            ...(options.headers || {})
        },
        body: options.body
    });

    const data = await response.json().catch(() => ({
        success: false,
        message: "응답을 읽을 수 없습니다."
    }));

    if (!response.ok || data.success === false) {
        throw new Error(data.message || "요청 처리에 실패했습니다.");
    }

    return data;
}

function formatClock(timestamp) {
    return new Date(timestamp).toLocaleTimeString("ko-KR", {
        hour: "2-digit",
        minute: "2-digit",
        second: "2-digit"
    });
}

function formatDurationFromSeconds(totalSeconds) {
    const safeSeconds = Math.max(0, Number(totalSeconds) || 0);
    const hours = Math.floor(safeSeconds / 3600);
    const minutes = Math.floor((safeSeconds % 3600) / 60);
    const seconds = safeSeconds % 60;

    return `${padNumber(hours)}시간${padNumber(minutes)}분${padNumber(seconds)}초`;
}

function padNumber(value) {
    return String(value).padStart(2, "0");
}

function createRankingKey(item) {
    return item.nickname;
}

function showToast(message) {
    elements.toast.textContent = message;
    elements.toast.classList.remove("hidden");

    window.clearTimeout(state.toastTimerId);
    state.toastTimerId = window.setTimeout(() => {
        elements.toast.classList.add("hidden");
    }, 2600);
}

function showPresenceToast(message) {
    if (!elements.presenceToast) {
        return;
    }

    elements.presenceToast.textContent = message;
    elements.presenceToast.classList.remove("hidden");

    window.clearTimeout(state.presenceToastTimerId);
    state.presenceToastTimerId = window.setTimeout(() => {
        elements.presenceToast.classList.add("hidden");
    }, 1600);
}

function escapeHtml(value) {
    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll("\"", "&quot;")
        .replaceAll("'", "&#39;");
}


