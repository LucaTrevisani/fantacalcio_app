<template>
  <div>
    <div class="page-header">
      <div>
        <h1 class="page-title">Formazioni</h1>
        <p class="page-subtitle">Scopri quali moduli Mantra puoi schierare con la tua rosa</p>
      </div>
    </div>

    <!-- Team selector -->
    <div class="card" style="margin-bottom:24px;padding:16px 20px">
      <div style="display:flex;align-items:center;gap:16px;flex-wrap:wrap">
        <label class="form-label" style="margin:0;white-space:nowrap">Seleziona squadra:</label>
        <select v-model="selectedTeamId" style="max-width:280px;flex:1">
          <option :value="null">— Scegli una squadra —</option>
          <option v-for="t in teams" :key="t.id" :value="t.id">{{ t.name }}</option>
        </select>
        <div v-if="selectedTeam" style="display:flex;gap:8px;flex-wrap:wrap;align-items:center">
          <span style="font-size:13px;color:var(--text-muted)">{{ roster.length }} giocatori ·</span>
          <template v-for="[role, count] in rosterBreakdown" :key="role">
            <span class="mr-badge" :class="'mr-' + role">{{ role }} ×{{ count }}</span>
          </template>
        </div>
      </div>
    </div>

    <!-- Empty state -->
    <div v-if="!selectedTeamId" class="empty-state" style="padding:80px">
      <div class="empty-state-icon">📋</div>
      <div class="empty-state-title">Seleziona una squadra</div>
      <div class="empty-state-desc">Scegli la squadra per vedere i moduli schierabili.</div>
    </div>

    <template v-else>
      <!-- Summary bar -->
      <div class="stats-row" style="margin-bottom:20px">
        <div class="stat-card">
          <div class="stat-value" style="color:var(--primary)">{{ feasibleCount }}</div>
          <div class="stat-label">Schierabili</div>
        </div>
        <div class="stat-card">
          <div class="stat-value" style="color:var(--text-muted)">{{ FORMATIONS.length - feasibleCount }}</div>
          <div class="stat-label">Non schierabili</div>
        </div>
        <div class="stat-card">
          <div class="stat-value" style="color:var(--text)">{{ roster.length }}</div>
          <div class="stat-label">Giocatori in rosa</div>
        </div>
      </div>

      <!-- Formation cards -->
      <div class="formations-grid">
        <div
          v-for="formation in formationsWithResult"
          :key="formation.name"
          class="formation-card"
          :class="formation.possible ? 'formation-ok' : 'formation-ko'"
        >
          <div class="formation-card-header">
            <div class="formation-name">{{ formation.name }}</div>
            <div class="formation-badge" :class="formation.possible ? 'badge-ok' : 'badge-ko'">
              <span v-if="formation.possible">✓ Schierabile</span>
              <span v-else>✗ Incompleta</span>
            </div>
          </div>

          <!-- Field con posizionamento assoluto -->
          <div class="field">
            <!-- cerchio di centrocampo -->
            <div class="field-circle"></div>
            <!-- linea di metà campo -->
            <div class="field-midline"></div>
            <!-- area di rigore attacco -->
            <div class="field-box"></div>

            <div
              v-for="(slot, si) in formation.slots"
              :key="si"
              class="field-slot"
              :class="slotColorClass(slot.roles)"
              :style="{ left: slot.x + '%', top: slot.y + '%' }"
              :title="slot.roles.join(' / ')"
            >
              <div class="slot-label">{{ slot.label }}</div>
              <div v-if="formation.possible && formation.assignment[si] >= 0" class="slot-player">
                {{ shortName(roster[formation.assignment[si]]?.name) }}
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { teamApi } from '../services/api.js'

// ── Formazioni Mantra 2026/2027 ───────────────────────────────────────
// Ogni slot: { label, roles, x, y }
// x = posizione orizzontale % (0=sinistra, 100=destra)
// y = posizione verticale %  (0=portiere, 100=attacco)
const FORMATIONS = [
  {
    name: '3-4-3',
    slots: [
      { label: 'P',    roles: ['Por'],           x: 50, y:  6 },
      { label: 'DC',   roles: ['Dc','B'],         x: 22, y: 22 },
      { label: 'DC',   roles: ['Dc','B'],         x: 50, y: 20 },
      { label: 'DC/B', roles: ['Dc','B'],         x: 78, y: 22 },
      { label: 'E',    roles: ['E'],              x:  8, y: 46 },
      { label: 'M/C',  roles: ['M','C'],          x: 35, y: 44 },
      { label: 'C',    roles: ['C'],              x: 65, y: 44 },
      { label: 'E',    roles: ['E'],              x: 92, y: 46 },
      { label: 'W/A',  roles: ['W','A','Pc'],     x: 20, y: 70 },
      { label: 'A/PC', roles: ['A','Pc'],         x: 50, y: 72 },
      { label: 'W/A',  roles: ['W','A','Pc'],     x: 80, y: 70 },
    ]
  },
  {
    name: '3-4-1-2',
    slots: [
      { label: 'P',    roles: ['Por'],            x: 50, y:  6 },
      { label: 'DC',   roles: ['Dc','B'],          x: 22, y: 22 },
      { label: 'DC',   roles: ['Dc','B'],          x: 50, y: 20 },
      { label: 'DC/B', roles: ['Dc','B'],          x: 78, y: 22 },
      { label: 'E',    roles: ['E'],               x:  8, y: 43 },
      { label: 'M/C',  roles: ['M','C'],           x: 35, y: 41 },
      { label: 'C',    roles: ['C'],               x: 65, y: 41 },
      { label: 'E',    roles: ['E'],               x: 92, y: 43 },
      { label: 'T',    roles: ['T'],               x: 50, y: 60 },
      { label: 'A/PC', roles: ['A','Pc'],          x: 32, y: 76 },
      { label: 'A/PC', roles: ['A','Pc'],          x: 68, y: 76 },
    ]
  },
  {
    name: '3-4-2-1',
    slots: [
      { label: 'P',    roles: ['Por'],            x: 50, y:  6 },
      { label: 'DC',   roles: ['Dc','B'],          x: 22, y: 22 },
      { label: 'DC',   roles: ['Dc','B'],          x: 50, y: 20 },
      { label: 'DC/B', roles: ['Dc','B'],          x: 78, y: 22 },
      { label: 'M',    roles: ['M'],               x: 22, y: 42 },
      { label: 'M/C',  roles: ['M','C'],           x: 50, y: 40 },
      { label: 'E',    roles: ['E'],               x: 78, y: 42 },
      { label: 'E/W',  roles: ['E','W'],           x: 20, y: 60 },
      { label: 'T',    roles: ['T'],               x: 50, y: 59 },
      { label: 'T/A',  roles: ['T','A','Pc'],      x: 78, y: 62 },
      { label: 'A/PC', roles: ['A','Pc'],          x: 50, y: 80 },
    ]
  },
  {
    name: '3-5-2',
    slots: [
      { label: 'P',    roles: ['Por'],            x: 50, y:  6 },
      { label: 'DC',   roles: ['Dc','B'],          x: 22, y: 22 },
      { label: 'DC',   roles: ['Dc','B'],          x: 50, y: 20 },
      { label: 'DC/B', roles: ['Dc','B'],          x: 78, y: 22 },
      { label: 'M',    roles: ['M'],               x: 25, y: 41 },
      { label: 'M/C',  roles: ['M','C'],           x: 50, y: 39 },
      { label: 'E',    roles: ['E'],               x: 78, y: 41 },
      { label: 'E/W',  roles: ['E','W'],           x: 14, y: 58 },
      { label: 'C',    roles: ['C'],               x: 65, y: 57 },
      { label: 'A/PC', roles: ['A','Pc'],          x: 32, y: 76 },
      { label: 'A/PC', roles: ['A','Pc'],          x: 68, y: 76 },
    ]
  },
  {
    name: '3-5-1-1',
    slots: [
      { label: 'P',    roles: ['Por'],            x: 50, y:  6 },
      { label: 'DC',   roles: ['Dc','B'],          x: 22, y: 22 },
      { label: 'DC',   roles: ['Dc','B'],          x: 50, y: 20 },
      { label: 'DC/B', roles: ['Dc','B'],          x: 78, y: 22 },
      { label: 'M',    roles: ['M'],               x: 25, y: 41 },
      { label: 'C',    roles: ['C'],               x: 50, y: 39 },
      { label: 'M',    roles: ['M'],               x: 75, y: 41 },
      { label: 'E/W',  roles: ['E','W'],           x: 12, y: 58 },
      { label: 'E/W',  roles: ['E','W'],           x: 88, y: 58 },
      { label: 'T/A',  roles: ['T','A','Pc'],      x: 50, y: 69 },
      { label: 'A/PC', roles: ['A','Pc'],          x: 50, y: 84 },
    ]
  },
  {
    name: '4-3-3',
    slots: [
      { label: 'P',    roles: ['Por'],            x: 50, y:  6 },
      { label: 'DD',   roles: ['Dd'],              x: 10, y: 22 },
      { label: 'DC',   roles: ['Dc','B'],          x: 36, y: 20 },
      { label: 'DC',   roles: ['Dc','B'],          x: 64, y: 20 },
      { label: 'DS',   roles: ['Ds'],              x: 90, y: 22 },
      { label: 'M/C',  roles: ['M','C'],           x: 28, y: 46 },
      { label: 'M',    roles: ['M'],               x: 50, y: 43 },
      { label: 'C',    roles: ['C'],               x: 72, y: 46 },
      { label: 'W/A',  roles: ['W','A','Pc'],      x: 15, y: 70 },
      { label: 'A/PC', roles: ['A','Pc'],          x: 50, y: 72 },
      { label: 'W/A',  roles: ['W','A','Pc'],      x: 85, y: 70 },
    ]
  },
  {
    name: '4-3-1-2',
    slots: [
      { label: 'P',      roles: ['Por'],           x: 50, y:  6 },
      { label: 'DD',     roles: ['Dd'],             x: 10, y: 22 },
      { label: 'DC',     roles: ['Dc','B'],         x: 36, y: 20 },
      { label: 'DC',     roles: ['Dc','B'],         x: 64, y: 20 },
      { label: 'DS',     roles: ['Ds'],             x: 90, y: 22 },
      { label: 'M/C',    roles: ['M','C'],          x: 28, y: 43 },
      { label: 'M',      roles: ['M'],              x: 50, y: 41 },
      { label: 'C',      roles: ['C'],              x: 72, y: 43 },
      { label: 'T',      roles: ['T'],              x: 50, y: 60 },
      { label: 'T/A/PC', roles: ['T','A','Pc'],    x: 30, y: 77 },
      { label: 'A/PC',   roles: ['A','Pc'],         x: 70, y: 77 },
    ]
  },
  {
    name: '4-4-2',
    slots: [
      { label: 'P',    roles: ['Por'],            x: 50, y:  6 },
      { label: 'DD',   roles: ['Dd'],              x: 10, y: 22 },
      { label: 'DC',   roles: ['Dc','B'],          x: 36, y: 20 },
      { label: 'DC',   roles: ['Dc','B'],          x: 64, y: 20 },
      { label: 'DS',   roles: ['Ds'],              x: 90, y: 22 },
      { label: 'M/C',  roles: ['M','C'],           x: 38, y: 41 },
      { label: 'E',    roles: ['E'],               x: 78, y: 42 },
      { label: 'E/W',  roles: ['E','W'],           x: 15, y: 58 },
      { label: 'C',    roles: ['C'],               x: 60, y: 57 },
      { label: 'A/PC', roles: ['A','Pc'],          x: 32, y: 76 },
      { label: 'A/PC', roles: ['A','Pc'],          x: 68, y: 76 },
    ]
  },
  {
    name: '4-1-4-1',
    slots: [
      { label: 'P',    roles: ['Por'],            x: 50, y:  6 },
      { label: 'DD',   roles: ['Dd'],              x: 10, y: 22 },
      { label: 'DC',   roles: ['Dc','B'],          x: 36, y: 20 },
      { label: 'DC',   roles: ['Dc','B'],          x: 64, y: 20 },
      { label: 'DS',   roles: ['Ds'],              x: 90, y: 22 },
      { label: 'M',    roles: ['M'],               x: 50, y: 38 },
      { label: 'C/T',  roles: ['C','T'],           x: 14, y: 56 },
      { label: 'T',    roles: ['T'],               x: 40, y: 53 },
      { label: 'E/W',  roles: ['E','W'],           x: 65, y: 53 },
      { label: 'W',    roles: ['W'],               x: 88, y: 56 },
      { label: 'A/PC', roles: ['A','Pc'],          x: 50, y: 78 },
    ]
  },
  {
    name: '4-4-1-1',
    slots: [
      { label: 'P',    roles: ['Por'],            x: 50, y:  6 },
      { label: 'DD',   roles: ['Dd'],              x: 10, y: 22 },
      { label: 'DC',   roles: ['Dc','B'],          x: 36, y: 20 },
      { label: 'DC',   roles: ['Dc','B'],          x: 64, y: 20 },
      { label: 'DS',   roles: ['Ds'],              x: 90, y: 22 },
      { label: 'M',    roles: ['M'],               x: 35, y: 41 },
      { label: 'C',    roles: ['C'],               x: 65, y: 41 },
      { label: 'E/W',  roles: ['E','W'],           x: 10, y: 57 },
      { label: 'E/W',  roles: ['E','W'],           x: 90, y: 57 },
      { label: 'T/A',  roles: ['T','A','Pc'],      x: 50, y: 68 },
      { label: 'A/PC', roles: ['A','Pc'],          x: 50, y: 83 },
    ]
  },
  {
    name: '4-2-3-1',
    slots: [
      { label: 'P',    roles: ['Por'],            x: 50, y:  6 },
      { label: 'DD',   roles: ['Dd'],              x: 10, y: 22 },
      { label: 'DC',   roles: ['Dc','B'],          x: 36, y: 20 },
      { label: 'DC',   roles: ['Dc','B'],          x: 64, y: 20 },
      { label: 'DS',   roles: ['Ds'],              x: 90, y: 22 },
      { label: 'M',    roles: ['M'],               x: 35, y: 41 },
      { label: 'M/C',  roles: ['M','C'],           x: 65, y: 41 },
      { label: 'W/T',  roles: ['W','T'],           x: 14, y: 58 },
      { label: 'T',    roles: ['T'],               x: 50, y: 57 },
      { label: 'W/A',  roles: ['W','A','Pc'],      x: 86, y: 58 },
      { label: 'A/PC', roles: ['A','Pc'],          x: 50, y: 78 },
    ]
  },
]

// ── Stato ─────────────────────────────────────────────────────────────
const teams = ref([])
const selectedTeamId = ref(null)

const selectedTeam = computed(() => teams.value.find(t => t.id === selectedTeamId.value))

const roster = computed(() => {
  if (!selectedTeam.value) return []
  return (selectedTeam.value.auctionEntries || []).map(e => ({
    name: e.playerName,
    mantraRoles: (e.playerMantraRole || '').split(';').map(r => r.trim()).filter(Boolean)
  }))
})

const rosterBreakdown = computed(() => {
  const counts = {}
  for (const p of roster.value)
    for (const r of p.mantraRoles) counts[r] = (counts[r] || 0) + 1
  const ORDER = ['Por','Pc','Dc','Dd','Ds','B','E','M','C','T','W','A']
  const sorted = []
  for (const r of ORDER) if (counts[r]) sorted.push([r, counts[r]])
  for (const [r, c] of Object.entries(counts)) if (!ORDER.includes(r)) sorted.push([r, c])
  return sorted
})

// ── Backtracking assignment ───────────────────────────────────────────
function tryAssign(slots, players) {
  const assignment = new Array(slots.length).fill(-1)
  const used = new Set()
  function backtrack(i) {
    if (i === slots.length) return true
    for (let j = 0; j < players.length; j++) {
      if (used.has(j)) continue
      if (slots[i].roles.some(r => players[j].mantraRoles.includes(r))) {
        used.add(j); assignment[i] = j
        if (backtrack(i + 1)) return true
        used.delete(j); assignment[i] = -1
      }
    }
    return false
  }
  return { possible: backtrack(0), assignment }
}

const formationsWithResult = computed(() => {
  const players = roster.value
  return FORMATIONS.map(f => {
    const { possible, assignment } = tryAssign(f.slots, players)
    return { ...f, possible, assignment }
  })
})

const feasibleCount = computed(() => formationsWithResult.value.filter(f => f.possible).length)

// ── Helpers ───────────────────────────────────────────────────────────
function slotColorClass(roles) {
  const first = roles[0]
  if (first === 'Por') return 'slot-por'
  if (['Dc','Dd','Ds','B'].includes(first)) return 'slot-def'
  if (['E','M','C'].includes(first)) return 'slot-mid'
  if (['T','W'].includes(first)) return 'slot-trq'
  if (['A','Pc'].includes(first)) return 'slot-att'
  return 'slot-unknown'
}

function shortName(name) {
  if (!name) return ''
  const parts = name.trim().split(' ')
  return parts[parts.length - 1]
}

onMounted(async () => { teams.value = (await teamApi.getAll()).data })
</script>

<style scoped>
.formations-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 20px;
}

/* ── Formation card ── */
.formation-card {
  border-radius: var(--radius);
  border: 2px solid var(--border);
  background: var(--surface);
  overflow: clip;
  display: flex;
  flex-direction: column;
  transition: box-shadow .2s, border-color .2s;
}
.formation-card:hover { box-shadow: 0 4px 20px rgba(0,0,0,.1); }
.formation-ok { border-color: #22c55e; }
.formation-ko { border-color: var(--border); opacity: .72; }

.formation-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 11px 14px;
  background: var(--surface2);
  border-bottom: 1px solid var(--border);
}
.formation-name { font-size: 17px; font-weight: 800; letter-spacing: -.3px; }
.formation-badge { font-size: 11px; font-weight: 700; padding: 3px 9px; border-radius: 20px; }
.badge-ok { background: #dcfce7; color: #14532d; }
.badge-ko { background: #f1f5f9; color: #64748b; }

/* ── Field ── */
.field {
  position: relative;
  /* aspect-ratio 3:4 (campo verticale) */
  aspect-ratio: 3 / 4;
  background: linear-gradient(180deg, #2a6b2a 0%, #357a35 40%, #357a35 60%, #2a6b2a 100%);
  overflow: clip;
}

/* decorazioni campo */
.field-midline {
  position: absolute;
  left: 8%; right: 8%;
  top: 50%;
  height: 1px;
  background: rgba(255,255,255,.28);
}
.field-circle {
  position: absolute;
  left: 50%; top: 50%;
  transform: translate(-50%, -50%);
  width: 28%; aspect-ratio: 1;
  border: 1px solid rgba(255,255,255,.28);
  border-radius: 50%;
}
.field-box {
  position: absolute;
  left: 25%; right: 25%;
  bottom: 3%; height: 12%;
  border: 1px solid rgba(255,255,255,.20);
}

/* ── Slot ── */
.field-slot {
  position: absolute;
  transform: translate(-50%, -50%);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  padding: 4px 7px;
  border-radius: 7px;
  min-width: 40px;
  border: 1.5px solid rgba(255,255,255,.3);
  background: rgba(255,255,255,.12);
  backdrop-filter: blur(2px);
  z-index: 2;
  cursor: default;
  transition: transform .15s;
}
.field-slot:hover { transform: translate(-50%, -50%) scale(1.08); }

.slot-label {
  font-size: 10px;
  font-weight: 800;
  letter-spacing: .3px;
  color: #fff;
  text-shadow: 0 1px 3px rgba(0,0,0,.6);
  white-space: nowrap;
  line-height: 1.2;
}
.slot-player {
  font-size: 8.5px;
  font-weight: 600;
  color: rgba(255,255,255,.92);
  text-shadow: 0 1px 2px rgba(0,0,0,.7);
  max-width: 54px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  line-height: 1.2;
}

/* colori slot per reparto */
.slot-por     { background: rgba(245,158,11,.40); border-color: #f59e0b; }
.slot-def     { background: rgba(34,197,94,.32);  border-color: #22c55e; }
.slot-mid     { background: rgba(59,130,246,.38); border-color: #3b82f6; }
.slot-trq     { background: rgba(168,85,247,.38); border-color: #a855f7; }
.slot-att     { background: rgba(239,68,68,.38);  border-color: #ef4444; }
.slot-unknown { background: rgba(255,255,255,.18); }
</style>
