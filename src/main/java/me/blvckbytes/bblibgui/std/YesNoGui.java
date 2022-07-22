package me.blvckbytes.bblibgui.std;

import me.blvckbytes.bblibconfig.ConfigValue;
import me.blvckbytes.bblibgui.*;
import me.blvckbytes.bblibgui.param.YesNoParam;
import me.blvckbytes.bblibpackets.IFakeItemCommunicator;
import me.blvckbytes.bblibutil.APlugin;
import me.blvckbytes.bblibdi.AutoConstruct;
import me.blvckbytes.bblibdi.AutoInject;
import me.blvckbytes.bblibutil.TriResult;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 06/30/2022

  Offers the viewer a chance to either choose yes or no.
*/
@AutoConstruct
public class YesNoGui extends AGui<YesNoParam> {

  // Players which made a selection in the GUI don't trigger the callback on close
  private final Set<Player> madeSelection;

  public YesNoGui(
    @AutoInject APlugin plugin,
    @AutoInject IFakeItemCommunicator fakeItemCommunicator
  ) {
    super(3, "", i -> (
      ConfigValue.immediate(i.getArg().getType())
    ), plugin, fakeItemCommunicator);

    this.madeSelection = new HashSet<>();
  }

  @Override
  protected boolean closed(GuiInstance<YesNoParam> inst) {
    if (!madeSelection.remove(inst.getViewer()))
      inst.getArg().getChoice().accept(TriResult.EMPTY, inst);
    return false;
  }

  @Override
  protected boolean opening(GuiInstance<YesNoParam> inst) {
    Player p = inst.getViewer();
    IStdGuiItemProvider itemProvider = inst.getArg().getItemProvider();
    GuiLayoutSection layout = inst.getArg().getLayout();

    inst.applyLayoutParameters(layout);

    Map<String, String> slots = layout != null ? layout.getSlots() : new HashMap<>();

    // Render the back button, if a callback has been set
    if (inst.getArg().getBackButton() != null) {
      inst.fixedItem(
        slots.getOrDefault("back", "18"),
        () -> itemProvider.getItem(StdGuiItem.BACK, null),
        e -> {
          madeSelection.add(p);
          inst.getArg().getBackButton().accept(inst);
        }, null
      );
    }

    // Yes button
    inst.fixedItem(
      slots.getOrDefault("yes", "11"),
      () -> inst.getArg().getYesButton(), e -> {
        madeSelection.add(p);
        inst.getArg().getChoice().accept(TriResult.SUCC, inst);
      },
      null
    );

    // No button
    inst.fixedItem(
      slots.getOrDefault("no", "15"),
      () -> inst.getArg().getNoButton(),
      e -> {
        madeSelection.add(p);
        inst.getArg().getChoice().accept(TriResult.ERR, inst);
      },
      null
    );

    return true;
  }
}
