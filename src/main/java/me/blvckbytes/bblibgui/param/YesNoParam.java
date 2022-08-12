package me.blvckbytes.bblibgui.param;

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.blvckbytes.bblibgui.GuiInstance;
import me.blvckbytes.bblibgui.GuiLayoutSection;
import me.blvckbytes.bblibgui.IStdGuiItemProvider;
import me.blvckbytes.bblibutil.TriResult;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 06/30/2022

  The parameter used to open a new yes/no GUI.

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
public class YesNoParam {
  // Type of yes/no to be displayed in the title
  private String type;

  // Provider for standard parameters used in GUIs
  private IStdGuiItemProvider itemProvider;

  // Optional custom GUI layout specification
  private @Nullable GuiLayoutSection layout;

  // Button to display for YES
  private ItemStack yesButton;

  // Button to display for NO
  private ItemStack noButton;

  // Three states: SUCC=yes; ERR=no; EMPTY=inv closed
  private BiConsumer<TriResult, GuiInstance<?>> choice;

  // Back button, providing a ref to the GUI about to navigate away from
  private @Nullable Consumer<GuiInstance<YesNoParam>> backButton;
}
