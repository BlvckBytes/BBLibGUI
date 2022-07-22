package me.blvckbytes.bblibgui;

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.blvckbytes.bblibgui.listener.InventoryManipulationEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Function;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/22/2022

  Represents an item which resides in a managed GUI.
*/
@Getter
@AllArgsConstructor
public class GuiItem {
  // Item supplier function
  private Function<Integer, ItemStack> item;

  // Click event consumer
  private @Nullable Consumer<InventoryManipulationEvent> onClick;

  // How often this item should be updated, in ticks, null means never
  private @Nullable Integer updatePeriod;
}
