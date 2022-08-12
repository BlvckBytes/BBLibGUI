package me.blvckbytes.bblibgui;

import me.blvckbytes.bblibutil.IEnum;

import java.util.function.Function;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 07/23/2022

  An enum which is used in combination with filtering based
  on a list of fields of the bound representitive object.

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
public interface IFilterEnum<T extends Enum<?>> extends IEnum<T> {

  /**
   * Get a list of texts to search through when provided a model's instance
   */
  Function<Object, String[]> getTexts();

  /**
   * Get the next enum value in the enum's ordinal sequence
   * and wrap around if performed on the last value
   * @return Next enum value
   */
  IFilterEnum<T> nextValue();

}
