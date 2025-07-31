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

package org.springblade.gateway.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springblade.gateway.provider.ResponseProvider;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * 异常处理
 *
 * @author Chill
 */
@Order(-1)  // 设置最高优先级（数值越小优先级越高），确保最先处理异常
@Configuration(proxyBeanMethods = false)  // 声明为配置类，proxyBeanMethods=false优化运行时性能
@RequiredArgsConstructor  // Lombok生成包含final字段的构造函数
public class ErrorExceptionHandler implements ErrorWebExceptionHandler {  // 实现Spring WebFlux异常处理接口

	private final ObjectMapper objectMapper;  // JSON序列化工具

	/**
	 * 异常处理方法（核心逻辑）
	 * @param exchange 当前请求的上下文（包含请求/响应对象）
	 * @param ex 捕获到的异常
	 * @return Mono<Void> 异步处理结果
	 */
	@Override
	public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
		ServerHttpRequest request = exchange.getRequest();  // 获取请求对象
		ServerHttpResponse response = exchange.getResponse();  // 获取响应对象

		// 如果响应已提交（如已经开始写回客户端），直接返回异常
		if (response.isCommitted()) {
			return Mono.error(ex);
		}

		// 设置响应头为JSON格式
		response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

		// 特殊处理ResponseStatusException（保留其原始状态码）
		if (ex instanceof ResponseStatusException) {
			response.setStatusCode(((ResponseStatusException) ex).getStatusCode());
		}

		// 异步写入响应内容
		return response.writeWith(Mono.fromSupplier(() -> {
			DataBufferFactory bufferFactory = response.bufferFactory();  // 获取数据缓冲区工厂
			try {
				// 确定HTTP状态码（默认500）
				int status = 500;
				if (response.getStatusCode() != null) {
					status = response.getStatusCode().value();
				}

				// 构建响应体：状态码+错误信息
				Map<String, Object> result = ResponseProvider.response(
					status,
					this.buildMessage(request, ex)  // 生成错误详情
				);

				// 将响应体序列化为JSON字节流
				return bufferFactory.wrap(objectMapper.writeValueAsBytes(result));
			} catch (JsonProcessingException e) {
				// JSON序列化失败时返回空数据
				return bufferFactory.wrap(new byte[0]);
			}
		}));
	}

	/**
	 * 构建标准化的错误信息
	 * @param request 当前请求对象
	 * @param ex 异常对象
	 * @return 格式化后的错误信息字符串
	 */
	private String buildMessage(ServerHttpRequest request, Throwable ex) {
		StringBuilder message = new StringBuilder("Failed to handle request [");
		message.append(request.getMethod().name());  // 请求方法（GET/POST等）
		message.append(" ");
		message.append(request.getURI());  // 请求URI
		message.append("]");

		// 追加异常信息（如果存在）
		if (ex != null) {
			message.append(": ");
			message.append(ex.getMessage());
		}

		return message.toString();
	}
}
