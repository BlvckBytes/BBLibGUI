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
