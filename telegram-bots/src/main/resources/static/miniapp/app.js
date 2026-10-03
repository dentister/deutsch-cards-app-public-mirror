(function () {
  "use strict";

  var tg = window.Telegram && window.Telegram.WebApp;
  var API = "/api/miniapp";
  var MAX_RENDER = 200;
  var initData = (tg && tg.initData) || "";

  var searchEl = document.getElementById("search");
  var listEl = document.getElementById("list");
  var statusEl = document.getElementById("status");
  var emptyEl = document.getElementById("empty");
  var saveBtn = document.getElementById("saveBtn");

  var catalog = [];
  var selected = new Set(); // word ids (numbers)
  var currentQuery = ""; // drives the list; only the search box updates it

  if (tg) { tg.ready(); tg.expand(); }

  function authHeaders(extra) {
    return Object.assign({ "X-Telegram-Init-Data": initData }, extra || {});
  }

  function ruText(ru) { return (ru || "").split(";").join(", "); }

  function matches(w, q) {
    return (w.de && w.de.toLowerCase().indexOf(q) !== -1) ||
           (w.ru && w.ru.toLowerCase().indexOf(q) !== -1);
  }

  function updateSaveUi() {
    var label = "Save (" + selected.size + ")";
    if (tg && tg.MainButton) {
      tg.MainButton.setText(label);
      tg.MainButton.show();
    } else {
      saveBtn.hidden = false;
      saveBtn.textContent = label;
    }
    statusEl.textContent = selected.size + " selected";
  }

  function row(w) {
    var label = document.createElement("label");
    label.className = "row";

    var cb = document.createElement("input");
    cb.type = "checkbox";
    cb.checked = selected.has(w.id);
    cb.dataset.id = w.id;

    var text = document.createElement("div");
    text.className = "text";

    var de = document.createElement("div");
    de.className = "de";
    de.textContent = w.de + (w.level ? "  ·  " + w.level : "");

    var ru = document.createElement("div");
    ru.className = "ru";
    ru.textContent = ruText(w.ru);

    text.appendChild(de);
    text.appendChild(ru);
    label.appendChild(cb);
    label.appendChild(text);
    return label;
  }

  function render() {
    var q = currentQuery;
    var items = q
      ? catalog.filter(function (w) { return matches(w, q); })
      : catalog.filter(function (w) { return selected.has(w.id); });
    var shown = items.slice(0, MAX_RENDER);

    listEl.innerHTML = "";
    var frag = document.createDocumentFragment();
    shown.forEach(function (w) { frag.appendChild(row(w)); });
    listEl.appendChild(frag);

    if (shown.length === 0) {
      emptyEl.hidden = false;
      emptyEl.textContent = q
        ? "No matches."
        : "Start typing to find words. Selected words appear here.";
    } else {
      emptyEl.hidden = true;
    }

    if (items.length > MAX_RENDER) {
      var more = document.createElement("div");
      more.className = "more";
      more.textContent = "Showing first " + MAX_RENDER + " of " + items.length + ". Refine your search.";
      listEl.appendChild(more);
    }
  }

  listEl.addEventListener("change", function (e) {
    var cb = e.target;
    if (cb && cb.type === "checkbox") {
      var id = Number(cb.dataset.id);
      if (cb.checked) { selected.add(id); } else { selected.delete(id); }
      updateSaveUi();
    }
  });

  var searchTimer;
  searchEl.addEventListener("input", function () {
    currentQuery = searchEl.value.trim().toLowerCase();
    clearTimeout(searchTimer);
    searchTimer = setTimeout(render, 120);
  });

  function save() {
    var payload = JSON.stringify({ wordIds: Array.from(selected) });
    if (tg && tg.MainButton) { tg.MainButton.showProgress(); }
    fetch(API + "/selection", {
      method: "POST",
      headers: authHeaders({ "Content-Type": "application/json" }),
      body: payload
    }).then(function (res) {
      if (!res.ok) { throw new Error("HTTP " + res.status); }
      if (tg) {
        if (tg.HapticFeedback) { tg.HapticFeedback.notificationOccurred("success"); }
        tg.close();
      } else {
        statusEl.textContent = "Saved " + selected.size + " words";
      }
    }).catch(function (err) {
      statusEl.textContent = "Save failed: " + err.message;
      if (tg && tg.showAlert) { tg.showAlert("Save failed: " + err.message); }
    }).finally(function () {
      if (tg && tg.MainButton) { tg.MainButton.hideProgress(); }
    });
  }

  if (tg && tg.MainButton) { tg.MainButton.onClick(save); }
  saveBtn.addEventListener("click", save);

  function load() {
    Promise.all([
      fetch(API + "/words"),
      fetch(API + "/selection", { headers: authHeaders() })
    ]).then(function (responses) {
      var wordsRes = responses[0];
      var selRes = responses[1];
      if (!wordsRes.ok) { throw new Error("catalog HTTP " + wordsRes.status); }
      return Promise.all([
        wordsRes.json(),
        selRes.ok ? selRes.json() : []
      ]);
    }).then(function (data) {
      catalog = data[0] || [];
      (data[1] || []).forEach(function (id) { selected.add(Number(id)); });
      updateSaveUi();
      render();
    }).catch(function (err) {
      statusEl.textContent = "Failed to load: " + err.message;
      emptyEl.hidden = false;
      emptyEl.textContent = "Could not load words.";
    });
  }

  load();
})();
