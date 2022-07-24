package me.blvckbytes.bblibgui.std;

import me.blvckbytes.bblibconfig.ConfigValue;
import me.blvckbytes.bblibgui.*;
import me.blvckbytes.bblibgui.param.SingleChoiceParam;
import me.blvckbytes.bblibpackets.IFakeItemCommunicator;
import me.blvckbytes.bblibutil.APlugin;
import me.blvckbytes.bblibdi.AutoConstruct;
import me.blvckbytes.bblibdi.AutoInject;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/29/2022

  Allows the user to choose one of multiple paged choices, each represented
  by an itemstack with a corresponding bound object.
*/
@AutoConstruct
public class SingleChoiceGui extends AGui<SingleChoiceParam> {

  // Players which have chosen already
  private final Map<GuiInstance<?>, Object> haveChosen;

  private final AnvilSearchGui searchGui;

  public SingleChoiceGui(
    @AutoInject APlugin plugin,
    @AutoInject AnvilSearchGui searchGui,
    @AutoInject IFakeItemCommunicator fakeItemCommunicator
  ) {
    super(5, "10-16,19-25,28-34", i -> (
      ConfigValue.immediate(i.getArg().getTitle())
    ), plugin, fakeItemCommunicator);

    this.haveChosen = new HashMap<>();
    this.searchGui = searchGui;
  }

  @Override
  protected void terminated(GuiInstance<SingleChoiceParam> inst) {
    haveChosen.remove(inst);
  }

  @Override
  protected boolean closed(GuiInstance<SingleChoiceParam> inst) {
    if (!haveChosen.containsKey(inst)) {
      Consumer<GuiInstance<SingleChoiceParam>> closed = inst.getArg().getClosed();
      if (closed != null)
        closed.accept(inst);
    }
    return false;
  }

  @Override
  protected boolean opening(GuiInstance<SingleChoiceParam> inst) {
    Player p = inst.getViewer();
    SingleChoiceParam arg = inst.getArg();
    IStdGuiItemProvider itemProvider = arg.getItemProvider();
    GuiLayoutSection layout = arg.getSingleChoiceLayout();

    inst.applyLayoutParameters(layout);

    Map<String, String> slots = layout != null ? layout.getSlots() : new HashMap<>();

    inst.addPagination(
      slots.getOrDefault("prevPage", "38"),
      slots.getOrDefault("currentPage", "40"),
      slots.getOrDefault("nextPage", "42"),
      itemProvider
    );

    // Reopens this instance on the next tick when called
    // and terminates the passed anvil search GUI
    Consumer<GuiInstance<SingleChoiceParam>> reopen = i -> {
      i.terminate();
      plugin.runTask(() -> inst.reopen(AnimationType.SLIDE_UP));
    };

    // Search button
    inst.fixedItem(
      slots.getOrDefault("search", "44"),
      () -> itemProvider.getItem(StdGuiItem.SEARCH, null),
      e -> {
        // Create a carbon copy of the param and re-route callbacks
        SingleChoiceParam scp = new SingleChoiceParam(
          arg.getTitle(), arg.getRepresentitives(), itemProvider,
          arg.getSelectionTransform(), arg.getSearchLayout(),
          arg.getSearchFields(),arg.getFilter(),

          // Go back to the single choice GUI after selecting
          (o, i) -> {
            haveChosen.put(inst, o);
            arg.getSelected().accept(o, i);
            reopen.accept(i);
          },

          // Re-open the choice if nothing was chosen or back was clicked
          reopen, reopen,

          arg.getSingleChoiceLayout()
        );

        // Temporarily add to chosen just to not trigger any callbacks prematurely
        haveChosen.put(inst, null);
        plugin.runTask(() -> haveChosen.remove(inst), 2);
        searchGui.show(p, scp, null);
      }, null
    );

    if (inst.getArg().getBackButton() != null) {
      inst.fixedItem(
        slots.getOrDefault("back", "36"),
        () -> itemProvider.getItem(StdGuiItem.BACK, null),
        e -> {
          haveChosen.put(inst, null);
          inst.getArg().getBackButton().accept(inst);
        }, null
      );
    }

    inst.setPageContents(() -> {
      Object previousChoice = haveChosen.get(inst);
      Function<ItemStack, ItemStack> selTr = inst.getArg().getSelectionTransform();

      return inst.getArg().getRepresentitives().stream()
        .map(choice -> new GuiItem(
          s -> (
            (selTr != null && choice.getA().equals(previousChoice)) ?
              selTr.apply(choice.getB()) :
              choice.getB()
          ),
          e -> {
            haveChosen.put(inst, choice.getA());
            inst.getArg().getSelected().accept(choice.getA(), inst);
          },
          null
        ))
        .collect(Collectors.toList());
      });

    return true;
  }
}
