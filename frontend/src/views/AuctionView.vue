<template>
  <div>
    <div class="page-header">
      <div>
        <h1 class="page-title">Asta</h1>
        <p class="page-subtitle">Gestisci l'assegnazione dei giocatori alle squadre</p>
      </div>
      <button class="btn btn-primary" @click="showAssignModal = true" :disabled="teams.length === 0">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
          <line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/>
        </svg>
        Assegna giocatore
      </button>
    </div>

    <!-- Alert -->
    <transition name="fade">
      <div v-if="alertMsg" class="alert" :class="alertType === 'error' ? 'alert-error' : 'alert-success'" style="margin-bottom:20px">
        {{ alertMsg }}
      </div>
    </transition>

    <!-- Empty state -->
    <div v-if="teams.length === 0" class="empty-state" style="padding:80px">
      <div class="empty-state-icon">🏆</div>
      <div class="empty-state-title">Nessuna squadra configurata</div>
      <div class="empty-state-desc">Vai alla <router-link to="/config" style="color:var(--primary);font-weight:600">Configurazione</router-link> per aggiungere squadre e partecipanti.</div>
    </div>

    <template v-else>
      <!-- Summary cards -->
      <div class="section">
        <div class="section-title">Riepilogo crediti</div>
        <div class="grid-3">
          <div v-for="team in teams" :key="team.id" class="team-summary-card">
            <div class="team-card-header">
              <div class="team-card-name">{{ team.name }}</div>
              <div class="team-card-participants">
                {{ team.participants?.map(p => p.name).join(' · ') || 'Nessun partecipante' }}
              </div>
            </div>
            <div class="team-card-body">
              <!-- Credits -->
              <div class="team-card-credits" style="margin-bottom:10px">
                <div>
                  <div class="credit-number" :style="{ color: creditColor(team) }">
                    {{ team.remainingCredits }}
                  </div>
                  <div class="credit-label">crediti rimasti</div>
                </div>
                <div style="text-align:right">
                  <div style="font-size:13px;font-weight:700;color:var(--text-muted)">
                    {{ team.spentCredits }} / {{ team.maxCredits }}
                  </div>
                  <div class="credit-label">spesi / totali</div>
                </div>
              </div>
              <div class="credits-bar">
                <div class="credits-bar-fill" :class="creditClass(team)"
                  :style="{ width: creditPct(team) + '%' }"></div>
              </div>
              <div style="display:flex;justify-content:space-between;margin-top:8px;font-size:12px;color:var(--text-muted)">
                <span>{{ team.auctionEntries?.length || 0 }} giocatori</span>
                <span>{{ creditPct(team) }}% usato</span>
              </div>

              <!-- Mantra role breakdown -->
              <div v-if="team.auctionEntries?.length" style="margin-top:12px;padding-top:10px;border-top:1px solid var(--border)">
                <div style="font-size:10.5px;font-weight:700;text-transform:uppercase;letter-spacing:.5px;color:var(--text-muted);margin-bottom:7px">
                  Ruoli Mantra
                </div>
                <div style="display:flex;flex-wrap:wrap;gap:5px">
                  <template v-for="[role, count] in mantraBreakdownSorted(team.auctionEntries)" :key="role">
                    <div class="mantra-count-badge" :class="'mr-badge mr-' + role">
                      <span>{{ role }}</span>
                      <span class="mantra-count">{{ count }}</span>
                    </div>
                  </template>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Per-team player detail -->
      <div class="section">
        <div class="section-title">Rosa per squadra</div>
        <div style="display:flex;flex-direction:column;gap:16px">
          <div v-for="team in teams" :key="'detail-'+team.id" class="card">
            <div class="card-header">
              <div style="flex:1">
                <div style="display:flex;align-items:center;gap:12px;flex-wrap:wrap">
                  <div class="card-title">{{ team.name }}</div>
                  <div class="card-subtitle">
                    {{ team.auctionEntries?.length || 0 }} giocatori ·
                    {{ team.spentCredits }} cr. spesi ·
                    <span :style="{ color: creditColor(team), fontWeight: 600 }">{{ team.remainingCredits }} rimasti</span>
                  </div>
                </div>
                <!-- Mantra breakdown inline -->
                <div v-if="team.auctionEntries?.length" style="display:flex;flex-wrap:wrap;gap:4px;margin-top:8px">
                  <template v-for="[role, count] in mantraBreakdownSorted(team.auctionEntries)" :key="role">
                    <div class="mantra-count-badge" :class="'mr-badge mr-' + role">
                      <span>{{ role }}</span>
                      <span class="mantra-count">{{ count }}</span>
                    </div>
                  </template>
                </div>
              </div>
              <button class="btn btn-secondary btn-sm" @click="selectTeamAndOpen(team)">
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                  <line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/>
                </svg>
                Aggiungi
              </button>
            </div>

            <div v-if="!team.auctionEntries?.length" style="padding:24px 20px;color:var(--text-light);font-size:13px;text-align:center">
              Nessun giocatore assegnato
            </div>
            <div v-else class="table-wrap" style="margin-top:12px">
              <table>
                <thead>
                  <tr>
                    <th>Giocatore</th>
                    <th>Sq. reale</th>
                    <th>R.</th>
                    <th>Ruolo Mantra</th>
                    <th style="text-align:right">Q.A.</th>
                    <th style="text-align:right">Prezzo asta</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="entry in sortedEntries(team.auctionEntries)" :key="entry.id"
                    :class="'row-' + entry.playerRole">
                    <td style="font-weight:600">{{ entry.playerName }}</td>
                    <td style="color:var(--text-muted);font-size:13px">{{ entry.playerTeam }}</td>
                    <td>
                      <span class="badge-role" :class="'badge-' + entry.playerRole">{{ entry.playerRole || '?' }}</span>
                    </td>
                    <td>
                      <MantraRoles :mantra-role="entry.playerMantraRole" />
                    </td>
                    <td style="text-align:right;color:var(--text-muted);font-size:13px">{{ entry.playerCurrentPrice ?? '—' }}</td>
                    <td style="text-align:right">
                      <span style="font-weight:800;font-size:14px;color:var(--primary)">{{ entry.price }}</span>
                      <span style="font-size:11px;color:var(--text-muted)"> cr.</span>
                    </td>
                    <td style="text-align:right">
                      <button class="btn btn-danger btn-xs" @click="removeEntry(entry.id)" title="Rimuovi">
                        <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                          <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
                        </svg>
                      </button>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- ── MODAL assegnazione ──────────────────────────────── -->
    <div v-if="showAssignModal" class="modal-overlay" @click.self="closeModal">
      <div class="modal">
        <div class="modal-title">🔨 Assegna giocatore</div>

        <div class="form-group" style="margin-bottom:14px">
          <label class="form-label">Squadra</label>
          <select v-model="form.fantaTeamId">
            <option :value="null">Seleziona squadra...</option>
            <option v-for="t in teams" :key="t.id" :value="t.id">
              {{ t.name }} — {{ t.remainingCredits }} cr. rimasti
            </option>
          </select>
        </div>

        <div class="form-group" style="margin-bottom:4px">
          <label class="form-label">Cerca giocatore disponibile</label>
          <input v-model="playerSearch" placeholder="Inizia a scrivere il nome..." @input="searchPlayers" autofocus />
        </div>

        <div v-if="filteredPlayers.length" class="player-search-list" style="margin-bottom:14px">
          <div
            v-for="p in filteredPlayers" :key="p.id"
            class="player-search-item"
            :class="{ selected: form.playerId === p.id }"
            @click="selectPlayer(p)">
            <div>
              <span style="font-weight:600;font-size:13.5px">{{ p.name }}</span>
              <span style="color:var(--text-muted);font-size:12px;margin-left:8px">{{ p.team }}</span>
            </div>
            <div style="display:flex;gap:6px;align-items:center;flex-shrink:0;flex-wrap:wrap;justify-content:flex-end">
              <span class="badge-role" :class="'badge-' + p.role">{{ p.role }}</span>
              <MantraRoles :mantra-role="p.mantraRole" />
              <span style="font-size:12px;color:var(--text-muted);margin-left:2px">{{ p.currentPrice ?? '?' }} cr.</span>
            </div>
          </div>
        </div>
        <div v-else-if="playerSearch.length >= 2" style="color:var(--text-muted);font-size:13px;margin-bottom:14px;padding:8px;text-align:center">
          Nessun giocatore disponibile trovato
        </div>

        <div v-if="form.playerId" class="alert alert-success" style="margin-bottom:14px">
          ✓ Selezionato: <strong>{{ selectedPlayerName }}</strong>
        </div>

        <div class="form-group" style="margin-bottom:8px">
          <label class="form-label">Prezzo pagato (crediti)</label>
          <input v-model.number="form.price" type="number" min="1" placeholder="Es: 45" />
        </div>

        <div v-if="formError" class="alert alert-error" style="margin-top:8px">{{ formError }}</div>

        <div class="modal-actions">
          <button class="btn btn-ghost" @click="closeModal">Annulla</button>
          <button class="btn btn-primary" @click="assignPlayer"
            :disabled="!form.playerId || !form.fantaTeamId || !form.price || saving">
            <span v-if="saving" class="spinner"></span>
            <svg v-else width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg>
            Assegna
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { teamApi, playerApi, auctionApi } from '../services/api.js'
import MantraRoles from '../components/MantraRoles.vue'

const teams = ref([])
const availablePlayers = ref([])
const showAssignModal = ref(false)
const playerSearch = ref('')
const filteredPlayers = ref([])
const alertMsg = ref('')
const alertType = ref('success')
const saving = ref(false)
const formError = ref('')
const form = ref({ playerId: null, fantaTeamId: null, price: null })
const selectedPlayerName = ref('')

// ── Mantra breakdown ─────────────────────────────────────────────────
const MANTRA_ORDER = ['Por', 'Pc', 'Dc', 'Dd', 'Ds', 'B', 'E', 'M', 'C', 'T', 'W', 'A']

/**
 * Returns [[role, count], ...] sorted by canonical Mantra order.
 * A player with "Dc;Dd" contributes +1 to Dc AND +1 to Dd.
 */
function mantraBreakdown(entries) {
  const counts = {}
  for (const entry of (entries || [])) {
    if (!entry.playerMantraRole) continue
    for (const r of entry.playerMantraRole.split(';').map(s => s.trim()).filter(Boolean)) {
      counts[r] = (counts[r] || 0) + 1
    }
  }
  return counts
}

function mantraBreakdownSorted(entries) {
  const counts = mantraBreakdown(entries)
  const sorted = []
  for (const r of MANTRA_ORDER) {
    if (counts[r]) sorted.push([r, counts[r]])
  }
  // append any unknown roles
  for (const [r, c] of Object.entries(counts)) {
    if (!MANTRA_ORDER.includes(r)) sorted.push([r, c])
  }
  return sorted
}

// ── Sorting entries by role ───────────────────────────────────────────
const ROLE_ORDER = { P: 0, D: 1, C: 2, A: 3 }

function sortedEntries(entries) {
  return [...(entries || [])].sort((a, b) =>
    (ROLE_ORDER[a.playerRole] ?? 9) - (ROLE_ORDER[b.playerRole] ?? 9) ||
    a.playerName.localeCompare(b.playerName)
  )
}

// ── Credit helpers ────────────────────────────────────────────────────
function creditPct(team) {
  return team.maxCredits ? Math.min(100, Math.round((team.spentCredits / team.maxCredits) * 100)) : 0
}
function creditClass(team) {
  const p = creditPct(team)
  return p < 60 ? 'credits-ok' : p < 85 ? 'credits-warn' : 'credits-danger'
}
function creditColor(team) {
  const p = creditPct(team)
  return p < 60 ? 'var(--primary)' : p < 85 ? '#d97706' : '#dc2626'
}

// ── Data loading ──────────────────────────────────────────────────────
async function loadTeams() { teams.value = (await teamApi.getAll()).data }
async function loadAvailablePlayers() {
  availablePlayers.value = (await playerApi.getAll({ status: 'AVAILABLE' })).data
}

// ── Player search in modal ────────────────────────────────────────────
function searchPlayers() {
  if (playerSearch.value.length < 2) { filteredPlayers.value = []; return }
  const q = playerSearch.value.toLowerCase()
  filteredPlayers.value = availablePlayers.value
    .filter(p => p.name.toLowerCase().includes(q))
    .slice(0, 15)
}

function selectPlayer(p) {
  form.value.playerId = p.id
  selectedPlayerName.value = `${p.name} (${p.team}, ${p.role})`
  filteredPlayers.value = []
  playerSearch.value = ''
}

function selectTeamAndOpen(team) {
  form.value.fantaTeamId = team.id
  showAssignModal.value = true
}

function closeModal() {
  showAssignModal.value = false
  form.value = { playerId: null, fantaTeamId: null, price: null }
  playerSearch.value = ''; filteredPlayers.value = []
  selectedPlayerName.value = ''; formError.value = ''
}

// ── Auction actions ───────────────────────────────────────────────────
async function assignPlayer() {
  formError.value = ''
  if (!form.value.playerId || !form.value.fantaTeamId || !form.value.price) {
    formError.value = 'Compila tutti i campi.'; return
  }
  saving.value = true
  try {
    await auctionApi.assign({
      playerId: form.value.playerId,
      fantaTeamId: form.value.fantaTeamId,
      price: form.value.price
    })
    showAlert('Giocatore assegnato con successo!', 'success')
    closeModal()
    await Promise.all([loadTeams(), loadAvailablePlayers()])
  } catch (e) {
    formError.value = e.response?.data?.message || 'Errore durante l\'assegnazione.'
  } finally {
    saving.value = false
  }
}

async function removeEntry(entryId) {
  if (!confirm('Rimuovere l\'assegnazione? Il giocatore tornerà disponibile.')) return
  try {
    await auctionApi.remove(entryId)
    showAlert('Assegnazione rimossa.', 'success')
    await Promise.all([loadTeams(), loadAvailablePlayers()])
  } catch (e) {
    showAlert(e.response?.data?.message || 'Errore nella rimozione.', 'error')
  }
}

function showAlert(msg, type) {
  alertMsg.value = msg; alertType.value = type
  setTimeout(() => alertMsg.value = '', 4000)
}

onMounted(() => Promise.all([loadTeams(), loadAvailablePlayers()]))
</script>

<style scoped>
.fade-enter-active, .fade-leave-active { transition: opacity .3s, transform .3s; }
.fade-enter-from, .fade-leave-to { opacity: 0; transform: translateY(-6px); }
</style>
