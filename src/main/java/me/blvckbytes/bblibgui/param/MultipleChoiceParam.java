package me.blvckbytes.bblibgui.param;

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.blvckbytes.bblibgui.FilterFunction;
import me.blvckbytes.bblibgui.GuiInstance;
import me.blvckbytes.bblibgui.GuiLayoutSection;
import me.blvckbytes.bblibgui.IStdGuiItemProvider;
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
  Created On: 06/30/2022

  The parameter used to open a new multiple choice GUI.
*/
@Getter
@AllArgsConstructor
public class MultipleChoiceParam {
  // GUI title
  private String title;

  // List of choices, objects represented by itemstacks
  private List<Tuple<Object, ItemStack>> representitives;

  // Provider for standard parameters used in GUIs
  private IStdGuiItemProvider itemProvider;

  // Optional custom GUI layout specification
  // Available slots: prevPage, currPage, nextPage, back, submit, newChoice
  private @Nullable GuiLayoutSection layout;

  // Optional custom GUI layout specification
  // Available slots: See SingleChoiceParam
  private @Nullable GuiLayoutSection choiceLayout;

  // Optional custom GUI layout specification
  // Available slots: See SingleChoiceParam
  private @Nullable GuiLayoutSection searchLayout;

  // Used to transform selected items
  private @Nullable Function<ItemStack, ItemStack> selectionTransform;

  // Available fields for filtering
  private IEnum<?> searchFields;

  // Custom external filtering function
  private FilterFunction customFilter;

  // Selection callback, provides the bound object and the GUI ref
  private BiConsumer<List<Object>, GuiInstance<MultipleChoiceParam>> selected;

  // Inventory close callback, providing a ref to the closed GUI
  private @Nullable Consumer<GuiInstance<MultipleChoiceParam>> closed;

  // Back button, providing a ref to the GUI about to navigate away from
  private @Nullable Consumer<GuiInstance<MultipleChoiceParam>> backButton;
}
