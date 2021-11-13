<template>
  <v-app>
    <v-app-bar
      app
      dark
    >

      <div class="d-flex align-center">
        <v-img
          alt="Bay Leaf Logo"
          class="shrink mr-2"
          contain
          src="./assets/bay-leaf-48.png"
          transition="scale-transition"
          width="40"
        />
        <h2 style="font-family: roboto; font-size: 30px">Bay Leaf Examples</h2>
      </div>

      <v-spacer></v-spacer>
      <v-btn-toggle v-model="toggle" mandatory>
        <v-btn color="primary">Simple</v-btn>
        <v-btn color="primary">Complex</v-btn>
      </v-btn-toggle>
    </v-app-bar>

    <v-main>
      <template v-if="isSimple">
        <BayLeafExamples/>
      </template>
      <template v-else>
        <MultiServiceExample/>
      </template>
    </v-main>
  </v-app>
</template>

<script>
import BayLeafExamples from './components/BayLeafExamples';
import MultiServiceExample from './components/MultiServiceExample.vue';

export default {
  name: 'App',

  components: {
    BayLeafExamples,
    MultiServiceExample,
  },

  data: () => {
    return {
      toggle: "Simple"
    }
  },

  computed: {
    isSimple: function() {
      return this.toggle === 0;
    }
  },
};
</script>

<style lang="scss">
  @import "~ag-grid-community/dist/styles/ag-grid.css";
  @import "~ag-grid-community/dist/styles/ag-theme-alpine-dark.css";
  @import "~ag-grid-community/dist/styles/ag-theme-balham-dark.css";
</style>
