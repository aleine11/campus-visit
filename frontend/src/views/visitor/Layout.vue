<template>
  <div class="visitor-layout">
    <!-- 访客前台顶部导航 -->
    <header class="header">
      <div class="logo">哈尔滨剑桥学院 · 校园参观预约系统</div>
      <nav class="nav">
        <router-link to="/visitor/home">首页</router-link>
        <router-link to="/visitor/notice">公告</router-link>
        <router-link to="/visitor/session">预约参观</router-link>
        <router-link to="/visitor/profile">个人中心</router-link>
      </nav>
    </header>

    <!-- 内容区 -->
    <main class="content">
      <router-view />
    </main>

    <!-- AI 悬浮弹窗占位 -->
    <div class="ai-float-btn">AI 咨询</div>

    <!-- 页脚 -->
    <footer class="footer">
      <p>© 2026 哈尔滨剑桥学院 智能科学与工程学院 · 毕业设计</p>
    </footer>
  </div>
</template>

<script setup>
// 访客前台布局组件
</script>

<style scoped>
.visitor-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 32px;
  height: 60px;
  background: #409eff;
  color: #fff;
}
.logo {
  font-size: 18px;
  font-weight: 600;
}
.nav a {
  margin-left: 24px;
  color: #fff;
  font-size: 14px;
}
.nav a.router-link-active {
  border-bottom: 2px solid #fff;
  padding-bottom: 4px;
}
.content {
  flex: 1;
  padding: 24px 32px;
}
.ai-float-btn {
  position: fixed;
  right: 32px;
  bottom: 80px;
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: #67c23a;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.2);
}
.footer {
  padding: 16px;
  text-align: center;
  font-size: 12px;
  color: #909399;
  background: #fff;
}
</style>
