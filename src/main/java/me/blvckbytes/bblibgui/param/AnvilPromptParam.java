package me.blvckbytes.bblibgui.param;

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.blvckbytes.bblibgui.GuiInstance;
import me.blvckbytes.bblibgui.IStdGuiItemProvider;
import me.blvckbytes.bblibutil.Tuple;
import me.blvckbytes.bblibutil.UnsafeFunction;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 07/13/2022

  The parameter used to open a new anvil prompt GUI.

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
public class AnvilPromptParam implements IAnvilGuiParam<AnvilPromptParam> {
  // GUI title
  private String title;

  // Back button which provides the GUI ref
  private Consumer<GuiInstance<AnvilPromptParam>> backButton;

  // Value transformer for string inputs
  private UnsafeFunction<String, Object> transformer;

  // Confirmation button, takes the transformed object and returns a tuple
  // of the validation state as well as the confirmation button item
  private Function<Object, Tuple<Boolean, ItemStack>> confirmationButton;

  // Provider for standard parameters used in GUIs
  private IStdGuiItemProvider itemProvider;

  // Confirmation callback, provides the transformed object and the GUI ref
  private BiConsumer<Object, GuiInstance<AnvilPromptParam>> confirmed;

  // Callback to run when closing the GUI, providing the GUI ref
  private @Nullable Consumer<GuiInstance<AnvilPromptParam>> closed;
}
