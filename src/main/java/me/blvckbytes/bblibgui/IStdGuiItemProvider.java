package me.blvckbytes.bblibgui;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 07/03/2022

  Provides all available standard GUI items with templating abilities.
*/
public interface IStdGuiItemProvider {

  /**
   * Get a standard GUI item
   * @param item Type of item
   * @param variables Variables to use on the item template
   */
  ItemStack getItem(StdGuiItem item, @Nullable Map<String, String> variables);

}
