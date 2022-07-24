package me.blvckbytes.bblibgui;

import me.blvckbytes.bblibutil.Tuple;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 07/21/2022

  Represents a custom filter function for anvil search GUIs.
*/
@FunctionalInterface
public interface FilterFunction {

  /**
   * Apply the current search parameters to a list of items
   * @param search Search string
   * @param filter Current field filter
   * @param items List of items to filter
   * @return Filtered items
   */
  List<Tuple<Object, ItemStack>> apply(String search, IFilterEnum<?> filter, List<Tuple<Object, ItemStack>> items);

}
