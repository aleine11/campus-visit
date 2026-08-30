/**
 * 表单校验规则（与后端 DTO 校验注解 1:1 对应）
 *
 * 原则：前端先拦一道（体验好、少一次请求），后端必再校验（安全兜底）。
 * 正则必须与后端 RegisterDTO / ReservationSubmitDTO 完全一致，
 * 否则会出现"前端过了后端挂"的割裂体验。
 */

/** 用户名：4~20 位字母数字下划线（对齐 RegisterDTO） */
export const USERNAME_PATTERN = /^[a-zA-Z0-9_]{4,20}$/

/** 密码：6~20 位且同时含字母和数字（对齐 RegisterDTO 的正向先行断言写法） */
export const PASSWORD_PATTERN = /^(?=.*[a-zA-Z])(?=.*\d).{6,20}$/

/** 手机号：11 位大陆手机号，1 开头第二位 3-9（对齐后端） */
export const PHONE_PATTERN = /^1[3-9]\d{9}$/

/** 通用必填规则 */
const required = (message, trigger = 'blur') => ({ required: true, message, trigger })

export const rules = {
  username: [
    required('请输入用户名'),
    { pattern: USERNAME_PATTERN, message: '用户名须为 4~20 位字母、数字或下划线', trigger: 'blur' },
  ],
  password: [
    required('请输入密码'),
    { pattern: PASSWORD_PATTERN, message: '密码须为 6~20 位，且同时包含字母和数字', trigger: 'blur' },
  ],
  realName: [
    required('请输入真实姓名'),
    { min: 2, max: 10, message: '真实姓名长度须在 2~10 字之间', trigger: 'blur' },
  ],
  phone: [
    required('请输入手机号'),
    { pattern: PHONE_PATTERN, message: '手机号格式不正确（须为 11 位大陆手机号）', trigger: 'blur' },
  ],
  reason: [
    required('请填写参观事由'),
    { min: 5, max: 200, message: '参观事由长度须在 5~200 字之间', trigger: 'blur' },
  ],
}
