package me.blvckbytes.bblibgui.listener;

import me.blvckbytes.bblibdi.AutoConstruct;
import me.blvckbytes.bblibdi.AutoInject;
import me.blvckbytes.bblibpackets.MCReflect;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.*;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/24/2022

  Listens to inventory changes and translates them into custom
  events for a more convenient way of handling them around the system.
*/
@AutoConstruct
public class InventoryListener implements Listener {

  // List of smeltable items
  private static final List<Material> smeltable;
  private final MCReflect refl;

  static {
    smeltable = new ArrayList<>();

    Iterator<Recipe> iter = Bukkit.recipeIterator();
    while (iter.hasNext()) {
      Recipe recipe = iter.next();

      if (recipe instanceof FurnaceRecipe)
        smeltable.add(((FurnaceRecipe) recipe).getInput().getType());
    }
  }

  public InventoryListener(
    @AutoInject MCReflect refl
  ) {
    this.refl = refl;
  }

  @EventHandler
  public void onClick(InventoryClickEvent e) {
    if (!(e.getWhoClicked() instanceof Player))
      return;

    Player p = (Player) e.getWhoClicked();

    // Clicked into void
    if (e.getClickedInventory() == null)
      return;

    // Swapped slot contents using hotbar keys
    if (
      e.getAction() == InventoryAction.HOTBAR_SWAP ||
      e.getAction() == InventoryAction.HOTBAR_MOVE_AND_READD
    ) {

      ItemStack hotbar = e.getHotbarButton() >= 0 ? p.getInventory().getItem(e.getHotbarButton()) : null;
      ItemStack target = e.getClickedInventory().getItem(e.getSlot());

      // Swapped two items
      if (hotbar != null && target != null) {
        // Moved around only in their own inventory
        if (p.getInventory().equals(e.getClickedInventory())) {
          if (checkCancellation(p.getInventory(), p.getInventory(), p, ManipulationAction.SWAP, e.getHotbarButton(), e.getSlot(), e.getClick()))
            e.setCancelled(true);
        }

        else {
          if (checkCancellation(p.getInventory(), e.getClickedInventory(), p, ManipulationAction.SWAP, e.getHotbarButton(), e.getSlot(), e.getClick()))
            e.setCancelled(true);
        }
      }

      // Moved into hotbar
      else if (hotbar == null && target != null) {
        if (checkCancellation(e.getClickedInventory(), p.getInventory(), p, ManipulationAction.MOVE, e.getSlot(), e.getHotbarButton(), e.getClick()))
          e.setCancelled(true);
      }

      // Moved into foreign
      else if (hotbar != null) {
        if (checkCancellation(p.getInventory(), e.getClickedInventory(), p, ManipulationAction.MOVE, e.getHotbarButton(), e.getSlot(), e.getClick()))
          e.setCancelled(true);
      }

      // Otherwise, both slots were empty
      return;
    }

    // Moved from one inventory into another
    if (e.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
      Inventory top = e.getView().getTopInventory();

      Inventory from;
      Inventory to;

      // Move up into foreign inventory
      if (!top.equals(e.getClickedInventory())) {
        from = p.getInventory();
        to = top;
      }

      // Moved down into own inventory
      else {
        from = top;
        to = p.getInventory();
      }

      ItemStack item = from.getItem(e.getSlot());
      if (item == null)
        return;

      Set<Integer> targetSlots = determineMoveSlots(item, to);

      // No more space to move into
      if (targetSlots.size() == 0)
        return;

      if (targetSlots.stream().anyMatch(slot -> checkCancellation(from, to, p, ManipulationAction.MOVE, e.getSlot(), slot, e.getClick())))
        e.setCancelled(true);

      return;
    }

    // Picked up any number of items
    if (
      e.getAction() == InventoryAction.PICKUP_ALL ||
      e.getAction() == InventoryAction.PICKUP_HALF ||
      e.getAction() == InventoryAction.PICKUP_ONE ||
      e.getAction() == InventoryAction.PICKUP_SOME ||
      e.getAction() == InventoryAction.COLLECT_TO_CURSOR
    ) {
      Inventory inv = e.getClickedInventory();
      Inventory top = e.getView().getTopInventory();

      Set<Integer> slotsOwn = new HashSet<>(), slotsTop = new HashSet<>();

      // Clicked within the top inventory, add that slot
      if (inv.equals(top))
        slotsTop.add(e.getSlot());

      // Must be the own inventory
      else
        slotsOwn.add(e.getSlot());

      // Collected all similar items to the cursor
      boolean isCollect = e.getAction() == InventoryAction.COLLECT_TO_CURSOR;
      if (isCollect && e.getCursor() != null) {
        ItemStack cursor = e.getCursor();
        int remaining = cursor.getMaxStackSize() - cursor.getAmount();

        // While there's still space on the cursor stack, search for further items and their slots
        // If there's a top inventory, that is always being preferred, and only if there's space after
        // exhausting that top inventory, the bottom (own) inventory is considered

        for (int slot : makeMoveSlotPattern(cursor, top, false, false)) {
          ItemStack curr = top.getItem(slot);

          if (remaining <= 0)
            break;

          if (curr == null || !curr.isSimilar(cursor))
            continue;

          slotsTop.add(slot);
          remaining -= curr.getAmount();
        }

        for (int slot : makeMoveSlotPattern(cursor, p.getInventory(), false, false)) {
          ItemStack curr = p.getInventory().getItem(slot);

          if (remaining <= 0)
            break;

          if (curr == null || !curr.isSimilar(cursor))
            continue;

          slotsOwn.add(slot);
          remaining -= curr.getAmount();
        }
      }

      ManipulationAction action = isCollect ? ManipulationAction.COLLECT : ManipulationAction.PICKUP;

      if (slotsTop.stream().anyMatch(slot -> checkCancellation(top, p, action, slot, e.getClick())))
        e.setCancelled(true);

      if (slotsOwn.stream().anyMatch(slot -> checkCancellation(p.getInventory(), p, action, slot, e.getClick())))
        e.setCancelled(true);

      return;
    }

    // Placed down any number of items
    if (
      e.getAction() == InventoryAction.PLACE_ALL ||
        e.getAction() == InventoryAction.PLACE_ONE ||
        e.getAction() == InventoryAction.PLACE_SOME
    ) {
      if (checkCancellation(e.getClickedInventory(), p, ManipulationAction.PLACE, e.getSlot(), e.getClick()))
        e.setCancelled(true);
      return;
    }

    // Swapped a slot with the cursor contents
    if (e.getAction() == InventoryAction.SWAP_WITH_CURSOR) {
      if (checkCancellation(e.getClickedInventory(), p, ManipulationAction.SWAP, e.getSlot(), e.getClick()))
        e.setCancelled(true);
      return;
    }

    // Dropped any number of items from a slot
    if (
      e.getAction() == InventoryAction.DROP_ONE_SLOT ||
      e.getAction() == InventoryAction.DROP_ALL_SLOT
    ) {
      if (checkCancellation(e.getClickedInventory(), p, ManipulationAction.DROP, e.getSlot(), e.getClick()))
        e.setCancelled(true);
      return;
    }

    if (checkCancellation(e.getClickedInventory(), p, ManipulationAction.CLICK, e.getSlot(), e.getClick()))
      e.setCancelled(true);
  }

  @EventHandler
  public void onDrag(InventoryDragEvent e) {
    if (!(e.getWhoClicked() instanceof Player))
      return;

    Player p = (Player) e.getWhoClicked();
    Inventory top = p.getOpenInventory().getTopInventory();

    // Check whether this drag event needs to be cancelled by firing an individual
    // place event for each slot (because that's what in effect occurs). If any event
    // receiver cancels any of the slots, the whole drag event needs to be cancelled.
    boolean cancel = e.getRawSlots().stream()
      .anyMatch(slot -> {

        // This slot didn't affect the top inventory, always permit
        if (slot >= top.getSize())
          return false;

        return checkCancellation(e.getInventory(), p, ManipulationAction.PLACE, slot, ClickType.RIGHT);
      });

    if (cancel)
      e.setCancelled(true);
  }

  /**
   * Check whether the expressed action has been cancelled by any event receiver
   *
   * @param inv    Inventory of action
   * @param p      Event causing player
   * @param action Action that has been taken
   * @param slot   Slot of action
   * @param click    Type of click
   * @return True if the action needs to be cancelled
   */
  private boolean checkCancellation(Inventory inv, Player p, ManipulationAction action, int slot, ClickType click) {
    return checkCancellation(inv, inv, p, action, slot, slot, click);
  }

  /**
   * Check whether the expressed action has been cancelled by any event receiver
   *
   * @param fromInv  Inventory that has been taken from
   * @param toInv    Inventory that has been added to
   * @param p        Event causing player
   * @param action   Action that has been taken
   * @param fromSlot Slot that has been taken from
   * @param toSlot   Slot that has been added to
   * @param click    Type of click
   * @return True if the action needs to be cancelled
   */
  private boolean checkCancellation(Inventory fromInv, Inventory toInv, Player p, ManipulationAction action, int fromSlot, int toSlot, ClickType click) {
    InventoryManipulationEvent ime = new InventoryManipulationEvent(
      fromInv, toInv, p, action, fromSlot, toSlot, click
    );

    Bukkit.getPluginManager().callEvent(ime);
    return ime.isCancelled();
  }

  /**
   * Tries to create the slot pattern minecraft uses when movingitems around
   * efficiently, either through shift or through collecting items, etc.
   * @param item Item which is involved and can be used to exclude slots which cannot hold it
   * @param to Target inventory
   * @return List of slots in the correct order for further processing
   */
  private List<Integer> makeMoveSlotPattern(@Nullable ItemStack item, Inventory to, boolean rowReverse, boolean colReverse) {
    List<Integer> slots = new ArrayList<>();
    int rows = to.getSize() / 9;

    /*
      The player inventory is handled in row-reverse and has been shifted up
      by a row (while wrapping) so the first row ends up in the hot-bar
      8-0,35-27,26-18,17-9
    */
    if (to instanceof PlayerInventory) {
      for (int row = rowReverse ? rows - 1 : 0; rowReverse && row >= 0 || !rowReverse && row < rows; row += rowReverse ? -1 : 1) {
        for (int slot = colReverse ? row * 9 + 9 - 1 : row * 9; colReverse && slot >= row * 9 || !colReverse && slot < row * 9 + 9; slot += colReverse ? -1 : 1) {
          slots.add((slot + 9) % (9 * 4));
        }
      }
    }

    /*
      Chest inventories are just looped top down, left to right, in natural order
      0-8,9-17,18-26,27-35
    */
    else if (to.getType() == InventoryType.CHEST) {
      for (int row = 0; row < rows; row++) {
        for (int slot = row * 9; slot < row * 9 + 9; slot++) {
          slots.add(slot);
        }
      }
    }

    /*
      Furnaces basically only accept items to smelt or fuel, so there's
      only one possible slot for moves, depending on the material
      0: smelting, 1: power, 2: smelted
    */
    else if (to.getType() == InventoryType.FURNACE) {
      // No item provided, both slots are a possibility
      if (item == null) {
        slots.add(0);
        slots.add(1);
      }

      // Is smeltable and can only go into 0
      else if (smeltable.contains(item.getType()))
        slots.add(0);

      // Is a fuel source and can only go into 1
      else if (refl.getBurnTime(item.getType()).isPresent())
        slots.add(1);
    }

    // Not specifically defined above, just take the slots in the order
    // they appear, which may not always be the case but is sure better
    // than not responding at all
    else {
      for (int i = 0; i < to.getSize(); i++)
        slots.add(i);
    }

    return slots;
  }

  /**
   * Determines the slots into which an item has been moved into after shift
   * click moving it into another inventory
   * @param item The item which will been moved
   * @param to The inventory it will be moved into
   * @return Set of slots that are affected
   */
  private Set<Integer> determineMoveSlots(ItemStack item, Inventory to) {
    Set<Integer> slots = new HashSet<>();
    int firstEmpty = -1;
    int remaining = item.getAmount();

    for (int slot : makeMoveSlotPattern(item, to, true, true)) {
      ItemStack content = to.getItem(slot);

      // Save the index of the first empty slot
      if (firstEmpty < 0 && content == null)
        firstEmpty = slot;

      if (remaining <= 0)
        continue;

      // Empty slot, wouldn't put it here, unless it's the first occurrence (already stored)
      if (content == null)
        continue;

      int contentFree = content.getMaxStackSize() - content.getAmount();

      // Cannot stack with this item
      if (!content.isSimilar(item) || contentFree == 0)
        continue;

      // Put as many items on this stack as possible
      remaining -= contentFree;
      slots.add(slot);
    }

    // No stackable items found, use the first empty slot if it's available
    if (slots.size() == 0 && firstEmpty >= 0)
      slots.add(firstEmpty);

    return slots;
  }
}
