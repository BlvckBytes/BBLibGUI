package me.blvckbytes.bblibgui;

import lombok.Getter;
import lombok.Setter;
import me.blvckbytes.bblibconfig.ConfigValue;
import me.blvckbytes.bblibconfig.IItemBuilderFactory;
import me.blvckbytes.bblibgui.listener.InventoryManipulationEvent;
import me.blvckbytes.bblibreflect.communicator.IPacketCommunicatorRegistry;
import me.blvckbytes.bblibreflect.communicator.parameter.SetSlotParameter;
import me.blvckbytes.bblibutil.APlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/21/2022

  A personalized, live instance of a GUI template.

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
public class GuiInstance<T> {

  private static int instanceCounter;

  // Items which are on fixed slots
  private final Map<Integer, GuiItem> fixedItems;

  // A cache of all inventory slots, as external shims may also be animated
  private final List<ItemStack> invCache;

  // A list of pages, where each page maps a used page slot to an item
  private final List<Map<Integer, GuiItem>> pages;

  // Redraw listeners per slot
  private final Map<Integer, List<Runnable>> redrawListeners;

  private final IPacketCommunicatorRegistry communicatorRegistry;
  private final IItemBuilderFactory builderFactory;
  private final APlugin plugin;
  private final int instanceId;

  // Pagination offset when animating non-chest GUIs using shims
  private int paginationOffset;

  private int currPage;
  private GuiAnimation currAnimation;
  private Runnable updatePagination;

  @Setter private Supplier<List<GuiItem>> pageContents;
  @Setter private Consumer<Long> tickReceiver;
  @Setter private boolean animationsEnabled;
  @Setter private List<Integer> pageSlots;
  @Setter private BiFunction<Integer, ItemStack, Boolean> slotShim;
  @Getter private int rows;
  @Getter private ItemStack spacer;
  @Getter private final AGui<T> template;
  @Getter private final AtomicBoolean animating;
  @Getter private final Player viewer;
  @Getter private Inventory inv;
  @Getter private final T arg;
  @Getter private String currTitle;
  @Getter private Inventory previousInv;

  /**
   * Create a new GUI instance from a template instance
   * @param viewer Viewer of this instance
   * @param template Template instance
   * @param arg Argument of this instance
   * @param plugin JavaPlugin ref
   * @param communicatorRegistry IPacketCommunicatorRegistry ref
   * @param builderFactory IItemBuilderFactory ref
   */
  public GuiInstance(
    Player viewer,
    AGui<T> template,
    T arg,
    APlugin plugin,
    IPacketCommunicatorRegistry communicatorRegistry,
    IItemBuilderFactory builderFactory
  ) {
    this.instanceId = instanceCounter++;
    this.viewer = viewer;
    this.template = template;
    this.arg = arg;
    this.plugin = plugin;
    this.communicatorRegistry = communicatorRegistry;
    this.builderFactory = builderFactory;

    this.fixedItems = new HashMap<>();
    this.redrawListeners = new HashMap<>();
    this.pages = new ArrayList<>();
    this.invCache = new ArrayList<>();
    this.pageSlots = new ArrayList<>(template.getPageSlots());
    this.animating = new AtomicBoolean(false);
    this.animationsEnabled = true;
    this.rows = template.getRows();
    this.currTitle = template.getTitle().apply(this).asScalar();

    // In order to evaluate the title supplier, this call needs to follow
    // after the instance's property assignments
    if (template.getType() == InventoryType.CHEST)
      this.inv = Bukkit.createInventory(null, template.getRows() * 9, currTitle);
    else {
      this.inv = Bukkit.createInventory(null, template.getType(), currTitle);
      this.paginationOffset = this.inv.getSize();
    }
  }

  //=========================================================================//
  //                                    API                                  //
  //=========================================================================//

  //////////////////////////////// Switching //////////////////////////////////

  /**
   * Switches to another GUI
   * @param transition Transition to play while switching GUIs
   * @param gui GUI to switch to
   * @param arg Argument for the gui
   */
  public<A> void switchTo(
    @Nullable AnimationType transition,
    @Nullable AGui<A> gui,
    A arg
  ) {
    if (gui != null)
      gui.show(viewer, arg, transition, inv);
    else
      viewer.closeInventory();
  }

  /**
   * Reopens another instance from the template with the
   * exact same viewer and argument and animates using a previous inventory
   * @param animation Animation to play when opening
   * @param previous Previous inventory
   */
  public<A> void reopen(@Nullable AnimationType animation, @Nullable GuiInstance<A> previous) {
    // Check that the title is still valid
    syncTitle();

    // Open on the next tick to avoid glitches
    plugin.runTask(() -> {
      viewer.openInventory(inv);
      refreshPageContents(false);
      redraw("*");
      open(animation, previous == null ? null : previous.getInv());
    });
  }

  /**
   * Reopens another instance from the template with the
   * exact same viewer and argument
   * @param animation Animation to play when opening
   */
  public void reopen(AnimationType animation) {
    reopen(animation, null);
  }

  //////////////////////////////// Inventory //////////////////////////////////

  /**
   * Checks if the title needs to be synchronized (has changed) and
   * will force a inventory recreation if the title it outdated
   */
  private void syncTitle() {
    String newTitle = template.getTitle().apply(this).asScalar();

    // Title still up to date
    if (newTitle.equals(currTitle))
      return;

    // Force inventory recreation
    resize(rows);
  }

  /**
   * Morph this inventory into another type
   * @param type Target type
   */
  public void morph(InventoryType type) {
    if (type == InventoryType.CHEST)
      throw new IllegalArgumentException("Please use resize() for chest inventories!");
    this.inv = Bukkit.createInventory(null, type, currTitle);
  }

  /**
   * Resizes the inventory to a given number of rows and copies over as much of
   * the previous content as will fit
   * @param rows Number of new rows
   */
  public void resize(int rows) {
    this.rows = rows;
    this.currTitle = template.getTitle().apply(this).asScalar();
    Inventory newInv = Bukkit.createInventory(null, rows * 9, currTitle);

    // Copy over contents
    for (int i = 0; i < Math.min(this.inv.getSize(), newInv.getSize()); i++)
      setItem(newInv, i, this.inv.getItem(i));

    // Store the previous inventory to ignore it's close event, then show the new UI
    this.previousInv = this.inv;
    this.inv = newInv;
    viewer.openInventory(this.inv);
  }

  /**
   * Opens the inventory for it's viewer
   * @param animation Animation to display when opening the GUI
   * @param animateFrom Inventory to animate based off of (transitions)
   */
  public void open(@Nullable AnimationType animation, @Nullable Inventory animateFrom) {
    template.getActiveInstances().put(viewer, this);

    // Not a chest means not supported (either of the two)
    if ((animateFrom == null || animateFrom.getType() == InventoryType.CHEST) && inv.getType() == InventoryType.CHEST)
      playAnimation(animation, animateFrom == null ? null : animateFrom.getContents(), invCache.toArray(ItemStack[]::new), null, 0);

    template.opened(this);
  }

  /**
   * Tells this instance that it will never be reused
   * and local state can be cleared out of memory.
   */
  public void terminate() {
    template.terminated(this);
  }

  /**
   * Fast forwards the currently active animation, if there is any
   */
  public void fastForwardAnimating() {
    if (currAnimation != null)
      currAnimation.fastForward();
  }

  /**
   * Refresh the page contents by requesting from the page content supplier
   * @param resetPage Whether to navigate back to page zero
   */
  public void refreshPageContents(boolean resetPage) {
    // Cannot fetch page contents as there is no supplier set
    if (this.pageContents == null)
      return;

    // Got no pages to display on
    if (this.pageSlots.size() == 0)
      return;

    // Clear pages
    pages.clear();

    for (GuiItem item : this.pageContents.get()) {
      // Create a new page either initially or if the last page is already fully used
      if (pages.isEmpty() || pages.get(pages.size() - 1).size() >= pageSlots.size())
        pages.add(new HashMap<>());

      // Add the new item to the last page and determine it's slot
      Map<Integer, GuiItem> targetPage = pages.get(pages.size() - 1);
      int slot = pageSlots.get(targetPage.size());
      targetPage.put(slot, item);
    }

    if (resetPage)
      currPage = 0;

    // Move back as many pages as necessary to not be out of bounds
    // after re-fetching the pages (results might have shrunken)
    else if (currPage >= pages.size())
      currPage = pages.size() == 0 ? 0 : pages.size() - 1;

    redrawPaging();
  }

  /**
   * Add a fixed item, which is an item that will always have the same position,
   * no matter of the viewer's state
   * @param slotExpr Slot(s) to set this item to
   * @param item An item supplier
   * @param onClick Action to run when this item has been clicked
   * @param updatePeriod Item update period in ticks, null means never
   */
  public void fixedItem(
    String slotExpr,
    Supplier<ItemStack> item,
    @Nullable Consumer<InventoryManipulationEvent> onClick,
    Integer updatePeriod
  ) {
    for (int slotNumber : template.slotExprToSlots(slotExpr))
      fixedItems.put(slotNumber, new GuiItem(s -> item.get(), onClick, updatePeriod));
  }

  /**
   * Add a fixed item, which is an item that will always have the same position,
   * no matter of the viewer's state
   * @param slot Slot to set this item to
   * @param item An item supplier
   * @param onClick Action to run when this item has been clicked
   * @param updatePeriod Item update period in ticks, null means never
   */
  public void fixedItem(
    int slot,
    Supplier<ItemStack> item,
    @Nullable Consumer<InventoryManipulationEvent> onClick,
    Integer updatePeriod
  ) {
    fixedItems.put(slot, new GuiItem(s -> item.get(), onClick, updatePeriod));
  }

  /**
   * Adds a previous, an indicator as well as a next item as fixed and
   * standardized items to the GUI
   * @param prevSlotExpr Slot of the previous button
   * @param indicatorSlotExpr Slot of the page indicator
   * @param nextSlotExpr Slot of the next button
   * @param itemProvider Items provider ref
   */
  public void addPagination(
    String prevSlotExpr,
    String indicatorSlotExpr,
    String nextSlotExpr,
    IStdGuiItemProvider itemProvider
  ) {
    updatePagination = () -> {
      plugin.runTask(() -> redraw(indicatorSlotExpr + "," + nextSlotExpr + "," + prevSlotExpr), 5);
    };

    fixedItem(
      prevSlotExpr,
      () -> (
        itemProvider.getItem(
          StdGuiItem.PREV_PAGE,
          ConfigValue.makeEmpty()
            .withVariable("disabled", currPage == 0)
            .exportVariables()
        )
      ),
      e -> {
        if (!(e.getClick().isLeftClick() || e.getClick().isRightClick()))
          return;

        previousPage(AnimationType.SLIDE_RIGHT, e.getClick().isRightClick());
        redraw(indicatorSlotExpr + "," + nextSlotExpr + "," + prevSlotExpr);
      },
      null
    );

    fixedItem(
      indicatorSlotExpr, () -> (
        itemProvider.getItem(
          StdGuiItem.PAGE_INDICATOR,
          ConfigValue.makeEmpty()
            .withVariable("curr_page", getCurrentPage())
            .withVariable("num_pages", getNumPages())
            .withVariable("page_num_items", getCurrPageNumItems())
            .withVariable("total_num_items", getTotalNumItems())
            .withVariable("page_size", getPageSize())
            .exportVariables()
        )
      ), null, null);

    fixedItem(
      nextSlotExpr,
      () -> (
        itemProvider.getItem(
          StdGuiItem.NEXT_PAGE,
          ConfigValue.makeEmpty()
            .withVariable("disabled", currPage >= pages.size() - 1)
            .exportVariables()
        )
      ),
      e -> {
        if (!(e.getClick().isLeftClick() || e.getClick().isRightClick()))
          return;

        nextPage(AnimationType.SLIDE_LEFT, e.getClick().isRightClick());
        redraw(indicatorSlotExpr + "," + nextSlotExpr + "," + prevSlotExpr);
      },
      null
    );
  }

  /**
   * Adds a fill of fixed items consiting of the provided material to the GUI
   * @param item Item to set
   */
  public void addFill(ItemStack item) {
    StringBuilder slotExpr = new StringBuilder();

    for (int i = 0; i < rows * 9; i++) {
      if (!pageSlots.contains(i))
        slotExpr.append(i == 0 ? "" : ",").append(i);
    }

    addSpacer(slotExpr.toString(), item);
  }

  /**
   * Adds a border of fixed items consiting of the provided material to the GUI
   * @param item Item to set
   */
  public void addBorder(ItemStack item) {
    StringBuilder slotExpr = new StringBuilder();

    for (int i = 0; i < rows; i++) {
      int firstSlot = 9 * i, lastSlot = firstSlot + 8;

      slotExpr.append(i == 0 ? "" : ",").append(firstSlot);

      // First or last, use full range
      if (i == 0 || i == rows - 1)
        slotExpr.append('-');

      // Inbetween, only use first and last
      else
        slotExpr.append(',');

      slotExpr.append(lastSlot);
    }

    addSpacer(slotExpr.toString(), item);
  }

  /**
   * Adds a spacer with no name to a given slot
   * @param slotExpr Where to set the item
   * @param item Item to set
   */
  public void addSpacer(String slotExpr, ItemStack item) {
    spacer = item;
    fixedItem(slotExpr, () -> spacer, null, null);
  }

  /**
   * Redraw only the dynamic page items
   */
  public void redrawPaging() {
    updatePage(null);

    if (updatePagination != null)
      updatePagination.run();
  }

  /**
   * Redraw a specified set of slots for a given player
   * @param slotExpr Slots to redraw
   */
  public void redraw(String slotExpr) {
    // Iterate all slots which should be redrawn
    for (int slot : template.slotExprToSlots(slotExpr)) {

      // Vacant slot, skip
      GuiItem target = getItem(slot).orElse(null);
      if (target == null)
        continue;

      // Update the item by re-calling it's supplier
      setItem(this.inv, slot, target.getItem().apply(slot));
    }
  }

  /**
   * Register a new listener for a specific slot's redraw events
   * @param slotExpr Target slot expression
   * @param callback Event listener
   */
  public void onRedrawing(String slotExpr, Runnable callback) {
    template.slotExprToSlots(slotExpr).forEach(slot -> {
      if (!this.redrawListeners.containsKey(slot))
        this.redrawListeners.put(slot, new ArrayList<>());
      this.redrawListeners.get(slot).add(callback);
    });
  }

  /**
   * Get an item by it's current slot
   * @param slot Slot to search in
   * @return Optional item, empty if that slot is vacant
   */
  public Optional<GuiItem> getItem(int slot) {
    // Check for fixed items
    GuiItem fixed = fixedItems.get(slot);
    if (fixed != null)
      return Optional.of(fixed);

    // Got no page items
    if (pages.size() == 0)
      return Optional.empty();

    // Check for page items
    GuiItem pageItem = pages.get(currPage).get(slot);
    if (pageItem != null)
      return Optional.of(pageItem);

    // This slot has to be vacant
    return Optional.empty();
  }

  //////////////////////////////// Paging //////////////////////////////////

  /**
   * Checks whether there is a next page to navigate to
   */
  public boolean hasNextPage() {
    return pages.size() > currPage + 1;
  }

  /**
   * Navigate to the next page
   */
  public void nextPage(@Nullable AnimationType animation, boolean last) {
    if (!hasNextPage())
      return;

    if (updatePagination != null)
      updatePagination.run();

    ItemStack[] before = invCache
      .subList(paginationOffset, invCache.size())
      .toArray(ItemStack[]::new);

    // Advance to the next page (or last page) and force an update
    currPage = last ? pages.size() - 1 : currPage + 1;
    updatePage(null);

    playAnimation(animation, before, invCache
      .subList(paginationOffset, invCache.size())
      .toArray(ItemStack[]::new), pageSlots, paginationOffset);
  }

  /**
   * Checks whether there is a previous page to navigate to
   */
  public boolean hasPreviousPage() {
    return currPage > 0;
  }

  /**
   * Navigate to the previous page
   */
  public void previousPage(@Nullable AnimationType animation, boolean first) {
    if (updatePagination != null)
      updatePagination.run();

    if (!hasPreviousPage())
      return;

    ItemStack[] before = invCache
      .subList(paginationOffset, invCache.size())
      .toArray(ItemStack[]::new);

    // Advance to the previous page and force an update
    currPage = first ? 0 : currPage - 1;
    updatePage(null);

    playAnimation(animation, before, invCache
      .subList(paginationOffset, invCache.size())
      .toArray(ItemStack[]::new), pageSlots, paginationOffset);
  }

  /**
   * Get the number of available pages
   */
  public int getNumPages() {
    return Math.max(1, pages.size());
  }

  /**
   * Get the current page
   */
  public int getCurrentPage() {
    return currPage + 1;
  }

  /**
   * Get the size of a page
   */
  public int getPageSize() {
    return pageSlots.size();
  }

  /**
   * Get the added number of items of all pages
   */
  public int getTotalNumItems() {
    return pages.stream().reduce(0, (acc, curr) -> acc + curr.size(), Integer::sum);
  }

  /**
   * Get the number of items on the current page
   */
  public int getCurrPageNumItems() {
    if (pages.size() == 0)
      return 0;
    return pages.get(currPage).size();
  }

  /**
   * Update the current page's items within the GUI inventory
   * @param time Current relative time, null to force an update upon all items
   */
  public void updatePage(@Nullable Long time) {
    // There are no pages yet, clear all page slots
    if (pages.size() == 0) {
      for (int pageSlot : pageSlots)
        setItem(this.inv, pageSlot, null);
      return;
    }

    // Start out with all available page slots and remove used slots one at a time
    List<Integer> remaining = new ArrayList<>(this.pageSlots);

    // Loop all items of the current page
    for (Map.Entry<Integer, GuiItem> pageItem : pages.get(currPage).entrySet()) {
      GuiItem item = pageItem.getValue();
      remaining.remove(pageItem.getKey());

      // Only update on force updates or if the time is a multiple of the item's period
      if (time == null || (item.getUpdatePeriod() != null && item.getUpdatePeriod() > 0 && time % item.getUpdatePeriod() == 0))
        setItem(this.inv, pageItem.getKey(), item.getItem().apply(pageItem.getKey()));
    }

    // Clear unused page slots
    for (Integer vacantPageSlot : remaining)
      setItem(this.inv, vacantPageSlot, null);
  }

  //////////////////////////////// Updating //////////////////////////////////

  /**
   * Called whenever the template ticks all of it's instances
   * @param time Relative time in ticks
   */
  public void tick(long time) {
    if (tickReceiver != null)
      tickReceiver.accept(time);

    // Tick all fixed items
    for (Map.Entry<Integer, GuiItem> itemE : fixedItems.entrySet()) {
      GuiItem item = itemE.getValue();

      // Only tick this item if it has a period which has elapsed
      if (item.getUpdatePeriod() != null && time % item.getUpdatePeriod() == 0)
        setItem(this.inv, itemE.getKey(), item.getItem().apply(itemE.getKey()));
    }

    // Tick all page items
    updatePage(time);
  }

  /**
   * Applies all basic parameters of a layout, if the layout is provided
   * @param layout Layout to apply
   */
  public void applyLayoutParameters(@Nullable GuiLayoutSection layout) {
    if (layout == null)
      return;

    setAnimationsEnabled(layout.isAnimated());
    resize(layout.getRows());

    if (layout.getFill() != null)
      addFill(layout.getFill().asItem(builderFactory).build());
    else if (layout.getBorder() != null)
      addBorder(layout.getBorder().asItem(builderFactory).build());

    setPageSlots(template.slotExprToSlots(layout.getPaginated()));
  }

  /**
   * Set a slot within the inventory only clientside
   * @param item Item to set
   * @param slot Slot to set the item to
   */
  public void setClientSlot(ItemStack item, int slot) {
    communicatorRegistry.sendToPlayer(new SetSlotParameter(item, null, slot, true), viewer, null);
  }

  /**
   * Play a given animation on the GUI and manage entering
   * and leaving the animation lock state
   * @param animation Animation to play
   * @param from Items to animate from
   * @param mask List of slots to animate, leave at null to animate all slots
   */
  private void playAnimation(
    @Nullable AnimationType animation,
    @Nullable ItemStack[] from,
    @Nullable ItemStack[] to,
    @Nullable List<Integer> mask,
    int offs
  ) {
    if (animation == null || !animationsEnabled)
      return;

    // Fastforward the currently playing animation, if any
    if (currAnimation != null)
      currAnimation.fastForward();

    // Enter animating lock and start playing
    animating.set(true);
    currAnimation = new GuiAnimation(
      plugin, animation,
      from, to,
      (s, i) -> setItem(inv, s, i),
      offs, mask, spacer,
      () -> {
        // Leave animating lock
        currAnimation = null;
        animating.set(false);
      }
    );
  }

  /**
   * Sets an item to a specified slot in the current inventory and
   * protects against index out of range calls
   * @param slot Slot to set at
   * @param item Item to set
   */
  private void setItem(Inventory inv, int slot, ItemStack item) {
    // Call all redraw listeners for this slot
    redrawListeners.getOrDefault(slot, new ArrayList<>()).forEach(Runnable::run);

    while (slot >= invCache.size())
      invCache.add(null);
    invCache.set(slot, item);

    // The slot shim took over control for this slot
    if (slotShim != null && slotShim.apply(slot, item))
      return;

    if (slot < inv.getSize()) {
      inv.setItem(slot, item);

      // Weird bug with not being able to set items in anvils on lower versions
      // Force the item using a fake item
      if (inv.getType() == InventoryType.ANVIL)
        setClientSlot(item, slot);
    }
  }

  @Override
  public int hashCode() {
    return instanceId;
  }
}
