<template>
  <div>
    <!-- Header -->
    <div class="page-header">
      <div>
        <h1 class="page-title">Giocatori</h1>
        <p class="page-subtitle">Lista quotazioni Serie A · stagione 2026/27</p>
      </div>
      <div style="display:flex;gap:8px;flex-wrap:wrap">
        <button class="btn btn-secondary" @click="refresh" :disabled="loading">
          <span v-if="loading" class="spinner"></span>
          <svg v-else width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M23 4v6h-6"/><path d="M1 20v-6h6"/><path d="M3.51 9a9 9 0 0114.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0020.49 15"/></svg>
          Aggiorna dal sito
        </button>
        <label class="btn btn-secondary" style="cursor:pointer">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="12" y1="18" x2="12" y2="12"/><line x1="9" y1="15" x2="15" y2="15"/></svg>
          Carica Excel
          <input type="file" accept=".xlsx,.xls" @change="uploadExcel" hidden ref="excelInput">
        </label>
        <label class="btn btn-secondary" style="cursor:pointer">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
          Carica CSV
          <input type="file" accept=".csv" @change="uploadCsv" hidden ref="csvInput">
        </label>
      </div>
    </div>

    <!-- Alert -->
    <transition name="fade">
      <div v-if="message" class="alert" :class="messageType === 'error' ? 'alert-error' : 'alert-success'" style="margin-bottom:20px">
        <span>{{ message }}</span>
        <span v-if="messageType === 'error'" style="font-size:12px;opacity:.85">
          💡 Il sito usa JavaScript per caricare i dati. Scarica il file Excel da
          <strong>fantacalcio.it → Quotazioni → icona download</strong> e usa "Carica Excel".
        </span>
      </div>
    </transition>

    <!-- Stats (sempre sul dataset filtrato) -->
    <div class="stats-row" style="margin-bottom:20px">
      <div class="stat-card">
        <div class="stat-value" style="color:var(--text)">{{ displayedPlayers.length }}</div>
        <div class="stat-label">Totale</div>
      </div>
      <div class="stat-card" v-for="[r, label, color] in roleStats" :key="r">
        <div class="stat-value" :style="{ color }">{{ countMantra(r) }}</div>
        <div class="stat-label">{{ label }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-value" style="color:#22c55e">{{ displayedPlayers.filter(p=>p.status==='AVAILABLE').length }}</div>
        <div class="stat-label">Disponibili</div>
      </div>
      <div class="stat-card">
        <div class="stat-value" style="color:#ef4444">{{ displayedPlayers.filter(p=>p.status==='SOLD').length }}</div>
        <div class="stat-label">Venduti</div>
      </div>
    </div>

    <!-- Table card -->
    <div class="card">
      <!-- Filters -->
      <div class="filters-bar">
        <div class="form-group" style="max-width:220px">
          <label class="form-label">Cerca</label>
          <input v-model="filters.search" placeholder="Nome giocatore..." @input="debouncedLoad" />
        </div>
        <div class="form-group" style="max-width:170px">
          <label class="form-label">Ruolo Mantra</label>
          <select v-model="filters.mantraRole">
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
        <div class="form-group" style="max-width:160px">
          <label class="form-label">Squadra</label>
          <select v-model="filters.team" @change="loadPlayers">
            <option value="">Tutte le squadre</option>
            <option v-for="t in teams" :key="t" :value="t">{{ t }}</option>
          </select>
        </div>
        <div class="form-group" style="max-width:140px">
          <label class="form-label">Stato</label>
          <select v-model="filters.status" @change="loadPlayers">
            <option value="">Tutti</option>
            <option value="AVAILABLE">Disponibili</option>
            <option value="SOLD">Venduti</option>
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

      <!-- Table -->
      <div class="table-wrap">
        <div v-if="loadingPlayers" class="empty-state">
          <span class="spinner" style="width:28px;height:28px;border-width:3px;color:var(--primary)"></span>
        </div>
        <div v-else-if="sortedPlayers.length === 0" class="empty-state">
          <div class="empty-state-icon">👥</div>
          <div class="empty-state-title">Nessun giocatore trovato</div>
          <div class="empty-state-desc">Carica il file Excel/CSV di fantacalcio.it oppure prova ad aggiornare dal sito.</div>
        </div>
        <table v-else>
          <thead>
            <tr>
              <th style="width:36px">#</th>
              <th class="sortable" :class="{ 'sort-active': sortKey === 'name' }" @click="setSort('name')">
                Giocatore <SortIcon field="name" :current-key="sortKey" :dir="sortDir" />
              </th>
              <th class="sortable" :class="{ 'sort-active': sortKey === 'team' }" @click="setSort('team')">
                Squadra <SortIcon field="team" :current-key="sortKey" :dir="sortDir" />
              </th>
              <th class="sortable" :class="{ 'sort-active': sortKey === 'role' }" @click="setSort('role')">
                R. <SortIcon field="role" :current-key="sortKey" :dir="sortDir" />
              </th>
              <th>Ruolo Mantra</th>
              <th class="sortable" :class="{ 'sort-active': sortKey === 'initialPrice' }" @click="setSort('initialPrice')" style="text-align:right">
                Q.I. <SortIcon field="initialPrice" :current-key="sortKey" :dir="sortDir" />
              </th>
              <th class="sortable" :class="{ 'sort-active': sortKey === 'currentPrice' }" @click="setSort('currentPrice')" style="text-align:right">
                Q.A. <SortIcon field="currentPrice" :current-key="sortKey" :dir="sortDir" />
              </th>
              <th class="sortable" :class="{ 'sort-active': sortKey === 'status' }" @click="setSort('status')">
                Stato <SortIcon field="status" :current-key="sortKey" :dir="sortDir" />
              </th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(p, i) in sortedPlayers" :key="p.id" :class="'row-' + p.role">
              <td style="color:var(--text-light);font-size:12px;font-weight:500">{{ i + 1 }}</td>
              <td>
                <span class="player-link" @click="openPlayerModal(p)" style="font-weight:600;cursor:pointer;color:var(--primary);text-decoration:underline;text-underline-offset:2px">{{ p.name }}</span>
              </td>
              <td><span style="color:var(--text-muted);font-size:13px;font-weight:500">{{ p.team || '—' }}</span></td>
              <td><span class="badge-role" :class="'badge-' + p.role">{{ p.role || '?' }}</span></td>
              <td><MantraRoles :mantra-role="p.mantraRole" /></td>
              <td style="text-align:right;color:var(--text-muted);font-size:13px">{{ p.initialPrice ?? '—' }}</td>
              <td style="text-align:right;font-weight:700;font-size:14px">{{ p.currentPrice ?? '—' }}</td>
              <td>
                <span class="badge-status" :class="p.status === 'SOLD' ? 'badge-sold' : 'badge-available'">
                  {{ p.status === 'SOLD' ? 'Venduto' : 'Disponibile' }}
                </span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Mantra legend -->
      <div class="role-legend" v-if="sortedPlayers.length">
        <span style="font-weight:600;color:var(--text-muted);margin-right:4px">Ruoli Mantra:</span>
        <span v-for="[r, label] in mantraLegend" :key="r" class="mr-badge" :class="'mr-'+r" :title="label">{{ r }}</span>
      </div>
    </div>

    <!-- Player stats modal -->
    <teleport to="body">
      <transition name="modal-fade">
        <div v-if="modalPlayer" class="modal-backdrop" @click.self="closeModal">
          <div class="modal-box">
            <!-- Header -->
            <div class="modal-header">
              <div>
                <div style="font-size:18px;font-weight:700;color:var(--text)">{{ modalPlayer.name }}</div>
                <div style="display:flex;align-items:center;gap:8px;margin-top:4px">
                  <span style="color:var(--text-muted);font-size:13px">{{ modalPlayer.team }}</span>
                  <MantraRoles :mantra-role="modalPlayer.mantraRole" />
                </div>
              </div>
              <button class="btn-close" @click="closeModal">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
              </button>
            </div>

            <!-- Body -->
            <div class="modal-body">
              <div v-if="modalLoading" style="text-align:center;padding:32px;color:var(--text-muted)">
                <span class="spinner" style="width:24px;height:24px;border-width:2px;color:var(--primary)"></span>
              </div>
              <div v-else-if="modalStats.length === 0" style="text-align:center;padding:32px;color:var(--text-muted);font-size:14px">
                Nessuna statistica disponibile per questo giocatore.<br>
                <span style="font-size:12px">Carica un file Excel dalla pagina Statistiche.</span>
              </div>
              <template v-else>
                <!-- Season selector -->
                <div style="margin-bottom:20px">
                  <div style="font-size:12px;font-weight:600;color:var(--text-muted);margin-bottom:6px;text-transform:uppercase;letter-spacing:.04em">Stagione</div>
                  <div style="display:flex;gap:8px;flex-wrap:wrap">
                    <button v-for="s in modalSeasons" :key="s"
                      class="btn btn-sm"
                      :class="modalSeason === s ? 'btn-primary' : 'btn-secondary'"
                      @click="modalSeason = s">
                      {{ s }}
                    </button>
                  </div>
                </div>

                <!-- Stats for selected season -->
                <template v-if="modalCurrentStat">
                  <div class="stats-grid">
                    <div class="stat-tile" title="Fantamedia">
                      <div class="tile-label">FM</div>
                      <div class="tile-value" style="color:var(--primary)">{{ fmStr(modalCurrentStat.fm) }}</div>
                    </div>
                    <div class="stat-tile" title="Media Voto">
                      <div class="tile-label">MV</div>
                      <div class="tile-value">{{ fmStr(modalCurrentStat.mv) }}</div>
                    </div>
                    <div class="stat-tile" title="Presenze">
                      <div class="tile-label">PV</div>
                      <div class="tile-value">{{ modalCurrentStat.pv ?? '—' }}</div>
                    </div>
                    <div class="stat-tile" title="Gol Fatti">
                      <div class="tile-label">Gol</div>
                      <div class="tile-value">{{ modalCurrentStat.gf ?? '—' }}</div>
                    </div>
                    <div class="stat-tile" title="Assist">
                      <div class="tile-label">Ass</div>
                      <div class="tile-value">{{ modalCurrentStat.ass ?? '—' }}</div>
                    </div>
                    <div class="stat-tile" title="Ammonizioni">
                      <div class="tile-label">Amm</div>
                      <div class="tile-value">{{ modalCurrentStat.amm ?? '—' }}</div>
                    </div>
                    <div class="stat-tile" title="Espulsioni">
                      <div class="tile-label">Esp</div>
                      <div class="tile-value">{{ modalCurrentStat.esp ?? '—' }}</div>
                    </div>
                    <div v-if="modalPlayer.role === 'P' || modalPlayer.role === 'D'" class="stat-tile" title="Gol Subiti">
                      <div class="tile-label">GS</div>
                      <div class="tile-value">{{ modalCurrentStat.gs ?? '—' }}</div>
                    </div>
                    <div v-if="modalPlayer.role === 'P'" class="stat-tile" title="Rigori Parati">
                      <div class="tile-label">RP</div>
                      <div class="tile-value">{{ modalCurrentStat.rp ?? '—' }}</div>
                    </div>
                    <div v-if="modalPlayer.role === 'P'" class="stat-tile" title="Rigori Calciati">
                      <div class="tile-label">RC</div>
                      <div class="tile-value">{{ modalCurrentStat.rc ?? '—' }}</div>
                    </div>
                    <div class="stat-tile" title="Autogol">
                      <div class="tile-label">AU</div>
                      <div class="tile-value">{{ modalCurrentStat.au ?? '—' }}</div>
                    </div>
                  </div>
                </template>
              </template>
            </div>
          </div>
        </div>
      </transition>
    </teleport>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { playerApi, statsApi } from '../services/api.js'
import MantraRoles from '../components/MantraRoles.vue'

// ── Inline sort-icon component ──────────────────────────────────────
const SortIcon = {
  props: ['field', 'currentKey', 'dir'],
  template: `
    <span class="sort-icon" :class="currentKey === field ? 'dir-' + dir : ''">
      <span class="asc"></span>
      <span class="desc"></span>
    </span>
  `
}

// ── State ────────────────────────────────────────────────────────────
const players = ref([])           // raw data from server
const loading = ref(false)
const loadingPlayers = ref(false)
const message = ref('')
const messageType = ref('success')
const excelInput = ref(null)
const csvInput = ref(null)
let debounceTimer = null

const filters = ref({ search: '', mantraRole: '', team: '', status: '' })
const sortKey = ref('currentPrice')
const sortDir = ref('desc')

// ── Helpers ───────────────────────────────────────────────────────────
function getMantraRoles(player) {
  return player.mantraRole
    ? player.mantraRole.split(';').map(r => r.trim()).filter(Boolean)
    : []
}

// ── Computed ─────────────────────────────────────────────────────────
const teams = computed(() => {
  const set = new Set(players.value.map(p => p.team).filter(Boolean))
  return [...set].sort()
})

/** Players after client-side Mantra role filter */
const displayedPlayers = computed(() => {
  const mr = filters.value.mantraRole
  if (!mr) return players.value
  return players.value.filter(p => getMantraRoles(p).includes(mr))
})

const ROLE_ORDER = { P: 0, D: 1, C: 2, A: 3 }
const STATUS_ORDER = { AVAILABLE: 0, SOLD: 1 }

/** displayedPlayers + sort */
const sortedPlayers = computed(() => {
  const arr = [...displayedPlayers.value]
  const k = sortKey.value
  const dir = sortDir.value === 'asc' ? 1 : -1

  arr.sort((a, b) => {
    let va = a[k], vb = b[k]
    if (k === 'role') {
      return ((ROLE_ORDER[va] ?? 9) - (ROLE_ORDER[vb] ?? 9)) * dir
    }
    if (k === 'status') {
      return ((STATUS_ORDER[va] ?? 9) - (STATUS_ORDER[vb] ?? 9)) * dir
    }
    if (typeof va === 'number' && typeof vb === 'number') {
      return ((va ?? -1) - (vb ?? -1)) * dir
    }
    return String(va ?? '').localeCompare(String(vb ?? '')) * dir
  })
  return arr
})

const roleStats = [
  ['Por', 'Portieri',     'var(--mr-Por-fg)'],
  ['Dc',  'Dif. C.',      'var(--mr-Dc-fg)'],
  ['B',   'Braccetti',    'var(--mr-B-fg)'],
  ['E',   'Esterni',      'var(--mr-E-fg)'],
  ['M',   'Mediani',      'var(--mr-M-fg)'],
  ['C',   'Centrocamp.',  'var(--mr-C-fg)'],
  ['W',   'Ali',          'var(--mr-W-fg)'],
  ['A',   'Attaccanti',   'var(--mr-A-fg)'],
]

/** Count players in displayedPlayers that have the given Mantra role */
function countMantra(role) {
  return displayedPlayers.value.filter(p => getMantraRoles(p).includes(role)).length
}

const mantraLegend = [
  ['Por','Portiere'],
  ['Dc','Dif. Centrale'], ['Dd','Terzino Destro'], ['Ds','Terzino Sinistro'], ['B','Braccetto'],
  ['E','Esterno'], ['M','Mediano'], ['C','Centrocampista'], ['T','Trequartista'], ['W','Ala'],
  ['A','Attaccante'], ['Pc','Punta Centrale']
]

// ── Sort ─────────────────────────────────────────────────────────────
function setSort(key) {
  if (sortKey.value === key) {
    sortDir.value = sortDir.value === 'asc' ? 'desc' : 'asc'
  } else {
    sortKey.value = key
    sortDir.value = ['currentPrice', 'initialPrice'].includes(key) ? 'desc' : 'asc'
  }
}

// ── Data loading ─────────────────────────────────────────────────────
function debouncedLoad() {
  clearTimeout(debounceTimer)
  debounceTimer = setTimeout(loadPlayers, 300)
}

async function loadPlayers() {
  loadingPlayers.value = true
  try {
    const res = await playerApi.getAll({
      team:   filters.value.team   || undefined,
      status: filters.value.status || undefined,
      search: filters.value.search || undefined
      // Mantra role is filtered client-side
    })
    players.value = res.data
  } finally {
    loadingPlayers.value = false
  }
}

async function refresh() {
  loading.value = true; message.value = ''
  try {
    const res = await playerApi.refreshFromSite()
    message.value = res.data.message
    messageType.value = res.data.success ? 'success' : 'error'
    if (res.data.success) await loadPlayers()
  } catch (e) {
    message.value = e.response?.data?.message || 'Errore durante il refresh.'
    messageType.value = 'error'
  } finally {
    loading.value = false
    setTimeout(() => message.value = '', 8000)
  }
}

async function uploadExcel(e) {
  const file = e.target.files[0]; if (!file) return
  loading.value = true; message.value = ''
  try {
    const res = await playerApi.uploadExcel(file)
    message.value = res.data.message
    messageType.value = res.data.success ? 'success' : 'error'
    if (res.data.success) await loadPlayers()
  } catch (e) {
    message.value = e.response?.data?.message || 'Errore nel caricamento Excel.'
    messageType.value = 'error'
  } finally {
    loading.value = false; excelInput.value.value = ''
    setTimeout(() => message.value = '', 7000)
  }
}

async function uploadCsv(e) {
  const file = e.target.files[0]; if (!file) return
  loading.value = true; message.value = ''
  try {
    const res = await playerApi.uploadCsv(file)
    message.value = res.data.message
    messageType.value = res.data.success ? 'success' : 'error'
    if (res.data.success) await loadPlayers()
  } catch (e) {
    message.value = e.response?.data?.message || 'Errore nel caricamento CSV.'
    messageType.value = 'error'
  } finally {
    loading.value = false; csvInput.value.value = ''
    setTimeout(() => message.value = '', 7000)
  }
}

function resetFilters() {
  filters.value = { search: '', mantraRole: '', team: '', status: '' }
  loadPlayers()
}

// ── Player stats modal ────────────────────────────────────────────────
const modalPlayer = ref(null)
const modalStats = ref([])       // all seasons for this player
const modalSeason = ref('')
const modalLoading = ref(false)

const modalSeasons = computed(() => modalStats.value.map(s => s.season))
const modalCurrentStat = computed(() => modalStats.value.find(s => s.season === modalSeason.value) || null)

async function openPlayerModal(player) {
  modalPlayer.value = player
  modalStats.value = []
  modalSeason.value = ''
  modalLoading.value = true
  try {
    const res = await statsApi.getByPlayer(player.id)
    modalStats.value = res.data
    if (res.data.length > 0) modalSeason.value = res.data[0].season
  } finally {
    modalLoading.value = false
  }
}

function closeModal() {
  modalPlayer.value = null
}

function fmStr(val) {
  if (val == null) return '—'
  return val.toFixed(2).replace('.', ',')
}

onMounted(loadPlayers)
</script>

<style scoped>
.fade-enter-active, .fade-leave-active { transition: opacity .3s, transform .3s; }
.fade-enter-from, .fade-leave-to { opacity: 0; transform: translateY(-6px); }

/* Modal */
.modal-backdrop {
  position: fixed; inset: 0; z-index: 1000;
  background: rgba(0,0,0,.45);
  display: flex; align-items: center; justify-content: center;
  padding: 16px;
}
.modal-box {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 12px;
  width: 100%; max-width: 540px;
  max-height: 90vh;
  display: flex; flex-direction: column;
  box-shadow: 0 24px 64px rgba(0,0,0,.25);
}
.modal-header {
  display: flex; align-items: flex-start; justify-content: space-between;
  padding: 20px 20px 16px;
  border-bottom: 1px solid var(--border);
}
.modal-body { padding: 20px; overflow-y: auto; }
.btn-close {
  background: none; border: none; cursor: pointer;
  color: var(--text-muted); padding: 4px; border-radius: 6px;
  display: flex; align-items: center;
  transition: background .15s, color .15s;
}
.btn-close:hover { background: var(--bg); color: var(--text); }

/* Stats grid */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(80px, 1fr));
  gap: 10px;
}
.stat-tile {
  background: var(--bg);
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 10px 8px;
  text-align: center;
}
.tile-label { font-size: 11px; font-weight: 600; color: var(--text-muted); text-transform: uppercase; letter-spacing: .04em; margin-bottom: 4px; }
.tile-value { font-size: 18px; font-weight: 700; color: var(--text); }

/* Modal transition */
.modal-fade-enter-active, .modal-fade-leave-active { transition: opacity .2s; }
.modal-fade-enter-from, .modal-fade-leave-to { opacity: 0; }
.modal-fade-enter-active .modal-box, .modal-fade-leave-active .modal-box { transition: transform .2s; }
.modal-fade-enter-from .modal-box, .modal-fade-leave-to .modal-box { transform: scale(.96); }
</style>
