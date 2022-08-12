package me.blvckbytes.bblibgui;

import me.blvckbytes.bblibutil.Tuple;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 07/21/2022

  Represents a custom filter function for anvil search GUIs.

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
@FunctionalInterface
public interface FilterFunction {

  /**
   * Apply the current search parameters to a list of items
   * @param search Search string
   * @param filter Current field filter
   * @param items List of items to filter
   * @return Filtered items
   */
  List<Tuple<Object, ItemStack>> apply(String search, IFilterEnum<?> filter, List<Tuple<Object, ItemStack>> items);

}
