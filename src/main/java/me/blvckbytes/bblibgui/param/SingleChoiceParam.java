package me.blvckbytes.bblibgui.param;

import lombok.AllArgsConstructor;
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
*/
@Getter
@AllArgsConstructor
public class SingleChoiceParam implements IAnvilGuiParam<SingleChoiceParam> {
  // GUI title
  private String title;

  // List of choices, objects represented by itemstacks
  private List<Tuple<Object, ItemStack>> representitives;

  // Provider for standard parameters used in GUIs
  private IStdGuiItemProvider itemProvider;

  // Optional custom GUI layout specification
  private @Nullable GuiLayoutSection layout, searchLayout;

  // Used to transform selected items
  private @Nullable Function<ItemStack, ItemStack> selectionTransform;

  // Available fields for filtering
  private Class<? extends Enum<?>> searchFields;

  // Custom external filtering function
  private @Nullable FilterFunction customFilter;

  // Selection callback, provides the bound object and the GUI ref
  private BiConsumer<Object, GuiInstance<SingleChoiceParam>> selected;

  // Inventory close callback, providing a ref to the closed GUI
  private @Nullable Consumer<GuiInstance<SingleChoiceParam>> closed;

  // Back button, providing a ref to the GUI about to navigate away from
  private @Nullable Consumer<GuiInstance<SingleChoiceParam>> backButton;
}