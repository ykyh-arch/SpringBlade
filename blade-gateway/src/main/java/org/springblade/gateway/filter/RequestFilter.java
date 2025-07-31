package org.springblade.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.stream.Collectors;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.addOriginalRequestUrl;

/**
 * request过滤器，将类似 /api/service/xxx 的路径重写为 /service/xxx
 *
 * @author lengleng
 */
@Component
public class RequestFilter implements GlobalFilter, Ordered {

	/**
	 * 过滤器核心方法，处理请求路径重写
	 * @param exchange 服务器网络交换对象，包含请求和响应信息
	 * @param chain 过滤器链，用于继续执行后续过滤器
	 * @return Mono<Void> 异步处理结果
	 */
	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		// 获取原始请求对象
		ServerHttpRequest request = exchange.getRequest();

		// 将原始请求URL添加到交换属性中（用于后续处理）
		addOriginalRequestUrl(exchange, request.getURI());

		// 获取原始请求路径（例如：/api/service/xxx）
		String rawPath = request.getURI().getRawPath();

		// 路径重写逻辑：
		// 1. 使用StringUtils将路径按"/"分割成数组
		// 2. 跳过第一个元素（通常是上下文路径/api）
		// 3. 用"/"重新拼接剩余部分
		String newPath = "/" + Arrays.stream(StringUtils.tokenizeToStringArray(rawPath, "/"))
			.skip(1L)  // 跳过第一个路径段
			.collect(Collectors.joining("/"));  // 重新拼接路径

		// 创建新的请求对象（基于原始请求的变体）
		ServerHttpRequest newRequest = request.mutate()  // 创建请求变体构建器
			.path(newPath)  // 设置新路径
			.build();  // 构建新请求

		// 将新请求URI放入交换属性（供下游过滤器使用）
		exchange.getAttributes().put(GATEWAY_REQUEST_URL_ATTR, newRequest.getURI());

		// 继续执行过滤器链，使用修改后的请求对象
		return chain.filter(
			exchange.mutate()  // 创建交换对象变体
				.request(newRequest.mutate().build())  // 设置新请求
				.build()  // 构建新交换对象
		);
	}

	/**
	 * 获取过滤器执行顺序（数值越小优先级越高）
	 * @return 过滤器优先级值
	 */
	@Override
	public int getOrder() {
		return -1000;  // 设置为高优先级（在大多数过滤器之前执行）
	}

}
