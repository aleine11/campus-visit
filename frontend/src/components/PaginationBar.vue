<template>
  <!-- 通用分页条（对标 frontend-prototype.md 5.4：默认每页 10 条） -->
  <div class="pagination-bar">
    <el-pagination
      :current-page="current"
      :page-size="size"
      :total="total"
      :page-sizes="[10, 20, 50]"
      layout="total, sizes, prev, pager, next, jumper"
      background
      @current-change="handleCurrentChange"
      @size-change="handleSizeChange"
    />
  </div>
</template>

<script setup>
/**
 * 通用分页条
 *
 * 为什么封装：每个列表页都要 el-pagination + 两个回调，参数结构（current/size/total）
 * 与后端 IPage 完全一致，封装后列表页只写一行 <PaginationBar ... />
 */
const props = defineProps({
  current: { type: Number, default: 1 }, // 当前页码
  size: { type: Number, default: 10 }, // 每页条数
  total: { type: Number, default: 0 }, // 总条数（后端 IPage.total）
})

const emit = defineEmits(['change'])

/** 换页码：通知父组件"用新 current 重新查" */
function handleCurrentChange(page) {
  emit('change', { current: page, size: props.size })
}

/** 换每页条数：条数变了要重置回第 1 页（否则可能停在总页数之外） */
function handleSizeChange(size) {
  emit('change', { current: 1, size })
}
</script>

<style scoped>
.pagination-bar {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}
</style>
