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
package org.springblade.system.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.AllArgsConstructor;
import org.springblade.core.secure.BladeUser;
import org.springblade.core.tool.constant.BladeConstant;
import org.springblade.core.tool.node.ForestNodeMerger;
import org.springblade.core.tool.support.Kv;
import org.springblade.core.tool.utils.Func;
import org.springblade.core.tool.utils.StringUtil;
import org.springblade.system.dto.MenuDTO;
import org.springblade.system.entity.Menu;
import org.springblade.system.entity.RoleMenu;
import org.springblade.system.entity.RoleScope;
import org.springblade.system.mapper.MenuMapper;
import org.springblade.system.service.IMenuService;
import org.springblade.system.service.IRoleMenuService;
import org.springblade.system.service.IRoleScopeService;
import org.springblade.system.vo.MenuVO;
import org.springblade.system.wrapper.MenuWrapper;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 服务实现类
 *
 * @author Chill
 */
@Service
@AllArgsConstructor
public class MenuServiceImpl extends ServiceImpl<MenuMapper, Menu> implements IMenuService {

	private final IRoleMenuService roleMenuService;
	private final IRoleScopeService roleScopeService;
	private final static String PARENT_ID = "parentId";

	@Override
	public IPage<MenuVO> selectMenuPage(IPage<MenuVO> page, MenuVO menu) {
		return page.setRecords(baseMapper.selectMenuPage(page, menu));
	}

	@Override
	public List<MenuVO> lazyMenuList(Long parentId, Map<String, Object> param) {
		if (Func.isEmpty(Func.toStr(param.get(PARENT_ID)))) {
			parentId = null;
		}
		return baseMapper.lazyMenuList(parentId, param);
	}

	@Override
	public List<MenuVO> routes(String roleId) {
		if (StringUtil.isBlank(roleId)) {
			return null;
		}
		// 获取所有菜单数据
		List<Menu> allMenus = baseMapper.allMenu();
		// 根据角色ID获取该角色拥有的菜单（角色菜单）
		List<Menu> roleMenus = baseMapper.roleMenu(Func.toLongList(roleId));
		// 创建路由菜单列表，初始化为角色拥有的菜单
		List<Menu> routes = new LinkedList<>(roleMenus);
		// 对每个角色拥有的菜单进行递归处理，查找其所有父级菜单
		roleMenus.forEach(roleMenu -> recursion(allMenus, routes, roleMenu));
		// 路由菜单排序
		routes.sort(Comparator.comparing(Menu::getSort));
		// 菜单包装类
		MenuWrapper menuWrapper = new MenuWrapper();
		// 过滤得到菜单
		List<Menu> collect = routes.stream().filter(x -> Func.equals(x.getCategory(), 1)).collect(Collectors.toList());
		return menuWrapper.listNodeVO(collect);
	}

	/**
	 * 递归查找菜单的所有父级菜单
	 * @param allMenus 所有菜单列表
	 * @param routes 最终返回的菜单路由列表
	 * @param roleMenu 当前处理的角色菜单项
	 */
	public void recursion(List<Menu> allMenus, List<Menu> routes, Menu roleMenu) {
		// 在所有菜单中查找当前菜单的父菜单
		Optional<Menu> menu = allMenus.stream().filter(x -> Func.equals(x.getId(), roleMenu.getParentId())).findFirst();
		// 如果找到父菜单且路由列表中尚未包含该父菜单
		if (menu.isPresent() && !routes.contains(menu.get())) {
			// 将父菜单添加到路由列表
			routes.add(menu.get());
			// 递归继续查找父菜单的父菜单
			recursion(allMenus, routes, menu.get());
		}
	}

	@Override
	public List<MenuVO> buttons(String roleId) {
		List<Menu> buttons = baseMapper.buttons(Func.toLongList(roleId));
		MenuWrapper menuWrapper = new MenuWrapper();
		return menuWrapper.listNodeVO(buttons);
	}

	@Override
	public List<MenuVO> tree() {
		return ForestNodeMerger.merge(baseMapper.tree());
	}

	@Override
	public List<MenuVO> grantTree(BladeUser user) {
		return ForestNodeMerger.merge(user.getTenantId().equals(BladeConstant.ADMIN_TENANT_ID) ? baseMapper.grantTree() : baseMapper.grantTreeByRole(Func.toLongList(user.getRoleId())));
	}

	@Override
	public List<MenuVO> grantDataScopeTree(BladeUser user) {
		return ForestNodeMerger.merge(user.getTenantId().equals(BladeConstant.ADMIN_TENANT_ID) ? baseMapper.grantDataScopeTree() : baseMapper.grantDataScopeTreeByRole(Func.toLongList(user.getRoleId())));
	}

	@Override
	public List<String> roleTreeKeys(String roleIds) {
		List<RoleMenu> roleMenus = roleMenuService.list(Wrappers.<RoleMenu>query().lambda().in(RoleMenu::getRoleId, Func.toLongList(roleIds)));
		return roleMenus.stream().map(roleMenu -> Func.toStr(roleMenu.getMenuId())).collect(Collectors.toList());
	}

	@Override
	public List<String> dataScopeTreeKeys(String roleIds) {
		List<RoleScope> roleScopes = roleScopeService.list(Wrappers.<RoleScope>query().lambda().in(RoleScope::getRoleId, Func.toLongList(roleIds)));
		return roleScopes.stream().map(roleScope -> Func.toStr(roleScope.getScopeId())).collect(Collectors.toList());
	}

	@Override
	public List<Kv> authRoutes(BladeUser user) {
		if (Func.isEmpty(user)) {
			return null;
		}
		List<MenuDTO> routes = baseMapper.authRoutes(Func.toLongList(user.getRoleId()));
		List<Kv> list = new ArrayList<>();
		routes.forEach(route -> list.add(Kv.init().set(route.getPath(), Kv.init().set("authority", Func.toStrArray(route.getAlias())))));
		return list;
	}

}
