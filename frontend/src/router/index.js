import { createRouter, createWebHistory } from 'vue-router'
import PlayersView from '../views/PlayersView.vue'
import ConfigView from '../views/ConfigView.vue'
import AuctionView from '../views/AuctionView.vue'
import FormationsView from '../views/FormationsView.vue'
import StatsView from '../views/StatsView.vue'
import AnalysisView from '../views/AnalysisView.vue'

export default createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/players' },
    { path: '/players', component: PlayersView },
    { path: '/stats', component: StatsView },
    { path: '/analysis', component: AnalysisView },
    { path: '/config', component: ConfigView },
    { path: '/auction', component: AuctionView },
    { path: '/formations', component: FormationsView }
  ]
})
