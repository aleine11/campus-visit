<template>
  <div class="admin-home">
    <el-card>
      <h2>欢迎使用校园参观预约与智能咨询后台</h2>
      <p>本页面为 Stage0 骨架占位，Stage2 开发后台首页时填充统计数据看板。</p>
      <el-row :gutter="16" style="margin-top: 16px">
        <el-col :span="6">
          <el-card shadow="hover"><div class="stat">今日预约 <strong>0</strong></div></el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover"><div class="stat">待审核 <strong>0</strong></div></el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover"><div class="stat">访客总数 <strong>0</strong></div></el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover"><div class="stat">问答次数 <strong>0</strong></div></el-card>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup>
// 管理员首页占位
</script>

<style scoped>
.admin-home h2 {
  font-size: 20px;
  margin-bottom: 12px;
}
.stat {
  text-align: center;
  font-size: 14px;
  color: #606266;
}
.stat strong {
  display: block;
  font-size: 28px;
  color: #409eff;
  margin-top: 8px;
}
</style>
