package me.metallicgoat.tweaksaddon.tweaks.advancedswords;

import de.marcely.bedwars.api.BedwarsAPI;
import me.metallicgoat.tweaksaddon.config.SwordsToolsConfig;
import me.metallicgoat.tweaksaddon.utils.Util;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class AntiChest implements Listener {

  @EventHandler
  public void onShiftClick(InventoryClickEvent e) {
    if (!SwordsToolsConfig.anti_chest_enabled)
      return;

    if (e.getInventory().getSize() > 26) {
      if (e.getClick().isShiftClick() && inArena((Player) e.getWhoClicked())) {
        final Inventory clicked = e.getClickedInventory();

        if (clicked == e.getWhoClicked().getInventory()) {
          final ItemStack clickedOn = e.getCurrentItem();

          if (clickedOn != null
              && SwordsToolsConfig.anti_chest_materials.contains(clickedOn.getType())
              && ToolSwordHelper.isNotToIgnore(clickedOn)) {
            e.setCancelled(true);
          }
        }
      }
    }
  }

  @EventHandler
  public void onInventoryClick(InventoryClickEvent e) {
    if (!SwordsToolsConfig.anti_chest_enabled)
      return;

    final Inventory clicked = e.getClickedInventory();
    if (!(e.getInventory().getSize() > 26))
      return;

    if (e.getAction() == InventoryAction.HOTBAR_SWAP || e.getAction() == InventoryAction.HOTBAR_MOVE_AND_READD) {
      final HumanEntity player = e.getWhoClicked();
      final Inventory inventory = player != null ? player.getInventory() : null;

      if (inventory != null && clicked != inventory) {
        final int swapSlot = e.getHotbarButton();
        ItemStack movingItem = null;

        if (e.getClick().name().equals("SWAP_OFFHAND"))
          movingItem = Util.getItemInOffHand(player);

        else if (swapSlot >= 0)
          movingItem = inventory.getItem(swapSlot);

        if (movingItem != null
            && SwordsToolsConfig.anti_chest_materials.contains(movingItem.getType())
            && ToolSwordHelper.isNotToIgnore(movingItem)) {
          e.setCancelled(true);
          return;
        }
      }
    }

    if (clicked != e.getWhoClicked().getInventory() && inArena((Player) e.getWhoClicked())) {
      // The cursor item is going into the top inventory
      final ItemStack onCursor = e.getCursor();
      if (onCursor != null
          && SwordsToolsConfig.anti_chest_materials.contains(onCursor.getType())
          && ToolSwordHelper.isNotToIgnore(onCursor)) {
        e.setCancelled(true);
      }
    }
  }

  @EventHandler
  public void onInventoryDrag(InventoryDragEvent e) {
    if (!SwordsToolsConfig.anti_chest_enabled)
      return;

    final ItemStack dragged = e.getOldCursor();
    if (SwordsToolsConfig.anti_chest_materials.contains(dragged.getType())
        && inArena((Player) e.getWhoClicked())
        && ToolSwordHelper.isNotToIgnore(dragged)) {

      final int inventorySize = e.getInventory().getSize();
      for (int i : e.getRawSlots()) {
        if (i < inventorySize) {
          e.setCancelled(true);
          break;
        }
      }
    }
  }

  private boolean inArena(Player p) {
    return BedwarsAPI.getGameAPI().getArenaByPlayer(p) != null;
  }
}
