<template>
  <!-- AI 思考中动画：3 个跳动圆点（对标 frontend-prototype.md 8.1） -->
  <div class="ai-typing">
    <span class="dot"></span>
    <span class="dot"></span>
    <span class="dot"></span>
    <span class="tip">AI 思考中...</span>
  </div>
</template>

<script setup>
// 无逻辑纯展示组件
</script>

<style scoped>
.ai-typing {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 8px 4px;
}
.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #409eff;
  animation: bounce 1.2s infinite ease-in-out;
}
.dot:nth-child(2) {
  animation-delay: 0.2s;
}
.dot:nth-child(3) {
  animation-delay: 0.4s;
}
.tip {
  font-size: 12px;
  color: #909399;
  margin-left: 4px;
}
@keyframes bounce {
  0%,
  60%,
  100% {
    transform: translateY(0);
    opacity: 0.4;
  }
  30% {
    transform: translateY(-6px);
    opacity: 1;
  }
}
</style>
