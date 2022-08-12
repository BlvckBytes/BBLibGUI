package me.blvckbytes.bblibgui.param;

import me.blvckbytes.bblibgui.GuiInstance;
import me.blvckbytes.bblibgui.IStdGuiItemProvider;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 07/13/2022

  The smallest required set of parameter properties when
  opening any type of anvil GUI.

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
public interface IAnvilGuiParam<T> {

  // GUI title
  String getTitle();

  // Back button, providing a ref to the GUI about to navigate away from
  Consumer<GuiInstance<T>> getBackButton();

  // Provider for standard parameters used in GUIs
  IStdGuiItemProvider getItemProvider();

  // Inventory close callback, providing a ref to the closed GUI
  @Nullable Consumer<GuiInstance<T>> getClosed();

}
