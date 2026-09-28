<template>
  <div>
    <div class="page-header">
      <div>
        <h1 class="page-title">Analisi Giocatori</h1>
        <p class="page-subtitle">Scheda pre-asta: watchlist, note e statistiche storiche</p>
      </div>
      <div style="display:flex;gap:8px;align-items:center">
        <span style="font-size:12px;color:var(--text-muted)">
          ★ {{ watchlistCount }} in watchlist
        </span>
        <button class="btn btn-secondary btn-sm" @click="filterWatchlist = !filterWatchlist" :class="{ 'btn-primary': filterWatchlist }">
          <svg width="13" height="13" viewBox="0 0 24 24" fill="currentColor"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/></svg>
          Solo watchlist
        </button>
      </div>
    </div>

    <!-- Filters -->
    <div class="card" style="margin-bottom:20px">
      <div class="card-body" style="padding:14px 16px">
        <div class="filters-bar" style="margin-bottom:0">
          <div class="form-group" style="max-width:220px">
            <label class="form-label">Cerca</label>
            <input v-model="filters.search" placeholder="Nome giocatore..." @input="debouncedLoad" />
          </div>
          <div class="form-group" style="max-width:170px">
            <label class="form-label">Ruolo Mantra</label>
            <select v-model="filters.mantraRole">
              <option value="">Tutti i ruoli</option>
              <optgroup label="Portieri"><option value="Por">Por – Portiere</option></optgroup>
              <optgroup label="Difensori">
                <option value="Dc">Dc – Difensore Centrale</option>
                <option value="Dd">Dd – Terzino Destro</option>
                <option value="Ds">Ds – Terzino Sinistro</option>
                <option value="B">B – Braccetto</option>
              </optgroup>
              <optgroup label="Centrocampisti">
                <option value="E">E – Esterno</option>
                <option value="M">M – Mediano</option>
                <option value="C">C – Centrocampista</option>
                <option value="T">T – Trequartista</option>
                <option value="W">W – Ala</option>
              </optgroup>
              <optgroup label="Attaccanti">
                <option value="A">A – Attaccante</option>
                <option value="Pc">Pc – Punta Centrale</option>
              </optgroup>
            </select>
          </div>
          <div class="form-group" style="max-width:160px">
            <label class="form-label">Squadra</label>
            <select v-model="filters.team" @change="loadPlayers">
              <option value="">Tutte le squadre</option>
              <option v-for="t in teams" :key="t" :value="t">{{ t }}</option>
            </select>
          </div>
          <div class="form-group" style="max-width:130px">
            <label class="form-label">Fascia</label>
            <select v-model="filters.tier">
              <option value="">Tutte</option>
              <option value="Alta">Alta</option>
              <option value="Media">Media</option>
              <option value="Bassa">Bassa</option>
            </select>
          </div>
          <div class="form-group" style="max-width:140px">
            <label class="form-label">Stagione stat.</label>
            <select v-model="selectedSeason">
              <option value="">Nessuna</option>
              <option v-for="s in seasons" :key="s" :value="s">{{ s }}</option>
            </select>
          </div>
          <div style="display:flex;align-items:flex-end;padding-bottom:1px">
            <button class="btn btn-ghost btn-sm" @click="resetFilters">
              <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
              Reset
            </button>
          </div>
          <div style="margin-left:auto;display:flex;align-items:flex-end;padding-bottom:2px;color:var(--text-muted);font-size:12px">
            {{ sortedPlayers.length }} risultati
          </div>
        </div>
      </div>
    </div>

    <!-- Table -->
    <div class="card">
      <div class="table-wrap">
        <div v-if="loadingPlayers" class="empty-state">
          <span class="spinner" style="width:28px;height:28px;border-width:3px;color:var(--primary)"></span>
        </div>
        <div v-else-if="sortedPlayers.length === 0" class="empty-state">
          <div class="empty-state-icon">🔍</div>
          <div class="empty-state-title">Nessun giocatore trovato</div>
        </div>
        <table v-else>
          <thead>
            <tr>
              <th style="width:32px"></th>
              <th class="sortable" :class="{ 'sort-active': sortKey==='name' }" @click="setSort('name')">
                Giocatore <SortIcon field="name" :current-key="sortKey" :dir="sortDir" />
              </th>
              <th class="sortable" :class="{ 'sort-active': sortKey==='team' }" @click="setSort('team')">
                Squadra <SortIcon field="team" :current-key="sortKey" :dir="sortDir" />
              </th>
              <th>Mantra</th>
              <th class="sortable" :class="{ 'sort-active': sortKey==='currentPrice' }" @click="setSort('currentPrice')" style="text-align:right">
                Q.A. <SortIcon field="currentPrice" :current-key="sortKey" :dir="sortDir" />
              </th>
              <th class="sortable" :class="{ 'sort-active': sortKey==='fm' }" @click="setSort('fm')" style="text-align:right">
                FM <SortIcon field="fm" :current-key="sortKey" :dir="sortDir" />
              </th>
              <th class="sortable" :class="{ 'sort-active': sortKey==='mv' }" @click="setSort('mv')" style="text-align:right">
                MV <SortIcon field="mv" :current-key="sortKey" :dir="sortDir" />
              </th>
              <th class="sortable" :class="{ 'sort-active': sortKey==='pv' }" @click="setSort('pv')" style="text-align:right">
                PV <SortIcon field="pv" :current-key="sortKey" :dir="sortDir" />
              </th>
              <th class="sortable" :class="{ 'sort-active': sortKey==='gf' }" @click="setSort('gf')" style="text-align:right">
                Gol <SortIcon field="gf" :current-key="sortKey" :dir="sortDir" />
              </th>
              <th class="sortable" :class="{ 'sort-active': sortKey==='ass' }" @click="setSort('ass')" style="text-align:right">
                Ass <SortIcon field="ass" :current-key="sortKey" :dir="sortDir" />
              </th>
              <th style="text-align:center">Fascia</th>
              <th style="min-width:200px">Note</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(p, i) in sortedPlayers" :key="p.id" :class="p.watchlisted ? 'row-watchlisted' : ''">
              <!-- Star -->
              <td style="text-align:center">
                <button class="star-btn" :class="{ active: p.watchlisted }" @click="toggleWatchlist(p)" :title="p.watchlisted ? 'Rimuovi dalla watchlist' : 'Aggiungi alla watchlist'">
                  <svg width="15" height="15" viewBox="0 0 24 24" :fill="p.watchlisted ? 'currentColor' : 'none'" stroke="currentColor" stroke-width="2">
                    <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/>
                  </svg>
                </button>
              </td>
              <td>
                <div style="font-weight:600">{{ p.name }}</div>
                <div style="font-size:11px;color:var(--text-muted)">{{ p.role }}</div>
              </td>
              <td style="color:var(--text-muted);font-size:13px">{{ p.team || '—' }}</td>
              <td><MantraRoles :mantra-role="p.mantraRole" /></td>
              <td style="text-align:right;font-weight:700;font-size:14px">{{ p.currentPrice ?? '—' }}</td>
              <!-- Stats from selected season -->
              <td style="text-align:right;font-weight:700;font-size:13px" :style="{ color: statsMap[p.id] ? 'var(--primary)' : 'var(--text-light)' }">
                {{ statsMap[p.id] ? fmStr(statsMap[p.id].fm) : '—' }}
              </td>
              <td style="text-align:right;font-size:13px;color:var(--text-muted)">
                {{ statsMap[p.id] ? fmStr(statsMap[p.id].mv) : '—' }}
              </td>
              <td style="text-align:right;font-size:13px;color:var(--text-muted)">
                {{ statsMap[p.id]?.pv ?? '—' }}
              </td>
              <td style="text-align:right;font-size:13px;color:var(--text-muted)">
                {{ statsMap[p.id]?.gf ?? '—' }}
              </td>
              <td style="text-align:right;font-size:13px;color:var(--text-muted)">
                {{ statsMap[p.id]?.ass ?? '—' }}
              </td>
              <td style="text-align:center">
                <TierBadge :tier="getTier(p.id)" />
              </td>
              <!-- Note -->
              <td>
                <textarea
                  class="note-input"
                  :value="p.analysisNote || ''"
                  placeholder="Note..."
                  rows="1"
                  @change="saveNote(p, $event.target.value)"
                  @input="autoResize($event)"
                ></textarea>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { playerApi, statsApi } from '../services/api.js'
import MantraRoles from '../components/MantraRoles.vue'

// ── Inline components ─────────────────────────────────────────────────
const SortIcon = {
  props: ['field', 'currentKey', 'dir'],
  template: `<span class="sort-icon" :class="currentKey === field ? 'dir-' + dir : ''"><span class="asc"></span><span class="desc"></span></span>`
}

const TierBadge = {
  props: ['tier'],
  template: `
    <span v-if="tier === 'Alta'"  style="display:inline-block;padding:2px 7px;border-radius:9999px;font-size:11px;font-weight:600;background:#dcfce7;color:#14532d">Alta</span>
    <span v-else-if="tier === 'Media'" style="display:inline-block;padding:2px 7px;border-radius:9999px;font-size:11px;font-weight:600;background:#dbeafe;color:#1e3a8a">Media</span>
    <span v-else-if="tier === 'Bassa'" style="display:inline-block;padding:2px 7px;border-radius:9999px;font-size:11px;font-weight:600;background:#f1f5f9;color:#475569">Bassa</span>
    <span v-else style="color:var(--text-light);font-size:12px">—</span>
  `
}

// ── State ─────────────────────────────────────────────────────────────
const players = ref([])
const loadingPlayers = ref(false)

const seasons = ref([])
const selectedSeason = ref('')
const seasonStats = ref([])   // StatsDTO[] for the selected season

const filters = ref({ search: '', mantraRole: '', team: '', tier: '' })
const filterWatchlist = ref(false)
const sortKey = ref('currentPrice')
const sortDir = ref('desc')
let debounceTimer = null

// ── Computed ──────────────────────────────────────────────────────────
const teams = computed(() => [...new Set(players.value.map(p => p.team).filter(Boolean))].sort())

const watchlistCount = computed(() => players.value.filter(p => p.watchlisted).length)

/** playerId → StatsDTO */
const statsMap = computed(() => {
  const m = {}
  for (const s of seasonStats.value) m[s.playerId] = s
  return m
})

/** Percentile-based tier per role, same logic as StatsView */
const tierMap = computed(() => {
  const byRole = { P: [], D: [], C: [], A: [] }
  for (const s of seasonStats.value) {
    if (byRole[s.playerRole] !== undefined && s.pv != null && s.pv >= 5 && s.fm != null)
      byRole[s.playerRole].push(s.fm)
  }
  const thresholds = {}
  for (const [role, fms] of Object.entries(byRole)) {
    if (!fms.length) { thresholds[role] = null; continue }
    const sorted = [...fms].sort((a, b) => a - b)
    thresholds[role] = { p25: percentile(sorted, 25), p75: percentile(sorted, 75) }
  }
  const map = {}
  for (const s of seasonStats.value) {
    if (s.fm == null) { map[s.playerId] = null; continue }
    const t = thresholds[s.playerRole]
    if (!t) { map[s.playerId] = null; continue }
    map[s.playerId] = s.fm >= t.p75 ? 'Alta' : s.fm <= t.p25 ? 'Bassa' : 'Media'
  }
  return map
})

function percentile(sorted, pct) {
  const idx = (pct / 100) * (sorted.length - 1)
  const lo = Math.floor(idx), hi = Math.ceil(idx)
  if (lo === hi) return sorted[lo]
  return sorted[lo] + (sorted[hi] - sorted[lo]) * (idx - lo)
}

function getTier(playerId) { return tierMap.value[playerId] ?? null }

const displayedPlayers = computed(() => {
  let arr = players.value
  if (filters.value.mantraRole)
    arr = arr.filter(p => (p.mantraRole || '').split(';').map(r => r.trim()).includes(filters.value.mantraRole))
  if (filters.value.tier)
    arr = arr.filter(p => getTier(p.id) === filters.value.tier)
  if (filterWatchlist.value)
    arr = arr.filter(p => p.watchlisted)
  return arr
})

const ROLE_ORDER = { P: 0, D: 1, C: 2, A: 3 }

const sortedPlayers = computed(() => {
  const arr = [...displayedPlayers.value]
  const dir = sortDir.value === 'asc' ? 1 : -1
  arr.sort((a, b) => {
    const k = sortKey.value
    if (['fm', 'mv', 'pv', 'gf', 'ass'].includes(k)) {
      const va = statsMap.value[a.id]?.[k] ?? -Infinity
      const vb = statsMap.value[b.id]?.[k] ?? -Infinity
      return (va - vb) * dir
    }
    if (k === 'role') return ((ROLE_ORDER[a.role] ?? 9) - (ROLE_ORDER[b.role] ?? 9)) * dir
    const va = a[k], vb = b[k]
    if (typeof va === 'number' && typeof vb === 'number') return ((va ?? -1) - (vb ?? -1)) * dir
    return String(va ?? '').localeCompare(String(vb ?? '')) * dir
  })
  return arr
})

// ── Helpers ───────────────────────────────────────────────────────────
function fmStr(val) {
  if (val == null) return '—'
  return val.toFixed(2).replace('.', ',')
}

function autoResize(e) {
  e.target.style.height = 'auto'
  e.target.style.height = e.target.scrollHeight + 'px'
}

// ── Sort ──────────────────────────────────────────────────────────────
function setSort(key) {
  if (sortKey.value === key) {
    sortDir.value = sortDir.value === 'asc' ? 'desc' : 'asc'
  } else {
    sortKey.value = key
    sortDir.value = ['currentPrice', 'initialPrice', 'fm', 'mv', 'pv', 'gf', 'ass'].includes(key) ? 'desc' : 'asc'
  }
}

// ── Data ──────────────────────────────────────────────────────────────
function debouncedLoad() {
  clearTimeout(debounceTimer)
  debounceTimer = setTimeout(loadPlayers, 300)
}

async function loadPlayers() {
  loadingPlayers.value = true
  try {
    const res = await playerApi.getAll({
      team:   filters.value.team   || undefined,
      search: filters.value.search || undefined
    })
    players.value = res.data
  } finally {
    loadingPlayers.value = false
  }
}

async function loadSeasons() {
  try {
    const res = await statsApi.getSeasons()
    seasons.value = res.data
    if (res.data.length > 0) selectedSeason.value = res.data[0]
  } catch {}
}

async function loadSeasonStats() {
  if (!selectedSeason.value) { seasonStats.value = []; return }
  try {
    const res = await statsApi.getBySeason(selectedSeason.value)
    seasonStats.value = res.data
  } catch { seasonStats.value = [] }
}

watch(selectedSeason, loadSeasonStats)

// ── Actions ───────────────────────────────────────────────────────────
async function toggleWatchlist(player) {
  const newVal = !player.watchlisted
  player.watchlisted = newVal
  try {
    await playerApi.updateAnalysis(player.id, { watchlisted: newVal })
  } catch {
    player.watchlisted = !newVal
  }
}

let noteTimers = {}
function saveNote(player, value) {
  player.analysisNote = value
  clearTimeout(noteTimers[player.id])
  noteTimers[player.id] = setTimeout(async () => {
    try {
      await playerApi.updateAnalysis(player.id, { analysisNote: value })
    } catch {}
  }, 800)
}

function resetFilters() {
  filters.value = { search: '', mantraRole: '', team: '', tier: '' }
  filterWatchlist.value = false
  loadPlayers()
}

onMounted(async () => {
  await Promise.all([loadPlayers(), loadSeasons()])
  await loadSeasonStats()
})
</script>

<style scoped>
.star-btn {
  background: none; border: none; cursor: pointer; padding: 4px;
  color: var(--text-light); border-radius: 4px;
  transition: color .15s, transform .1s;
  display: flex; align-items: center;
}
.star-btn:hover { color: #f59e0b; }
.star-btn.active { color: #f59e0b; }
.star-btn.active:hover { color: #d97706; }

.row-watchlisted { background: #fffbeb; }

.note-input {
  width: 100%; min-width: 180px;
  padding: 5px 8px;
  border: 1px solid var(--border);
  border-radius: 6px;
  background: var(--bg);
  color: var(--text);
  font-size: 12px;
  font-family: inherit;
  resize: none;
  overflow: hidden;
  line-height: 1.4;
  transition: border-color .15s;
}
.note-input:focus { outline: none; border-color: var(--primary); }
.note-input::placeholder { color: var(--text-light); }
</style>
