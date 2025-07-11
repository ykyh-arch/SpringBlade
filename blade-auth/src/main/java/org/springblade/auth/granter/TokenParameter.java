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
package org.springblade.auth.granter;

import lombok.Data;
import org.springblade.core.tool.support.Kv;

/**
 * TokenParameter
 *
 * @author Chill
 */
@Data
public class TokenParameter {

	// 底层是基于 LinkedCaseInsensitiveMap 实现的，可提供以下功能：
	// 1.不区分大小写的键查找，例如 "KEY"、"key" 和 "Key" 会被认为是同一个键，value 会覆盖上一个值
	// 2.同时需要保持插入顺序，基于 LinkedHashMap
	// 3.保留原始键的大小写形式，（最后一次插入的大小写形式）
	private Kv args = Kv.init();

}
