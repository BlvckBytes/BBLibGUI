package me.blvckbytes.bblibgui;

import lombok.Getter;
import me.blvckbytes.bblibconfig.AConfigSection;
import me.blvckbytes.bblibconfig.sections.CSMap;
import me.blvckbytes.bblibconfig.sections.ItemStackSection;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 07/05/2022

  Represents a section containing parameters a GUI layout may have.
*/
@Getter
public class GuiLayoutSection extends AConfigSection {

  private int rows;
  private @Nullable ItemStackSection fill;
  private @Nullable ItemStackSection border;
  private boolean animated;
  private String paginated;

  @CSMap(k = String.class, v = String.class)
  private Map<String, String> slots;

  public GuiLayoutSection() {
    this.rows = 1;
    this.paginated = "";
    this.slots = new HashMap<>();
  }
}
