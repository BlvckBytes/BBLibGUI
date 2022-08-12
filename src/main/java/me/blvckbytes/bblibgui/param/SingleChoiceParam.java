package me.blvckbytes.bblibgui.param;

import lombok.Getter;
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
  Created On: 05/29/2022

  The parameter used to open a new single choice GUI.

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
public class SingleChoiceParam extends AChoiceParam<SingleChoiceParam> {

  // Optional custom GUI layout specification
  private final @Nullable GuiLayoutSection singleChoiceLayout;

  public SingleChoiceParam(
    String title,
    List<Tuple<Object, ItemStack>> representitives,
    IStdGuiItemProvider itemProvider,
    @Nullable Function<ItemStack, ItemStack> selectionTransform,
    @Nullable GuiLayoutSection searchLayout,
    @Nullable IFilterEnum<?> searchFields,
    @Nullable FilterFunction filter,
    BiConsumer<Object, GuiInstance<SingleChoiceParam>> selected,
    @Nullable Consumer<GuiInstance<SingleChoiceParam>> closed,
    @Nullable Consumer<GuiInstance<SingleChoiceParam>> backButton,
    @Nullable GuiLayoutSection singleChoiceLayout
  ) {
    super(title, representitives, itemProvider, selectionTransform, searchLayout, searchFields, filter, selected, closed, backButton);
    this.singleChoiceLayout = singleChoiceLayout;
  }
}