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
  primary: '#BB86FC',
  secondary: '#03DAC5',
  accent: '#5F94F5',
  error: '#f44336',
  warning: '#ff9800',
  info: '#03a9f4',
  success: '#4caf50',
  background: '#333136',
};

require("./assets/styles/style.css");