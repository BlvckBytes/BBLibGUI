package me.blvckbytes.bblibgui.std;

import me.blvckbytes.bblibconfig.ConfigValue;
import me.blvckbytes.bblibgui.*;
import me.blvckbytes.bblibgui.param.MultipleChoiceParam;
import me.blvckbytes.bblibgui.param.SingleChoiceParam;
import me.blvckbytes.bblibreflect.IFakeItemCommunicator;
import me.blvckbytes.bblibutil.APlugin;
import me.blvckbytes.bblibdi.AutoConstruct;
import me.blvckbytes.bblibdi.AutoInject;
import me.blvckbytes.bblibutil.Tuple;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/29/2022

  Allows the user to choose one of multiple paged choices, each represented
  by an itemstack with a corresponding bound object.

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
public class MultipleChoiceGui extends AGui<MultipleChoiceParam> {

  private final Map<GuiInstance<?>, List<Tuple<Object, ItemStack>>> playerChoices;
  private final Set<GuiInstance<?>> tookAction;
  private final SingleChoiceGui singleChoiceGui;

  public MultipleChoiceGui(
    @AutoInject APlugin plugin,
    @AutoInject SingleChoiceGui singleChoiceGui,
    @AutoInject IFakeItemCommunicator fakeItemCommunicator
  ) {
    super(5, "10-16,19-25,28-34", i -> (
      ConfigValue.immediate(i.getArg().getTitle())
    ), plugin, fakeItemCommunicator);

    this.playerChoices = new HashMap<>();
    this.tookAction = new HashSet<>();
    this.singleChoiceGui = singleChoiceGui;
  }

  @Override
  protected void terminated(GuiInstance<MultipleChoiceParam> inst) {
    playerChoices.remove(inst);
  }

  @Override
  protected boolean closed(GuiInstance<MultipleChoiceParam> inst) {
    Consumer<GuiInstance<MultipleChoiceParam>> closed = inst.getArg().getClosed();

    // Took no valid action
    if (!tookAction.remove(inst)) {

      // Fire close callback (cancelling)
      if (closed != null)
        closed.accept(inst);

      return false;
    }

    return false;
  }

  @Override
  protected boolean opening(GuiInstance<MultipleChoiceParam> inst) {
    MultipleChoiceParam arg = inst.getArg();
    IStdGuiItemProvider itemProvider = arg.getItemProvider();
    GuiLayoutSection layout = arg.getMultipleChoiceLayout();

    inst.applyLayoutParameters(layout);

    Map<String, String> slots = layout != null ? layout.getSlots() : new HashMap<>();

    if (!playerChoices.containsKey(inst))
      playerChoices.put(inst, new ArrayList<>());

    List<Tuple<Object, ItemStack>> choices = playerChoices.get(inst);

    inst.addPagination(
      slots.getOrDefault("prevPage", "38"),
      slots.getOrDefault("currentPage", "40"),
      slots.getOrDefault("nextPage", "42"),
      itemProvider
    );

    // Add another choice
    inst.fixedItem(
      slots.getOrDefault("newChoice", "26"),
      () -> {
        int remaining = arg.getRepresentitives().size() - choices.size();
        return itemProvider.getItem(
          StdGuiItem.NEW_CHOICE,
          ConfigValue.makeEmpty()
            .withVariable("num_choices", choices.size())
            .withVariable("num_available", arg.getRepresentitives().size())
            .withVariable("disabled", remaining <= 0)
            .withVariable("remaining_choices", remaining)
            .exportVariables()
        );
      },
      e -> {
        // No more choices available
        if (arg.getRepresentitives().size() - choices.size() <= 0)
          return;

        // Allow to switch inventories temporary
        tookAction.add(inst);

        inst.switchTo(AnimationType.SLIDE_LEFT, singleChoiceGui, new SingleChoiceParam(
          arg.getTitle(),

          // Filter out already selected representitives
          arg.getRepresentitives().stream()
            .filter(repr -> !choices.contains(repr))
            .collect(Collectors.toList()),

          itemProvider, arg.getSelectionTransform(), arg.getSearchLayout(),
          arg.getSearchFields(), arg.getFilter(),

          // Add the new selection to the list of choices
          (sel, selInst) -> {
            tookAction.remove(inst);

            inst.getArg().getRepresentitives().stream()
              .filter(r -> r.getA().equals(sel))
              .findFirst().ifPresent(choice -> {
                // Do not allow for duplicate entries
                if (!choices.contains(choice))
                  choices.add(choice);
              });

            inst.refreshPageContents(false);
            inst.reopen(AnimationType.SLIDE_RIGHT, selInst);
          },
          // Closed
          selInst -> {
            tookAction.remove(inst);
            plugin.runTask(() -> inst.reopen(AnimationType.SLIDE_UP));
          },
          // Back button
          arg.getBackButton() == null ? null :
          selInst -> inst.reopen(AnimationType.SLIDE_RIGHT, selInst),

          arg.getSingleChoiceLayout()
        ));
    }, null);

    // Render the back button, if provided
    if (inst.getArg().getBackButton() != null) {
      inst.fixedItem(
        slots.getOrDefault("back", "36"),
        () -> itemProvider.getItem(StdGuiItem.BACK, null),
        e -> {
          tookAction.add(inst);
          inst.getArg().getBackButton().accept(inst);
        }, null
      );
    }

    // Submit the list of choices
    inst.fixedItem(
      slots.getOrDefault("submit", "44"),
      () -> (
        itemProvider.getItem(
          StdGuiItem.SUBMIT_CHOICES,
          ConfigValue.makeEmpty()
            .withVariable("num_choices", choices.size())
            .withVariable("disabled", choices.size() == 0)
            .withVariable("remaining_choices", arg.getRepresentitives().size() - choices.size())
            .exportVariables()
        )
      ), e -> {
        // Nothing chosen, ignore interactions
        if (choices.size() == 0)
          return;

        tookAction.add(inst);

        // Call selection callback
        inst.getArg().getSelected().accept(
          choices.stream()
            .map(Tuple::getA)
            .collect(Collectors.toList()),
          inst
        );
      },
      null
    );

    // Draw the player's choices as pages
    inst.setPageContents(() -> (
      choices.stream()
        .map(choice -> new GuiItem(
          // Transform selected items, if a transformer has been provided
          s -> arg.getSelectionTransform() == null ? choice.getB() : arg.getSelectionTransform().apply(choice.getB()),
          // Remove choices by clicking on them
          e -> {
            choices.remove(choice);
            inst.refreshPageContents(false);

            // Redraw the submit button, in case the last choice
            // has been removed and it's rendered inactive again
            inst.redraw(
              slots.getOrDefault("submit", "44") + "," +
              slots.getOrDefault("newChoice", "26")
            );
          },
          null
        ))
        .collect(Collectors.toList())
      ));

    return true;
  }
}
