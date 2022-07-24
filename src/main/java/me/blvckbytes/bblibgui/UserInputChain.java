package me.blvckbytes.bblibgui;

import me.blvckbytes.bblibconfig.ConfigValue;
import me.blvckbytes.bblibgui.param.AnvilPromptParam;
import me.blvckbytes.bblibgui.param.MultipleChoiceParam;
import me.blvckbytes.bblibgui.param.SingleChoiceParam;
import me.blvckbytes.bblibgui.param.YesNoParam;
import me.blvckbytes.bblibgui.std.*;
import me.blvckbytes.bblibutil.TriResult;
import me.blvckbytes.bblibutil.Tuple;
import me.blvckbytes.bblibutil.UnsafeFunction;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 06/02/2022

  Handles creating input chains, which consist of multiple forms of user input
  collectors which each get registered with a corresponding name. Each input may
  go back to the previous input or forwards to the next stage. When the last stage
  is being completed, all collected values are emitted through the callback. Whenever
  a stage cancels, the whole chain is being cancelled and the main screen will
  be re-opened.
*/
public class UserInputChain {

  private final Map<String, Object> values;
  private final GuiInstance<?> main;
  private final Consumer<Map<String, Object>> completion;
  private final List<Tuple<Supplier<@Nullable GuiInstance<?>>, @Nullable GuiInstance<?>>> stages;

  private boolean isCancelled;
  private int currStage;

  /**
   * Create a new user input chain which will collect
   * all stage's values into a map
   * @param main Main screen which initiated this chain
   * @param completion Completion callback, providing all collected values
   */
  public UserInputChain(GuiInstance<?> main, Consumer<Map<String, Object>> completion) {
    this.values = new HashMap<>();
    this.stages = new ArrayList<>();
    this.main = main;
    this.completion = completion;
  }

  /**
   * Add a new chat prompt stage
   * @param anvilPromptGui GUI ref
   * @param field Name of the field
   * @param title Anvil GUI title
   * @param stdProvider Provider for standard GUI items
   * @param transformer Chat prompt transformer (parsing, for example)
   * @param confirmationButton Button to display for confirmation (input is transformer result)
   * @param skip Optional skip predicate
   */
  public UserInputChain withPrompt(
    AnvilPromptGui anvilPromptGui,
    String field,
    Function<Map<String, Object>, ConfigValue> title,
    IStdGuiItemProvider stdProvider,
    UnsafeFunction<String, Object> transformer,
    BiFunction<Object, Map<String, Object>, Tuple<Boolean, ItemStack>> confirmationButton,
    @Nullable Function<Map<String, Object>, Boolean> skip
  ) {
    makeStage(anvilPromptGui, () -> (
      new AnvilPromptParam(
        title.apply(values).asScalar(),
        i -> this.previousStage(),
        transformer, input -> confirmationButton.apply(input, values), stdProvider,
        (v, inst) -> {
          values.put(field, v);
          nextStage();
        },
        inst -> this.cancel()
      )
    ), skip);
    return this;
  }

  /**
   * Add a new single choice stage
   * @param yesNoGui GUI ref
   * @param field Name of the field
   * @param type Type of choice (part of the screen title)
   * @param stdProvider Provider for standard GUI items
   * @param layout Layout for the GUI
   * @param yesButton Button to display for the YES action
   * @param noButton Button to display for the NO action
   * @param skip Optional skip predicate
   */
  public UserInputChain withYesNo(
    YesNoGui yesNoGui,
    String field,
    ConfigValue type,
    IStdGuiItemProvider stdProvider,
    @Nullable GuiLayoutSection layout,
    ItemStack yesButton,
    ItemStack noButton,
    @Nullable Function<Map<String, Object>, Boolean> skip
  ) {
    makeStage(yesNoGui, () -> (
      new YesNoParam(
        type.asScalar(), stdProvider, layout, yesButton, noButton,
        (selection, selectionInst) -> {
          if (isCancelled)
            return;

          // Closed
          if (selection == TriResult.EMPTY) {
            isCancelled = true;
            main.reopen(AnimationType.SLIDE_UP);
            return;
          }

          // Input
          values.put(field, selection == TriResult.SUCC);
          nextStage();
        },
        i -> this.previousStage()
      )
    ), skip);
    return this;
  }

  /**
   * Add a new multiple choice stage
   * @param multipleChoiceGui Multiple choice GUI ref
   * @param field Name of the field
   * @param title GUI title
   * @param representitives Input is the variable map, output is a list of representitive items and their values
   * @param itemProvider Provider for standard GUI items
   * @param selectionTransform Used to transform selected items
   * @param searchLayout Layout for the anvil search GUI
   * @param searchFields Fields that are available for searching within the anvil search GUI
   * @param filter External filter function for the anvil search GUI
   * @param singleChoiceLayout Layout for the single choice GUI
   * @param multipleChoiceLayout Layout for the multiple choice GUI
   * @param skip Optional skip predicate
   */
  public UserInputChain withMultipleChoice(
    MultipleChoiceGui multipleChoiceGui,
    String field,
    ConfigValue title,
    Function<Map<String, Object>, List<Tuple<Object, ItemStack>>> representitives,
    IStdGuiItemProvider itemProvider,
    @Nullable Function<ItemStack, ItemStack> selectionTransform,
    @Nullable GuiLayoutSection searchLayout,
    @Nullable IFilterEnum<?> searchFields,
    @Nullable FilterFunction filter,
    @Nullable GuiLayoutSection singleChoiceLayout,
    @Nullable GuiLayoutSection multipleChoiceLayout,
    @Nullable Function<Map<String, Object>, Boolean> skip
  ) {
    makeStage(multipleChoiceGui, () -> (
      new MultipleChoiceParam(
        title.asScalar(), representitives.apply(values), itemProvider,
        selectionTransform, searchLayout, searchFields, filter,

        // Selected
        (selection, selectionInst) -> {
          if (isCancelled)
            return;

          values.put(field, selection);
          nextStage();
        },

        // Closed
        selectionInst -> this.cancel(),

        // Back
        i -> this.previousStage(),

        singleChoiceLayout, multipleChoiceLayout
      )
    ), skip);
    return this;
  }

  /**
   * Add a new single choice stage
   * @param singleChoiceGui Single choice GUI ref
   * @param field Name of the field
   * @param title GUI title
   * @param representitives Input is the variable map, output is a list of representitive items and their values
   * @param itemProvider Provider for standard GUI items
   * @param selectionTransform Used to transform selected items
   * @param searchLayout Layout for the anvil search GUI
   * @param searchFields Fields that are available for searching within the anvil search GUI
   * @param filter External filter function for the anvil search GUI
   * @param singleChoiceLayout Layout for the single choice GUI
   * @param skip Optional skip predicate
   */
  public UserInputChain withSingleChoice(
    SingleChoiceGui singleChoiceGui,
    String field,
    ConfigValue title,
    Function<Map<String, Object>, List<Tuple<Object, ItemStack>>> representitives,
    IStdGuiItemProvider itemProvider,
    @Nullable Function<ItemStack, ItemStack> selectionTransform,
    @Nullable GuiLayoutSection searchLayout,
    @Nullable IFilterEnum<?> searchFields,
    @Nullable FilterFunction filter,
    @Nullable GuiLayoutSection singleChoiceLayout,
    @Nullable Function<Map<String, Object>, Boolean> skip
  ) {
    makeStage(singleChoiceGui, () -> (
      new SingleChoiceParam(
        title.asScalar(), representitives.apply(values), itemProvider,
        selectionTransform, searchLayout, searchFields, filter,

        // Selected
        (selection, selectionInst) -> {
          if (isCancelled)
            return;

          values.put(field, selection);
          nextStage();
        },

        // Closed
        selectionInst -> this.cancel(),

        // Back
        i -> this.previousStage(),

        singleChoiceLayout
      )
    ), skip);
    return this;
  }

  /**
   * Start processing the input chain
   */
  public void start() {
    nextStage();
  }

  /**
   * Terminates all instances which have been launched
   * within this chain's session
   */
  private void terminateInstances() {
    if (isCancelled)
      return;

    isCancelled = true;
    stages.forEach(v -> {
      if (v.getB() != null)
        v.getB().terminate();
    });
  }

  /**
   * Make a new lazily created GUI stage
   * @param gui GUI template ref
   * @param param GUI parameter builder
   * @param skip Stage skip predicate
   */
  private<T> void makeStage(
    AGui<T> gui,
    Supplier<T> param,
    @Nullable Function<Map<String, Object>, Boolean> skip
  ) {
    int index = stages.size();

    Supplier<@Nullable GuiInstance<?>> builder = () -> {
      if (skip != null && skip.apply(values))
        return null;

      Tuple<Supplier<GuiInstance<?>>, @Nullable GuiInstance<?>> data = stages.get(index);

      if (data.getB() != null)
        return data.getB();

      GuiInstance<?> inst = gui.createSilent(main.getViewer(), param.get()).orElse(null);
      data.setB(inst);
      return inst;
    };

    stages.add(new Tuple<>(builder, null));
  }

  /**
   * Cancels the whole chain
   */
  private void cancel() {
    if (isCancelled)
      return;

    isCancelled = true;
    terminateInstances();
    main.reopen(AnimationType.SLIDE_UP);
  }

  /**
   * Go to the previous stage or go back to the main screen
   * if there is no previous stage available
   */
  private void previousStage() {
    if (isCancelled)
      return;

    // No previous stage, cancel and reopen main
    currStage -= 2; // currstage is always leading by one
    if (currStage < 0) {
      isCancelled = true;
      GuiInstance<?> lastScreen = stages.size() > 0 ? stages.get(0).getA().get() : null;
      terminateInstances();
      main.reopen(lastScreen != null ? AnimationType.SLIDE_RIGHT : AnimationType.SLIDE_UP, lastScreen);
      return;
    }

    GuiInstance<?> lastScreen = stages.get(currStage + 1).getA().get();
    GuiInstance<?> stage = stages.get(currStage++).getA().get();

    // This stage is to be skipped
    if (stage == null) {
      previousStage();
      return;
    }

    stage.reopen(lastScreen == null ? AnimationType.SLIDE_UP : AnimationType.SLIDE_RIGHT, lastScreen);
  }

  /**
   * Go to the next stage or call the completion callback if
   * all stages have been processed already
   */
  private void nextStage() {
    if (isCancelled)
      return;

    // No more stages remaining
    if (currStage >= stages.size()) {
      // Invoke the completion callback
      if (completion != null)
        completion.accept(values);

      // Go back to the main screen
      GuiInstance<?> lastScreen = stages.size() > 0 ? stages.get(stages.size() - 1).getA().get() : null;
      main.reopen(lastScreen != null ? AnimationType.SLIDE_RIGHT : AnimationType.SLIDE_UP, lastScreen);
      return;
    }

    GuiInstance<?> lastScreen = currStage == 0 ? main : stages.get(currStage - 1).getA().get();
    GuiInstance<?> stage = stages.get(currStage++).getA().get();

    // This stage is to be skipped
    if (stage == null) {
      nextStage();
      return;
    }

    stage.reopen(lastScreen == null ? AnimationType.SLIDE_UP : AnimationType.SLIDE_LEFT, lastScreen);
  }
}
