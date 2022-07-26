package me.blvckbytes.bblibgui;

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.blvckbytes.bblibgui.listener.InventoryManipulationEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Function;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/22/2022

  Represents an item which resides in a managed GUI.

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
@Getter
@AllArgsConstructor
public class GuiItem {
  // Item supplier function
  private Function<Integer, ItemStack> item;

  // Click event consumer
  private @Nullable Consumer<InventoryManipulationEvent> onClick;

  // How often this item should be updated, in ticks, null means never
  private @Nullable Integer updatePeriod;
}
