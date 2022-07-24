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