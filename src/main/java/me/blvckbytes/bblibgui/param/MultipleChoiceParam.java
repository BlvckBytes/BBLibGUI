package me.blvckbytes.bblibgui.param;

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
public class MultipleChoiceParam extends AChoiceParam<MultipleChoiceParam> {

  // Optional custom GUI layout specification
  private final @Nullable GuiLayoutSection singleChoiceLayout, multipleChoiceLayout;

  public MultipleChoiceParam(
    String title,
    List<Tuple<Object, ItemStack>> representitives,
    IStdGuiItemProvider itemProvider,
    @Nullable Function<ItemStack, ItemStack> selectionTransform,
    @Nullable GuiLayoutSection searchLayout,
    IEnum<?> searchFields,
    FilterFunction filter,
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
