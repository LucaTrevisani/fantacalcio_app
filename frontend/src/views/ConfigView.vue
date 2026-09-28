<template>
  <div>
    <div class="page-header">
      <div>
        <h1 class="page-title">Configurazione</h1>
        <p class="page-subtitle">Gestisci le squadre e i partecipanti all'asta</p>
      </div>
    </div>

    <div class="grid-2" style="align-items:start">

      <!-- SQUADRE -->
      <div class="card">
        <div class="card-header" style="padding-bottom:16px">
          <div>
            <div class="card-title">🏆 Squadre Fanta</div>
            <div class="card-subtitle">{{ teams.length }} squadre configurate</div>
          </div>
        </div>
        <div class="divider" style="margin:0 0 16px"></div>
        <div class="card-body" style="padding-top:0">
          <!-- Form -->
          <div style="background:var(--surface2);border:1px solid var(--border);border-radius:var(--radius);padding:16px;margin-bottom:16px">
            <div class="form-row" style="margin-bottom:12px">
              <div class="form-group">
                <label class="form-label">Nome squadra</label>
                <input v-model="newTeam.name" placeholder="Es: La Banda del Buco" @keyup.enter="saveTeam" />
              </div>
              <div class="form-group" style="max-width:130px">
                <label class="form-label">Crediti max</label>
                <input v-model.number="newTeam.maxCredits" type="number" min="100" max="9999" />
              </div>
            </div>
            <div style="display:flex;gap:8px">
              <button class="btn btn-primary" @click="saveTeam" :disabled="!newTeam.name.trim()">
                <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                  <line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/>
                </svg>
                {{ editingTeam ? 'Salva modifiche' : 'Aggiungi squadra' }}
              </button>
              <button v-if="editingTeam" class="btn btn-ghost btn-sm" @click="cancelTeamEdit">Annulla</button>
            </div>
          </div>

          <!-- List -->
          <div v-if="teams.length === 0" class="empty-state" style="padding:32px">
            <div class="empty-state-icon" style="font-size:36px">🏆</div>
            <div class="empty-state-title">Nessuna squadra</div>
          </div>
          <div v-else style="display:flex;flex-direction:column;gap:8px">
            <div v-for="t in teams" :key="t.id"
              style="display:flex;align-items:center;justify-content:space-between;padding:12px 14px;background:var(--surface2);border:1px solid var(--border);border-radius:var(--radius-sm);gap:12px">
              <div style="flex:1">
                <div style="font-weight:700;font-size:14px">{{ t.name }}</div>
                <div style="font-size:12px;color:var(--text-muted);margin-top:2px">
                  {{ t.maxCredits }} crediti max ·
                  <span :style="{ color: t.participants?.length ? 'var(--primary)' : 'var(--text-light)' }">
                    {{ t.participants?.length || 0 }} partecipanti
                  </span>
                </div>
              </div>
              <div style="display:flex;gap:6px;flex-shrink:0">
                <button class="btn btn-secondary btn-sm" @click="editTeam(t)" title="Modifica">
                  <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 013 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
                </button>
                <button class="btn btn-danger btn-sm" @click="deleteTeam(t.id)" title="Elimina">
                  <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="3 6 5 6 21 6"/><path d="M19 6l-1 14a2 2 0 01-2 2H8a2 2 0 01-2-2L5 6"/><path d="M10 11v6M14 11v6"/></svg>
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- PARTECIPANTI -->
      <div class="card">
        <div class="card-header" style="padding-bottom:16px">
          <div>
            <div class="card-title">👥 Partecipanti</div>
            <div class="card-subtitle">{{ participants.length }} persone registrate</div>
          </div>
        </div>
        <div class="divider" style="margin:0 0 16px"></div>
        <div class="card-body" style="padding-top:0">
          <!-- Form -->
          <div style="background:var(--surface2);border:1px solid var(--border);border-radius:var(--radius);padding:16px;margin-bottom:16px">
            <div class="form-row" style="margin-bottom:12px">
              <div class="form-group">
                <label class="form-label">Nome</label>
                <input v-model="newParticipant.name" placeholder="Es: Mario Rossi" @keyup.enter="saveParticipant" />
              </div>
              <div class="form-group">
                <label class="form-label">Squadra (opzionale)</label>
                <select v-model="newParticipant.fantaTeamId">
                  <option :value="null">— Non assegnato —</option>
                  <option v-for="t in teams" :key="t.id" :value="t.id">{{ t.name }}</option>
                </select>
              </div>
            </div>
            <div style="display:flex;gap:8px">
              <button class="btn btn-primary" @click="saveParticipant" :disabled="!newParticipant.name.trim()">
                <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                  <line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/>
                </svg>
                {{ editingParticipant ? 'Salva modifiche' : 'Aggiungi partecipante' }}
              </button>
              <button v-if="editingParticipant" class="btn btn-ghost btn-sm" @click="cancelParticipantEdit">Annulla</button>
            </div>
          </div>

          <!-- List -->
          <div v-if="participants.length === 0" class="empty-state" style="padding:32px">
            <div class="empty-state-icon" style="font-size:36px">👤</div>
            <div class="empty-state-title">Nessun partecipante</div>
          </div>
          <div v-else style="display:flex;flex-direction:column;gap:8px">
            <div v-for="p in participants" :key="p.id"
              style="display:flex;align-items:center;justify-content:space-between;padding:12px 14px;background:var(--surface2);border:1px solid var(--border);border-radius:var(--radius-sm);gap:12px">
              <div style="flex:1">
                <div style="font-weight:700;font-size:14px">{{ p.name }}</div>
                <div style="margin-top:4px">
                  <span v-if="p.fantaTeamName"
                    style="display:inline-flex;align-items:center;gap:4px;font-size:11.5px;background:var(--primary-bg);color:var(--primary);border:1px solid #bbf7d0;padding:2px 8px;border-radius:20px;font-weight:600">
                    🏆 {{ p.fantaTeamName }}
                  </span>
                  <span v-else style="font-size:12px;color:var(--text-light)">Non assegnato</span>
                </div>
              </div>
              <div style="display:flex;gap:6px;flex-shrink:0">
                <button class="btn btn-secondary btn-sm" @click="editParticipant(p)" title="Modifica">
                  <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 013 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
                </button>
                <button class="btn btn-danger btn-sm" @click="deleteParticipant(p.id)" title="Elimina">
                  <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="3 6 5 6 21 6"/><path d="M19 6l-1 14a2 2 0 01-2 2H8a2 2 0 01-2-2L5 6"/><path d="M10 11v6M14 11v6"/></svg>
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { teamApi, participantApi } from '../services/api.js'

const teams = ref([])
const participants = ref([])
const newTeam = ref({ name: '', maxCredits: 500 })
const editingTeam = ref(null)
const newParticipant = ref({ name: '', fantaTeamId: null })
const editingParticipant = ref(null)

async function loadTeams() { teams.value = (await teamApi.getAll()).data }
async function loadParticipants() { participants.value = (await participantApi.getAll()).data }

async function saveTeam() {
  if (!newTeam.value.name.trim()) return
  const payload = { name: newTeam.value.name.trim(), maxCredits: newTeam.value.maxCredits }
  if (editingTeam.value) { await teamApi.update(editingTeam.value, payload); editingTeam.value = null }
  else await teamApi.create(payload)
  newTeam.value = { name: '', maxCredits: 500 }
  await loadTeams()
}
function editTeam(t) { editingTeam.value = t.id; newTeam.value = { name: t.name, maxCredits: t.maxCredits } }
function cancelTeamEdit() { editingTeam.value = null; newTeam.value = { name: '', maxCredits: 500 } }
async function deleteTeam(id) {
  if (!confirm('Eliminare questa squadra? Verranno rimossi anche i record d\'asta associati.')) return
  await teamApi.delete(id); await Promise.all([loadTeams(), loadParticipants()])
}

async function saveParticipant() {
  if (!newParticipant.value.name.trim()) return
  const payload = { name: newParticipant.value.name.trim(), fantaTeamId: newParticipant.value.fantaTeamId }
  if (editingParticipant.value) { await participantApi.update(editingParticipant.value, payload); editingParticipant.value = null }
  else await participantApi.create(payload)
  newParticipant.value = { name: '', fantaTeamId: null }
  await loadParticipants()
}
function editParticipant(p) { editingParticipant.value = p.id; newParticipant.value = { name: p.name, fantaTeamId: p.fantaTeamId } }
function cancelParticipantEdit() { editingParticipant.value = null; newParticipant.value = { name: '', fantaTeamId: null } }
async function deleteParticipant(id) {
  if (!confirm('Eliminare questo partecipante?')) return
  await participantApi.delete(id); await loadParticipants()
}

onMounted(() => Promise.all([loadTeams(), loadParticipants()]))
</script>
