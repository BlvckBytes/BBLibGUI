package me.blvckbytes.bblibgui;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 07/03/2022

  Enumerates all available standard GUI items.

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
public enum StdGuiItem {
  // Free text search button
  SEARCH,

  // Back to previous GUI
  BACK,

  // Anvil GUI search item in first slot
  SEARCH_PLACEHOLDER,

  // Anvil GUI search filter
  SEARCH_FILTER,

  // Anvil GUI prompt item in first slot
  PROMPT_PLACEHOLDER,

  // New choice when in multiple choice GUI
  NEW_CHOICE,

  // Submit choices when in multiple choice GUI
  SUBMIT_CHOICES,

  // Previous page of pagination
  PREV_PAGE,

  // Next page of pagination
  NEXT_PAGE,

  // Current page indicator of pagination
  PAGE_INDICATOR
}
