import Vue from 'vue'
import App from './App.vue'
import vuetify from './plugins/vuetify'

Vue.config.productionTip = false;

const app = new Vue({
  vuetify,
  render: h => h(App)
}).$mount('#app')

app.$vuetify.theme.dark = true;
app.$vuetify.theme.themes.dark = {
                                       primary: '#3f51b5',
                                       secondary: '#673ab7',
                                       accent: '#9c27b0',
                                       error: '#f44336',
                                       warning: '#ff9800',
                                       info: '#03a9f4',
                                       success: '#4caf50',
                                       background: '#333136',
                                       };