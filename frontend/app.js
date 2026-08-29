// Falls back to whatever host served this page (so opening the app via a phone's
// LAN IP "just works" without manually setting noteshare-api in localStorage).
const API = localStorage.getItem('noteshare-api') || `${window.location.protocol}//${window.location.hostname}:8080/api`;
const state = {
  token: localStorage.getItem('noteshare-token'),
  username: localStorage.getItem('noteshare-user'),
  mode: 'login',
  layer: 'notes',
  notes: [],
  feedNotes: [],
  allUsers: [],
  friends: [],
  incoming: [],
  outgoing: [],
  filter: 'all',
  search: '',
  sort: 'updated',
  sharesCache: {},
  openShareId: null,
};
const $ = (id) => document.getElementById(id);
const authView = $('authView'), appView = $('appView'), dialog = $('noteDialog');

function setOffline(isOffline) { $('offlineBanner').classList.toggle('hidden', !isOffline); }

async function request(path, options = {}) {
  const headers = { 'Content-Type': 'application/json', ...(options.headers || {}) };
  if (state.token) headers.Authorization = `Bearer ${state.token}`;
  let response;
  try {
    response = await fetch(`${API}${path}`, { ...options, headers });
  } catch {
    setOffline(true);
    throw new Error("Can't reach the server. Check the backend is running and you're on the same Wi-Fi.");
  }
  setOffline(false);
  const data = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(data.error || `Request failed (${response.status})`);
  return data;
}

function showMessage(element, text = '') { element.textContent = text; }
function toast(text) { const element = $('toast'); element.textContent = text; element.classList.add('show'); setTimeout(() => element.classList.remove('show'), 2600); }
function setMode(mode) { state.mode = mode; document.querySelectorAll('.tab').forEach(tab => tab.classList.toggle('active', tab.dataset.mode === mode)); $('authTitle').textContent = mode === 'login' ? 'Welcome back' : 'Make some room'; $('authHint').textContent = mode === 'login' ? 'Your notes are waiting.' : 'Start sharing the things that matter.'; $('authAction').textContent = mode === 'login' ? 'Open my notes' : 'Create my space'; showMessage($('authMessage')); }

async function showApp() {
  authView.classList.add('hidden'); appView.classList.remove('hidden');
  $('userLabel').textContent = `@${state.username}`;
  await Promise.all([loadNotes(), loadUsers(), loadFriends(), loadFriendRequests(), loadAccountVisibility()]);
  renderFriendsLayer();
}
function showAuth() { appView.classList.add('hidden'); authView.classList.remove('hidden'); }

async function authenticate(event) {
  event.preventDefault();
  showMessage($('authMessage'));
  const body = { username: $('username').value.trim(), password: $('password').value };
  try {
    const data = await request(state.mode === 'login' ? '/login' : '/register', { method: 'POST', body: JSON.stringify(body) });
    state.token = data.token; state.username = body.username;
    localStorage.setItem('noteshare-token', state.token);
    localStorage.setItem('noteshare-user', state.username);
    showApp();
  } catch (error) { showMessage($('authMessage'), error.message); }
}

async function loadNotes() {
  try {
    state.notes = await request('/notes');
    renderNotes();
  } catch (error) {
    toast(error.message);
    if (error.message.includes('authenticated')) logout();
  }
}

async function loadUsers() {
  try { state.allUsers = (await request('/users')).filter(user => user !== state.username); }
  catch { state.allUsers = []; }
}

async function loadFriends() {
  try { state.friends = await request('/friends'); }
  catch { state.friends = []; }
}

async function loadFriendRequests() {
  try {
    const data = await request('/friends/requests');
    state.incoming = data.incoming || [];
    state.outgoing = data.outgoing || [];
  } catch { state.incoming = []; state.outgoing = []; }
  const badge = $('friendRequestBadge');
  badge.textContent = state.incoming.length;
  badge.classList.toggle('hidden', state.incoming.length === 0);
}

function escapeHtml(value) { return String(value).replace(/[&<>'"]/g, character => ({ '&':'&amp;', '<':'&lt;', '>':'&gt;', "'":'&#39;', '"':'&quot;' }[character])); }

// Deterministic per-username color, so the same person always gets the same "pin" color across the app.
function avatarHue(username) { let hash = 0; for (const char of username) hash = (hash * 31 + char.charCodeAt(0)) >>> 0; return hash % 360; }
function avatarStyle(username) { return `background:hsl(${avatarHue(username)} 46% 88%);color:hsl(${avatarHue(username)} 46% 28%)`; }
function initials(username) { return username.slice(0, 2).toUpperCase(); }
function avatarHtml(username, extraClass = '') { return `<span class="avatar ${extraClass}" style="${avatarStyle(username)}">${escapeHtml(initials(username))}</span>`; }

function computeVisibleNotes() {
  let notes = state.notes;
  if (state.filter === 'mine') notes = notes.filter(note => note.owner !== false);
  if (state.filter === 'shared') notes = notes.filter(note => note.owner === false);
  const q = state.search.trim().toLowerCase();
  if (q) notes = notes.filter(note => note.title.toLowerCase().includes(q) || note.content.toLowerCase().includes(q));
  notes = [...notes];
  if (state.sort === 'title') notes.sort((a, b) => a.title.localeCompare(b.title));
  else notes.sort((a, b) => new Date(b.updatedAt || b.createdAt || 0) - new Date(a.updatedAt || a.createdAt || 0));
  return notes;
}

function sharePanelHtml(note) {
  const shares = state.sharesCache[note.id] || [];
  // Only friends can be added - sharing with a stranger isn't allowed server-side either.
  const available = state.friends.filter(user => !shares.includes(user));
  const chips = shares.length
    ? shares.map(user => `<span class="share-chip">${avatarHtml(user, 'avatar-sm')}@${escapeHtml(user)}<button data-unshare="${note.id}" data-username="${escapeHtml(user)}" aria-label="Stop sharing with ${escapeHtml(user)}">×</button></span>`).join('')
    : '<span class="share-empty">Not shared with anyone yet</span>';
  const controls = available.length
    ? `<select data-share-select="${note.id}"><option value="">Choose a friend…</option>${available.map(user => `<option value="${escapeHtml(user)}">@${escapeHtml(user)}</option>`).join('')}</select><button type="button" data-share-add="${note.id}" class="quiet-button">Add</button>`
    : `<span class="share-empty">${state.friends.length ? 'Already shared with all your friends' : 'Add a friend first to share this note'}</span>`;
  return `<div class="share-panel"><div class="share-chips">${chips}</div><div class="share-controls">${controls}</div></div>`;
}

function findNoteById(id) { return state.notes.find(item => item.id === id) || state.feedNotes.find(item => item.id === id); }

function noteCardHtml(note, context = 'notes') {
  const isOwner = note.owner !== false;
  const visibilityTag = note.visibility === 'PUBLIC' ? '<span class="visibility-tag">Public</span>' : '';
  let badge = '';
  if (context === 'feed') {
    badge = `<span class="owner-badge">${avatarHtml(note.ownerUsername, 'avatar-sm')}@${escapeHtml(note.ownerUsername)}</span>`;
  } else if (!isOwner) {
    badge = `<span class="owner-badge">${avatarHtml(note.ownerUsername, 'avatar-sm')}Shared by @${escapeHtml(note.ownerUsername)}</span>`;
  }
  const dateLabel = note.updatedAt ? new Date(note.updatedAt).toLocaleDateString() : (note.createdAt ? new Date(note.createdAt).toLocaleDateString() : 'NOTE');
  const actions = isOwner
    ? `<button data-edit="${note.id}">Edit</button><button data-delete="${note.id}">Delete</button><button data-share="${note.id}">Share</button>`
    : '';
  const panel = isOwner && state.openShareId === note.id ? sharePanelHtml(note) : '';
  return `<article class="note-card ${isOwner ? 'note-mine' : 'note-shared'}" data-note="${note.id}">${badge}<h3>${escapeHtml(note.title)}</h3><p>${escapeHtml(note.content)}</p><div class="note-meta"><span>${dateLabel}${visibilityTag}</span><span class="note-actions">${actions}</span></div>${panel}</article>`;
}

function renderNotes() {
  const visible = computeVisibleNotes();
  $('noteCount').textContent = state.notes.length;
  $('emptyState').classList.toggle('hidden', state.notes.length > 0);
  if (state.notes.length === 0) { $('notesList').innerHTML = ''; return; }
  $('notesList').innerHTML = visible.length ? visible.map(note => noteCardHtml(note, 'notes')).join('') : '<p class="no-results">No notes match your search.</p>';
}

async function loadFeed() {
  try { state.feedNotes = await request('/feed'); }
  catch (error) { state.feedNotes = []; toast(error.message); }
  renderFeed();
}

function renderFeed() {
  $('feedEmpty').classList.toggle('hidden', state.feedNotes.length > 0);
  $('feedList').innerHTML = state.feedNotes.map(note => noteCardHtml(note, 'feed')).join('');
}

function openEditor(note = null) { $('dialogTitle').textContent = note ? 'Edit note' : 'New note'; $('noteId').value = note?.id || ''; $('noteTitle').value = note?.title || ''; $('noteContent').value = note?.content || ''; $('noteIsPublic').checked = note?.visibility === 'PUBLIC'; showMessage($('noteMessage')); dialog.showModal(); $('noteTitle').focus(); }

async function saveNote(event) {
  event.preventDefault();
  const id = $('noteId').value;
  const body = { title: $('noteTitle').value.trim(), content: $('noteContent').value.trim(), visibility: $('noteIsPublic').checked ? 'PUBLIC' : 'PRIVATE' };
  try {
    await request(id ? `/notes/${id}` : '/notes', { method: id ? 'PUT' : 'POST', body: JSON.stringify(body) });
    dialog.close();
    await loadNotes();
    if (state.layer === 'feed') await loadFeed();
    toast(id ? 'Note updated' : 'Note created');
  } catch (error) { showMessage($('noteMessage'), error.message); }
}

async function toggleSharePanel(id) {
  if (state.openShareId === id) { state.openShareId = null; renderNotes(); renderFeed(); return; }
  state.openShareId = id;
  if (!state.sharesCache[id]) {
    try { state.sharesCache[id] = await request(`/notes/${id}/shares`); }
    catch (error) { state.sharesCache[id] = []; toast(error.message); }
  }
  renderNotes();
  renderFeed();
}

async function handleNoteAction(event) {
  const button = event.target.closest('button');
  if (!button) return;
  const id = Number(button.dataset.edit || button.dataset.delete || button.dataset.share || button.dataset.shareAdd || button.dataset.unshare);
  const note = findNoteById(id);
  try {
    if (button.dataset.edit) openEditor(note);
    if (button.dataset.delete && confirm('Delete this note?')) {
      await request(`/notes/${id}`, { method: 'DELETE' });
      delete state.sharesCache[id];
      await loadNotes();
      if (state.layer === 'feed') await loadFeed();
      toast('Note deleted');
    }
    if (button.dataset.share) await toggleSharePanel(id);
    if (button.dataset.shareAdd) {
      const select = document.querySelector(`[data-share-select="${id}"]`);
      const username = select ? select.value : '';
      if (!username) { toast('Choose a friend to share with'); return; }
      await request(`/notes/${id}/share`, { method: 'POST', body: JSON.stringify({ username }) });
      state.sharesCache[id] = [...(state.sharesCache[id] || []), username];
      toast(`Shared with @${username}`);
      renderNotes();
      renderFeed();
    }
    if (button.dataset.unshare) {
      const username = button.dataset.username;
      await request(`/notes/${id}/share/${encodeURIComponent(username)}`, { method: 'DELETE' });
      state.sharesCache[id] = (state.sharesCache[id] || []).filter(user => user !== username);
      toast(`Removed @${username}`);
      renderNotes();
      renderFeed();
    }
  } catch (error) { toast(error.message); }
}

// ---------- Friends layer ----------
function personStatus(username) {
  if (state.friends.includes(username)) return 'friend';
  if (state.incoming.includes(username)) return 'incoming';
  if (state.outgoing.includes(username)) return 'outgoing';
  return 'none';
}

function networkPersonHtml(username) {
  const status = personStatus(username);
  let action;
  if (status === 'friend') action = '<span class="status-tag">Friends</span>';
  else if (status === 'incoming') action = `<button data-accept="${escapeHtml(username)}" class="quiet-button">Accept</button><button data-decline="${escapeHtml(username)}" class="quiet-button danger-text">Decline</button>`;
  else if (status === 'outgoing') action = `<span class="status-tag">Requested</span><button data-cancel="${escapeHtml(username)}" class="quiet-button">Cancel</button>`;
  else action = `<button data-add-friend="${escapeHtml(username)}" class="quiet-button">Add</button>`;
  return `<li>${avatarHtml(username)}<span>@${escapeHtml(username)}</span><span class="person-action">${action}</span></li>`;
}

function requestRowHtml(username, direction) {
  const action = direction === 'incoming'
    ? `<button data-accept="${escapeHtml(username)}" class="quiet-button">Accept</button><button data-decline="${escapeHtml(username)}" class="quiet-button danger-text">Decline</button>`
    : `<span class="status-tag">Requested</span><button data-cancel="${escapeHtml(username)}" class="quiet-button">Cancel</button>`;
  return `<li>${avatarHtml(username)}<span>@${escapeHtml(username)}</span><span class="person-action">${action}</span></li>`;
}

function friendRowHtml(username) {
  return `<li>${avatarHtml(username)}<span>@${escapeHtml(username)}</span><span class="person-action"><button data-unfriend="${escapeHtml(username)}" class="quiet-button danger-text">Remove</button></span></li>`;
}

function renderFriendsLayer() {
  $('incomingRequests').innerHTML = state.incoming.map(user => requestRowHtml(user, 'incoming')).join('');
  $('outgoingRequests').innerHTML = state.outgoing.map(user => requestRowHtml(user, 'outgoing')).join('');
  $('requestsEmpty').classList.toggle('hidden', state.incoming.length + state.outgoing.length > 0);
  $('friendCount').textContent = state.friends.length;
  $('friendsListEl').innerHTML = state.friends.length ? state.friends.map(friendRowHtml).join('') : '<li class="people-empty">No friends yet — add someone from the network list</li>';
  $('networkPeopleList').innerHTML = state.allUsers.length ? state.allUsers.map(networkPersonHtml).join('') : '<li class="people-empty">No one else here yet</li>';
}

async function handleFriendAction(event) {
  const button = event.target.closest('button');
  if (!button) return;
  const { addFriend, accept, decline, cancel, unfriend } = button.dataset;
  try {
    if (addFriend) { await request('/friends/requests', { method: 'POST', body: JSON.stringify({ username: addFriend }) }); toast(`Friend request sent to @${addFriend}`); }
    if (accept) { await request(`/friends/requests/${encodeURIComponent(accept)}/accept`, { method: 'POST' }); toast(`You and @${accept} are now friends`); }
    if (decline) { await request(`/friends/requests/${encodeURIComponent(decline)}`, { method: 'DELETE' }); toast('Request declined'); }
    if (cancel) { await request(`/friends/requests/${encodeURIComponent(cancel)}`, { method: 'DELETE' }); toast('Request canceled'); }
    if (unfriend) { await request(`/friends/${encodeURIComponent(unfriend)}`, { method: 'DELETE' }); toast(`Removed @${unfriend}`); }
    await Promise.all([loadFriends(), loadFriendRequests()]);
    renderFriendsLayer();
    renderNotes();
    if ((accept || unfriend) && state.layer === 'feed') await loadFeed();
  } catch (error) { toast(error.message); }
}

function switchLayer(target) {
  state.layer = target;
  document.querySelectorAll('.layer-tab').forEach(tab => tab.classList.toggle('active', tab.dataset.targetLayer === target));
  document.querySelectorAll('.layer').forEach(section => section.classList.toggle('hidden', section.id !== `${target}Layer`));
  if (target === 'feed') loadFeed();
}

async function loadAccountVisibility() {
  try {
    const data = await request('/account/visibility');
    $('accountPrivateToggle').checked = data.visibility === 'PRIVATE';
  } catch { /* leave default unchecked if this fails */ }
}

async function handleVisibilityToggle(event) {
  const wantsPrivate = event.target.checked;
  try {
    await request('/account/visibility', { method: 'PUT', body: JSON.stringify({ visibility: wantsPrivate ? 'PRIVATE' : 'PUBLIC' }) });
    toast(wantsPrivate ? 'Account set to private — only friends see your notes' : 'Account set to public — your public notes are visible to everyone');
  } catch (error) {
    event.target.checked = !wantsPrivate;
    toast(error.message);
  }
}

async function logout() {
  if (state.token) request('/logout', { method: 'POST' }).catch(() => {});
  state.token = null; state.username = null; state.sharesCache = {}; state.openShareId = null;
  localStorage.removeItem('noteshare-token'); localStorage.removeItem('noteshare-user');
  showAuth();
}

async function deleteAccount(event) {
  event.preventDefault();
  showMessage($('deleteMessage'));
  try {
    await request('/account', { method: 'DELETE', body: JSON.stringify({ password: $('deletePassword').value }) });
    $('deleteAccountDialog').close();
    $('deletePassword').value = '';
    state.token = null; state.username = null; state.sharesCache = {}; state.openShareId = null;
    localStorage.removeItem('noteshare-token'); localStorage.removeItem('noteshare-user');
    showAuth();
    toast('Account deleted');
  } catch (error) { showMessage($('deleteMessage'), error.message); }
}

document.querySelectorAll('.tab').forEach(tab => tab.addEventListener('click', () => setMode(tab.dataset.mode)));
document.querySelectorAll('.layer-tab').forEach(tab => tab.addEventListener('click', () => switchLayer(tab.dataset.targetLayer)));
$('authForm').addEventListener('submit', authenticate);
$('newNoteButton').addEventListener('click', () => openEditor());
$('noteForm').addEventListener('submit', saveNote);
$('notesList').addEventListener('click', handleNoteAction);
$('feedList').addEventListener('click', handleNoteAction);
$('logoutButton').addEventListener('click', logout);
$('accountPrivateToggle').addEventListener('change', handleVisibilityToggle);
$('closeDialog').addEventListener('click', () => dialog.close());
$('cancelDialog').addEventListener('click', () => dialog.close());
const deleteDialog = $('deleteAccountDialog');
$('deleteAccountButton').addEventListener('click', () => { showMessage($('deleteMessage')); deleteDialog.showModal(); });
$('closeDeleteDialog').addEventListener('click', () => deleteDialog.close());
$('cancelDeleteDialog').addEventListener('click', () => deleteDialog.close());
$('deleteAccountForm').addEventListener('submit', deleteAccount);
$('searchInput').addEventListener('input', (event) => { state.search = event.target.value; renderNotes(); });
$('sortSelect').addEventListener('change', (event) => { state.sort = event.target.value; renderNotes(); });
document.querySelectorAll('.filter').forEach(button => button.addEventListener('click', () => {
  state.filter = button.dataset.filter;
  document.querySelectorAll('.filter').forEach(b => b.classList.toggle('active', b === button));
  renderNotes();
}));
$('friendsLayer').addEventListener('click', handleFriendAction);

if (state.token && state.username) showApp();
