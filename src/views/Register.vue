<template>
  <div class="auth-page">
    <div class="auth-card">
      <div class="auth-header">
        <span class="logo-icon">📖</span>
        <h1>注册风享</h1>
        <p>加入我们，发现美好生活</p>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @keyup.enter="handleRegister">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="2~20个字符" size="large" :prefix-icon="User" maxlength="20" />
        </el-form-item>
        <el-form-item label="注册账号类型" prop="registerType">
          <el-radio-group v-model="form.registerType" size="large" class="register-type-group">
            <el-radio-button :value="0">
              <span class="radio-label">👤 用户端</span>
            </el-radio-button>
            <el-radio-button :value="1">
              <span class="radio-label">🛡️ 管理端</span>
            </el-radio-button>
          </el-radio-group>
        </el-form-item>

        <el-form-item v-if="form.registerType === 1" label="管理端注册码" prop="adminCode">
          <el-input
            v-model="form.adminCode"
            type="password"
            placeholder="请输入管理端注册码"
            size="large"
            show-password
            maxlength="32"
          />
        </el-form-item>

        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="选填" size="large" maxlength="15" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="选填" size="large" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="6~16位密码"
            size="large"
            show-password
            :prefix-icon="Lock"
          />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            placeholder="请再次输入密码"
            size="large"
            show-password
            :prefix-icon="Lock"
          />
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
          <el-button type="primary" size="large" :loading="loading" class="auth-btn" @click="handleRegister">
            注 册
          </el-button>
        </el-form-item>
      </el-form>

      <div class="auth-footer">
        已有账号？
        <router-link to="/login" class="link">立即登录</router-link>
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
  registerType: 0,  // 0=用户端 1=管理端
  adminCode: '',    // 管理端注册码
  phone: '',
  email: '',
  password: '',
  confirmPassword: '',
  captcha: ''
})

const validatePass = (rule, value, callback) => {
  if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 20, message: '用户名长度在 2~20 个字符', trigger: 'blur' }
  ],
  registerType: [
    { required: true, message: '请选择注册账号类型', trigger: 'change' }
  ],
  adminCode: [
    {
      validator: (rule, value, callback) => {
        if (form.registerType === 1 && !value) {
          callback(new Error('管理端注册码不能为空'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 16, message: '密码长度为 6~16 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validatePass, trigger: 'blur' }
  ],
  captcha: [
    { required: true, message: '请输入验证码', trigger: 'blur' }
  ]
}

async function handleRegister() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const registerData = {
      username: form.username,
      phone: form.phone || undefined,
      email: form.email || undefined,
      passwordHash: form.password,
      captcha: form.captcha,
      role: form.registerType  // 0=普通用户 1=管理员
    }
    // 管理端注册需附带注册码
    if (form.registerType === 1) {
      registerData.adminCode = form.adminCode
    }
    await store.register(registerData)
    ElMessage.success('注册成功')
    // 根据注册类型跳转
    if (form.registerType === 1) {
      router.push('/admin/dashboard')  // 管理端首页
    } else {
      router.push('/discover')  // 用户端首页
    }
  } catch (e) {
    ElMessage.error(e.message || '注册失败，请稍后重试')
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
  width: 440px;
  background: #fff;
  border-radius: 16px;
  padding: 40px 36px;
  box-shadow: 0 8px 30px rgba(0,0,0,0.06);
}

.auth-header {
  text-align: center;
  margin-bottom: 28px;
}

.auth-header .logo-icon {
  font-size: 48px;
}

.auth-header h1 {
  font-size: 26px;
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

.register-type-group {
  width: 100%;
}

.register-type-group :deep(.el-radio-button) {
  flex: 1;
}

.register-type-group :deep(.el-radio-button__inner) {
  width: 100%;
  text-align: center;
}

.radio-label {
  font-size: 15px;
}
</style>