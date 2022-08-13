package me.blvckbytes.bblibgui.std;

import me.blvckbytes.bblibconfig.ConfigValue;
import me.blvckbytes.bblibconfig.IItemBuilderFactory;
import me.blvckbytes.bblibgui.*;
import me.blvckbytes.bblibgui.listener.InventoryManipulationEvent;
import me.blvckbytes.bblibgui.param.SingleChoiceParam;
import me.blvckbytes.bblibreflect.IFakeItemCommunicator;
import me.blvckbytes.bblibreflect.IPacketInterceptor;
import me.blvckbytes.bblibreflect.MCReflect;
import me.blvckbytes.bblibutil.APlugin;
import me.blvckbytes.bblibdi.AutoConstruct;
import me.blvckbytes.bblibdi.AutoInject;
import me.blvckbytes.bblibutil.IEnum;
import me.blvckbytes.bblibutil.Tuple;
import me.blvckbytes.bblibutil.logger.ILogger;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/30/2022

  Offers the viewer an anvil inventory to title searches into while their
  own inventory is used to display results live, using fake items.

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
public class AnvilSearchGui extends AAnvilGui<SingleChoiceParam> implements Listener {

  // Shim slot offset to properly distinguish real slots from shim slots
  private static final int SHIM_OFFS = 3, PAGE_BEGIN = SHIM_OFFS + 9;

  // Time in milliseconds between update invocations when typing
  private static final long TYPE_DEBOUNCE_MS = 300;

  private final Map<GuiInstance<?>, Tuple<String, @Nullable IFilterEnum<?>>> filterState;
  private final Map<GuiInstance<?>, Tuple<Long, Runnable>> typingActions;
  private final IFakeItemCommunicator fakeItem;

  public AnvilSearchGui(
    @AutoInject APlugin plugin,
    @AutoInject IPacketInterceptor packetInterceptor,
    @AutoInject MCReflect refl,
    @AutoInject ILogger logger,
    @AutoInject IFakeItemCommunicator fakeItem,
    @AutoInject IItemBuilderFactory builderFactory
  ) {
    super(plugin, packetInterceptor, refl, logger, fakeItem, builderFactory);
    this.fakeItem = fakeItem;

    this.filterState = new HashMap<>();
    this.typingActions = new HashMap<>();
  }

  @Override
  protected void terminated(GuiInstance<SingleChoiceParam> inst) {
    filterState.remove(inst);
    typingActions.remove(inst);
  }

  @Override
  protected boolean closed(GuiInstance<SingleChoiceParam> inst) {
    super.closed(inst);

    Player p = inst.getViewer();

    // Restore the inventory contents again by updating the inv
    p.updateInventory();
    return false;
  }

  @Override
  protected boolean opening(GuiInstance<SingleChoiceParam> inst) {
    IStdGuiItemProvider itemProvider = inst.getArg().getItemProvider();
    GuiLayoutSection layout = inst.getArg().getSearchLayout();

    ////////////////////////// Type Debouncing //////////////////////////

    // Check typing actions on every tick
    inst.setTickReceiver(time -> {
      Tuple<Long, Runnable> lastAction = typingActions.get(inst);

      // Either currently no action available or the debounce period has not yet elapsed
      if (lastAction == null || System.currentTimeMillis() - lastAction.getA() < TYPE_DEBOUNCE_MS)
        return;

      // Process the action
      typingActions.remove(inst);
      lastAction.getB().run();
    });

    ////////////////////////// Inventory Items //////////////////////////

    Map<String, String> slots = layout != null ? layout.getSlots() : new HashMap<>();

    // Set the background or fill in the hotbar
    if (layout != null && (layout.getFill() != null || layout.getBorder() != null))
      inst.addSpacer(SHIM_OFFS + "-" + (SHIM_OFFS + 8), layout.getFill().asItem(builderFactory, null).build());

    // Call super after fill/border to allow for overriding fixed items
    super.opening(inst);

    // Page slots are the three rows of the inventory
    inst.setPageSlots(slotExprToSlots(PAGE_BEGIN + "-" + (26 + PAGE_BEGIN)));

    // Pagination in hotbar
    inst.addPagination(
      String.valueOf(Integer.parseInt(slots.getOrDefault("prevPage", "1")) + SHIM_OFFS),
      String.valueOf(Integer.parseInt(slots.getOrDefault("currentPage", "3")) + SHIM_OFFS),
      String.valueOf(Integer.parseInt(slots.getOrDefault("nextPage", "5")) + SHIM_OFFS),
      itemProvider
    );

    // Filter mode in hotbar, only render if an enum has been provided
    if (getFilterState(inst).getB() != null) {
      inst.fixedItem(
        String.valueOf(Integer.parseInt(slots.getOrDefault("searchFilter", "7")) + SHIM_OFFS),
        () -> {
          Tuple<String, @Nullable IFilterEnum<?>> filter = getFilterState(inst);

          return itemProvider.getItem(
            StdGuiItem.SEARCH_FILTER,
            ConfigValue.makeEmpty()
              .withVariable("filters", Arrays.stream(filter.getB().listValues()).map(IEnum::name).collect(Collectors.joining(",")))
              .withVariable("active_filter_index", filter.getB().ordinal())
              .exportVariables()
          );
        },
        e -> {
          // Cycle to the next available filter field
          Tuple<String, @Nullable IFilterEnum<?>> filter = getFilterState(inst);

          filter.setB(filter.getB().nextValue());

          // Redraw the item
          inst.redraw(String.valueOf(Integer.parseInt(slots.getOrDefault("searchFilter", "7")) + SHIM_OFFS));

          // Force results update
          onTyping(inst, filter.getA());
        },
        null
      );
    }

    // Set all slots above the offset to the player's inventory
    inst.setSlotShim((slot, item) -> {
      if (slot < SHIM_OFFS)
        return false;

      // Take off the offset again
      slot -= SHIM_OFFS;

      fakeItem.setFakeInventorySlot(
        inst.getViewer(), item, slot
      );

      return true;
    });

    //////////////////////////// Anvil Items ////////////////////////////

    // This item serves as a placeholder to get the typing functionality up and working, while
    // it also informes the player about the concept of filtering
    inst.fixedItem("0", () -> (
      itemProvider.getItem(StdGuiItem.SEARCH_PLACEHOLDER, null)
    ), null, null);

    return true;
  }

  @Override
  protected void opened(GuiInstance<SingleChoiceParam> inst) {
    // Force-render the whole screen, as the underlying GUI doesn't know of the extra shim slots
    inst.redraw("0-" + (SHIM_OFFS + 9 * 4));

    // Initial call if the client doesn't automatically send
    // the first packet when setting the fake item
    onTyping(inst, "");
  }

  //=========================================================================//
  //                                  Listener                               //
  //=========================================================================//

  @EventHandler
  public void onClick(InventoryManipulationEvent e) {
    Player p = e.getPlayer();

    // Has no active instance
    GuiInstance<SingleChoiceParam> inst = getActiveInstances().get(p);
    if (inst == null)
      return;

    // Clicked into a slot of the anvil, redraw to keep fake items alive
    if (
      e.getTargetInventory().equals(inst.getInv()) ||
      e.getOriginInventory().equals(inst.getInv())
    ) {
      plugin.runTask(() -> {
        redraw(inst);

        // Also clear the cursor, as on some versions
        // the fake items glitches onto the cursor
        p.setItemOnCursor(null);
      });
      return;
    }

    // Player inventory is neither origin nor target, ignore
    boolean isOrigin = p.getInventory().equals(e.getOriginInventory());
    if (!isOrigin && !p.getInventory().equals(e.getTargetInventory()))
      return;

    // Always cancel clicks and clear the cursor
    e.setCancelled(true);
    p.setItemOnCursor(null);

    int slot = isOrigin ? e.getOriginSlot() : e.getTargetSlot();
    GuiItem clicked = inst.getItem(slot + SHIM_OFFS).orElse(null);

    // Run on next tick as fake items tend to dissapear after a click
    plugin.runTask(() -> {
      redraw(inst);

      if (clicked != null && clicked.getOnClick() != null)
        clicked.getOnClick().accept(e);
    });
  }

  //=========================================================================//
  //                                 Utilities                               //
  //=========================================================================//

  /**
   * Calculates a number which represents the difference between all available words
   * within the list of texts and the search words, where every text word may only match once.
   * @param words Words to match
   * @param texts Texts to search in
   * @return Difference, < 0 if there was no match for all words
   */
  private int calculateDifference(String[] words, String... texts) {
    // Create a list of unique words which all of the texts contain
    Set<String> availWords = new HashSet<>();
    for (String text : texts) {

      // Account for provider errors
      if (text == null)
        continue;

      for (String textWord : text.split(" "))
        availWords.add(textWord.toLowerCase());
    }

    // Iterate all words and count sum the total diff
    int totalDiff = 0;
    for (String word : words) {

      // Find the best match for the current word in all remaining words
      String bestMatch = null;
      int bestMatchDiff = Integer.MAX_VALUE;
      for (String availWord : availWords) {

        // Not cotaining the target word
        int index = availWord.indexOf(word.toLowerCase());
        if (index < 0)
          continue;

        // The difference is determined by how many other chars are padding the search word
        int diff = availWord.length() - word.length();

        // Compare and update the local best match
        if (diff < bestMatchDiff) {
          bestMatchDiff = diff;
          bestMatch = availWord;
        }
      }

      // No match found for the current word, all words need to match, thus cancel
      if (bestMatch == null)
        return -1;

      // Remove the matching word from the list and add it's difference to the total
      availWords.remove(bestMatch);
      totalDiff += bestMatchDiff;
    }

    return totalDiff;
  }


  /**
   * Redraws the whole screen, including shim slots
   */
  private void redraw(GuiInstance<?> inst) {
    inst.redraw("0-" + (SHIM_OFFS + 9 * 4));
  }

  /**
   * Called whenever the player types within the anvil GUI
   * @param inst Instance ref
   * @param text Typed text
   */
  @Override
  protected void onTyping(GuiInstance<SingleChoiceParam> inst, String text) {
    // Register the latest typing action with the current time-stamp
    typingActions.put(inst, new Tuple<>(
      System.currentTimeMillis(),

      // Function that encloses all current parameters and
      // will apply them to the page contents on execution
      () -> {
        // Filter the representitives by the search string
        List<Tuple<Object, ItemStack>> results = filterRepresentitives(inst, text);
        Player p = inst.getViewer();

        // Set page contents
        inst.setPageContents(() -> (
          results.stream()
            .map(t -> (
              new GuiItem(
                i -> t.getB(),
                e -> {
                  // Call the selection callback
                  madeSelection.add(p);
                  inst.getArg().getSelected().accept(t.getA(), inst);
                },
                null
              )
            ))
            .collect(Collectors.toList())
        ));

        inst.refreshPageContents(true);
      }
    ));
  }

  /**
   * Filter the available representitive items according to the provided search term
   * @param inst Instance ref to get the representitives from
   * @param search Search term to filter by
   * @return Filtered list to display
   */
  private List<Tuple<Object, ItemStack>> filterRepresentitives(GuiInstance<SingleChoiceParam> inst, String search) {
    FilterFunction externalFilter = inst.getArg().getFilter();
    Tuple<String, @Nullable IFilterEnum<?>> filterState = getFilterState(inst);

    // Update search buffer
    filterState.setA(search);

    // Apply the external filter, if provided
    if (externalFilter != null)
      return externalFilter.apply(search, filterState.getB(), inst.getArg().getRepresentitives());

    // Create a value buffer of either size two (displayname, lore) or the number of available fields
    IFilterEnum<?> fields = filterState.getB();
    String[] searchWords = search.toLowerCase().split(" ");

    // Extend each of the results by it's diff metric
    List<Tuple<Tuple<Object, ItemStack>, Integer>> results = new ArrayList<>();

    for (Tuple<Object, ItemStack> item : inst.getArg().getRepresentitives()) {

      String[] texts;

      // No fields available, use displayname and lore of the item as a fallback
      if (fields == null) {
        ItemMeta meta = item.getB().getItemMeta();
        texts = new String[] {
          meta == null ? "" : meta.getDisplayName(),
          meta == null ? "" : String.join(" ", meta.getLore())
        };
      }

      // Use the enum as an extractor on the current object
      else
        texts = fields.getTexts().apply(item.getA());

      // Skip items which don't match at all
      int diff = calculateDifference(searchWords, texts);
      if (diff >= 0)
        results.add(new Tuple<>(item, diff));
    }

    // Sort by diff ascending to have the most relevant results come first
    // and unpack from the previously attached metric again
    return results.stream()
      .sorted(Comparator.comparingInt(Tuple::getB))
      .map(Tuple::getA)
      .collect(Collectors.toList());
  }

  /**
   * Get the current filter state of a GUI and initially create it internally
   * @param inst GUI instance
   * @return Filter state
   */
  private Tuple<String, @Nullable IFilterEnum<?>> getFilterState(GuiInstance<SingleChoiceParam> inst) {
    if (!filterState.containsKey(inst))
      filterState.put(inst, new Tuple<>("", inst.getArg().getSearchFields()));
    return filterState.get(inst);
  }
}
