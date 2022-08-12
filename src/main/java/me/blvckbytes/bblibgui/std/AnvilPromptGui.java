package me.blvckbytes.bblibgui.std;

import me.blvckbytes.bblibconfig.ConfigValue;
import me.blvckbytes.bblibconfig.ItemStackBuilder;
import me.blvckbytes.bblibgui.*;
import me.blvckbytes.bblibgui.param.AnvilPromptParam;
import me.blvckbytes.bblibreflect.IFakeItemCommunicator;
import me.blvckbytes.bblibreflect.IPacketInterceptor;
import me.blvckbytes.bblibreflect.MCReflect;
import me.blvckbytes.bblibutil.APlugin;
import me.blvckbytes.bblibdi.AutoConstruct;
import me.blvckbytes.bblibdi.AutoInject;
import me.blvckbytes.bblibutil.Triple;
import me.blvckbytes.bblibutil.Tuple;
import me.blvckbytes.bblibutil.logger.ILogger;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 07/13/2022

  A text prompt anvil GUI which offers an external way to validate input
  and will update the confirmation item in realtime, as the user types.

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
public class AnvilPromptGui extends AAnvilGui<AnvilPromptParam> {

  private final Map<GuiInstance<?>, Triple<Boolean, Object, ItemStack>> inputs;

  public AnvilPromptGui(
    @AutoInject APlugin plugin,
    @AutoInject IPacketInterceptor packetInterceptor,
    @AutoInject MCReflect refl,
    @AutoInject ILogger logger,
    @AutoInject IFakeItemCommunicator fakeItemCommunicator
  ) {
    super(plugin, packetInterceptor, refl, logger, fakeItemCommunicator);
    this.inputs = new HashMap<>();
  }

  @Override
  protected void terminated(GuiInstance<AnvilPromptParam> inst) {
    inputs.remove(inst);
  }

  @Override
  protected boolean closed(GuiInstance<AnvilPromptParam> inst) {
    super.closed(inst);
    return false;
  }

  @Override
  protected boolean opening(GuiInstance<AnvilPromptParam> inst) {
    super.opening(inst);

    IStdGuiItemProvider itemProvider = inst.getArg().getItemProvider();
    ItemStack placeholder = itemProvider.getItem(StdGuiItem.PROMPT_PLACEHOLDER, null);

    // Initially call with the placeholder's name, as that will be the initial text value
    onTyping(inst, placeholder.getItemMeta() == null ? "" : placeholder.getItemMeta().getDisplayName());

    // This item serves as a placeholder to get the typing functionality up and working, while
    // it also informes the player that there is a prompt waiting
    inst.fixedItem("0", () -> {
      Triple<Boolean, Object, ItemStack> inpT = inputs.get(inst);
      String inp = (inpT == null || inpT.getB() == null) ? "" : inpT.getB().toString();

      if (inp.isBlank())
        return placeholder;

      // Set the previously entered text as the placeholder's name
      // to resume where typing has been left off
      return new ItemStackBuilder(placeholder, placeholder.getAmount())
        .withName(ConfigValue.immediate(inp))
        .build();
    }, null, null);

    // This item is the "result" and will be rendered after every change
    // of the input, to signal validation
    inst.fixedItem(
      "2",
      () -> getData(inst).getC(),
      e -> {
        if (!e.getClick().isLeftClick())
          return;

        Triple<Boolean, Object, ItemStack> data = getData(inst);

        // Input invalid, skip
        if (!data.getA())
          return;

        madeSelection.add(inst.getViewer());
        inst.getArg().getConfirmed().accept(data.getB(), inst);
      },
      null
    );

    return true;
  }

  @Override
  void onTyping(GuiInstance<AnvilPromptParam> inst, String text) {
    // Normalize input and apply pre-processing
    text = ChatColor.translateAlternateColorCodes('&', text.trim());

    // Try to transform the input, fall back to the string value on exceptions
    Object v = text;
    try {
      v = inst.getArg().getTransformer().apply(text);
    } catch (Exception ignored) {}

    // Run validation on the new input value
    Tuple<Boolean, ItemStack> validation = inst.getArg().getConfirmationButton().apply(v);

    // Store the result and redraw the confirmation button
    inputs.put(inst, new Triple<>(validation.getA(), v, validation.getB()));
    inst.redraw("2");
  }

  /**
   * Get the cached data (validation state, transformed input, confirmation item) by GUI ref
   * @param inst GUI ref
   * @return Cached data or empty defaults
   */
  private Triple<Boolean, Object, ItemStack> getData(GuiInstance<AnvilPromptParam> inst) {
    return inputs.getOrDefault(inst, new Triple<>(false, "", null));
  }
}
