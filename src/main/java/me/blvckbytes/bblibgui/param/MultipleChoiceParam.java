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
  Created On: 06/30/2022

  The parameter used to open a new multiple choice GUI.

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
public class MultipleChoiceParam extends AChoiceParam<MultipleChoiceParam> {

  // Optional custom GUI layout specification
  private final @Nullable GuiLayoutSection singleChoiceLayout, multipleChoiceLayout;

  public MultipleChoiceParam(
    String title,
    List<Tuple<Object, ItemStack>> representitives,
    IStdGuiItemProvider itemProvider,
    @Nullable Function<ItemStack, ItemStack> selectionTransform,
    @Nullable GuiLayoutSection searchLayout,
    @Nullable IFilterEnum<?> searchFields,
    @Nullable FilterFunction filter,
    BiConsumer<Object, GuiInstance<MultipleChoiceParam>> selected,
    @Nullable Consumer<GuiInstance<MultipleChoiceParam>> closed,
    @Nullable Consumer<GuiInstance<MultipleChoiceParam>> backButton,
    @Nullable GuiLayoutSection singleChoiceLayout,
    @Nullable GuiLayoutSection multipleChoiceLayout
  ) {
    super(title, representitives, itemProvider, selectionTransform, searchLayout, searchFields, filter, selected, closed, backButton);
    this.singleChoiceLayout = singleChoiceLayout;
    this.multipleChoiceLayout = multipleChoiceLayout;
  }
}
