/**
 * RAG 知识库文档管理接口（对标 architecture.md 模块 8，全部 🔒 admin）
 *   GET    /admin/knowledge/page          文档分页（解析状态轮询用）
 *   POST   /admin/knowledge/upload        上传文档（multipart，后端异步解析+向量化）
 *   DELETE /admin/knowledge/{id}          删除文档（先删 Milvus 向量再删 MySQL 行）
 *   POST   /admin/knowledge/{id}/reparse  重新解析（向量丢失时重建）
 */
import request from '@/utils/request'

/**
 * 8.1 文档分页
 * @param {Object} params { fileName, fileType, status, current, size } 均可选
 */
export function page(params) {
  return request.get('/admin/knowledge/page', { params })
}

/**
 * 8.2 上传文档（pdf/txt/docx，≤50MB，后端校验 40030 类型不支持）
 * @param {File} file 文件对象
 * @param {(percent: number)=>void} onProgress 上传进度回调（0~100）
 */
export function upload(file, onProgress) {
  const formData = new FormData()
  formData.append('file', file) // 字段名必须叫 file，对齐后端 @RequestParam("file")
  return request.post('/admin/knowledge/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    // axios 上传进度：e.loaded 已传字节 / e.total 总字节
    onUploadProgress: (e) => {
      if (onProgress && e.total) onProgress(Math.round((e.loaded / e.total) * 100))
    },
  })
}

/** 8.3 删除文档（同步删 Milvus 全部向量，Milvus 失败 → 后端 40050 且不删 MySQL） */
export function remove(id) {
  return request.delete(`/admin/knowledge/${id}`)
}

/** 8.4 重新解析（删旧向量 → status 重置解析中 → 重新分块向量化） */
export function reparse(id) {
  return request.post(`/admin/knowledge/${id}/reparse`)
}
