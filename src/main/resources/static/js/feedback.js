const FEEDBACK_STORAGE_KEY = "ddanjit-session-token";

const feedbackState = {
    sessionToken: localStorage.getItem(FEEDBACK_STORAGE_KEY),
    me: null,
    toastTimerId: null
};

const feedbackElements = {
    onlineCount: document.getElementById("feedbackOnlineCount"),
    form: document.getElementById("feedbackForm"),
    nickname: document.getElementById("feedbackNickname"),
    school: document.getElementById("feedbackSchool"),
    content: document.getElementById("feedbackContent"),
    list: document.getElementById("feedbackList"),
    userHint: document.getElementById("feedbackUserHint"),
    toast: document.getElementById("feedbackToast")
};

document.addEventListener("DOMContentLoaded", async () => {
    feedbackElements.form?.addEventListener("submit", handleFeedbackSubmit);
    await Promise.all([
        loadOnlineCount(),
        loadFeedbackPosts(),
        restoreFeedbackUser()
    ]);
});

async function restoreFeedbackUser() {
    if (!feedbackState.sessionToken) {
        return;
    }

    try {
        const response = await feedbackApiRequest("/api/auth/me");
        feedbackState.me = response.data;
        feedbackElements.nickname.value = response.data.nickname || "";
        feedbackElements.school.value = response.data.school || "";
        feedbackElements.userHint.textContent = "현재 로그인한 닉네임과 학교가 자동으로 채워졌습니다.";
    } catch (error) {
        feedbackState.sessionToken = null;
        localStorage.removeItem(FEEDBACK_STORAGE_KEY);
    }
}

async function loadOnlineCount() {
    try {
        const response = await feedbackApiRequest("/api/presence/count");
        feedbackElements.onlineCount.textContent = `${response.data.count}명`;
    } catch (error) {
        feedbackElements.onlineCount.textContent = "0명";
    }
}

async function loadFeedbackPosts() {
    try {
        const response = await feedbackApiRequest("/api/feedback/posts");
        renderFeedbackPosts(response.data.posts || []);
    } catch (error) {
        renderFeedbackEmpty("아직 등록된 피드백이 없습니다.");
    }
}

async function handleFeedbackSubmit(event) {
    event.preventDefault();

    const payload = {
        nickname: feedbackElements.nickname.value.trim(),
        school: feedbackElements.school.value.trim(),
        content: feedbackElements.content.value.trim()
    };

    try {
        await feedbackApiRequest("/api/feedback/posts", {
            method: "POST",
            body: JSON.stringify(payload)
        });

        feedbackElements.content.value = "";
        if (!feedbackState.me) {
            feedbackElements.nickname.value = payload.nickname;
            feedbackElements.school.value = payload.school;
        }

        await loadFeedbackPosts();
        showFeedbackToast("피드백이 등록되었습니다.");
    } catch (error) {
        showFeedbackToast(error.message);
    }
}

function renderFeedbackPosts(posts) {
    if (!feedbackElements.list) {
        return;
    }

    if (!Array.isArray(posts) || posts.length === 0) {
        renderFeedbackEmpty("아직 등록된 피드백이 없습니다.");
        return;
    }

    const fragment = document.createDocumentFragment();

    posts.forEach((post) => {
        const article = document.createElement("article");
        article.className = "feedback-post";
        article.innerHTML = `
            <div class="feedback-post-head">
                <div class="feedback-post-meta">
                    <strong>${post.nickname}</strong>
                    <span>${post.school}</span>
                </div>
                <time>${formatFeedbackTime(post.createdAt)}</time>
            </div>
            <p class="feedback-post-content"></p>
        `;
        article.querySelector(".feedback-post-content").textContent = post.content;
        fragment.append(article);
    });

    feedbackElements.list.replaceChildren(fragment);
}

function renderFeedbackEmpty(message) {
    feedbackElements.list.innerHTML = `<div class="empty-box">${message}</div>`;
}

async function feedbackApiRequest(url, options = {}) {
    const headers = {};

    if (options.body) {
        headers["Content-Type"] = "application/json";
    }

    if (feedbackState.sessionToken) {
        headers.Authorization = `Bearer ${feedbackState.sessionToken}`;
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

function formatFeedbackTime(timestamp) {
    return new Date(timestamp).toLocaleString("ko-KR", {
        year: "numeric",
        month: "2-digit",
        day: "2-digit",
        hour: "2-digit",
        minute: "2-digit"
    });
}

function showFeedbackToast(message) {
    feedbackElements.toast.textContent = message;
    feedbackElements.toast.classList.remove("hidden");

    window.clearTimeout(feedbackState.toastTimerId);
    feedbackState.toastTimerId = window.setTimeout(() => {
        feedbackElements.toast.classList.add("hidden");
    }, 2600);
}
