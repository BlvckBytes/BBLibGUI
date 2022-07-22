package me.blvckbytes.bblibgui;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import me.blvckbytes.bblibconfig.ConfigValue;
import me.blvckbytes.bblibgui.listener.InventoryManipulationEvent;
import me.blvckbytes.bblibpackets.IFakeItemCommunicator;
import me.blvckbytes.bblibutil.APlugin;
import me.blvckbytes.bblibdi.IAutoConstructed;
import org.apache.commons.lang.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.IntStream;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/21/2022

  The base of all GUIs which implements basic functionality.
*/
public abstract class AGui<T> implements IAutoConstructed, Listener {

  protected final APlugin plugin;

  private final IFakeItemCommunicator fakeItemCommunicator;

  // Mapping players to their active instances
  @Getter
  private final Map<Player, GuiInstance<T>> activeInstances;

  private int tickerHandle;

  // Inventory title supplier
  @Getter
  @Setter(AccessLevel.PROTECTED)
  private Function<GuiInstance<T>, ConfigValue> title;

  @Getter
  private final int rows;

  @Getter
  private final InventoryType type;

  // Set of slots which are reserved for pages (items may differ for every session)
  @Getter
  private final List<Integer> pageSlots;

  /**
   * Create a new GUI template base
   * @param rows Number of rows
   * @param pageSlotExpr Which slots to use for dynamic paging
   * @param title Title supplier for the inventory
   * @param plugin Plugin ref
   * @param fakeItemCommunicator IFakeItemCommunicator ref
   */
  protected AGui(
    int rows,
    String pageSlotExpr,
    Function<GuiInstance<T>, ConfigValue> title,
    APlugin plugin,
    IFakeItemCommunicator fakeItemCommunicator
  ) {
    this(rows, pageSlotExpr, title, InventoryType.CHEST, plugin, fakeItemCommunicator);
  }

  /**
   * Create a new GUI template base
   * @param rows Number of rows
   * @param pageSlotExpr Which slots to use for dynamic paging
   * @param title Title supplier for the inventory
   * @param plugin Plugin ref
   * @param fakeItemCommunicator IFakeItemCommunicator ref
   */
  protected AGui(
    int rows,
    String pageSlotExpr,
    Function<GuiInstance<T>, ConfigValue> title,
    InventoryType type,
    APlugin plugin,
    IFakeItemCommunicator fakeItemCommunicator
  ) {
    this.rows = rows;
    this.title = title;
    this.plugin = plugin;
    this.type = type;
    this.fakeItemCommunicator = fakeItemCommunicator;

    this.pageSlots = slotExprToSlots(pageSlotExpr);
    this.activeInstances = new HashMap<>();
    this.tickerHandle = -1;
  }

  //=========================================================================//
  //                                   API                                   //
  //=========================================================================//

  /**
   * Create a new instance of this GUI but don't yet render it
   * @param viewer Inventory viewer
   * @param arg Argument to be passed to the instance
   */
  public Optional<GuiInstance<T>> createSilent(
    Player viewer,
    T arg
  ) {
    GuiInstance<T> inst = new GuiInstance<>(viewer, this, arg, plugin, fakeItemCommunicator);
    if (!opening(inst))
      return Optional.empty();
    return Optional.of(inst);
  }

  /**
   * Show a personalized instance of this GUI to a player
   * @param viewer Inventory viewer
   * @param arg Argument to be passed to the instance
   * @param animation Animation to display when opening the GUI
   * @param animateFrom Inventory to animate based off of (transitions)
   */
  public Optional<GuiInstance<T>> show(
    Player viewer,
    T arg,
    @Nullable AnimationType animation,
    @Nullable Inventory animateFrom
  ) {
    GuiInstance<T> inst = createSilent(viewer, arg).orElse(null);

    if (inst == null)
      return Optional.empty();

    // Initially draw the whole gui and fetch pages
    viewer.openInventory(inst.getInv());
    inst.refreshPageContents(false);
    inst.redraw("*");
    inst.open(animation, animateFrom);

    return Optional.of(inst);
  }

  /**
   * Show a personalized instance of this GUI to a player
   * @param viewer Inventory viewer
   * @param arg Argument to be passed to the instance
   * @param animation Animation to display when opening the GUI
   */
  public Optional<GuiInstance<T>> show(Player viewer, T arg, @Nullable AnimationType animation) {
    return show(viewer, arg, animation, null);
  }

  @Override
  public void cleanup() {
    if (tickerHandle >= 0)
      Bukkit.getScheduler().cancelTask(tickerHandle);

    // Destroy all instances
    for (Iterator<GuiInstance<T>> instI = activeInstances.values().iterator(); instI.hasNext();) {
      GuiInstance<T> inst = instI.next();
      inst.getViewer().closeInventory();
      closed(inst);
      instI.remove();
    }
  }

  @Override
  public void initialize() {
    AtomicInteger time = new AtomicInteger(0);
    tickerHandle = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
      // Tick all instances
      for (GuiInstance<T> instance : activeInstances.values()) {
        // Don't tick animating GUIs
        if (!instance.getAnimating().get())
          instance.tick(time.getAndIncrement());
      }
    }, 0L, 1L);
  }

  //=========================================================================//
  //                                Internals                                //
  //=========================================================================//

  /**
   * Called when a GUI has been terminated and will never be used again
   * @param inst Instance of the GUI terminated
   */
  protected void terminated(GuiInstance<T> inst) {}

  /**
   * Called when a GUI has been closed by a player
   * @param inst Instance of the GUI closed
   * @return Whether to prevent closing the GUI
   */
  abstract protected boolean closed(GuiInstance<T> inst);

  /**
   * Called before a GUI is being shown to a player
   * @param inst Instance of the GUI opening
   * @return Whether to open the GUI, false cancels
   */
  abstract protected boolean opening(GuiInstance<T> inst);

  /**
   * Called after a GUI was opened for a player (includes reuses)
   * @param inst Instance of the GUI opened
   */
  protected void opened(GuiInstance<T> inst) {}

  //=========================================================================//
  //                                Listener                                 //
  //=========================================================================//

  @EventHandler
  public void onManip(InventoryManipulationEvent e) {
    GuiInstance<T> inst = activeInstances.get(e.getPlayer());

    // Has no active instance yet
    if (inst == null)
      return;

    // Instance is neither origin nor target, ignore
    boolean isOrigin = inst.getInv().equals(e.getOriginInventory());
    if (!isOrigin && !inst.getInv().equals(e.getTargetInventory()))
      return;

    // Always cancel as soon as possible as a fallback,
    // as permitting is usually the exception
    e.setCancelled(true);

    // Ignore interactions while animating and fast forward them
    if (inst.getAnimating().get()) {
      inst.fastForwardAnimating();
      return;
    }

    // Clicked on a used slot which has a click event bound to it
    GuiItem clicked = inst.getItem(isOrigin ? e.getOriginSlot() : e.getTargetSlot()).orElse(null);
    if (clicked != null && clicked.getOnClick() != null)
      clicked.getOnClick().accept(e);
  }

  @EventHandler
  public void onClose(InventoryCloseEvent e) {
    if (!(e.getPlayer() instanceof Player))
      return;

    Player p = (Player) e.getPlayer();

    // The player has no instance or this instance was not affected
    GuiInstance<T> inst = activeInstances.get(p);
    if (inst == null || !inst.getInv().equals(e.getInventory()))
      return;

    // Destroy the instance
    activeInstances.remove(p);

    // Prevent closing the inventory
    if (closed(inst))
      plugin.runTask(() -> inst.reopen(null));
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent e) {
    activeInstances.remove(e.getPlayer());
  }

  //=========================================================================//
  //                                Utilities                                //
  //=========================================================================//

  /**
   * Convert a slot expression to a set of slot indices
   * @param slotExpr Slot expression
   */
  public List<Integer> slotExprToSlots(String slotExpr) {
    if (slotExpr.isBlank())
      return new ArrayList<>();

    List<Integer> slots = new ArrayList<>();

    for (String range : slotExpr.split(",")) {
      String[] rangeData = range.split("-");

      if (rangeData[0].equals("*")) {
        IntStream.range(0, rows * 9).forEach(slots::add);
        break;
      }

      int from = Integer.parseInt(rangeData[0]);

      if (rangeData.length == 1) {
        slots.add(from);
        continue;
      }

      int to = Integer.parseInt(rangeData[1]);

      for (int i = from; from > to ? (i >= to) : (i <= to); i += (from > to ? -1 : 1))
        slots.add(i);
    }

    return slots;
  }

  /**
   * Formats a constant to a human readable string
   * @param constant Constant to format
   * @return Formatted string
   */
  public String formatConstant(String constant) {
    return WordUtils.capitalizeFully(constant.replace("_", " ").replace(".", " "));
  }

  /**
   * Wraps a given text on multiple lines by counting the chars per line
   * @param text Text to wrap in length
   * @return Wrapped text
   */
  public String wrapText(String text) {
    StringBuilder res = new StringBuilder();
    int charsPerLine = 35, remChPerLine = charsPerLine;
    boolean isFirstLine = true;

    for (String word : text.split(" ")) {
      int wlen = word.length();

      if (word.contains("\n"))
        remChPerLine = charsPerLine;

      if ((isFirstLine && wlen > remChPerLine / 2) || wlen > remChPerLine) {
        isFirstLine = false;
        res.append("\n").append(word).append(" ");
        remChPerLine = charsPerLine;
        continue;
      }

      res.append(word).append(" ");
      remChPerLine -= wlen + 1;
    }

    return res.toString();
  }
}
