package com.campus.visit.interceptor;

import com.campus.visit.annotation.RequiresLogin;
import com.campus.visit.annotation.RequiresRole;
import com.campus.visit.common.BusinessException;
import com.campus.visit.common.ResultCode;
import com.campus.visit.utils.JwtUtil;
import com.campus.visit.utils.UserContext;
import com.campus.visit.utils.UserContext.LoginUser;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.lang.reflect.Method;

/**
 * JWT 鉴权拦截器 —— 核心门禁
 *
 * 拦截流程（对标 architecture.md 第八章）：
 *   1. 判断目标方法/类是否有 @RequiresLogin 或 @RequiresRole 注解
 *      都没有 → 放行（公开接口：登录/注册/公告列表/场次列表）
 *   2. 有注解 → 取 Authorization 请求头
 *      为空或不以 "Bearer " 开头 → 抛 401 UNAUTHORIZED
 *   3. 调 JwtUtil.verify(token) 校验签名 + 过期
 *      失败 → 抛 401 UNAUTHORIZED
 *   4. 解析 JWT payload → 组装 LoginUser → UserContext.set()
 *   5. 有 @RequiresRole 注解 → 比对 role 和 superOnly
 *      不匹配 → 抛 403 FORBIDDEN
 *   6. 放行，Controller 里直接 UserContext.get() 取当前用户
 *
 * 请求结束后 afterCompletion 必须调 UserContext.clear()
 * 否则 Tomcat 线程池复用时会串用户（严重安全问题！）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 只拦截 Controller 方法，放行静态资源
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        Method method = handlerMethod.getMethod();
        Class<?> targetClass = handlerMethod.getBeanType();

        // ========== 第 1 步：判断是否需要鉴权 ==========
        // 类或方法上有 @RequiresLogin / @RequiresRole 都算需要鉴权
        boolean needLogin = targetClass.isAnnotationPresent(RequiresLogin.class)
                || method.isAnnotationPresent(RequiresLogin.class)
                || targetClass.isAnnotationPresent(RequiresRole.class)
                || method.isAnnotationPresent(RequiresRole.class);

        if (!needLogin) {
            return true;  // 公开接口，直接放行
        }

        // ========== 第 2 步：取 Authorization 头 ==========
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("请求未携带 Authorization 头: {}", request.getRequestURI());
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        String token = authHeader.substring(7);  // 去掉 "Bearer " 前缀

        // ========== 第 3 步：校验 JWT ==========
        if (!jwtUtil.verify(token)) {
            log.warn("JWT 校验失败: {}", request.getRequestURI());
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        // ========== 第 4 步：解析载荷 → 写入 UserContext ==========
        Claims claims = jwtUtil.parseToken(token);
        if (claims == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        LoginUser loginUser = new LoginUser(
                jwtUtil.getUserId(claims),
                jwtUtil.getRole(claims),
                jwtUtil.getIsSuper(claims),
                jwtUtil.getUsername(claims)
        );
        UserContext.set(loginUser);
        log.debug("用户 [{}] 已登录, role={}, uri={}", loginUser.getUsername(), loginUser.getRole(), request.getRequestURI());

        // ========== 第 5 步：角色校验 ==========
        // 方法级注解优先，其次类级注解
        RequiresRole roleAnno = method.getAnnotation(RequiresRole.class);
        if (roleAnno == null) {
            roleAnno = targetClass.getAnnotation(RequiresRole.class);
        }

        if (roleAnno != null) {
            String requiredRole = roleAnno.value();
            if (!requiredRole.equals(loginUser.getRole())) {
                log.warn("用户 [{}] 角色不符: 需要 {}，实际 {}", loginUser.getUsername(), requiredRole, loginUser.getRole());
                throw new BusinessException(ResultCode.FORBIDDEN);
            }
            if (roleAnno.superOnly() && !Boolean.TRUE.equals(loginUser.getIsSuper())) {
                log.warn("用户 [{}] 非超管，无权限访问超管专属接口", loginUser.getUsername());
                throw new BusinessException(ResultCode.FORBIDDEN);
            }
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // ========== 必须清理 ThreadLocal！ ==========
        // Tomcat 线程池复用线程，不清理会导致"串用户"
        UserContext.clear();
    }
}
