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
package org.springblade.system.wrapper;

import org.springblade.common.constant.CommonConstant;
import org.springblade.core.mp.support.BaseEntityWrapper;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.node.ForestNodeMerger;
import org.springblade.core.tool.utils.BeanUtil;
import org.springblade.core.tool.utils.Func;
import org.springblade.core.tool.utils.SpringUtil;
import org.springblade.system.entity.Menu;
import org.springblade.system.feign.IDictClient;
import org.springblade.system.service.IMenuService;
import org.springblade.system.vo.MenuVO;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 包装类,返回视图层所需的字段
 * 菜单包装类，用于将 Menu 实体转换为 MenuVO 视图对象，继承自 BaseEntityWrapper，提供基础实体转换能力
 *
 * @author Chill
 */
public class MenuWrapper extends BaseEntityWrapper<Menu, MenuVO> {

	// 菜单服务接口（静态变量，类加载时初始化）
	private static IMenuService menuService;
	// 字典服务客户端（静态变量，类加载时初始化）
	private static IDictClient dictClient;

	// 静态初始化块，在类加载时从Spring容器获取Bean
	static {
		menuService = SpringUtil.getBean(IMenuService.class);
		dictClient = SpringUtil.getBean(IDictClient.class);
	}

	/**
	 * 构建 MenuWrapper 实例的静态工厂方法
	 * @return MenuWrapper 实例
	 */
	public static MenuWrapper build() {
		return new MenuWrapper();
	}

	/**
	 * 将 Menu 实体转换为 MenuVO 视图对象
	 * @param menu 菜单实体
	 * @return 菜单视图对象
	 */
	@Override
	public MenuVO entityVO(Menu menu) {
		// 使用BeanUtil工具类复制属性
		MenuVO menuVO = BeanUtil.copyProperties(menu, MenuVO.class);

		// 设置父级菜单名称
		if (Func.equals(menu.getParentId(), CommonConstant.TOP_PARENT_ID)) {
			// 如果是顶级菜单，设置默认父级名称
			menuVO.setParentName(CommonConstant.TOP_PARENT_NAME);
		} else {
			// 非顶级菜单，查询父级菜单并设置名称
			Menu parent = menuService.getById(menu.getParentId());
			menuVO.setParentName(parent.getName());
		}

		// 从字典服务获取各类字典值并设置显示名称
		// 1. 获取菜单分类名称
		R<String> d1 = dictClient.getValue("menu_category", Func.toInt(menuVO.getCategory()));
		// 2. 获取按钮功能名称
		R<String> d2 = dictClient.getValue("button_func", Func.toInt(menuVO.getAction()));
		// 3. 获取是否打开名称
		R<String> d3 = dictClient.getValue("yes_no", Func.toInt(menuVO.getIsOpen()));

		// 设置字典值到视图对象
		if (d1.isSuccess()) {
			menuVO.setCategoryName(d1.getData());
		}
		if (d2.isSuccess()) {
			menuVO.setActionName(d2.getData());
		}
		if (d3.isSuccess()) {
			menuVO.setIsOpenName(d3.getData());
		}

		return menuVO;
	}

	/**
	 * 将菜单列表转换为树形节点VO列表（非懒加载模式）
	 * @param list 菜单实体列表
	 * @return 树形结构的菜单VO列表
	 */
	public List<MenuVO> listNodeVO(List<Menu> list) {
		// 先将实体列表转换为VO列表
		List<MenuVO> collect = list.stream().map(menu -> BeanUtil.copyProperties(menu, MenuVO.class)).collect(Collectors.toList());
		// 使用ForestNodeMerger合并成树形结构
		return ForestNodeMerger.merge(collect);
	}

	/**
	 * 将菜单VO列表转换为树形节点VO列表（懒加载模式）
	 * @param list 菜单VO列表
	 * @return 树形结构的菜单VO列表
	 */
	public List<MenuVO> listNodeLazyVO(List<MenuVO> list) {
		// 直接使用ForestNodeMerger合并成树形结构
		return ForestNodeMerger.merge(list);
	}

}
