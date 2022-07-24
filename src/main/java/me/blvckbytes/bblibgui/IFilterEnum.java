package me.blvckbytes.bblibgui;

import me.blvckbytes.bblibutil.IEnum;

import java.lang.reflect.Field;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 07/23/2022

  An enum which is used in combination with filtering based
  on a list of fields of the bound representitive object.
*/
public interface IFilterEnum<T extends Enum<?>> extends IEnum<T> {

  /**
   * Get a list of target fields to use for filtering
   */
  Field[] getFields();

  /**
   * Get the next enum value in the enum's ordinal sequence
   * and wrap around if performed on the last value
   * @return Next enum value
   */
  IFilterEnum<T> nextValue();

}
