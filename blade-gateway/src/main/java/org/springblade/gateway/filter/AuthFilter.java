/**
 * Copyright (c) 2018-2099, Chill Zhuang 庄骞 (bladejava@qq.com).
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springblade.gateway.filter;

import com.alibaba.nacos.common.utils.StringUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.launch.props.BladeProperties;
import org.springblade.gateway.props.AuthProperties;
import org.springblade.gateway.provider.AuthProvider;
import org.springblade.gateway.provider.ResponseProvider;
import org.springblade.gateway.utils.JwtCrypto;
import org.springblade.gateway.utils.JwtUtil;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

import static org.springblade.gateway.utils.JwtCrypto.BLADE_CRYPTO_AES_KEY;

/**
 * 鉴权认证，对请求 head 进行校验逻辑，主要包括：
 * 检查请求路径是否需要认证
 * 从请求头或参数中获取JWT token
 * 验证token的有效性
 * 对加密token进行解密
 * 返回未授权的响应（当认证失败时）
 *
 * @author Chill
 */
@Slf4j
@Component
@AllArgsConstructor
public class AuthFilter implements GlobalFilter, Ordered {
	// 认证配置属性
	private final AuthProperties authProperties;
	// JSON处理工具
	private final ObjectMapper objectMapper;
	// 系统配置属性
	private final BladeProperties bladeProperties;
	// Ant风格路径匹配器
	private final AntPathMatcher antPathMatcher = new AntPathMatcher();


	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		// 获取请求路径
		String path = exchange.getRequest().getURI().getPath();
		// 检查是否是跳过认证的路径
		if (isSkip(path)) {
			// 如果是跳过路径，直接放行
			return chain.filter(exchange);
		}

		// 获取响应对象
		ServerHttpResponse resp = exchange.getResponse();
		// 从请求头获取token
		String headerToken = exchange.getRequest().getHeaders().getFirst(AuthProvider.AUTH_KEY);
		// 从请求参数获取token
		String paramToken = exchange.getRequest().getQueryParams().getFirst(AuthProvider.AUTH_KEY);

		// 检查token是否存在
		if (StringUtils.isBlank(headerToken) && StringUtils.isBlank(paramToken)) {
			// 如果token不存在，返回未授权响应
			return unAuth(resp, "缺失令牌,鉴权失败");
		}

		// 优先使用header中的token
		String auth = StringUtils.isBlank(headerToken) ? paramToken : headerToken;
		// 从token中提取有效部分
		String token = JwtUtil.getToken(auth);

		// 校验加密Token的合法性
		if (JwtUtil.isCrypto(auth)) {
			// 如果是加密token，则进行解密
			token = JwtCrypto.decryptToString(token, bladeProperties.getEnvironment().getProperty(BLADE_CRYPTO_AES_KEY));
		}

		// 解析JWT token
		Claims claims = JwtUtil.parseJWT(token);
		if (claims == null) {
			// 如果解析失败，返回未授权响应
			return unAuth(resp, "请求未授权");
		}

		// 认证通过，继续执行过滤器链
		return chain.filter(exchange);
	}

	/**
	 * 检查路径是否需要跳过认证
	 * @param path 请求路径
	 * @return 是否跳过认证
	 */
	private boolean isSkip(String path) {
		// 检查路径是否匹配默认跳过路径或配置的跳过路径
		return AuthProvider.getDefaultSkipUrl().stream().anyMatch(pattern -> antPathMatcher.match(pattern, path))
			|| authProperties.getSkipUrl().stream().anyMatch(pattern -> antPathMatcher.match(pattern, path));
	}

	/**
	 * 返回未授权的响应
	 * @param resp 响应对象
	 * @param msg 错误信息
	 * @return Mono<Void>
	 */
	private Mono<Void> unAuth(ServerHttpResponse resp, String msg) {
		// 设置响应状态为401未授权
		resp.setStatusCode(HttpStatus.UNAUTHORIZED);
		// 设置响应头
		resp.getHeaders().add("Content-Type", "application/json;charset=UTF-8");
		String result = "";
		try {
			// 将错误信息转换为JSON格式
			result = objectMapper.writeValueAsString(ResponseProvider.unAuth(msg));
		} catch (JsonProcessingException e) {
			// 记录JSON处理异常
			log.error(e.getMessage(), e);
		}
		// 创建响应缓冲区
		DataBuffer buffer = resp.bufferFactory().wrap(result.getBytes(StandardCharsets.UTF_8));
		// 返回响应
		return resp.writeWith(Flux.just(buffer));
	}

	@Override
	public int getOrder() {
		// 设置过滤器执行顺序，数值越小优先级越高
		return -100;
	}

}
