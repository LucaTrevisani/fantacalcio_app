<template>
  <div>
    <!-- Header -->
    <div class="page-header">
      <div>
        <h1 class="page-title">Statistiche Storiche</h1>
        <p class="page-subtitle">Rendimento stagionale dei giocatori per fascia di qualità</p>
      </div>
    </div>

    <!-- Upload card -->
    <div class="card" style="margin-bottom:20px">
      <div class="card-header">
        <h2 class="card-title">Carica statistiche</h2>
      </div>
      <div class="card-body">
        <div class="form-row" style="align-items:flex-end;gap:12px;flex-wrap:wrap">
          <div class="form-group" style="flex:1;min-width:220px">
            <label class="form-label">File Excel (.xlsx)</label>
            <input type="file" accept=".xlsx,.xls" @change="onFileChange" ref="fileInput" />
          </div>
          <div class="form-group" style="max-width:160px">
            <label class="form-label">Stagione</label>
            <input v-model="uploadSeason" placeholder="es. 2024-25" />
          </div>
          <div style="padding-bottom:2px">
            <button class="btn btn-primary" @click="doUpload" :disabled="!uploadFile || !uploadSeason || uploading">
              <span v-if="uploading" class="spinner"></span>
              <svg v-else width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                <path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4"/>
                <polyline points="17 8 12 3 7 8"/>
                <line x1="12" y1="3" x2="12" y2="15"/>
              </svg>
              Carica
            </button>
          </div>
        </div>
        <transition name="fade">
          <div v-if="uploadMessage" class="alert" :class="uploadError ? 'alert-error' : 'alert-success'" style="margin-top:12px">
            {{ uploadMessage }}
          </div>
        </transition>
      </div>
    </div>

    <!-- Season selector -->
    <div class="card" style="margin-bottom:20px">
      <div class="card-body">
        <div class="form-row" style="align-items:center;gap:16px">
          <div class="form-group" style="max-width:200px;margin-bottom:0">
            <label class="form-label">Stagione</label>
            <select v-model="selectedSeason" @change="loadStats">
              <option value="">— Seleziona stagione —</option>
              <option v-for="s in seasons" :key="s" :value="s">{{ s }}</option>
            </select>
          </div>
          <div v-if="loadingStats" style="display:flex;align-items:center;gap:8px;color:var(--text-muted);font-size:13px;padding-top:18px">
            <span class="spinner" style="width:16px;height:16px;border-width:2px;color:var(--primary)"></span>
            Caricamento...
          </div>
        </div>
      </div>
    </div>

    <!-- Empty state: no seasons at all -->
    <div v-if="seasons.length === 0 && !loadingStats" class="empty-state">
      <div class="empty-state-icon">📊</div>
      <div class="empty-state-title">Nessuna stagione disponibile</div>
      <div class="empty-state-desc">Carica un file Excel con le statistiche di una stagione per iniziare.</div>
    </div>

    <template v-if="selectedSeason && stats.length">
      <!-- Stats bar -->
      <div class="stats-row" style="margin-bottom:20px">
        <div class="stat-card">
          <div class="stat-value" style="color:var(--text)">{{ stats.length }}</div>
          <div class="stat-label">Totale</div>
        </div>
        <div class="stat-card">
          <div class="stat-value" style="color:#14532d">{{ tierCount('Alta') }}</div>
          <div class="stat-label">Fascia Alta</div>
        </div>
        <div class="stat-card">
          <div class="stat-value" style="color:#1e3a8a">{{ tierCount('Media') }}</div>
          <div class="stat-label">Fascia Media</div>
        </div>
        <div class="stat-card">
          <div class="stat-value" style="color:#475569">{{ tierCount('Bassa') }}</div>
          <div class="stat-label">Fascia Bassa</div>
        </div>
      </div>

      <!-- Table card -->
      <div class="card">
        <!-- Filters -->
        <div class="filters-bar">
          <div class="form-group" style="max-width:220px">
            <label class="form-label">Cerca</label>
            <input v-model="filterSearch" placeholder="Nome giocatore..." />
          </div>
          <div class="form-group" style="max-width:180px">
            <label class="form-label">Ruolo Mantra</label>
            <select v-model="filterMantra">
              <option value="">Tutti i ruoli</option>
              <optgroup label="Portieri">
                <option value="Por">Por – Portiere</option>
              </optgroup>
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
          <div class="form-group" style="max-width:150px">
            <label class="form-label">Fascia</label>
            <select v-model="filterTier">
              <option value="">Tutte</option>
              <option value="Alta">Alta</option>
              <option value="Media">Media</option>
              <option value="Bassa">Bassa</option>
            </select>
          </div>
          <div style="margin-left:auto;display:flex;align-items:flex-end;padding-bottom:2px;color:var(--text-muted);font-size:12px">
            {{ filteredStats.length }} risultati
          </div>
        </div>

        <!-- Table -->
        <div class="table-wrap">
          <table v-if="filteredStats.length">
            <thead>
              <tr>
                <th style="width:36px">#</th>
                <th class="sortable" :class="{ 'sort-active': sortKey === 'playerName' }" @click="setSort('playerName')">
                  Giocatore <SortIcon field="playerName" :current-key="sortKey" :dir="sortDir" />
                </th>
                <th>Mantra</th>
                <th>Fascia</th>
                <th class="sortable" :class="{ 'sort-active': sortKey === 'fm' }" @click="setSort('fm')" style="text-align:right">
                  FM <SortIcon field="fm" :current-key="sortKey" :dir="sortDir" />
                </th>
                <th class="sortable" :class="{ 'sort-active': sortKey === 'mv' }" @click="setSort('mv')" style="text-align:right">
                  MV <SortIcon field="mv" :current-key="sortKey" :dir="sortDir" />
                </th>
                <th class="sortable" :class="{ 'sort-active': sortKey === 'pv' }" @click="setSort('pv')" style="text-align:right">
                  PV <SortIcon field="pv" :current-key="sortKey" :dir="sortDir" />
                </th>
                <th class="sortable" :class="{ 'sort-active': sortKey === 'gf' }" @click="setSort('gf')" style="text-align:right">
                  Gol <SortIcon field="gf" :current-key="sortKey" :dir="sortDir" />
                </th>
                <th class="sortable" :class="{ 'sort-active': sortKey === 'ass' }" @click="setSort('ass')" style="text-align:right">
                  Ass <SortIcon field="ass" :current-key="sortKey" :dir="sortDir" />
                </th>
                <th class="sortable" :class="{ 'sort-active': sortKey === 'gs' }" @click="setSort('gs')" style="text-align:right">
                  GS <SortIcon field="gs" :current-key="sortKey" :dir="sortDir" />
                </th>
                <th class="sortable" :class="{ 'sort-active': sortKey === 'rp' }" @click="setSort('rp')" style="text-align:right">
                  RP <SortIcon field="rp" :current-key="sortKey" :dir="sortDir" />
                </th>
                <th class="sortable" :class="{ 'sort-active': sortKey === 'amm' }" @click="setSort('amm')" style="text-align:right">
                  Amm <SortIcon field="amm" :current-key="sortKey" :dir="sortDir" />
                </th>
                <th class="sortable" :class="{ 'sort-active': sortKey === 'esp' }" @click="setSort('esp')" style="text-align:right">
                  Esp <SortIcon field="esp" :current-key="sortKey" :dir="sortDir" />
                </th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(p, i) in sortedStats" :key="p.playerId" :style="rowBorderStyle(p)">
                <td style="color:var(--text-light);font-size:12px;font-weight:500">{{ i + 1 }}</td>
                <td>
                  <span style="font-weight:600">{{ p.playerName }}</span>
                  <span style="color:var(--text-muted);font-size:12px;margin-left:6px">{{ p.playerTeam }}</span>
                </td>
                <td><MantraRoles :mantra-role="p.playerMantraRole" /></td>
                <td><TierBadge :tier="getTier(p)" /></td>
                <td style="text-align:right;font-weight:700;font-size:14px">{{ fmStr(p.fm) }}</td>
                <td style="text-align:right;color:var(--text-muted);font-size:13px">{{ fmStr(p.mv) }}</td>
                <td style="text-align:right;color:var(--text-muted);font-size:13px">{{ p.pv ?? '—' }}</td>
                <td style="text-align:right;color:var(--text-muted);font-size:13px">{{ p.gf ?? '—' }}</td>
                <td style="text-align:right;color:var(--text-muted);font-size:13px">{{ p.ass ?? '—' }}</td>
                <td style="text-align:right;color:var(--text-muted);font-size:13px">{{ showGoalkeeper(p, 'gs') }}</td>
                <td style="text-align:right;color:var(--text-muted);font-size:13px">{{ showGoalkeeper(p, 'rp') }}</td>
                <td style="text-align:right;color:var(--text-muted);font-size:13px">{{ p.amm ?? '—' }}</td>
                <td style="text-align:right;color:var(--text-muted);font-size:13px">{{ p.esp ?? '—' }}</td>
              </tr>
            </tbody>
          </table>
          <div v-else-if="!loadingStats" class="empty-state">
            <div class="empty-state-icon">🔍</div>
            <div class="empty-state-title">Nessun risultato</div>
            <div class="empty-state-desc">Prova a modificare i filtri di ricerca.</div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { statsApi } from '../services/api.js'
import MantraRoles from '../components/MantraRoles.vue'

// ── Inline components ────────────────────────────────────────────────

const SortIcon = {
  props: ['field', 'currentKey', 'dir'],
  template: `
    <span class="sort-icon" :class="currentKey === field ? 'dir-' + dir : ''">
      <span class="asc"></span>
      <span class="desc"></span>
    </span>
  `
}

const TierBadge = {
  props: ['tier'],
  template: `
    <span v-if="tier === 'Alta'" style="display:inline-block;padding:2px 8px;border-radius:9999px;font-size:11px;font-weight:600;background:#dcfce7;color:#14532d">🔥 Alta</span>
    <span v-else-if="tier === 'Media'" style="display:inline-block;padding:2px 8px;border-radius:9999px;font-size:11px;font-weight:600;background:#dbeafe;color:#1e3a8a">📊 Media</span>
    <span v-else-if="tier === 'Bassa'" style="display:inline-block;padding:2px 8px;border-radius:9999px;font-size:11px;font-weight:600;background:#f1f5f9;color:#475569">📉 Bassa</span>
    <span v-else style="color:var(--text-light);font-size:12px">—</span>
  `
}

// ── State ─────────────────────────────────────────────────────────────
const seasons = ref([])
const selectedSeason = ref('')
const stats = ref([])
const loadingStats = ref(false)

const uploadFile = ref(null)
const uploadSeason = ref('')
const uploading = ref(false)
const uploadMessage = ref('')
const uploadError = ref(false)
const fileInput = ref(null)

const filterSearch = ref('')
const filterMantra = ref('')
const filterTier = ref('')
const sortKey = ref('fm')
const sortDir = ref('desc')

// ── Percentile-based tier calculation ────────────────────────────────
/**
 * For each classic role (P, D, C, A), compute 25th/75th percentile of fm
 * among players with pv >= 5. Everyone gets a tier if fm != null.
 */
const tierMap = computed(() => {
  const byRole = { P: [], D: [], C: [], A: [] }
  for (const p of stats.value) {
    const role = p.playerRole
    if (role && byRole[role] !== undefined && p.pv != null && p.pv >= 5 && p.fm != null) {
      byRole[role].push(p.fm)
    }
  }

  // Compute percentiles per role
  const thresholds = {}
  for (const [role, fms] of Object.entries(byRole)) {
    if (fms.length === 0) { thresholds[role] = null; continue }
    const sorted = [...fms].sort((a, b) => a - b)
    const p25 = percentile(sorted, 25)
    const p75 = percentile(sorted, 75)
    thresholds[role] = { p25, p75 }
  }

  // Assign tier to every player
  const map = {}
  for (const p of stats.value) {
    if (p.fm == null) { map[p.playerId] = null; continue }
    const role = p.playerRole
    const t = thresholds[role]
    if (!t) { map[p.playerId] = null; continue }
    if (p.fm >= t.p75) map[p.playerId] = 'Alta'
    else if (p.fm <= t.p25) map[p.playerId] = 'Bassa'
    else map[p.playerId] = 'Media'
  }
  return map
})

function percentile(sorted, pct) {
  const idx = (pct / 100) * (sorted.length - 1)
  const lo = Math.floor(idx)
  const hi = Math.ceil(idx)
  if (lo === hi) return sorted[lo]
  return sorted[lo] + (sorted[hi] - sorted[lo]) * (idx - lo)
}

function getTier(p) {
  return tierMap.value[p.playerId] ?? null
}

function tierCount(tier) {
  return stats.value.filter(p => getTier(p) === tier).length
}

// ── Filtered + sorted stats ───────────────────────────────────────────
const filteredStats = computed(() => {
  let arr = stats.value
  if (filterSearch.value) {
    const q = filterSearch.value.toLowerCase()
    arr = arr.filter(p => p.playerName?.toLowerCase().includes(q))
  }
  if (filterMantra.value) {
    arr = arr.filter(p => {
      const roles = p.playerMantraRole
        ? p.playerMantraRole.split(';').map(r => r.trim())
        : []
      return roles.includes(filterMantra.value)
    })
  }
  if (filterTier.value) {
    arr = arr.filter(p => getTier(p) === filterTier.value)
  }
  return arr
})

const ROLE_ORDER = { P: 0, D: 1, C: 2, A: 3 }

const sortedStats = computed(() => {
  const arr = [...filteredStats.value]
  const k = sortKey.value
  const dir = sortDir.value === 'asc' ? 1 : -1
  arr.sort((a, b) => {
    let va = a[k], vb = b[k]
    if (k === 'playerRole') {
      return ((ROLE_ORDER[va] ?? 9) - (ROLE_ORDER[vb] ?? 9)) * dir
    }
    if (typeof va === 'number' && typeof vb === 'number') {
      return ((va ?? -Infinity) - (vb ?? -Infinity)) * dir
    }
    return String(va ?? '').localeCompare(String(vb ?? '')) * dir
  })
  return arr
})

// ── Helpers ───────────────────────────────────────────────────────────
function fmStr(val) {
  if (val == null) return '—'
  return val.toFixed(2).replace('.', ',')
}

function showGoalkeeper(p, field) {
  const role = p.playerRole
  if (role !== 'P' && role !== 'D') return '—'
  return p[field] ?? '—'
}

function rowBorderStyle(p) {
  const tier = getTier(p)
  if (tier === 'Alta') return 'border-left: 3px solid #16a34a'
  if (tier === 'Media') return 'border-left: 3px solid #3b82f6'
  if (tier === 'Bassa') return 'border-left: 3px solid #94a3b8'
  return ''
}

// ── Sort ──────────────────────────────────────────────────────────────
function setSort(key) {
  if (sortKey.value === key) {
    sortDir.value = sortDir.value === 'asc' ? 'desc' : 'asc'
  } else {
    sortKey.value = key
    sortDir.value = ['fm', 'mv', 'pv', 'gf', 'ass', 'gf', 'rp', 'gs'].includes(key) ? 'desc' : 'asc'
  }
}

// ── Data ──────────────────────────────────────────────────────────────
async function loadSeasons() {
  try {
    const res = await statsApi.getSeasons()
    seasons.value = res.data
    if (seasons.value.length > 0 && !selectedSeason.value) {
      selectedSeason.value = seasons.value[0]
      await loadStats()
    }
  } catch (e) {
    console.error('Error loading seasons', e)
  }
}

async function loadStats() {
  if (!selectedSeason.value) { stats.value = []; return }
  loadingStats.value = true
  try {
    const res = await statsApi.getBySeason(selectedSeason.value)
    stats.value = res.data
  } catch (e) {
    console.error('Error loading stats', e)
    stats.value = []
  } finally {
    loadingStats.value = false
  }
}

function onFileChange(e) {
  uploadFile.value = e.target.files[0] || null
}

async function doUpload() {
  if (!uploadFile.value || !uploadSeason.value) return
  uploading.value = true
  uploadMessage.value = ''
  uploadError.value = false
  try {
    const fd = new FormData()
    fd.append('file', uploadFile.value)
    fd.append('season', uploadSeason.value)
    const res = await statsApi.upload(fd)
    uploadMessage.value = `Importati ${res.data.count} giocatori per la stagione ${res.data.season}.`
    uploadError.value = false
    // Refresh seasons list and auto-select the just-uploaded season
    await loadSeasons()
    selectedSeason.value = uploadSeason.value
    await loadStats()
    // Reset file input
    if (fileInput.value) fileInput.value.value = ''
    uploadFile.value = null
  } catch (e) {
    uploadMessage.value = e.response?.data?.error || 'Errore durante il caricamento.'
    uploadError.value = true
  } finally {
    uploading.value = false
    setTimeout(() => { uploadMessage.value = '' }, 8000)
  }
}

onMounted(loadSeasons)
</script>

<style scoped>
.fade-enter-active, .fade-leave-active { transition: opacity .3s, transform .3s; }
.fade-enter-from, .fade-leave-to { opacity: 0; transform: translateY(-6px); }

tbody tr { border-left: 3px solid transparent; }
</style>
