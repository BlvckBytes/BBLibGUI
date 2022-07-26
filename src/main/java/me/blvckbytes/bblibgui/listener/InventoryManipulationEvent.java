package me.blvckbytes.bblibgui.listener;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/24/2022

  Represents a pre-analyzed change in an inventory.

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
@Getter
public class InventoryManipulationEvent extends Event {

  private static final HandlerList HANDLERS = new HandlerList();

  // Origin has always been taken from and target inventory is where the items are taken to
  // If an action only affects one slot, origin will be equal to target
  private final Inventory originInventory, targetInventory;
  private final @Nullable Inventory clickedInventory;

  private final Player player;
  private final ManipulationAction action;
  private final int originSlot, targetSlot, clickedSlot;
  private final ClickType click;
  private final int sequenceId, sequenceTotal;

  @Setter
  private boolean cancelled;

  public InventoryManipulationEvent(
    Inventory originInventory,
    Inventory targetInventory,
    @Nullable Inventory clickedInventory,
    Player player,
    ManipulationAction action,
    int originSlot,
    int targetSlot,
    int clickedSlot,
    ClickType click,
    int sequenceId,
    int sequenceTotal
  ) {
    this.originInventory = originInventory;
    this.targetInventory = targetInventory;
    this.clickedInventory = clickedInventory;
    this.player = player;
    this.action = action;
    this.originSlot = originSlot;
    this.targetSlot = targetSlot;
    this.clickedSlot = clickedSlot;
    this.click = click;
    this.sequenceId = sequenceId;
    this.sequenceTotal = sequenceTotal;
    this.cancelled = false;
  }

  /**
   * Get the non-zero-based pressed hotbar key, if any
   * @return Horbar key if pressed
   */
  public Optional<Integer> getHotbarKey() {
    // -1 means Offhand, an alternative hotbar slot
    if (
      action == ManipulationAction.MOVE &&
        (
          // Move into offhand
          (targetInventory == player.getInventory() && targetSlot == -1) ||
          // Move from offhand
          (originInventory == player.getInventory() && originSlot == -1)
        )
    )
      return Optional.of(-1);

    // No number key has been pressed
    if (click != ClickType.NUMBER_KEY)
      return Optional.empty();

    // Swapped items between hotbar and target slot
    // Hotbar slot will always be origin, by definition
    if (action == ManipulationAction.SWAP)
      return Optional.of(originSlot + 1);

    // Moved either from the hotbar into the inv or the other way around
    if (action == ManipulationAction.MOVE) {
      // Moved from their own inventory, thus originSlot is the hotbar key
      if (originInventory.equals(player.getInventory()))
        return Optional.of(originSlot + 1);

      // Moved into their hotbar, thus target is the hotbar slot
      else
        return Optional.of(targetSlot + 1);
    }

    return Optional.empty();
  }

  /**
   * Checks whether this event is the last call of a batch-call
   */
  public boolean isLastOfBatch() {
    return sequenceId == sequenceTotal;
  }

  @Override
  public HandlerList getHandlers() {
    return HANDLERS;
  }

  public static HandlerList getHandlerList() {
    return HANDLERS;
  }

  @Override
  public String toString() {
    return (
      "InventoryManipulationEvent (\n" +
      "  player=" + player.getName() + "\n" +
      "  originSlot=" + originSlot + "\n" +
      "  targetSlot=" + targetSlot + "\n" +
      "  clickedSlot=" + clickedSlot + "\n" +
      "  action=" + action + "\n" +
      "  originInventoryHolder=" + originInventory.getHolder() + "\n" +
      "  targetInventoryHolder=" + targetInventory.getHolder() + "\n" +
      "  clickedInventoryHolder=" + (clickedInventory == null ? null : clickedInventory.getHolder()) + "\n" +
      "  sequence=" + sequenceId + "/" + sequenceTotal + "\n" +
      "  horbarKey=" + getHotbarKey().orElse(null) + "\n" +
      ")"
    );
  }
}
