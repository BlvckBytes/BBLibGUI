package me.blvckbytes.bblibgui.param;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import me.blvckbytes.bblibgui.*;
import me.blvckbytes.bblibutil.Tuple;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 07/24/2022

  Base class of GUI parameters which correspond to any type of choice menu.

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
public abstract class AChoiceParam<T> implements IAnvilGuiParam<T> {

  //////////////////////// Layout & Items ////////////////////////

  // GUI title
  private final String title;

  // List of choices, objects represented by itemstacks
  @Setter private List<Tuple<Object, ItemStack>> representitives;

  // Provider for standard parameters used in GUIs
  private final IStdGuiItemProvider itemProvider;

  // Used to transform selected items (when going back a menu, for example)
  private @Nullable Function<ItemStack, ItemStack> selectionTransform;

  // Optional custom search GUI layout specification
  private final @Nullable GuiLayoutSection searchLayout;

  ////////////////////////// Searching //////////////////////////

  // Available fields for filtering
  private final @Nullable IFilterEnum<?> searchFields;

  // External filtering function
  private final @Nullable FilterFunction filter;

  ////////////////////////// Callbacks //////////////////////////

  // Selection callback, provides the bound object and the GUI ref
  private final BiConsumer<Object, GuiInstance<T>> selected;

  // Inventory close callback, providing a ref to the closed GUI
  private final @Nullable Consumer<GuiInstance<T>> closed;

  // Back button, providing a ref to the GUI about to navigate away from
  private final @Nullable Consumer<GuiInstance<T>> backButton;

}
