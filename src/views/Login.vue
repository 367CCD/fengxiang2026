<template>
  <div class="auth-page">
    <div class="auth-card">
      <div class="auth-header">
        <span class="logo-icon">📖</span>
        <h1>风享</h1>
        <p>发现美好生活</p>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @keyup.enter="handleLogin">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名，2~20个字符" size="large" :prefix-icon="User" maxlength="20" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码，6~16位"
            size="large"
            show-password
            :prefix-icon="Lock"
          />
        </el-form-item>

        <el-form-item label="账号类型" prop="loginType">
          <el-radio-group v-model="form.loginType" size="large" class="login-type-group">
            <el-radio-button :value="0">
              <span class="radio-label">👤 用户端</span>
            </el-radio-button>
            <el-radio-button :value="1">
              <span class="radio-label">🛡️ 管理端</span>
            </el-radio-button>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="验证码" prop="captcha" class="captcha-item">
          <div class="captcha-field">
            <el-input
              v-model="form.captcha"
              placeholder="验证码"
              size="large"
              maxlength="4"
              class="captcha-input"
            />
            <Captcha ref="captchaRef" />
          </div>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" size="large" :loading="loading" class="auth-btn" @click="handleLogin">
            登 录
          </el-button>
        </el-form-item>
      </el-form>

      <div class="auth-footer">
        还没有账号？
        <router-link to="/register" class="link">立即注册</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '../stores/user'
import Captcha from '../components/Captcha.vue'

const router = useRouter()
const store = useUserStore()
const formRef = ref(null)
const captchaRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
  captcha: '',
  loginType: 0  // 0=用户端 1=管理端
})

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 20, message: '用户名长度在 2~20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 16, message: '密码长度为 6~16 位', trigger: 'blur' }
  ],
  captcha: [
    { required: true, message: '请输入验证码', trigger: 'blur' }
  ],
  loginType: [
    { required: true, message: '请选择账号类型', trigger: 'change' }
  ]
}

async function handleLogin() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const result = await store.login({
      username: form.username,
      passwordHash: form.password,
      captcha: form.captcha,
      loginType: form.loginType
    })
    ElMessage.success('登录成功')
    // 根据登录类型跳转
    if (form.loginType === 1) {
      router.push('/admin/dashboard')  // 管理端首页
    } else {
      router.push('/discover')  // 用户端首页
    }
  } catch (e) {
    ElMessage.error(e.message || '登录失败，请检查账号密码')
    captchaRef.value?.refresh()
    form.captcha = ''
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #fff5f5 0%, #fff0f5 100%);
}

.auth-card {
  width: 420px;
  background: #fff;
  border-radius: 16px;
  padding: 40px 36px;
  box-shadow: 0 8px 30px rgba(0,0,0,0.06);
}

.auth-header {
  text-align: center;
  margin-bottom: 32px;
}

.auth-header .logo-icon {
  font-size: 48px;
}

.auth-header h1 {
  font-size: 28px;
  color: var(--primary-color);
  margin-top: 8px;
}

.auth-header p {
  color: var(--text-secondary);
  font-size: 14px;
  margin-top: 4px;
}

.auth-btn {
  width: 100%;
  height: 44px;
  font-size: 16px;
  letter-spacing: 4px;
}

.auth-footer {
  text-align: center;
  font-size: 14px;
  color: var(--text-secondary);
  margin-top: 16px;
}

.link {
  color: var(--primary-color);
  text-decoration: none;
  font-weight: 500;
}

.captcha-field {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
}

.captcha-input {
  flex: 1;
  min-width: 160px;
}

.captcha-input :deep(.el-input__wrapper) {
  height: 44px;
  padding: 0 16px;
}

.captcha-input :deep(.el-input__inner) {
  font-size: 20px;
  letter-spacing: 8px;
  font-weight: 700;
}

.el-form-item {
  margin-bottom: 22px;
}

.el-form-item.captcha-item {
  margin-bottom: 18px;
}

.login-type-group {
  width: 100%;
}

.login-type-group :deep(.el-radio-button) {
  flex: 1;
}

.login-type-group :deep(.el-radio-button__inner) {
  width: 100%;
  text-align: center;
}

.radio-label {
  font-size: 15px;
}
</style>