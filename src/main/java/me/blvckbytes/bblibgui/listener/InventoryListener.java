package me.blvckbytes.bblibgui.listener;

import me.blvckbytes.bblibdi.AutoConstruct;
import me.blvckbytes.bblibdi.AutoInject;
import me.blvckbytes.bblibreflect.IReflectionHelper;
import me.blvckbytes.bblibutil.APlugin;
import me.blvckbytes.bblibutil.Triple;
import me.blvckbytes.bblibutil.Tuple;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.*;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/24/2022

  Listens to inventory changes and translates them into custom
  events for a more convenient way of handling them around the system.

  This program is free software: you can redistribute it and/or modify
  it under the terms of the GNU Affero General Public License as published
  by the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU Affero General Public License for more details.

  You should have received a copy of the GNU Affero General Public License
  along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/
@AutoConstruct
public class InventoryListener implements Listener {

  // List of smeltable items
  private static final List<Material> smeltable;

  static {
    smeltable = new ArrayList<>();

    Iterator<Recipe> iter = Bukkit.recipeIterator();
    while (iter.hasNext()) {
      Recipe recipe = iter.next();

      if (recipe instanceof FurnaceRecipe)
        smeltable.add(((FurnaceRecipe) recipe).getInput().getType());
    }
  }

  private final APlugin plugin;
  private final IReflectionHelper reflection;

  public InventoryListener(
    @AutoInject APlugin plugin,
    @AutoInject IReflectionHelper reflection
  ) {
    this.plugin = plugin;
    this.reflection = reflection;
  }

  @EventHandler
  public void onClick(InventoryClickEvent e) {
    if (!(e.getWhoClicked() instanceof Player))
      return;

    Player p = (Player) e.getWhoClicked();
    Inventory clickedInventory = (
      // Check whether the raw slot is bigger than the inventory clicked, which
      // will always be the top inventory when having two inventories open. If so,
      // take the players inventory as the clicked inventory.
      e.getRawSlot() >= e.getInventory().getSize() ?
      p.getInventory() : e.getInventory()
    );

    // Clicked into void
    if (clickedInventory == null)
      return;

    int clickedSlot = e.getSlot();

    // Swapped slot contents using hotbar keys
    if (
      e.getAction() == InventoryAction.HOTBAR_SWAP ||
      e.getAction() == InventoryAction.HOTBAR_MOVE_AND_READD
    ) {

      ItemStack hotbar = e.getHotbarButton() >= 0 ? p.getInventory().getItem(e.getHotbarButton()) : null;
      ItemStack target = clickedInventory.getItem(clickedSlot);

      // Swapped two items
      if (hotbar != null && target != null) {
        // Moved around only in their own inventory
        if (p.getInventory().equals(clickedInventory)) {
          if (cvB(checkCancellation(p.getInventory(), p.getInventory(), e.getClickedInventory(), p, ManipulationAction.SWAP, e.getHotbarButton(), clickedSlot, clickedSlot, e.getClick())))
            e.setCancelled(true);
        }

        else {
          if (cvB(checkCancellation(p.getInventory(), clickedInventory, e.getClickedInventory(), p, ManipulationAction.SWAP, e.getHotbarButton(), clickedSlot, clickedSlot, e.getClick())))
            e.setCancelled(true);
        }
      }

      // Moved into hotbar
      else if (hotbar == null && target != null) {
        if (cvB(checkCancellation(clickedInventory, p.getInventory(), e.getClickedInventory(), p, ManipulationAction.MOVE, clickedSlot, e.getHotbarButton(), e.getHotbarButton(), e.getClick())))
          e.setCancelled(true);
      }

      // Moved into foreign
      else if (hotbar != null) {
        if (cvB(checkCancellation(p.getInventory(), clickedInventory, e.getClickedInventory(), p, ManipulationAction.MOVE, e.getHotbarButton(), clickedSlot, clickedSlot, e.getClick())))
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
      if (!top.equals(clickedInventory)) {
        from = p.getInventory();
        to = top;
      }

      // Moved down into own inventory
      else {
        from = top;
        to = p.getInventory();
      }

      ItemStack item = from.getItem(clickedSlot);
      if (item == null)
        return;

      Set<Integer> targetSlots = determineMoveSlots(item, to);

      // No more space to move into
      if (targetSlots.size() == 0)
        return;

      // TODO: Implement an ignore mode for each of the target slot's checks

      if (targetSlots.stream().anyMatch(slot -> checkCancellation(from, to, e.getClickedInventory(), p, ManipulationAction.MOVE, clickedSlot, slot, clickedSlot, e.getClick())))
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
      Inventory top = e.getView().getTopInventory();

      Set<Integer> slotsOwn = new HashSet<>(), slotsTop = new HashSet<>();

      // Clicked within the top inventory, add that slot
      if (clickedInventory.equals(top))
        slotsTop.add(clickedSlot);

      // Must be the own inventory
      else
        slotsOwn.add(clickedSlot);

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

      boolean cancel = false;
      int totalTop = slotsTop.size(), totalOwn = slotsOwn.size();

      List<Triple<Inventory, Integer, ItemStack>> ignoreBackup = new ArrayList<>();

      int c = 0;
      for (int slot : slotsTop) {
        Boolean mode = checkCancellation(top, e.getClickedInventory(), e.getClickedInventory(), p, action, slot, clickedSlot, clickedSlot, e.getClick(), ++c, totalTop);

        // Ignore this field
        if (mode == null) {
          ignoreBackup.add(new Triple<>(top, slot, new ItemStack(top.getItem(slot))));
          continue;
        }

        if (mode) {
          cancel = true;
          break;
        }
      }

      c = 0;
      for (int slot : slotsOwn) {
        Boolean mode = checkCancellation(p.getInventory(), e.getClickedInventory(), e.getClickedInventory(), p, action, slot, clickedSlot, clickedSlot, e.getClick(), ++c, totalOwn);

        // Ignore this field
        if (mode == null) {
          ignoreBackup.add(new Triple<>(p.getInventory(), slot, new ItemStack(p.getInventory().getItem(slot))));
          continue;
        }

        if (mode) {
          cancel = true;
          break;
        }
      }

      if (cancel)
        e.setCancelled(true);

      // Ignore backup list is populated
      if (action == ManipulationAction.COLLECT && ignoreBackup.size() > 0) {

        // Nothing to do, the cursor cannot possibly have gathered any items out of this slot
        if (p.getItemOnCursor() != null && p.getItemOnCursor().getAmount() == p.getItemOnCursor().getMaxStackSize())
          return;

        // Only tried to collect the ignored target slot itself, do nothing
        if (slotsTop.contains(clickedSlot) && slotsTop.size() == 1 && slotsOwn.size() == 0)
          return;

        // On next tick (right after the action)
        plugin.runTask(() -> {
          // Restore field by field and keep track of how many items were taken
          int ignoreSubtract = 0;
          for (Triple<Inventory, Integer, ItemStack> ignoreSlot : ignoreBackup) {
            ignoreSlot.getA().setItem(ignoreSlot.getB(), ignoreSlot.getC());
            ignoreSubtract += ignoreSlot.getC().getAmount();
          }

          List<Tuple<? extends Inventory, Integer>> slots = Stream.concat(
            // Only affected slots from the top inventory
            slotsTop.stream().map(s -> new Tuple<>(top, s)),
            // All slots from the player's inventory
            IntStream.range(0, p.getInventory().getStorageContents().length)
              .mapToObj(s -> new Tuple<>(p.getInventory(), s))
          )
            // Filter out slots which were ignore-backuped
            .filter(t -> ignoreBackup.stream().noneMatch(t2 -> (
              // Same inventory and same slot
              t.getA().equals(t2.getA()) && t.getB().equals(t2.getB()))
            ))
            .collect(Collectors.toList());

          // Iterate all slots and try to subtract the ignore-amount again
          for (Tuple<? extends Inventory, Integer> slotT : slots) {
            Inventory slotInv = slotT.getA();
            int slot = slotT.getB();

            if (ignoreSubtract <= 0)
              break;

            // Item unusable for subtraction
            ItemStack curr = slotInv.getItem(slot);
            if (curr == null || !curr.isSimilar(p.getItemOnCursor()))
              continue;

            int rem = Math.min(ignoreSubtract, curr.getAmount());
            ignoreSubtract -= rem;

            if (rem == curr.getAmount()) {
              slotInv.setItem(slot, null);
              continue;
            }

            curr.setAmount(curr.getAmount() - rem);
          }

          // Found enough to subtract elsewhere
          if (ignoreSubtract <= 0)
            return;

          // Subtract remaining from cursor (should never happen!)
          ItemStack cursor = p.getItemOnCursor();
          cursor.setAmount(cursor.getAmount() - ignoreSubtract);
          p.setItemOnCursor(cursor);
        });
      }

      return;
    }

    // Placed down any number of items
    if (
      e.getAction() == InventoryAction.PLACE_ALL ||
        e.getAction() == InventoryAction.PLACE_ONE ||
        e.getAction() == InventoryAction.PLACE_SOME
    ) {
      if (cvB(checkCancellation(clickedInventory, e.getClickedInventory(), p, ManipulationAction.PLACE, clickedSlot, clickedSlot, e.getClick())))
        e.setCancelled(true);
      return;
    }

    // Swapped a slot with the cursor contents
    if (e.getAction() == InventoryAction.SWAP_WITH_CURSOR) {
      if (cvB(checkCancellation(clickedInventory, e.getClickedInventory(), p, ManipulationAction.SWAP, clickedSlot, clickedSlot, e.getClick())))
        e.setCancelled(true);
      return;
    }

    // Dropped any number of items from a slot
    if (
      e.getAction() == InventoryAction.DROP_ONE_SLOT ||
      e.getAction() == InventoryAction.DROP_ALL_SLOT
    ) {
      if (cvB(checkCancellation(clickedInventory, e.getClickedInventory(), p, ManipulationAction.DROP, clickedSlot, clickedSlot, e.getClick())))
        e.setCancelled(true);
      return;
    }

    if (cvB(checkCancellation(clickedInventory, e.getClickedInventory(), p, ManipulationAction.CLICK, clickedSlot, clickedSlot, e.getClick())))
      e.setCancelled(true);
  }

  @EventHandler
  public void onDrag(InventoryDragEvent e) {
    if (!(e.getWhoClicked() instanceof Player))
      return;

    Player p = (Player) e.getWhoClicked();
    Inventory top = p.getOpenInventory().getTopInventory();

    Integer[] slots = new Integer[e.getRawSlots().size()];
    e.getRawSlots().toArray(slots);

    /*
      HACK Description
      The (only) origin of this event, Container.java from NMS source, saves the
      old cursor and applies the drag reduction to it *before* calling this event.
      Then, if the event is cancelled, that same old cursor will be restored, the
      event-cursor (setCursor) is only applied if the event is allowed. This in effect
      doesn't allow for any proper cursor modifications during the event, which
      is inacceptable for this library. Consumers of the InventoryManipulationEvent
      should see the true cursor without any modifications (yet) and be able to modify
      it at will. If the event is cancelled, there are no reductions anyways, and the
      modified cursor is being forced by re-writing it on the next tick. If the event
      is not cancelled, the reduced cursor is re-applied.
     */

    // HACK: Part 1
    // Undo cursor modifications by event origin
    p.setItemOnCursor(e.getOldCursor());

    // Check whether this drag event needs to be cancelled by firing an individual
    // place event for each slot (because that's what in effect occurs). If any event
    // receiver cancels any of the slots, the whole drag event needs to be cancelled.

    boolean cancel = false;
    for (int i = 0; i < slots.length; i++) {
      int slot = slots[i];

      // This slot didn't affect the top inventory, always permit
      if (slot >= top.getSize())
        continue;

      // Not a cancel cause
      if (!cvB(checkCancellation(e.getInventory(), e.getInventory(), p, ManipulationAction.PLACE, slot, slots[0], ClickType.RIGHT, i + 1, slots.length)))
        continue;

      // Cancels, stop iterating
      cancel = true;
      break;
    }

    if (cancel) {
      e.setCancelled(true);

      // HACK: Part 2
      // Cache the cursor now end restore it right after the event ends
      ItemStack cursorCache = p.getItemOnCursor();
      plugin.runTask(() -> p.setItemOnCursor(cursorCache));
    }

    // HACK: Part 3
    // Restore to the reduced state again if the event hasn't been cancelled
    else
      p.setItemOnCursor(e.getCursor());
  }

  /**
   * Convert a nullable boolean's null state to true
   */
  private boolean cvB(@Nullable Boolean input) {
    return input == null || input;
  }

  /**
   * Check whether the expressed action has been cancelled by any event receiver
   *
   * @param inv    Inventory of action
   * @param clickedInv    Inventory that has been actively clicked
   * @param p      Event causing player
   * @param action Action that has been taken
   * @param slot   Slot of action
   * @param clickedSlot Slot that has been actively clicked
   * @param click    Type of click
   * @return True if the action needs to be cancelled
   */
  private @Nullable Boolean checkCancellation(Inventory inv, Inventory clickedInv, Player p, ManipulationAction action, int slot, int clickedSlot, ClickType click) {
    return checkCancellation(inv, inv, clickedInv, p, action, slot, slot, clickedSlot, click, 1, 1);
  }

  /**
   * Check whether the expressed action has been cancelled by any event receiver
   *
   * @param inv    Inventory of action
   * @param clickedInv    Inventory that has been actively clicked
   * @param p      Event causing player
   * @param action Action that has been taken
   * @param slot   Slot of action
   * @param clickedSlot Slot that has been actively clicked
   * @param click    Type of click
   * @param sequenceId    Sequence ID
   * @param sequenceTotal Total number of sequence items
   * @return True if the action needs to be cancelled
   */
  private @Nullable Boolean checkCancellation(Inventory inv, Inventory clickedInv, Player p, ManipulationAction action, int slot, int clickedSlot, ClickType click, int sequenceId, int sequenceTotal) {
    return checkCancellation(inv, inv, clickedInv, p, action, slot, slot, clickedSlot, click, sequenceId, sequenceTotal);
  }

  /**
   * Check whether the expressed action has been cancelled by any event receiver
   *
   * @param fromInv  Inventory that has been taken from
   * @param toInv    Inventory that has been added to
   * @param clickedInv    Inventory that has been actively clicked
   * @param p        Event causing player
   * @param action   Action that has been taken
   * @param fromSlot Slot that has been taken from
   * @param toSlot   Slot that has been added to
   * @param clickedSlot Slot that has been actively clicked
   * @param click    Type of click
   * @return True if the action needs to be cancelled
   */
  private @Nullable Boolean checkCancellation(Inventory fromInv, Inventory toInv, Inventory clickedInv, Player p, ManipulationAction action, int fromSlot, int toSlot, int clickedSlot, ClickType click) {
    return checkCancellation(fromInv, toInv, clickedInv, p, action, fromSlot, toSlot, clickedSlot, click, 1, 1);
  }

  /**
   * Check whether the expressed action has been cancelled by any event receiver
   *
   * @param fromInv  Inventory that has been taken from
   * @param toInv    Inventory that has been added to
   * @param clickedInv    Inventory that has been actively clicked
   * @param p        Event causing player
   * @param action   Action that has been taken
   * @param fromSlot Slot that has been taken from
   * @param toSlot   Slot that has been added to
   * @param clickedSlot Slot that has been actively clicked
   * @param click    Type of click
   * @param sequenceId    Sequence ID
   * @param sequenceTotal Total number of sequence items
   * @return True if the action needs to be cancelled
   */
  private @Nullable Boolean checkCancellation(Inventory fromInv, Inventory toInv, Inventory clickedInv, Player p, ManipulationAction action, int fromSlot, int toSlot, int clickedSlot, ClickType click, int sequenceId, int sequenceTotal) {
    InventoryManipulationEvent ime = new InventoryManipulationEvent(
      fromInv, toInv, clickedInv, p, action, fromSlot, toSlot, clickedSlot, click, sequenceId, sequenceTotal
    );

    Bukkit.getPluginManager().callEvent(ime);
    return ime.getCancelled();
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
      else if (reflection.getBurnTime(item.getType()).isPresent())
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
