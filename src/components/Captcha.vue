<template>
  <div class="captcha-wrapper">
    <div class="captcha-row">
      <img
        class="captcha-canvas"
        :src="captchaUrl"
        @click="refresh"
        title="点击刷新验证码"
        :width="width"
        :height="height"
        alt="验证码"
      />
      <el-button text type="primary" size="small" @click="refresh">
        换一张
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  width: { type: Number, default: 130 },
  height: { type: Number, default: 42 }
})

const timestamp = ref(Date.now())

const captchaUrl = computed(() => `/api/captcha?t=${timestamp.value}`)

function refresh() {
  timestamp.value = Date.now()
}

defineExpose({ refresh })
</script>

<style scoped>
.captcha-wrapper {
  width: 100%;
}

.captcha-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.captcha-canvas {
  cursor: pointer;
  border-radius: 6px;
  border: 1px solid #e0e0e0;
  flex-shrink: 0;
}
</style>