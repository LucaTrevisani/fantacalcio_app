<template>
  <div class="mantra-roles" v-if="roles.length">
    <span
      v-for="r in roles"
      :key="r"
      class="mr-badge"
      :class="mantraClass(r)"
      :title="mantraFull(r)"
    >{{ r }}</span>
  </div>
  <span v-else style="color:var(--text-light);font-size:12px">—</span>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({ mantraRole: String })

const roles = computed(() =>
  props.mantraRole
    ? props.mantraRole.split(';').map(r => r.trim()).filter(Boolean)
    : []
)

const CLASSES = {
  Por: 'mr-Por', Pc: 'mr-Pc',
  Dc: 'mr-Dc', Dd: 'mr-Dd', Ds: 'mr-Ds', B: 'mr-B',
  E: 'mr-E', M: 'mr-M', C: 'mr-C',
  T: 'mr-T', W: 'mr-W',
  A: 'mr-A'
}
const FULL = {
  Por: 'Portiere',
  Dc: 'Dif. Centrale', Dd: 'Terzino Destro', Ds: 'Terzino Sinistro', B: 'Braccetto',
  E: 'Esterno', M: 'Mediano', C: 'Centrocampista',
  T: 'Trequartista', W: 'Ala',
  A: 'Attaccante', Pc: 'Punta Centrale'
}

function mantraClass(r) { return CLASSES[r] || 'mr-unknown' }
function mantraFull(r)  { return FULL[r] || r }
</script>
