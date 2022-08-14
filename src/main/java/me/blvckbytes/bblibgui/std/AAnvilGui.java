package me.blvckbytes.bblibgui.std;

import me.blvckbytes.bblibconfig.ConfigValue;
import me.blvckbytes.bblibconfig.IItemBuilderFactory;
import me.blvckbytes.bblibgui.AGui;
import me.blvckbytes.bblibgui.GuiInstance;
import me.blvckbytes.bblibgui.StdGuiItem;
import me.blvckbytes.bblibgui.param.IAnvilGuiParam;
import me.blvckbytes.bblibreflect.*;
import me.blvckbytes.bblibreflect.communicator.SetSlotCommunicator;
import me.blvckbytes.bblibutil.APlugin;
import me.blvckbytes.bblibutil.logger.ILogger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 07/13/2022

  The base of every anvil GUI, providing a typing hook as well as
  the required closing and back button handling.

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
public abstract class AAnvilGui<T extends IAnvilGuiParam<T>> extends AGui<T> implements IPacketModifier {

  // Whether the player has made a selection yet
  protected final Set<Player> madeSelection;

  private final Class<?> C_PO_ITEM_NAME;
  private final Field F_PO_ITEM_NAME__NAME;

  protected AAnvilGui(
    APlugin plugin,
    ILogger logger,
    IReflectionHelper reflection,
    SetSlotCommunicator slotCommunicator,
    IPacketInterceptor packetInterceptor,
    IItemBuilderFactory builderFactory
  ) throws Exception {
    super(1, "", i -> (
      ConfigValue.immediate(i.getArg().getTitle())
    ), InventoryType.ANVIL, plugin, logger, reflection, slotCommunicator, packetInterceptor, builderFactory);

    this.madeSelection = new HashSet<>();

    C_PO_ITEM_NAME = requireClass(RClass.PACKET_I_ITEM_NAME);
    F_PO_ITEM_NAME__NAME = requireScalarField(C_PO_ITEM_NAME, String.class, 0, false, false, null);

    packetInterceptor.register(this, ModificationPriority.LOW);
  }

  //=========================================================================//
  //                                    API                                  //
  //=========================================================================//

  /**
   * Called whenever the player types within the anvil GUI
   * @param inst Instance ref
   * @param text Typed text
   */
  abstract void onTyping(GuiInstance<T> inst, String text);

  @Override
  protected boolean closed(GuiInstance<T> inst) {
    Consumer<GuiInstance<T>> closed = inst.getArg().getClosed();
    if (!madeSelection.remove(inst.getViewer()) && closed != null)
      closed.accept(inst);
    return false;
  }

  @Override
  protected boolean opening(GuiInstance<T> inst) {

    // Back button
    if (inst.getArg().getBackButton() != null) {
      inst.fixedItem(
        "1",
        () -> inst.getArg().getItemProvider().getItem(StdGuiItem.BACK, null),
        e -> {
          madeSelection.add(inst.getViewer());
          inst.getArg().getBackButton().accept(inst);
        }, null
      );
    }

    // Fallback is that the third slot is empty
    inst.fixedItem("2", () -> null, null, null);

    return true;
  }

  //=========================================================================//
  //                                Interceptor                              //
  //=========================================================================//

  @Override
  public Object modifyIncoming(IPacketReceiver receiver, Object incoming) {
    UUID u = receiver.getUuid();

    // Not a player
    if (u == null)
      return incoming;

    Player p = Bukkit.getPlayer(u);

    // Not the target packet
    if (p == null || !C_PO_ITEM_NAME.isInstance(incoming))
      return incoming;

    GuiInstance<T> inst = getActiveInstances().get(p);
    if (inst == null)
      return incoming;

    try {
      // Synchronize from async packet thread
      String text = ((String) F_PO_ITEM_NAME__NAME.get(incoming)).trim();
      plugin.runTask(() -> onTyping(inst, text));
    } catch (Exception e) {
      logger.logError(e);
    }

    // Drop the packet
    return null;
  }

  @Override
  public Object modifyOutgoing(IPacketReceiver receiver, Object outgoing) {
    return outgoing;
  }
}
