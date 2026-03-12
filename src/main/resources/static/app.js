(function () {
  const AVATAR_COLORS = [
    "#bf4c24", "#116466", "#7a5195", "#ef5675", "#2d6a4f",
    "#e07b39", "#3a86a8", "#6a4c93", "#1982c4", "#8ac926"
  ];
  const EMOJIS = [
    "\u{1F44D}", "\u{1F44E}", "\u{2764}\u{FE0F}", "\u{1F602}", "\u{1F525}", "\u{1F389}", "\u{1F44B}", "\u{1F914}",
    "\u{1F60D}", "\u{1F62D}", "\u{1F4AF}", "\u{2705}", "\u{274C}", "\u{1F680}", "\u{1F4A1}", "\u{1F440}",
    "\u{1F64F}", "\u{1F60E}", "\u{1F31F}", "\u{1F4AC}", "\u{1F3AF}", "\u{26A1}", "\u{1F4AA}", "\u{1F381}"
  ];

  const state = {
    token: localStorage.getItem("pulsechat.authToken") || "",
    theme: localStorage.getItem("pulsechat.theme") || "light",
    me: null,
    conversations: [],
    users: [],
    selectedId: null,
    client: null,
    connecting: null,
    socketUserId: null,
    roomSubs: [],
    userSubs: [],
    messageIds: new Set(),
    typingUsers: new Map(),
    typingTimer: null,
    lastSenderId: null,
    unreadCounts: {},
    onlineUsers: new Set()
  };

  const el = byId([
    "showLoginButton", "showRegisterButton", "loginForm", "registerForm",
    "loginUserIdInput", "loginPasswordInput", "registerDisplayNameInput",
    "registerUserIdInput", "registerPasswordInput", "activeIdentity",
    "activeIdentityName", "activeIdentityId", "logoutButton", "refreshSessionButton",
    "refreshButton", "newGroupButton", "newDirectButton", "conversationList",
    "conversationBadge", "conversationTitle", "conversationMeta", "memberStrip",
    "conversationWarning", "blockConversationButton", "deleteConversationButton",
    "messageList", "typingIndicator", "composerForm", "messageInput", "sendButton",
    "aiPromptButton", "copyConversationButton", "activityLog", "connectionBadge",
    "wsUrlLabel", "userDirectory", "conversationSheet", "composerOverlay",
    "closeSheetButton", "sheetEyebrow", "sheetTitle", "conversationForm",
    "conversationType", "conversationTitleInput", "memberIdsInput", "toastStack",
    "themeToggle", "conversationSearch", "charCounter", "scrollBottomButton",
    "emojiButton", "emojiPicker", "emojiGrid"
  ]);

  const wsUrl = `${location.protocol === "https:" ? "wss" : "ws"}://${location.host}/ws`;
  el.wsUrlLabel.textContent = wsUrl;

  applyTheme(state.theme);
  buildEmojiPicker();
  wire();
  bootstrap();

  async function bootstrap() {
    if (!state.token) return renderLoggedOut();
    try {
      state.me = await api("GET", "/api/auth/me");
      renderIdentity();
      await loadWorkspace();
    } catch {
      logout(false);
      renderLoggedOut();
    }
  }

  function wire() {
    el.showLoginButton.onclick = () => setAuthMode("login");
    el.showRegisterButton.onclick = () => setAuthMode("register");
    el.loginForm.onsubmit = async (e) => { e.preventDefault(); await login(); };
    el.registerForm.onsubmit = async (e) => { e.preventDefault(); await register(); };
    el.logoutButton.onclick = () => logout(true);
    el.refreshSessionButton.onclick = async () => loadWorkspace();
    el.refreshButton.onclick = async () => loadWorkspace();
    el.newGroupButton.onclick = () => openSheet("GROUP");
    el.newDirectButton.onclick = () => openSheet("DIRECT");
    el.closeSheetButton.onclick = closeSheet;
    el.composerOverlay.onclick = closeSheet;
    el.conversationForm.onsubmit = async (e) => { e.preventDefault(); await createConversation(); };
    el.composerForm.onsubmit = async (e) => { e.preventDefault(); await sendMessage(); };
    el.aiPromptButton.onclick = () => {
      if (!el.messageInput.value.startsWith("@ai ")) el.messageInput.value = (`@ai ${el.messageInput.value}`).trim();
      autosize();
      el.messageInput.focus();
    };
    el.messageInput.oninput = () => {
      autosize();
      updateCharCounter();
      announceTyping();
    };
    el.copyConversationButton.onclick = async () => {
      const c = selected();
      if (!c) return;
      try {
        await navigator.clipboard.writeText(c.id);
        toast("Conversation ID copied.");
      } catch {
        toast("Copy failed.", true);
      }
    };
    el.deleteConversationButton.onclick = async () => deleteConversation();
    el.blockConversationButton.onclick = async () => toggleConversationBlock();
    el.themeToggle.onclick = () => toggleTheme();
    el.conversationSearch.oninput = () => filterConversations();
    el.scrollBottomButton.onclick = () => { scrollEnd(true); el.scrollBottomButton.classList.add("hidden"); };
    el.messageList.onscroll = () => {
      const gap = el.messageList.scrollHeight - el.messageList.scrollTop - el.messageList.clientHeight;
      el.scrollBottomButton.classList.toggle("hidden", gap < 120);
    };
    el.emojiButton.onclick = (e) => { e.stopPropagation(); el.emojiPicker.classList.toggle("hidden"); };
    document.addEventListener("click", (e) => {
      if (!el.emojiPicker.contains(e.target) && e.target !== el.emojiButton) el.emojiPicker.classList.add("hidden");
    });
    document.addEventListener("keydown", (e) => {
      if (e.key === "Escape") { closeSheet(); el.emojiPicker.classList.add("hidden"); }
      if ((e.ctrlKey || e.metaKey) && e.key === "Enter" && document.activeElement === el.messageInput) {
        e.preventDefault();
        sendMessage();
      }
    });
  }

  function setAuthMode(mode) {
    const showLogin = mode === "login";
    el.showLoginButton.classList.toggle("active", showLogin);
    el.showRegisterButton.classList.toggle("active", !showLogin);
    el.loginForm.classList.toggle("hidden", !showLogin);
    el.registerForm.classList.toggle("hidden", showLogin);
  }

  async function login() {
    const userId = clean(el.loginUserIdInput.value);
    const password = el.loginPasswordInput.value.trim();
    if (!userId || !password) return toast("Enter user ID and password.", true);
    try {
      const res = await raw("POST", "/api/auth/login", { userId, password }, false);
      applyAuth(res);
      toast(`Welcome back, ${res.displayName}.`);
      await loadWorkspace();
    } catch (e) {
      toast(e.message || "Login failed.", true);
    }
  }

  async function register() {
    const displayName = clean(el.registerDisplayNameInput.value);
    const userId = clean(el.registerUserIdInput.value);
    const password = el.registerPasswordInput.value.trim();
    if (!displayName || !userId || !password) return toast("Fill display name, user ID, and password.", true);
    try {
      const res = await raw("POST", "/api/auth/register", { displayName, userId, password }, false);
      applyAuth(res);
      toast(`Account created for ${res.displayName}.`);
      await loadWorkspace();
    } catch (e) {
      toast(e.message || "Registration failed.", true);
    }
  }

  function applyAuth(res) {
    state.token = res.authToken;
    localStorage.setItem("pulsechat.authToken", state.token);
    state.me = { userId: res.userId, displayName: res.displayName };
    el.loginPasswordInput.value = "";
    el.registerPasswordInput.value = "";
    disconnect();
    state.selectedId = null;
    renderIdentity();
  }

  function logout(showToast) {
    state.token = "";
    state.me = null;
    state.conversations = [];
    state.users = [];
    state.selectedId = null;
    localStorage.removeItem("pulsechat.authToken");
    disconnect();
    renderLoggedOut();
    if (showToast) toast("Logged out.");
  }

  function renderLoggedOut() {
    setAuthMode("login");
    el.activeIdentity.classList.add("hidden");
    el.showLoginButton.disabled = false;
    el.showRegisterButton.disabled = false;
    el.refreshButton.disabled = true;
    el.newGroupButton.disabled = true;
    el.newDirectButton.disabled = true;
    el.aiPromptButton.disabled = true;
    el.conversationList.innerHTML = empty("Login required", "Create an account or sign in to load conversations.");
    el.userDirectory.innerHTML = empty("No access yet", "Sign in to browse users and launch direct chats.");
    clearStage(false);
  }

  function renderIdentity() {
    const auth = Boolean(state.me);
    el.activeIdentity.classList.toggle("hidden", !auth);
    el.showLoginButton.disabled = auth;
    el.showRegisterButton.disabled = auth;
    el.refreshButton.disabled = !auth;
    el.newGroupButton.disabled = !auth;
    el.newDirectButton.disabled = !auth;
    if (auth) {
      el.activeIdentityName.textContent = state.me.displayName;
      el.activeIdentityId.textContent = `@${state.me.userId}`;
      el.activeIdentity.classList.remove("hidden");
      el.loginForm.classList.add("hidden");
      el.registerForm.classList.add("hidden");
    }
  }

  async function loadWorkspace() {
    if (!state.me) return renderLoggedOut();
    try {
      const [conversations, users] = await Promise.all([api("GET", "/api/conversations"), api("GET", "/api/users")]);
      state.conversations = Array.isArray(conversations) ? conversations : [];
      state.users = Array.isArray(users) ? users : [];
      renderConversations();
      renderUsers();
      pollPresence();
      if (state.selectedId && state.conversations.some((c) => c.id === state.selectedId)) return selectConversation(state.selectedId);
      if (state.conversations.length) return selectConversation(state.conversations[0].id);
      clearStage(true);
    } catch (e) {
      toast(e.message || "Failed to load workspace.", true);
    }
  }

  function renderConversations() {
    if (!state.me) return;
    if (!state.conversations.length) {
      el.conversationList.innerHTML = empty("No rooms yet", "Use New Group or start a one-on-one chat from the people directory.");
      return;
    }
    const query = (el.conversationSearch.value || "").toLowerCase();
    const filtered = query ? state.conversations.filter((c) => c.title.toLowerCase().includes(query)) : state.conversations;
    el.conversationList.innerHTML = "";
    if (!filtered.length) {
      el.conversationList.innerHTML = empty("No matches", "Try a different search term.");
      return;
    }
    filtered.forEach((c) => {
      const button = document.createElement("button");
      button.type = "button";
      button.className = `conversation-card${c.id === state.selectedId ? " active" : ""}`;
      const status = c.blocked ? `<span class="conversation-pill">Blocked</span>` : "";
      const unread = state.unreadCounts[c.id] || 0;
      const unreadBadge = unread > 0 ? `<span class="unread-badge">${unread > 99 ? "99+" : unread}</span>` : "";
      const avatarName = c.type === "DIRECT" ? (c.memberIds.find((m) => m !== (state.me && state.me.userId)) || "?") : c.title;
      button.innerHTML = `
        <div class="conversation-card-row">${avatar(avatarName, false)}
        <div style="flex:1;min-width:0">
        <div class="conversation-card-meta"><span>${c.type === "DIRECT" ? "1:1" : "GROUP"}</span><span>${relative(c.updatedAt || c.createdAt)}</span></div>
        <div class="conversation-card-title">${safe(c.title)}</div>
        <div class="conversation-card-foot"><span>${c.memberIds.length} members</span><span>${c.id.slice(0, 8)}</span></div>
        </div></div>
        ${status}${unreadBadge}`;
      button.onclick = async () => selectConversation(c.id);
      el.conversationList.appendChild(button);
    });
  }

  function renderUsers() {
    if (!state.me) return;
    if (!state.users.length) {
      el.userDirectory.innerHTML = empty("No other users", "Create another account to test one-on-one chat.");
      return;
    }
    el.userDirectory.innerHTML = "";
    state.users.forEach((u) => {
      const card = document.createElement("div");
      card.className = "directory-user";
      const status = userStatus(u);
      const online = state.onlineUsers.has(u.userId);
      card.innerHTML = `<div class="directory-user-head">${avatar(u.displayName || u.userId, online, true)}<div><div class="directory-user-name">${safe(u.displayName)}</div><div class="directory-user-id">@${safe(u.userId)}</div></div></div>${status ? `<div class="directory-status">${safe(status)}</div>` : ""}`;

      const actions = document.createElement("div");
      actions.className = "directory-actions";

      const directButton = document.createElement("button");
      directButton.type = "button";
      directButton.className = "button button-secondary";
      directButton.textContent = "Start 1:1 chat";
      directButton.disabled = !u.directChatAllowed;
      directButton.onclick = async () => startDirect(u);
      actions.appendChild(directButton);

      const blockButton = document.createElement("button");
      blockButton.type = "button";
      blockButton.className = u.blockedByMe ? "button button-ghost" : "button button-danger";
      blockButton.textContent = u.blockedByMe ? "Unblock" : "Block";
      blockButton.onclick = async () => toggleBlock(u);
      actions.appendChild(blockButton);

      card.appendChild(actions);
      el.userDirectory.appendChild(card);
    });
  }

  async function startDirect(user) {
    if (!user.directChatAllowed) {
      return toast("Direct chat is unavailable because one user blocked the other.", true);
    }
    try {
      const c = await api("POST", "/api/conversations", {
        title: `Direct with ${user.displayName}`,
        type: "DIRECT",
        memberIds: [user.userId]
      });
      await loadWorkspace();
      await selectConversation(c.id);
      toast(`Direct chat ready with ${user.displayName}.`);
    } catch (e) {
      toast(e.message || "Direct chat creation failed.", true);
    }
  }

  async function selectConversation(id) {
    const c = state.conversations.find((x) => x.id === id);
    if (!c) return;
    state.selectedId = id;
    state.unreadCounts[id] = 0;
    renderConversations();
    renderHeader(c);
    await loadHistory(c.id);
    await ensureSocket();
    subscribeRoom(c.id);
  }

  function renderHeader(c) {
    el.conversationBadge.textContent = c.type === "GROUP" ? "Group room" : "One-on-one";
    el.conversationTitle.textContent = c.title;
    el.conversationMeta.textContent = `${c.memberIds.length} members | Updated ${relative(c.updatedAt || c.createdAt)}`;
    el.memberStrip.innerHTML = "";
    c.memberIds.forEach((m) => {
      const chip = document.createElement("div");
      chip.className = "member-chip";
      const online = state.onlineUsers.has(m);
      chip.innerHTML = `${avatar(m, online, true, true)}${safe(m)}`;
      el.memberStrip.appendChild(chip);
    });
    const partner = c.type === "DIRECT" ? directPartner(c) : null;
    const warning = conversationWarning(c, partner);
    el.conversationWarning.textContent = warning;
    el.conversationWarning.classList.toggle("hidden", !warning);
    el.blockConversationButton.classList.toggle("hidden", c.type !== "DIRECT" || !partner);
    if (partner) {
      el.blockConversationButton.textContent = partner.blockedByMe ? "Unblock User" : "Block User";
      el.blockConversationButton.disabled = false;
    }
    el.deleteConversationButton.classList.remove("hidden");
    el.deleteConversationButton.textContent = deleteLabel(c);
    el.messageInput.disabled = c.blocked;
    el.sendButton.disabled = c.blocked;
    el.aiPromptButton.disabled = c.blocked;
    el.copyConversationButton.disabled = false;
    if (c.blocked) {
      el.messageInput.value = "";
      autosize();
    }
  }

  async function loadHistory(id) {
    try {
      const history = await api("GET", `/api/conversations/${id}/messages?limit=100`);
      const items = Array.isArray(history.items) ? history.items : [];
      state.messageIds = new Set();
      state.lastSenderId = null;
      el.messageList.innerHTML = "";
      if (!items.length) {
        el.messageList.innerHTML = empty("Room is ready", "Start the first message to light up the thread.");
        return;
      }
      items.forEach((m) => appendMessage(m, false));
      scrollEnd(false);
    } catch (e) {
      toast(e.message || "Could not load message history.", true);
    }
  }

  async function ensureSocket() {
    if (!state.me) throw new Error("Login first.");
    if (state.client && state.client.connected && state.socketUserId === state.me.userId) return setOnline(true);
    if (state.connecting) return state.connecting;
    disconnect();
    setOnline(false);
    state.client = new StompJs.Client({
      brokerURL: wsUrl,
      reconnectDelay: 2500,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      connectHeaders: { "x-auth-token": state.token }
    });
    state.connecting = new Promise((resolve, reject) => {
      let done = false;
      state.client.onConnect = () => {
        state.socketUserId = state.me.userId;
        setOnline(true);
        resetSubs();
        subscribeUserQueues();
        if (state.selectedId) subscribeRoom(state.selectedId);
        state.connecting = null;
        done = true;
        resolve();
      };
      state.client.onStompError = (frame) => fail(frame && frame.body ? frame.body : "STOMP error");
      state.client.onWebSocketError = () => fail("WebSocket connection failed.");
      state.client.onWebSocketClose = () => {
        setOnline(false);
        resetSubs();
      };
      function fail(message) {
        setOnline(false);
        if (!done) {
          state.connecting = null;
          done = true;
          reject(new Error(message));
        } else {
          toast(message, true);
        }
      }
    });
    state.client.activate();
    return state.connecting;
  }

  function subscribeUserQueues() {
    if (!state.client || !state.client.connected) return;
    state.userSubs.push(state.client.subscribe("/user/queue/acks", (f) => {
      const ack = JSON.parse(f.body);
      log(`Message persisted at ${clock(ack.persistedAt)}.`);
    }));
    state.userSubs.push(state.client.subscribe("/user/queue/errors", (f) => {
      const err = JSON.parse(f.body);
      toast(err.message || "Message rejected.", true);
    }));
  }

  function subscribeRoom(id) {
    if (!state.client || !state.client.connected) return;
    clearRoomSubs();
    state.roomSubs.push(state.client.subscribe(`/topic/conversations.${id}`, (f) => {
      const m = JSON.parse(f.body);
      if (m.conversationId === state.selectedId) {
        appendMessage(m, true);
      } else {
        state.unreadCounts[m.conversationId] = (state.unreadCounts[m.conversationId] || 0) + 1;
        playNotificationSound();
      }
      bumpConversation(m.conversationId, m.createdAt);
    }));
    state.roomSubs.push(state.client.subscribe(`/topic/conversations.${id}.typing`, (f) => {
      const s = JSON.parse(f.body);
      if (!state.me || s.userId === state.me.userId) return;
      if (s.typing) {
        state.typingUsers.set(s.userId, window.setTimeout(() => {
          state.typingUsers.delete(s.userId);
          renderTyping();
        }, 1800));
      } else {
        const t = state.typingUsers.get(s.userId);
        window.clearTimeout(t);
        state.typingUsers.delete(s.userId);
      }
      renderTyping();
    }));
  }

  async function sendMessage() {
    const c = selected();
    const text = el.messageInput.value.trim();
    if (!c || !text) return;
    try {
      await ensureSocket();
      state.client.publish({
        destination: "/app/chat.send",
        body: JSON.stringify({ conversationId: c.id, clientMessageId: crypto.randomUUID(), text })
      });
      el.messageInput.value = "";
      autosize();
      updateCharCounter();
      publishTyping(false);
    } catch (e) {
      toast(e.message || "Message send failed.", true);
    }
  }

  function announceTyping() {
    if (!state.selectedId || !state.client || !state.client.connected) return;
    publishTyping(true);
    window.clearTimeout(state.typingTimer);
    state.typingTimer = window.setTimeout(() => publishTyping(false), 1200);
  }

  function publishTyping(typing) {
    if (!state.selectedId || !state.client || !state.client.connected) return;
    state.client.publish({ destination: "/app/chat.typing", body: JSON.stringify({ conversationId: state.selectedId, typing }) });
  }

  function appendMessage(m, scroll) {
    if (state.messageIds.has(m.messageId)) return;
    state.messageIds.add(m.messageId);
    const emptyState = el.messageList.querySelector(".empty-state");
    if (emptyState) emptyState.remove();
    const tone = m.aiGenerated ? "ai" : (state.me && m.senderId === state.me.userId ? "self" : "other");
    const grouped = state.lastSenderId === m.senderId;
    state.lastSenderId = m.senderId;
    const article = document.createElement("article");
    article.className = `message ${tone}${grouped ? " grouped" : ""}`;
    article.innerHTML = `<div class="message-head">${avatar(m.senderId, false, false, true)}<div class="message-head-info"><span class="message-author">${safe(m.senderId)}</span><span class="message-time" title="${new Date(m.createdAt).toLocaleString()}">${clock(m.createdAt)}</span></div></div><div class="message-body">${linkify(safe(m.text))}</div>`;
    el.messageList.appendChild(article);
    if (scroll) {
      scrollEnd(true);
      if (state.me && m.senderId !== state.me.userId) playNotificationSound();
    }
  }

  function bumpConversation(id, updatedAt) {
    const c = state.conversations.find((x) => x.id === id);
    if (!c) return;
    c.updatedAt = updatedAt;
    state.conversations.sort((a, b) => new Date(b.updatedAt || b.createdAt) - new Date(a.updatedAt || a.createdAt));
    renderConversations();
    renderHeader(c);
  }

  function renderTyping() {
    const users = Array.from(state.typingUsers.keys());
    el.typingIndicator.textContent = !users.length ? "" : (users.length === 1 ? `${users[0]} is typing...` : `${users.join(", ")} are typing...`);
  }

  function openSheet(type) {
    if (!state.me) return toast("Login before creating conversations.", true);
    el.conversationType.value = type;
    el.sheetTitle.textContent = type === "GROUP" ? "New Group" : "New Direct";
    el.sheetEyebrow.textContent = type === "GROUP" ? "Create a shared room" : "Start a private thread";
    el.conversationTitleInput.placeholder = type === "GROUP" ? "Launch Squad" : "Direct with ava";
    el.conversationTitleInput.value = "";
    el.memberIdsInput.value = "";
    el.composerOverlay.classList.remove("hidden");
    el.conversationSheet.classList.remove("hidden");
    el.conversationTitleInput.focus();
  }

  function closeSheet() {
    el.composerOverlay.classList.add("hidden");
    el.conversationSheet.classList.add("hidden");
  }

  async function createConversation() {
    const type = el.conversationType.value;
    const memberIds = parseMembers(el.memberIdsInput.value);
    const first = state.users.find((u) => u.userId === memberIds[0]);
    const title = clean(el.conversationTitleInput.value) || (type === "DIRECT" && first ? `Direct with ${first.displayName}` : "");
    if (!title) return toast("Give the conversation a title.", true);
    if (type === "DIRECT" && memberIds.length !== 1) return toast("A one-on-one chat needs exactly one other member.", true);
    if (type === "GROUP" && memberIds.length < 1) return toast("Add at least one other member to create a group.", true);
    try {
      const c = await api("POST", "/api/conversations", { title, type, memberIds });
      closeSheet();
      await loadWorkspace();
      await selectConversation(c.id);
      toast(type === "GROUP" ? "Group ready." : "One-on-one chat ready.");
    } catch (e) {
      toast(e.message || "Conversation creation failed.", true);
    }
  }

  function clearStage(authenticated) {
    state.selectedId = null;
    state.messageIds = new Set();
    clearRoomSubs();
    el.conversationBadge.textContent = "No room selected";
    el.conversationTitle.textContent = authenticated ? "Choose a conversation" : "Login to start chatting";
    el.conversationMeta.textContent = authenticated ? "Create a group or direct thread to start chatting." : "Create an account or login to unlock one-on-one and group chat.";
    el.memberStrip.innerHTML = "";
    el.conversationWarning.textContent = "";
    el.conversationWarning.classList.add("hidden");
    el.messageList.innerHTML = empty(authenticated ? "No conversation selected" : "Authentication required", authenticated ? "Pick a room from the left rail or start a new one." : "Sign in above, then launch a group or a one-on-one thread.");
    el.typingIndicator.textContent = "";
    el.messageInput.disabled = true;
    el.sendButton.disabled = true;
    el.aiPromptButton.disabled = true;
    el.copyConversationButton.disabled = true;
    el.blockConversationButton.classList.add("hidden");
    el.deleteConversationButton.classList.add("hidden");
  }

  function disconnect() {
    if (state.client) {
      try { state.client.deactivate(); } catch {}
    }
    state.client = null;
    state.connecting = null;
    state.socketUserId = null;
    resetSubs();
    setOnline(false);
  }

  function resetSubs() {
    clearRoomSubs();
    state.userSubs.forEach((s) => s.unsubscribe());
    state.userSubs = [];
  }

  function clearRoomSubs() {
    state.roomSubs.forEach((s) => s.unsubscribe());
    state.roomSubs = [];
    state.typingUsers.forEach((t) => window.clearTimeout(t));
    state.typingUsers.clear();
    renderTyping();
  }

  function parseMembers(raw) {
    return raw.split(/[\\n,]/).map(clean).filter(Boolean).filter((v, i, a) => a.indexOf(v) === i).filter((v) => !state.me || v !== state.me.userId);
  }

  function selected() {
    return state.conversations.find((c) => c.id === state.selectedId) || null;
  }

  function setOnline(online) {
    el.connectionBadge.textContent = online ? "Online" : "Offline";
    el.connectionBadge.classList.toggle("online", online);
  }

  async function api(method, url, body) {
    return raw(method, url, body, true);
  }

  async function raw(method, url, body, includeAuth) {
    const headers = {};
    if (includeAuth && state.token) headers["X-Auth-Token"] = state.token;
    if (body !== undefined) headers["Content-Type"] = "application/json";
    const response = await fetch(url, { method, headers, body: body === undefined ? undefined : JSON.stringify(body) });
    const text = await response.text();
    const payload = parse(text);
    if (!response.ok) throw new Error(payload && payload.message ? payload.message : `Request failed with status ${response.status}`);
    return payload;
  }

  function log(message) {
    const row = document.createElement("div");
    row.className = "log-entry";
    row.textContent = `${clock(new Date().toISOString())} | ${message}`;
    el.activityLog.prepend(row);
    while (el.activityLog.children.length > 18) el.activityLog.removeChild(el.activityLog.lastChild);
  }

  function toast(message, error) {
    const node = document.createElement("div");
    node.className = `toast${error ? " error" : ""}`;
    node.textContent = message;
    el.toastStack.appendChild(node);
    window.setTimeout(() => node.remove(), 3400);
  }

  function empty(title, copy) {
    return `<div class="empty-state"><div><div class="empty-title">${title}</div><div class="empty-copy">${copy}</div></div></div>`;
  }

  async function deleteConversation() {
    const c = selected();
    if (!c) return;
    const confirmCopy = c.type === "GROUP"
      ? (c.admin ? "Delete this group for every member?" : "Leave this group?")
      : "Delete this chat from your account?";
    if (!window.confirm(confirmCopy)) return;
    try {
      await api("DELETE", `/api/conversations/${c.id}`);
      toast(c.type === "GROUP" ? (c.admin ? "Group deleted." : "Left group.") : "Chat deleted.");
      await loadWorkspace();
    } catch (e) {
      toast(e.message || "Conversation removal failed.", true);
    }
  }

  async function toggleConversationBlock() {
    const c = selected();
    if (!c || c.type !== "DIRECT") return;
    const partner = directPartner(c);
    if (!partner) return;
    await toggleBlock(partner);
  }

  async function toggleBlock(user) {
    try {
      if (user.blockedByMe) {
        await api("DELETE", `/api/blocks/${encodeURIComponent(user.userId)}`);
        toast(`Unblocked ${user.displayName}.`);
      } else {
        await api("POST", `/api/blocks/${encodeURIComponent(user.userId)}`);
        toast(`Blocked ${user.displayName}.`);
      }
      await loadWorkspace();
    } catch (e) {
      toast(e.message || "Block update failed.", true);
    }
  }

  function autosize() {
    el.messageInput.style.height = "auto";
    el.messageInput.style.height = `${Math.min(el.messageInput.scrollHeight, 180)}px`;
  }

  function scrollEnd(smooth) {
    el.messageList.scrollTo({ top: el.messageList.scrollHeight, behavior: smooth ? "smooth" : "auto" });
  }

  function relative(value) {
    if (!value) return "just now";
    const date = new Date(value);
    const minutes = Math.round((Date.now() - date.getTime()) / 60000);
    if (minutes < 1) return "just now";
    if (minutes < 60) return `${minutes}m ago`;
    const hours = Math.round(minutes / 60);
    if (hours < 24) return `${hours}h ago`;
    return date.toLocaleDateString(undefined, { month: "short", day: "numeric" });
  }

  function clock(value) {
    return new Date(value).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });
  }

  function parse(text) {
    try { return text ? JSON.parse(text) : null; } catch { return null; }
  }

  function clean(value) {
    return String(value || "").trim();
  }

  function directPartner(conversation) {
    const partnerId = (conversation.memberIds || []).find((memberId) => !state.me || memberId !== state.me.userId);
    if (!partnerId) return null;
    return state.users.find((user) => user.userId === partnerId) || {
      userId: partnerId,
      displayName: partnerId,
      blockedByMe: conversation.blocked,
      hasBlockedMe: false,
      directChatAllowed: !conversation.blocked
    };
  }

  function conversationWarning(conversation, partner) {
    if (!conversation.blocked || !partner) return "";
    if (partner.blockedByMe && partner.hasBlockedMe) {
      return "Both sides blocked this direct chat. Messaging is disabled.";
    }
    if (partner.blockedByMe) {
      return `You blocked ${partner.displayName}. Unblock to send direct messages again.`;
    }
    return `${partner.displayName} blocked you. Direct messaging is disabled.`;
  }

  function deleteLabel(conversation) {
    if (conversation.type === "DIRECT") return "Delete Chat";
    return conversation.admin ? "Delete Group" : "Leave Group";
  }

  function userStatus(user) {
    if (user.blockedByMe && user.hasBlockedMe) return "You blocked each other";
    if (user.blockedByMe) return "Blocked by you";
    if (user.hasBlockedMe) return "Blocked you";
    return "";
  }

  function safe(value) {
    return String(value)
      .replaceAll("&", "&amp;")
      .replaceAll("<", "&lt;")
      .replaceAll(">", "&gt;")
      .replaceAll("\"", "&quot;")
      .replaceAll("'", "&#39;");
  }

  function byId(ids) {
    return ids.reduce((acc, id) => {
      acc[id] = document.getElementById(id);
      return acc;
    }, {});
  }

  /* ── Theme toggle ── */
  function applyTheme(theme) {
    document.documentElement.setAttribute("data-theme", theme);
    state.theme = theme;
    localStorage.setItem("pulsechat.theme", theme);
  }

  function toggleTheme() {
    applyTheme(state.theme === "dark" ? "light" : "dark");
  }

  /* ── Avatar generation ── */
  function avatarColor(name) {
    let hash = 0;
    for (let i = 0; i < name.length; i++) hash = name.charCodeAt(i) + ((hash << 5) - hash);
    return AVATAR_COLORS[Math.abs(hash) % AVATAR_COLORS.length];
  }

  function avatarInitials(name) {
    const parts = name.trim().split(/[\s\-_@.]+/).filter(Boolean);
    if (parts.length >= 2) return (parts[0][0] + parts[1][0]).toUpperCase();
    return name.slice(0, 2).toUpperCase();
  }

  function avatar(name, online, showPresence, small) {
    const color = avatarColor(name);
    const initials = avatarInitials(name);
    const sizeClass = small ? " avatar-sm" : "";
    const presenceClass = showPresence ? ` presence-dot${online ? " is-online" : ""}` : "";
    return `<div class="avatar${sizeClass}${presenceClass}" style="background:${color}">${initials}</div>`;
  }

  /* ── Character counter ── */
  function updateCharCounter() {
    const len = el.messageInput.value.length;
    el.charCounter.textContent = `${len} / 4000`;
    el.charCounter.classList.toggle("warn", len > 3200 && len <= 3800);
    el.charCounter.classList.toggle("danger", len > 3800);
  }

  /* ── URL linkification ── */
  function linkify(text) {
    return text.replace(/(https?:\/\/[^\s<&"']+)/gi, '<a href="$1" target="_blank" rel="noopener noreferrer">$1</a>');
  }

  /* ── Conversation search filter ── */
  function filterConversations() {
    renderConversations();
  }

  /* ── Notification sound ── */
  function playNotificationSound() {
    try {
      const ctx = new (window.AudioContext || window.webkitAudioContext)();
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();
      osc.connect(gain);
      gain.connect(ctx.destination);
      osc.frequency.setValueAtTime(880, ctx.currentTime);
      osc.frequency.setValueAtTime(660, ctx.currentTime + 0.08);
      gain.gain.setValueAtTime(0.08, ctx.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.2);
      osc.start(ctx.currentTime);
      osc.stop(ctx.currentTime + 0.2);
    } catch {}
  }

  /* ── Emoji picker ── */
  function buildEmojiPicker() {
    EMOJIS.forEach((emoji) => {
      const btn = document.createElement("button");
      btn.type = "button";
      btn.textContent = emoji;
      btn.onclick = () => {
        el.messageInput.value += emoji;
        el.messageInput.focus();
        autosize();
        updateCharCounter();
        el.emojiPicker.classList.add("hidden");
      };
      el.emojiGrid.appendChild(btn);
    });
  }

  /* ── Online presence polling ── */
  function pollPresence() {
    if (!state.me || !state.users.length) return;
    state.users.forEach(async (u) => {
      try {
        const res = await api("GET", `/api/presence/${encodeURIComponent(u.userId)}`);
        if (res && res.online) state.onlineUsers.add(u.userId);
        else state.onlineUsers.delete(u.userId);
      } catch {}
    });
    renderUsers();
  }
  setInterval(pollPresence, 30000);
})();
