package me.blvckbytes.bblibgui.param;

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.blvckbytes.bblibgui.*;
import me.blvckbytes.bblibutil.IEnum;
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
*/
@Getter
@AllArgsConstructor
public abstract class AChoiceParam<T> implements IAnvilGuiParam<T> {

  //////////////////////// Layout & Items ////////////////////////

  // GUI title
  private final String title;

  // List of choices, objects represented by itemstacks
  private final List<Tuple<Object, ItemStack>> representitives;

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
