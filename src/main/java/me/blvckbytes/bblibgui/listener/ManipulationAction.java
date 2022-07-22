package me.blvckbytes.bblibgui.listener;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/24/2022

  Represents the type of manipulation that can be performed on an inventory.
*/
public enum ManipulationAction {
  PICKUP,
  COLLECT,
  PLACE,
  SWAP,
  DROP,
  MOVE,

  // Click means that no item manipulations occurred
  CLICK;
}
