package me.blvckbytes.bblibgui;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 07/03/2022

  Provides all available standard GUI items with templating abilities.

  This program is free software: you can redistribute it and/or modify
  it under the terms of the GNU Affero General Public License as published
  by the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU Affero General Public License for more details.

  You should have received a copy of the GNU Affero General Public License
  along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/
public interface IStdGuiItemProvider {

  /**
   * Get a standard GUI item
   * @param item Type of item
   * @param variables Variables to use on the item template
   */
  ItemStack getItem(StdGuiItem item, @Nullable Map<String, String> variables);

}
