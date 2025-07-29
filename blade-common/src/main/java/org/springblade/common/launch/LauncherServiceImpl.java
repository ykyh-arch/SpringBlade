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
package org.springblade.common.launch;

import org.springblade.common.constant.LauncherConstant;
import org.springblade.core.launch.service.LauncherService;
import org.springblade.core.launch.utils.PropsUtil;
import org.springframework.boot.builder.SpringApplicationBuilder;

import java.util.Properties;

/**
 * 启动参数拓展，参考 {@link org.springblade.core.launch.StartEventListener }
 *
 * @author smallchil
 */
public class LauncherServiceImpl implements LauncherService {

	@Override
	public void launcher(SpringApplicationBuilder builder, String appName, String profile) {
		// 系统属性扩展启动参数，SimpleCommandLinePropertySource > JNDI Java 系统属性 > MapPropertySource > SystemEnvironmentPropertySource > 配置文件（如 application.yml）
		// 命令行参数 → JNDI（通常在 Servlet 容器中才会生效） → Java 系统属性（-D）→ 系统环境变量 → 配置文件 → 默认属性（通过 SpringApplication.setDefaultProperties() 设置）
		Properties props = System.getProperties();
		PropsUtil.setProperty(props, "spring.cloud.nacos.username", LauncherConstant.NACOS_USERNAME);
		PropsUtil.setProperty(props, "spring.cloud.nacos.password", LauncherConstant.NACOS_PASSWORD);
		// 注册中心，服务地址、命名空间、组信息
		PropsUtil.setProperty(props, "spring.cloud.nacos.discovery.server-addr", LauncherConstant.nacosAddr(profile));
		// PropsUtil.setProperty(props, "spring.cloud.nacos.discovery.namespace", LauncherConstant.nacosAddr(profile));
		// PropsUtil.setProperty(props, "spring.cloud.nacos.discovery.group", LauncherConstant.nacosAddr(profile));
		// 配置中心，服务地址、命名空间、组信息、配置文件格式等
		PropsUtil.setProperty(props, "spring.cloud.nacos.config.server-addr", LauncherConstant.nacosAddr(profile));
		// PropsUtil.setProperty(props, "spring.cloud.nacos.config.namespace", LauncherConstant.nacosAddr(profile));
		// PropsUtil.setProperty(props, "spring.cloud.nacos.config.group", LauncherConstant.nacosAddr(profile));
		// PropsUtil.setProperty(props, "spring.cloud.nacos.config.file-extension", LauncherConstant.nacosAddr(profile));

		PropsUtil.setProperty(props, "spring.cloud.sentinel.transport.dashboard", LauncherConstant.sentinelAddr(profile));
		PropsUtil.setProperty(props, "spring.zipkin.base-url", LauncherConstant.zipkinAddr(profile));
	}

}
