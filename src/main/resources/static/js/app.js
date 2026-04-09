const STORAGE_KEY = "ddanjit-session-token";
const HEARTBEAT_INTERVAL_MS = 10000;

const state = {
    sessionToken: localStorage.getItem(STORAGE_KEY),
    me: null,
    stompClient: null,
    connected: false,
    activeMobilePanel: "chat",
    manualDisconnect: false,
    heartbeatTimerId: null,
    uiTickerId: null,
    presenceToastTimerId: null,
    toastTimerId: null,
    rankingItems: [],
    onlineCount: 0,
    game2048: {
        board: [],
        score: 0,
        hasWon: false,
        isOver: false,
        initialized: false,
        status: "로그인 후 게임을 열 수 있습니다.",
        touchStartX: 0,
        touchStartY: 0
    },
    breakout: {
        initialized: false,
        running: false,
        paused: true,
        started: false,
        gameOver: false,
        cleared: false,
        pauseReason: "idle",
        score: 0,
        paddleDirection: 0,
        leftPressed: false,
        rightPressed: false,
        animationFrameId: null,
        lastTimestamp: 0,
        canvasWidth: 520,
        canvasHeight: 420,
        paddle: null,
        ball: null,
        bricks: [],
        theme: null,
        brickConfig: null,
        nextRowSeed: 0,
        speedIncreaseFactor: 1.0025,
        maxBallSpeed: 0.46
    }
};

function logRealtime(event, payload = {}) {
    console.info(`[realtime] ${event}`, payload);
}

const elements = {
    authSection: document.getElementById("authSection"),
    appSection: document.getElementById("appSection"),
    appLayout: document.querySelector(".app-layout"),
    profilePanel: document.querySelector(".profile-panel"),
    rankingPanel: document.querySelector(".ranking-panel"),
    chatPanel: document.getElementById("chatPanel"),
    loginForm: document.getElementById("loginForm"),
    signupForm: document.getElementById("signupForm"),
    logoutButton: document.getElementById("logoutButton"),
    chatForm: document.getElementById("chatForm"),
    chatInput: document.getElementById("chatInput"),
    chatMessages: document.getElementById("chatMessages"),
    rankingList: document.getElementById("rankingList"),
    game2048Button: document.getElementById("game2048Button"),
    breakoutButton: document.getElementById("breakoutButton"),
    game2048Modal: document.getElementById("game2048Modal"),
    game2048Backdrop: document.getElementById("game2048Backdrop"),
    closeGame2048Modal: document.getElementById("closeGame2048Modal"),
    restart2048Button: document.getElementById("restart2048Button"),
    game2048Grid: document.getElementById("game2048Grid"),
    game2048Score: document.getElementById("game2048Score"),
    game2048Status: document.getElementById("game2048Status"),
    game2048BoardShell: document.getElementById("game2048BoardShell"),
    breakoutModal: document.getElementById("breakoutModal"),
    breakoutBackdrop: document.getElementById("breakoutBackdrop"),
    closeBreakoutModal: document.getElementById("closeBreakoutModal"),
    breakoutStartButton: document.getElementById("breakoutStartButton"),
    breakoutPauseButton: document.getElementById("breakoutPauseButton"),
    breakoutRestartButton: document.getElementById("breakoutRestartButton"),
    breakoutCanvas: document.getElementById("breakoutCanvas"),
    breakoutScore: document.getElementById("breakoutScore"),
    breakoutStatus: document.getElementById("breakoutStatus"),
    breakoutGuide: document.querySelector(".breakout-guide"),
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
    createMobileAppSwitcher();
    bindEvents();
    syncResponsiveLayout();
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
    elements.game2048Button?.addEventListener("click", openGame2048Modal);
    elements.breakoutButton?.addEventListener("click", openBreakoutModal);
    elements.closeGame2048Modal?.addEventListener("click", closeGame2048Modal);
    elements.game2048Backdrop?.addEventListener("click", closeGame2048Modal);
    elements.restart2048Button?.addEventListener("click", reset2048Game);
    elements.closeBreakoutModal?.addEventListener("click", closeBreakoutModalHandler);
    elements.breakoutBackdrop?.addEventListener("click", closeBreakoutModalHandler);
    elements.breakoutStartButton?.addEventListener("click", handleBreakoutStartButton);
    elements.breakoutPauseButton?.addEventListener("click", handleBreakoutPauseButton);
    elements.breakoutRestartButton?.addEventListener("click", restartBreakoutGame);
    elements.game2048BoardShell?.addEventListener("touchstart", handleGame2048TouchStart, {passive: true});
    elements.game2048BoardShell?.addEventListener("touchend", handleGame2048TouchEnd, {passive: true});
    elements.breakoutCanvas?.addEventListener("touchstart", handleBreakoutTouchStart, {passive: false});
    elements.breakoutCanvas?.addEventListener("touchmove", handleBreakoutTouchMove, {passive: false});
    elements.breakoutCanvas?.addEventListener("touchend", handleBreakoutTouchEnd, {passive: true});
    elements.breakoutCanvas?.addEventListener("mousemove", handleBreakoutMouseMove);
    elements.breakoutCanvas?.addEventListener("mouseenter", handleBreakoutMouseMove);
    elements.breakoutCanvas?.addEventListener("mouseleave", handleBreakoutMouseLeave);
    elements.onlineUsersButton?.addEventListener("click", openOnlineUsersModal);
    elements.closeOnlineUsersModal?.addEventListener("click", closeOnlineUsersModal);
    elements.onlineUsersBackdrop?.addEventListener("click", closeOnlineUsersModal);
    document.addEventListener("keydown", handleGlobalKeydown);
    document.addEventListener("keyup", handleGlobalKeyup);
    window.addEventListener("resize", handleViewportResize, {passive: true});
}

function createMobileAppSwitcher() {
    if (!elements.appSection || !elements.appLayout || document.getElementById("mobileAppSwitcher")) {
        return;
    }

    const switcher = document.createElement("div");
    switcher.id = "mobileAppSwitcher";
    switcher.className = "mobile-app-switcher hidden";
    switcher.innerHTML = `
        <button type="button" class="mobile-app-switcher__button is-active" data-panel="chat">채팅</button>
        <button type="button" class="mobile-app-switcher__button" data-panel="profile">내 상태</button>
        <button type="button" class="mobile-app-switcher__button" data-panel="ranking">랭킹</button>
    `;

    switcher.querySelectorAll("[data-panel]").forEach((button) => {
        button.addEventListener("click", () => {
            setActiveMobilePanel(button.dataset.panel || "chat", {updateHash: true});
        });
    });

    elements.appLayout.parentElement?.insertBefore(switcher, elements.appLayout);
    elements.mobileAppSwitcher = switcher;
}

function handleViewportResize() {
    syncResponsiveLayout();

    if (!elements.breakoutModal?.classList.contains("hidden")) {
        renderBreakoutGame();
    }
}

function isMobileViewport() {
    return window.matchMedia("(max-width: 980px)").matches;
}

function syncResponsiveLayout() {
    updateBreakoutGuideText();

    if (!elements.appSection || !elements.mobileAppSwitcher) {
        return;
    }

    if (!isMobileViewport()) {
        elements.mobileAppSwitcher.classList.add("hidden");
        delete elements.appSection.dataset.mobilePanel;
        return;
    }

    elements.mobileAppSwitcher.classList.remove("hidden");
    setActiveMobilePanel(resolveInitialMobilePanel(), {updateHash: false});
}

function resolveInitialMobilePanel() {
    if (window.location.hash === "#ranking") {
        return "ranking";
    }

    if (window.location.hash === "#profile") {
        return "profile";
    }

    return state.activeMobilePanel || "chat";
}

function setActiveMobilePanel(panel, options = {}) {
    const nextPanel = ["chat", "profile", "ranking"].includes(panel) ? panel : "chat";
    state.activeMobilePanel = nextPanel;

    if (!elements.appSection) {
        return;
    }

    if (isMobileViewport()) {
        elements.appSection.dataset.mobilePanel = nextPanel;
    }

    elements.mobileAppSwitcher?.querySelectorAll("[data-panel]").forEach((button) => {
        const isActive = button.dataset.panel === nextPanel;
        button.classList.toggle("is-active", isActive);
        button.setAttribute("aria-pressed", String(isActive));
    });

    if (options.updateHash) {
        const hash = nextPanel === "chat" ? "#chat" : `#${nextPanel}`;
        history.replaceState(null, "", `${window.location.pathname}${hash}`);
    }
}

function updateBreakoutGuideText() {
    if (!elements.breakoutGuide) {
        return;
    }

    elements.breakoutGuide.textContent = isMobileViewport()
        ? "조작: 패들을 손가락으로 좌우로 밀어서 움직일 수 있습니다. 팝업을 닫아도 현재 상태는 그대로 유지됩니다."
        : "조작: 마우스 또는 좌우 방향키로 패들을 움직입니다. 팝업을 닫아도 현재 상태는 그대로 유지됩니다.";
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

function handleGlobalKeydown(event) {
    if (event.key === "Escape") {
        closeOnlineUsersModal();
        closeGame2048Modal();
        closeBreakoutModalHandler();
        return;
    }

    if (!elements.breakoutModal?.classList.contains("hidden")) {
        if (event.key === "ArrowLeft") {
            event.preventDefault();
            state.breakout.leftPressed = true;
            state.breakout.paddleDirection = -1;
            return;
        }

        if (event.key === "ArrowRight") {
            event.preventDefault();
            state.breakout.rightPressed = true;
            state.breakout.paddleDirection = 1;
            return;
        }
    }

    if (elements.game2048Modal?.classList.contains("hidden")) {
        return;
    }

    const direction = {
        ArrowUp: "up",
        ArrowDown: "down",
        ArrowLeft: "left",
        ArrowRight: "right"
    }[event.key];

    if (!direction) {
        return;
    }

    event.preventDefault();
    move2048(direction);
}

function handleGlobalKeyup(event) {
    if (elements.breakoutModal?.classList.contains("hidden")) {
        return;
    }

    if (event.key !== "ArrowLeft" && event.key !== "ArrowRight") {
        return;
    }

    event.preventDefault();

    if (event.key === "ArrowLeft") {
        state.breakout.leftPressed = false;
        state.breakout.paddleDirection = state.breakout.rightPressed ? 1 : 0;
        return;
    }

    state.breakout.rightPressed = false;
    state.breakout.paddleDirection = state.breakout.leftPressed ? -1 : 0;
}

function openGame2048Modal() {
    if (!state.sessionToken || !state.me) {
        showToast("2048 게임은 로그인 후 이용할 수 있어요.");
        elements.loginNickname?.focus();
        return;
    }

    closeOnlineUsersModal();
    closeBreakoutModalHandler();

    if (!state.game2048.initialized) {
        reset2048Game();
    } else {
        render2048Game();
    }

    elements.game2048Modal?.classList.remove("hidden");
}

function closeGame2048Modal() {
    elements.game2048Modal?.classList.add("hidden");
}

function openBreakoutModal() {
    if (!state.sessionToken || !state.me) {
        showToast("벽돌깨기 게임은 로그인 후 이용할 수 있어요.");
        elements.loginNickname?.focus();
        return;
    }

    closeOnlineUsersModal();
    closeGame2048Modal();

    if (!state.breakout.initialized) {
        initializeBreakoutGame();
    }

    elements.breakoutModal?.classList.remove("hidden");

    if (state.breakout.started && state.breakout.paused && state.breakout.pauseReason === "hidden" && !state.breakout.gameOver && !state.breakout.cleared) {
        startBreakoutGame();
        return;
    }

    renderBreakoutGame();
}

function closeBreakoutModalHandler() {
    if (!elements.breakoutModal || elements.breakoutModal.classList.contains("hidden")) {
        return;
    }

    if (state.breakout.running) {
        pauseBreakoutGame("hidden");
    }

    state.breakout.paddleDirection = 0;
    state.breakout.leftPressed = false;
    state.breakout.rightPressed = false;
    elements.breakoutModal.classList.add("hidden");
}

function reset2048Game() {
    const board = createEmpty2048Board();
    spawnRandom2048Tile(board);
    spawnRandom2048Tile(board);

    state.game2048 = {
        ...state.game2048,
        board,
        score: 0,
        hasWon: false,
        isOver: false,
        initialized: true,
        status: "방향키나 스와이프로 시작해 보세요."
    };

    render2048Game();
}

function render2048Game() {
    if (!elements.game2048Grid) {
        return;
    }

    const fragment = document.createDocumentFragment();

    state.game2048.board.flat().forEach((value) => {
        const cell = document.createElement("div");
        cell.className = `game2048-cell${value ? ` value-${value}` : ""}`;
        cell.textContent = value ? String(value) : "";
        fragment.append(cell);
    });

    elements.game2048Grid.replaceChildren(fragment);
    elements.game2048Score.textContent = String(state.game2048.score);
    elements.game2048Status.textContent = state.game2048.status;
}

function handleGame2048TouchStart(event) {
    const touch = event.changedTouches?.[0];
    if (!touch) {
        return;
    }

    state.game2048.touchStartX = touch.clientX;
    state.game2048.touchStartY = touch.clientY;
}

function handleGame2048TouchEnd(event) {
    const touch = event.changedTouches?.[0];
    if (!touch) {
        return;
    }

    const deltaX = touch.clientX - state.game2048.touchStartX;
    const deltaY = touch.clientY - state.game2048.touchStartY;
    const threshold = 30;

    if (Math.abs(deltaX) < threshold && Math.abs(deltaY) < threshold) {
        return;
    }

    if (Math.abs(deltaX) > Math.abs(deltaY)) {
        move2048(deltaX > 0 ? "right" : "left");
        return;
    }

    move2048(deltaY > 0 ? "down" : "up");
}

function handleBreakoutTouchStart(event) {
    if (elements.breakoutModal?.classList.contains("hidden") || !state.breakout.initialized) {
        return;
    }

    syncBreakoutPaddleToTouch(event);
}

function handleBreakoutTouchMove(event) {
    if (elements.breakoutModal?.classList.contains("hidden") || !state.breakout.initialized) {
        return;
    }

    event.preventDefault();
    syncBreakoutPaddleToTouch(event);
}

function handleBreakoutTouchEnd() {
    state.breakout.paddleDirection = 0;
    state.breakout.leftPressed = false;
    state.breakout.rightPressed = false;
}

function handleBreakoutMouseMove(event) {
    if (isMobileViewport()) {
        return;
    }

    syncBreakoutPaddleToClientX(event.clientX);
}

function handleBreakoutMouseLeave() {
    state.breakout.paddleDirection = 0;
    state.breakout.leftPressed = false;
    state.breakout.rightPressed = false;
}

function syncBreakoutPaddleToTouch(event) {
    const touch = event.touches?.[0] || event.changedTouches?.[0];
    if (!touch) {
        return;
    }

    syncBreakoutPaddleToClientX(touch.clientX);
}

function syncBreakoutPaddleToClientX(clientX) {
    if (elements.breakoutModal?.classList.contains("hidden") || !elements.breakoutCanvas || !state.breakout.paddle) {
        return;
    }

    const bounds = elements.breakoutCanvas.getBoundingClientRect();
    const scaleX = state.breakout.canvasWidth / bounds.width;
    const touchX = (clientX - bounds.left) * scaleX;
    const paddle = state.breakout.paddle;

    paddle.x = clamp(
        touchX - paddle.width / 2,
        0,
        state.breakout.canvasWidth - paddle.width
    );

    state.breakout.paddleDirection = 0;
    state.breakout.leftPressed = false;
    state.breakout.rightPressed = false;

    if (!state.breakout.running) {
        renderBreakoutGame();
    }
}

function move2048(direction) {
    if (!state.game2048.initialized || state.game2048.isOver) {
        return;
    }

    const result = run2048Move(state.game2048.board, direction);
    if (!result.moved) {
        state.game2048.status = "더 움직일 수 있는 방향으로 밀어보세요.";
        render2048Game();
        return;
    }

    spawnRandom2048Tile(result.board);

    const hasWon = state.game2048.hasWon || result.created2048;
    const isOver = !canMove2048(result.board);

    state.game2048 = {
        ...state.game2048,
        board: result.board,
        score: state.game2048.score + result.gainedScore,
        hasWon,
        isOver,
        status: isOver
            ? "게임 오버! 다시 시작해서 한 번 더 도전해 보세요."
            : hasWon && !state.game2048.hasWon
                ? "2048을 만들었어요! 계속 이어서 플레이할 수 있습니다."
                : "좋아요. 다음 수를 이어가 보세요."
    };

    render2048Game();
}

function createEmpty2048Board() {
    return Array.from({length: 4}, () => Array(4).fill(0));
}

function spawnRandom2048Tile(board) {
    const emptyCells = [];

    board.forEach((row, rowIndex) => {
        row.forEach((value, columnIndex) => {
            if (value === 0) {
                emptyCells.push({rowIndex, columnIndex});
            }
        });
    });

    if (emptyCells.length === 0) {
        return false;
    }

    const target = emptyCells[Math.floor(Math.random() * emptyCells.length)];
    board[target.rowIndex][target.columnIndex] = Math.random() < 0.9 ? 2 : 4;
    return true;
}

function run2048Move(board, direction) {
    const nextBoard = board.map((row) => row.slice());
    let moved = false;
    let gainedScore = 0;
    let created2048 = false;

    for (let index = 0; index < 4; index += 1) {
        const originalLine = get2048Line(nextBoard, direction, index);
        const forwardLine = direction === "right" || direction === "down"
            ? originalLine.slice().reverse()
            : originalLine.slice();
        const merged = merge2048Line(forwardLine);
        const resolvedLine = direction === "right" || direction === "down"
            ? merged.line.slice().reverse()
            : merged.line;

        if (!moved && !are2048LinesEqual(originalLine, resolvedLine)) {
            moved = true;
        }

        gainedScore += merged.score;
        created2048 = created2048 || merged.created2048;
        set2048Line(nextBoard, direction, index, resolvedLine);
    }

    return {
        board: nextBoard,
        moved,
        gainedScore,
        created2048
    };
}

function merge2048Line(line) {
    const filtered = line.filter((value) => value !== 0);
    const mergedLine = [];
    let score = 0;
    let created2048 = false;

    for (let index = 0; index < filtered.length; index += 1) {
        const current = filtered[index];
        const next = filtered[index + 1];

        if (current === next) {
            const mergedValue = current * 2;
            mergedLine.push(mergedValue);
            score += mergedValue;
            created2048 = created2048 || mergedValue === 2048;
            index += 1;
            continue;
        }

        mergedLine.push(current);
    }

    while (mergedLine.length < 4) {
        mergedLine.push(0);
    }

    return {
        line: mergedLine,
        score,
        created2048
    };
}

function get2048Line(board, direction, index) {
    if (direction === "left" || direction === "right") {
        return board[index].slice();
    }

    return board.map((row) => row[index]);
}

function set2048Line(board, direction, index, line) {
    if (direction === "left" || direction === "right") {
        board[index] = line.slice();
        return;
    }

    line.forEach((value, rowIndex) => {
        board[rowIndex][index] = value;
    });
}

function are2048LinesEqual(first, second) {
    return first.every((value, index) => value === second[index]);
}

function canMove2048(board) {
    for (let rowIndex = 0; rowIndex < 4; rowIndex += 1) {
        for (let columnIndex = 0; columnIndex < 4; columnIndex += 1) {
            const value = board[rowIndex][columnIndex];
            if (value === 0) {
                return true;
            }

            if (columnIndex < 3 && board[rowIndex][columnIndex + 1] === value) {
                return true;
            }

            if (rowIndex < 3 && board[rowIndex + 1][columnIndex] === value) {
                return true;
            }
        }
    }

    return false;
}

function initializeBreakoutGame() {
    state.breakout = {
        ...state.breakout,
        initialized: true,
        theme: readBreakoutTheme()
    };
    resetBreakoutGame();
}

function handleBreakoutStartButton() {
    startBreakoutGame();
}

function handleBreakoutPauseButton() {
    if (state.breakout.running) {
        pauseBreakoutGame("manual");
        return;
    }

    if (state.breakout.started && !state.breakout.gameOver && !state.breakout.cleared) {
        startBreakoutGame();
    }
}

function restartBreakoutGame() {
    cancelBreakoutAnimation();
    resetBreakoutGame();
}

function resetBreakoutGame() {
    const canvas = elements.breakoutCanvas;
    if (!canvas) {
        return;
    }

    const canvasWidth = canvas.width;
    const canvasHeight = canvas.height;
    const paddleWidth = 108;
    const paddleHeight = 14;
    const paddleY = canvasHeight - 34;
    const ballRadius = 10;
    const theme = readBreakoutTheme();
    const brickConfig = createBreakoutBrickConfig(canvasWidth, theme);
    const initialSpeed = 0.377;

    state.breakout = {
        ...state.breakout,
        initialized: true,
        running: false,
        paused: true,
        started: false,
        gameOver: false,
        cleared: false,
        pauseReason: "idle",
        score: 0,
        paddleDirection: 0,
        leftPressed: false,
        rightPressed: false,
        animationFrameId: null,
        lastTimestamp: 0,
        canvasWidth,
        canvasHeight,
        paddle: {
            x: (canvasWidth - paddleWidth) / 2,
            y: paddleY,
            width: paddleWidth,
            height: paddleHeight,
            speed: 0.62
        },
        ball: {
            x: canvasWidth / 2,
            y: paddleY - ballRadius - 12,
            radius: ballRadius,
            vx: (Math.random() > 0.5 ? 1 : -1) * 0.18,
            vy: -Math.sqrt(initialSpeed * initialSpeed - 0.18 * 0.18)
        },
        bricks: createBreakoutBricks(brickConfig),
        theme,
        brickConfig,
        nextRowSeed: brickConfig.rows
    };

    state.breakout.status = "게임 시작을 누르면 바로 진행됩니다.";
    renderBreakoutGame();
}

function startBreakoutGame() {
    if (!state.breakout.initialized) {
        initializeBreakoutGame();
    }

    if (state.breakout.gameOver || state.breakout.cleared) {
        resetBreakoutGame();
    }

    if (state.breakout.running) {
        return;
    }

    state.breakout.running = true;
    state.breakout.paused = false;
    state.breakout.started = true;
    state.breakout.pauseReason = "play";
    state.breakout.lastTimestamp = 0;
    state.breakout.status = "게임 진행 중";
    updateBreakoutControls();
    renderBreakoutGame();
    state.breakout.animationFrameId = window.requestAnimationFrame(stepBreakoutGame);
}

function pauseBreakoutGame(reason = "manual") {
    cancelBreakoutAnimation();
    state.breakout.running = false;
    state.breakout.paused = true;
    state.breakout.pauseReason = reason;
    state.breakout.lastTimestamp = 0;
    state.breakout.paddleDirection = 0;
    state.breakout.leftPressed = false;
    state.breakout.rightPressed = false;

    if (!state.breakout.gameOver && !state.breakout.cleared && state.breakout.started) {
        state.breakout.status = reason === "hidden"
            ? "이어서 플레이할 수 있습니다."
            : "일시정지됨";
    }

    updateBreakoutControls();
    renderBreakoutGame();
}

function cancelBreakoutAnimation() {
    if (!state.breakout.animationFrameId) {
        return;
    }

    window.cancelAnimationFrame(state.breakout.animationFrameId);
    state.breakout.animationFrameId = null;
}

function stepBreakoutGame(timestamp) {
    if (!state.breakout.running) {
        return;
    }

    if (!state.breakout.lastTimestamp) {
        state.breakout.lastTimestamp = timestamp;
    }

    const delta = Math.min(timestamp - state.breakout.lastTimestamp, 24);
    state.breakout.lastTimestamp = timestamp;

    updateBreakoutGame(delta);
    renderBreakoutGame();

    if (state.breakout.running) {
        state.breakout.animationFrameId = window.requestAnimationFrame(stepBreakoutGame);
    }
}

function updateBreakoutGame(delta) {
    const {paddle, ball, canvasWidth, canvasHeight} = state.breakout;
    if (!paddle || !ball) {
        return;
    }

    paddle.x += state.breakout.paddleDirection * paddle.speed * delta;
    paddle.x = clamp(paddle.x, 0, canvasWidth - paddle.width);

    const previousX = ball.x;
    const previousY = ball.y;

    ball.x += ball.vx * delta;
    ball.y += ball.vy * delta;

    if (ball.x - ball.radius <= 0) {
        ball.x = ball.radius;
        ball.vx = Math.abs(ball.vx);
    } else if (ball.x + ball.radius >= canvasWidth) {
        ball.x = canvasWidth - ball.radius;
        ball.vx = -Math.abs(ball.vx);
    }

    if (ball.y - ball.radius <= 0) {
        ball.y = ball.radius;
        ball.vy = Math.abs(ball.vy);
    }

    if (
        ball.vy > 0 &&
        ball.y + ball.radius >= paddle.y &&
        ball.y - ball.radius <= paddle.y + paddle.height &&
        ball.x + ball.radius >= paddle.x &&
        ball.x - ball.radius <= paddle.x + paddle.width
    ) {
        const speed = clamp(Math.hypot(ball.vx, ball.vy), 0.377, state.breakout.maxBallSpeed);
        const hitRatio = clamp((ball.x - (paddle.x + paddle.width / 2)) / (paddle.width / 2), -1, 1);
        ball.x = clamp(ball.x, paddle.x + ball.radius, paddle.x + paddle.width - ball.radius);
        ball.y = paddle.y - ball.radius - 1;
        ball.vx = speed * hitRatio;
        ball.vy = -Math.max(0.22, Math.sqrt(Math.max(speed * speed - ball.vx * ball.vx, 0.04)));
    }

    let collidedBrick = false;

    for (const brick of state.breakout.bricks) {
        if (!brick.active) {
            continue;
        }

        if (
            ball.x + ball.radius < brick.x ||
            ball.x - ball.radius > brick.x + brick.width ||
            ball.y + ball.radius < brick.y ||
            ball.y - ball.radius > brick.y + brick.height
        ) {
            continue;
        }

        brick.active = false;
        state.breakout.score += 100;

        const cameFromTop = previousY + ball.radius <= brick.y;
        const cameFromBottom = previousY - ball.radius >= brick.y + brick.height;
        const cameFromLeft = previousX + ball.radius <= brick.x;
        const cameFromRight = previousX - ball.radius >= brick.x + brick.width;

        if (cameFromTop || cameFromBottom) {
            ball.vy *= -1;
        } else if (cameFromLeft || cameFromRight) {
            ball.vx *= -1;
        } else {
            ball.vy *= -1;
        }

        adjustBreakoutBallSpeed(state.breakout.speedIncreaseFactor);
        collidedBrick = true;
        break;
    }

    if (collidedBrick) {
        const recycledRows = recycleBreakoutRows();
        if (recycledRows && !state.breakout.gameOver) {
            state.breakout.status = "한 줄 제거! 새 벽돌이 내려옵니다.";
        }
    }

    if (ball.y - ball.radius > canvasHeight) {
        finishBreakoutGame("gameOver");
    }
}

function finishBreakoutGame(result) {
    cancelBreakoutAnimation();
    state.breakout.running = false;
    state.breakout.paused = false;
    state.breakout.gameOver = result === "gameOver";
    state.breakout.cleared = false;
    state.breakout.pauseReason = result;
    state.breakout.paddleDirection = 0;
    state.breakout.status = "게임 오버! 다시 시작해서 한 번 더 도전해 보세요.";
    updateBreakoutControls();
    renderBreakoutGame();
}

function renderBreakoutGame() {
    if (!elements.breakoutCanvas) {
        return;
    }

    if (!state.breakout.theme) {
        state.breakout.theme = readBreakoutTheme();
    }

    elements.breakoutScore.textContent = String(state.breakout.score);
    elements.breakoutStatus.textContent = state.breakout.status;
    updateBreakoutControls();

    const context = elements.breakoutCanvas.getContext("2d");
    if (!context) {
        return;
    }

    drawBreakoutScene(context);
}

function updateBreakoutControls() {
    if (!elements.breakoutStartButton || !elements.breakoutPauseButton) {
        return;
    }

    elements.breakoutStartButton.textContent = state.breakout.started && !state.breakout.gameOver && !state.breakout.cleared
        ? (state.breakout.running ? "진행 중" : "계속하기")
        : "게임 시작";
    elements.breakoutStartButton.disabled = state.breakout.running;

    elements.breakoutPauseButton.textContent = state.breakout.running ? "일시정지" : "일시정지";
    elements.breakoutPauseButton.disabled = !state.breakout.started || state.breakout.gameOver || state.breakout.cleared;
}

function drawBreakoutScene(context) {
    const {canvasWidth, canvasHeight, theme, bricks, paddle, ball} = state.breakout;

    context.clearRect(0, 0, canvasWidth, canvasHeight);

    const background = context.createLinearGradient(0, 0, 0, canvasHeight);
    background.addColorStop(0, "rgba(251, 253, 255, 0.98)");
    background.addColorStop(1, "rgba(216, 244, 255, 0.96)");
    context.fillStyle = background;
    context.fillRect(0, 0, canvasWidth, canvasHeight);

    bricks.forEach((brick) => {
        if (!brick.active) {
            return;
        }

        drawRoundedRect(context, brick.x, brick.y, brick.width, brick.height, 8, brick.color, "rgba(79, 55, 35, 0.12)");
    });

    if (paddle) {
        drawRoundedRect(context, paddle.x, paddle.y, paddle.width, paddle.height, 8, theme.teal, theme.tealDeep);
    }

    if (ball) {
        context.beginPath();
        context.fillStyle = theme.coral;
        context.arc(ball.x, ball.y, ball.radius, 0, Math.PI * 2);
        context.fill();
        context.lineWidth = 3;
        context.strokeStyle = "rgba(79, 55, 35, 0.14)";
        context.stroke();
    }
}

function createBreakoutBrickConfig(canvasWidth, theme) {
    const rows = 5;
    const columns = 7;
    const paddingX = 18;
    const topOffset = 24;
    const gap = 10;
    const height = 22;
    const width = (canvasWidth - paddingX * 2 - gap * (columns - 1)) / columns;
    const colors = [theme.coral, theme.gold, theme.mint, theme.teal, "#ffb86f"];

    return {
        rows,
        columns,
        paddingX,
        topOffset,
        gap,
        height,
        width,
        rowStep: height + gap,
        colors
    };
}

function createBreakoutBricks(config) {
    const bricks = [];

    for (let rowIndex = 0; rowIndex < config.rows; rowIndex += 1) {
        for (let columnIndex = 0; columnIndex < config.columns; columnIndex += 1) {
            bricks.push(createBreakoutBrick(config, rowIndex, columnIndex, rowIndex));
        }
    }

    return bricks;
}

function createBreakoutBrick(config, rowIndex, columnIndex, colorSeed) {
    return {
        x: config.paddingX + columnIndex * (config.width + config.gap),
        y: config.topOffset + rowIndex * config.rowStep,
        width: config.width,
        height: config.height,
        color: config.colors[colorSeed % config.colors.length],
        active: true,
        row: rowIndex,
        column: columnIndex
    };
}

function recycleBreakoutRows() {
    const {brickConfig} = state.breakout;
    if (!brickConfig) {
        return false;
    }

    let recycled = false;

    while (true) {
        const clearedRow = findClearedBreakoutRow();
        if (clearedRow === null) {
            break;
        }

        recycled = true;
        state.breakout.bricks = state.breakout.bricks.filter((brick) => brick.row !== clearedRow);

        state.breakout.bricks.forEach((brick) => {
            if (brick.row < clearedRow) {
                brick.row += 1;
                brick.y += brickConfig.rowStep;
            }
        });

        const colorSeed = state.breakout.nextRowSeed;
        state.breakout.nextRowSeed += 1;

        for (let columnIndex = 0; columnIndex < brickConfig.columns; columnIndex += 1) {
            state.breakout.bricks.push(createBreakoutBrick(brickConfig, 0, columnIndex, colorSeed));
        }

        state.breakout.bricks.sort((left, right) => {
            if (left.row !== right.row) {
                return left.row - right.row;
            }
            return left.column - right.column;
        });
    }

    return recycled;
}

function findClearedBreakoutRow() {
    const rows = [...new Set(state.breakout.bricks.map((brick) => brick.row))].sort((left, right) => left - right);

    for (const row of rows) {
        const rowBricks = state.breakout.bricks.filter((brick) => brick.row === row);
        if (rowBricks.length > 0 && rowBricks.every((brick) => !brick.active)) {
            return row;
        }
    }

    return null;
}

function readBreakoutTheme() {
    const computedStyle = getComputedStyle(document.documentElement);
    return {
        coral: computedStyle.getPropertyValue("--coral").trim() || "#ff8d73",
        gold: computedStyle.getPropertyValue("--gold").trim() || "#f7ca63",
        mint: computedStyle.getPropertyValue("--mint").trim() || "#42c6ae",
        teal: computedStyle.getPropertyValue("--teal").trim() || "#0d8e97",
        tealDeep: computedStyle.getPropertyValue("--teal-deep").trim() || "#0d6f7c"
    };
}

function adjustBreakoutBallSpeed(multiplier) {
    const {ball, maxBallSpeed} = state.breakout;
    if (!ball) {
        return;
    }

    const currentSpeed = Math.hypot(ball.vx, ball.vy);
    if (!currentSpeed) {
        return;
    }

    const nextSpeed = clamp(currentSpeed * multiplier, 0.377, maxBallSpeed);
    const ratio = nextSpeed / currentSpeed;
    ball.vx *= ratio;
    ball.vy *= ratio;
}

function drawRoundedRect(context, x, y, width, height, radius, fillStyle, strokeStyle) {
    context.beginPath();
    context.moveTo(x + radius, y);
    context.lineTo(x + width - radius, y);
    context.quadraticCurveTo(x + width, y, x + width, y + radius);
    context.lineTo(x + width, y + height - radius);
    context.quadraticCurveTo(x + width, y + height, x + width - radius, y + height);
    context.lineTo(x + radius, y + height);
    context.quadraticCurveTo(x, y + height, x, y + height - radius);
    context.lineTo(x, y + radius);
    context.quadraticCurveTo(x, y, x + radius, y);
    context.closePath();
    context.fillStyle = fillStyle;
    context.fill();

    if (strokeStyle) {
        context.lineWidth = 2;
        context.strokeStyle = strokeStyle;
        context.stroke();
    }
}

function clamp(value, min, max) {
    return Math.min(Math.max(value, min), max);
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
    closeGame2048Modal();
    closeBreakoutModalHandler();
    elements.authSection.classList.remove("hidden");
    elements.appSection.classList.add("hidden");
}

function showAppSection() {
    elements.authSection.classList.add("hidden");
    elements.appSection.classList.remove("hidden");
    setActiveMobilePanel(resolveInitialMobilePanel(), {updateHash: false});
    syncResponsiveLayout();
    focusChatPanelIfRequested();
}

function focusChatPanelIfRequested() {
    if (window.location.hash !== "#chat") {
        return;
    }

    window.requestAnimationFrame(() => {
        if (!isMobileViewport()) {
            elements.chatPanel?.scrollIntoView({
                block: "center",
                behavior: "smooth"
            });
        }
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


