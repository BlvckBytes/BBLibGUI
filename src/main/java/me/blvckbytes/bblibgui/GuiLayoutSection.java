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

  /**
   * Get a numeric slot value or use a default on
   * formatting errors or absent values
   * @param name Key name within slots map
   * @param def Default value
   * @return Map value or default value
   */
  public int getSlotOrDefault(String name, int def) {
    try {
      return Integer.parseInt(slots.get(name));
    } catch (Exception e) {
      return def;
    }
  }
}
