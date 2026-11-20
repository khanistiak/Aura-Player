import http from 'node:http';
import fs from 'node:fs';
import path from 'node:path';

const PORT = Number(process.env.DEFAULT_APP_PORT || 3000);
const ROOT = process.cwd();
const APK_CANDIDATES = [
  path.join(ROOT, 'app/build/outputs/apk/debug/app-debug.apk'),
  path.join(ROOT, '.build-outputs/app-debug.apk'),
];
const LOGO_PATH = path.join(ROOT, 'app/src/main/res/drawable/aura_logo.png');
const RAW_DIR = path.join(ROOT, 'app/src/main/res/raw');

const SAMPLE_VIDEOS = [
  { file: 'sample_cyberpunk_4k.mp4', title: 'Cyberpunk 4K', folder: 'Cinema 4K', tag: '4K HDR' },
  { file: 'sample_alpine_glacier.mp4', title: 'Alpine Glacier', folder: 'Nature', tag: '1080p' },
  { file: 'sample_aurora_borealis.mp4', title: 'Aurora Borealis', folder: 'Nature', tag: '1080p' },
  { file: 'sample_obsidian_reel.mp4', title: 'Obsidian Reel', folder: 'Showreel', tag: 'HD' },
  { file: 'sample_studio_acoustic.mp4', title: 'Studio Acoustic', folder: 'Music', tag: 'HD' },
];

function getApkStats() {
  for (const candidate of APK_CANDIDATES) {
    if (fs.existsSync(candidate)) {
      const stat = fs.statSync(candidate);
      return { path: candidate, sizeBytes: stat.size, mtime: stat.mtime.toISOString() };
    }
  }
  return null;
}

function formatBytes(bytes) {
  if (!bytes) return '0 B';
  const mb = bytes / (1024 * 1024);
  return `${mb.toFixed(1)} MB`;
}

function renderHtml() {
  const apk = getApkStats();
  const availableSamples = SAMPLE_VIDEOS.filter((v) =>
    fs.existsSync(path.join(RAW_DIR, v.file))
  );
  const apkSizeLabel = apk ? formatBytes(apk.sizeBytes) : 'Not built';
  const firstVideo = availableSamples[0] || null;

  return `<!doctype html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>Aura Player — Android App Preview</title>
  <style>
    :root {
      --bg-top: #dceeff;
      --bg-right: #ece2ff;
      --bg-base: #f8f9fe;
      --glass: rgba(255, 255, 255, 0.72);
      --glass-border: rgba(255, 255, 255, 0.92);
      --ink: #1e2438;
      --muted: #5b647d;
      --accent: #2563eb;
      --accent-dark: #1d4ed8;
    }
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body {
      min-height: 100vh;
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Inter, sans-serif;
      color: var(--ink);
      background:
        radial-gradient(circle at 12% 8%, var(--bg-top) 0%, transparent 48%),
        radial-gradient(circle at 88% 12%, var(--bg-right) 0%, transparent 45%),
        var(--bg-base);
      padding: 24px 16px 40px;
    }
    .shell {
      max-width: 960px;
      margin: 0 auto;
      display: grid;
      gap: 20px;
    }
    .glass-card {
      background: var(--glass);
      backdrop-filter: blur(20px);
      -webkit-backdrop-filter: blur(20px);
      border: 1px solid var(--glass-border);
      border-radius: 24px;
      box-shadow: 0 12px 36px rgba(37, 99, 235, 0.08);
    }
    header {
      padding: 20px 24px;
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 16px;
      flex-wrap: wrap;
    }
    .brand {
      display: flex;
      align-items: center;
      gap: 14px;
    }
    .brand img {
      width: 52px;
      height: 52px;
      border-radius: 14px;
      object-fit: contain;
      background: #fff;
      padding: 4px;
      box-shadow: 0 6px 16px rgba(37, 99, 235, 0.16);
    }
    .brand h1 {
      font-size: 20px;
      font-weight: 700;
      letter-spacing: -0.02em;
    }
    .brand p {
      font-size: 13px;
      color: var(--muted);
      margin-top: 2px;
    }
    .btn {
      display: inline-flex;
      align-items: center;
      gap: 8px;
      background: var(--accent);
      color: #fff;
      text-decoration: none;
      font-weight: 600;
      font-size: 14px;
      padding: 11px 18px;
      border-radius: 14px;
      border: none;
      cursor: pointer;
      box-shadow: 0 6px 18px rgba(37, 99, 235, 0.28);
      transition: background 0.15s ease, transform 0.15s ease;
    }
    .btn:hover { background: var(--accent-dark); transform: translateY(-1px); }
    .main-grid {
      display: grid;
      grid-template-columns: minmax(0, 1.5fr) minmax(260px, 1fr);
      gap: 20px;
      align-items: start;
    }
    @media (max-width: 760px) {
      .main-grid { grid-template-columns: 1fr; }
    }
    .player-card {
      padding: 16px;
      display: flex;
      flex-direction: column;
      gap: 12px;
    }
    .video-wrap {
      background: #0b101e;
      border-radius: 16px;
      overflow: hidden;
      aspect-ratio: 16 / 9;
      display: flex;
      align-items: center;
      justify-content: center;
    }
    video {
      width: 100%;
      height: 100%;
      object-fit: contain;
      background: #0b101e;
    }
    .player-meta {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 12px;
      flex-wrap: wrap;
      padding: 2px 4px;
    }
    .player-title {
      font-weight: 700;
      font-size: 16px;
    }
    .player-sub {
      font-size: 12px;
      color: var(--muted);
    }
    .controls {
      display: flex;
      gap: 8px;
      align-items: center;
    }
    .ctrl-btn {
      background: rgba(37, 99, 235, 0.1);
      color: var(--accent);
      border: 1px solid rgba(37, 99, 235, 0.2);
      border-radius: 10px;
      padding: 6px 10px;
      font-size: 12px;
      font-weight: 600;
      cursor: pointer;
    }
    .ctrl-btn:hover { background: rgba(37, 99, 235, 0.18); }
    .side-card {
      padding: 18px;
      display: flex;
      flex-direction: column;
      gap: 14px;
    }
    .section-title {
      font-size: 13px;
      font-weight: 700;
      text-transform: uppercase;
      letter-spacing: 0.06em;
      color: var(--muted);
    }
    .playlist {
      display: flex;
      flex-direction: column;
      gap: 8px;
    }
    .track {
      width: 100%;
      text-align: left;
      background: rgba(255, 255, 255, 0.65);
      border: 1px solid rgba(30, 36, 56, 0.08);
      border-radius: 14px;
      padding: 11px 13px;
      cursor: pointer;
      display: flex;
      justify-content: space-between;
      align-items: center;
      transition: all 0.15s ease;
    }
    .track:hover { border-color: rgba(37, 99, 235, 0.35); background: #fff; }
    .track.active {
      border-color: var(--accent);
      background: rgba(37, 99, 235, 0.09);
    }
    .track-name { font-size: 14px; font-weight: 600; color: var(--ink); }
    .track-folder { font-size: 12px; color: var(--muted); margin-top: 2px; }
    .track-tag {
      font-size: 11px;
      font-weight: 700;
      color: var(--accent);
      background: rgba(37, 99, 235, 0.12);
      padding: 3px 8px;
      border-radius: 8px;
    }
    .specs {
      padding: 18px 22px;
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(170px, 1fr));
      gap: 14px;
    }
    .spec-label { font-size: 11px; text-transform: uppercase; letter-spacing: 0.05em; color: var(--muted); }
    .spec-val { font-size: 14px; font-weight: 700; margin-top: 4px; }
  </style>
</head>
<body>
  <div class="shell">
    <header class="glass-card">
      <div class="brand">
        <img src="/logo.png" alt="Aura Player" />
        <div>
          <h1>Aura Player</h1>
          <p>Native Android Kotlin &amp; Jetpack Compose Video Player &middot; v2.6.6</p>
        </div>
      </div>
      ${apk ? `<a class="btn" href="/download/app-debug.apk" download="AuraPlayer-debug.apk">Download Debug APK (${apkSizeLabel})</a>` : ''}
    </header>

    <div class="main-grid">
      <section class="glass-card player-card">
        <div class="video-wrap">
          <video id="videoEl" controls playsinline preload="metadata" ${firstVideo ? `src="/media/${firstVideo.file}"` : ''}></video>
        </div>
        <div class="player-meta">
          <div>
            <div class="player-title" id="nowTitle">${firstVideo ? firstVideo.title : 'No sample video'}</div>
            <div class="player-sub" id="nowFolder">${firstVideo ? `${firstVideo.folder} &middot; Bundled Sample Clip` : ''}</div>
          </div>
          <div class="controls">
            <button class="ctrl-btn" id="speedBtn" type="button">1.0x</button>
            <button class="ctrl-btn" id="fitBtn" type="button">Fit</button>
          </div>
        </div>
      </section>

      <aside class="glass-card side-card">
        <div class="section-title">Bundled Sample Library</div>
        <div class="playlist" id="playlist">
          ${availableSamples
            .map(
              (v, idx) => `
            <button type="button" class="track ${idx === 0 ? 'active' : ''}" data-file="${v.file}" data-title="${v.title}" data-folder="${v.folder}">
              <div>
                <div class="track-name">${v.title}</div>
                <div class="track-folder">${v.folder}</div>
              </div>
              <span class="track-tag">${v.tag}</span>
            </button>`
            )
            .join('')}
        </div>
      </aside>
    </div>

    <section class="glass-card specs">
      <div>
        <div class="spec-label">Application ID</div>
        <div class="spec-val">com.istiak.auraplayer</div>
      </div>
      <div>
        <div class="spec-label">SDK Target</div>
        <div class="spec-val">Android 7.0 – 16 (API 24–36)</div>
      </div>
      <div>
        <div class="spec-label">Media Engine</div>
        <div class="spec-val">Media3 ExoPlayer + FFmpeg</div>
      </div>
      <div>
        <div class="spec-label">Debug APK Artifact</div>
        <div class="spec-val">${apk ? `Ready (${apkSizeLabel})` : 'Not compiled'}</div>
      </div>
    </section>
  </div>

  <script>
    const video = document.getElementById('videoEl');
    const nowTitle = document.getElementById('nowTitle');
    const nowFolder = document.getElementById('nowFolder');
    const speedBtn = document.getElementById('speedBtn');
    const fitBtn = document.getElementById('fitBtn');
    const speeds = [0.75, 1.0, 1.25, 1.5, 2.0];
    let speedIdx = 1;
    let cropped = false;

    document.querySelectorAll('.track').forEach((btn) => {
      btn.addEventListener('click', () => {
        document.querySelectorAll('.track').forEach((b) => b.classList.remove('active'));
        btn.classList.add('active');
        video.src = '/media/' + btn.dataset.file;
        video.playbackRate = speeds[speedIdx];
        nowTitle.textContent = btn.dataset.title;
        nowFolder.textContent = btn.dataset.folder + ' · Bundled Sample Clip';
        video.play().catch(() => {});
      });
    });

    speedBtn.addEventListener('click', () => {
      speedIdx = (speedIdx + 1) % speeds.length;
      video.playbackRate = speeds[speedIdx];
      speedBtn.textContent = speeds[speedIdx].toFixed(2).replace(/\\.00$/, '.0') + 'x';
    });

    fitBtn.addEventListener('click', () => {
      cropped = !cropped;
      video.style.objectFit = cropped ? 'cover' : 'contain';
      fitBtn.textContent = cropped ? 'Crop' : 'Fit';
    });
  </script>
</body>
</html>`;
}

function serveFileWithRange(req, res, filePath, contentType, downloadName = null) {
  if (!fs.existsSync(filePath)) {
    res.writeHead(404, { 'Content-Type': 'text/plain' });
    res.end('Not Found');
    return;
  }
  const stat = fs.statSync(filePath);
  const total = stat.size;
  const headers = {
    'Content-Type': contentType,
    'Accept-Ranges': 'bytes',
  };
  if (downloadName) {
    headers['Content-Disposition'] = `attachment; filename="${downloadName}"`;
  }

  const range = req.headers.range;
  if (range) {
    const match = /bytes=(\d*)-(\d*)/.exec(range);
    const start = match && match[1] ? parseInt(match[1], 10) : 0;
    const end = match && match[2] ? Math.min(parseInt(match[2], 10), total - 1) : total - 1;
    if (start >= total || start > end) {
      res.writeHead(416, { 'Content-Range': `bytes */${total}` });
      res.end();
      return;
    }
    res.writeHead(206, {
      ...headers,
      'Content-Range': `bytes ${start}-${end}/${total}`,
      'Content-Length': end - start + 1,
    });
    fs.createReadStream(filePath, { start, end }).pipe(res);
  } else {
    res.writeHead(200, { ...headers, 'Content-Length': total });
    fs.createReadStream(filePath).pipe(res);
  }
}

const server = http.createServer((req, res) => {
  const url = new URL(req.url || '/', `http://localhost:${PORT}`);
  const pathname = url.pathname;

  if (pathname === '/' || pathname === '/index.html') {
    const html = renderHtml();
    res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
    res.end(html);
    return;
  }

  if (pathname === '/logo.png') {
    serveFileWithRange(req, res, LOGO_PATH, 'image/png');
    return;
  }

  if (pathname === '/download/app-debug.apk') {
    const apk = getApkStats();
    if (!apk) {
      res.writeHead(404, { 'Content-Type': 'text/plain' });
      res.end('APK not found');
      return;
    }
    serveFileWithRange(
      req,
      res,
      apk.path,
      'application/vnd.android.package-archive',
      'AuraPlayer-debug.apk'
    );
    return;
  }

  if (pathname.startsWith('/media/')) {
    const fileName = path.basename(pathname.slice('/media/'.length));
    const allowed = SAMPLE_VIDEOS.some((v) => v.file === fileName);
    if (!allowed) {
      res.writeHead(404, { 'Content-Type': 'text/plain' });
      res.end('Not Found');
      return;
    }
    serveFileWithRange(req, res, path.join(RAW_DIR, fileName), 'video/mp4');
    return;
  }

  if (pathname === '/api/info') {
    const apk = getApkStats();
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(
      JSON.stringify({
        name: 'Aura Player',
        applicationId: 'com.istiak.auraplayer',
        versionName: '2.6.6',
        apkReady: Boolean(apk),
        apkSizeBytes: apk ? apk.sizeBytes : 0,
      })
    );
    return;
  }

  res.writeHead(404, { 'Content-Type': 'text/plain' });
  res.end('Not Found');
});

server.listen(PORT, '0.0.0.0', () => {
  console.log(`Aura Player preview server running on http://0.0.0.0:${PORT}`);
});
