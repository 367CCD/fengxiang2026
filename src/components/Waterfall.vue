<template>
  <div class="waterfall" :style="waterfallStyle">
    <div
      v-for="(col, colIdx) in columns"
      :key="colIdx"
      class="waterfall-column"
      :style="{ width: colWidthPercent + '%', marginRight: colIdx < columns.length - 1 ? gap + 'px' : 0 }"
    >
      <div
        v-for="(post, itemIdx) in col"
        :key="post.id || itemIdx"
        class="waterfall-item"
        :style="{ marginBottom: gap + 'px' }"
      >
        <PostCard :post="post" @click="$emit('postClick', post)" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import PostCard from './PostCard.vue'

const props = defineProps({
  posts: { type: Array, default: () => [] },
  columns: { type: Number, default: 3 },
  gap: { type: Number, default: 16 }
})

defineEmits(['postClick'])

const colWidthPercent = computed(() => 100 / props.columns)

const columns = computed(() => {
  const cols = Array.from({ length: props.columns }, () => [])
  props.posts.forEach((post, idx) => {
    cols[idx % props.columns].push(post)
  })
  return cols
})

const waterfallStyle = computed(() => ({
  display: 'flex',
  alignItems: 'flex-start'
}))
</script>

<style scoped>
.waterfall-column {
  flex-shrink: 0;
}
.waterfall-item {
  break-inside: avoid;
}
</style>