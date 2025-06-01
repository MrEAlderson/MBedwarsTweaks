package me.metallicgoat.tweaksaddon.tweaks.misc;

import de.marcely.bedwars.api.GameAPI;
import de.marcely.bedwars.api.arena.Arena;
import de.marcely.bedwars.api.arena.ArenaStatus;
import de.marcely.bedwars.api.event.player.PlayerOpenArenaChestEvent.ChestType;
import de.marcely.bedwars.api.message.Message;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import me.metallicgoat.tweaksaddon.config.MainConfig;
import me.metallicgoat.tweaksaddon.config.SwordsToolsConfig;
import me.metallicgoat.tweaksaddon.tweaks.advancedswords.ToolSwordHelper;
import org.apache.commons.lang.mutable.MutableInt;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.Nullable;

public class PuchDepositChest implements Listener {

  @EventHandler
  public void onPlayerInteract(PlayerInteractEvent event) {
    if (!MainConfig.punch_deposit_chest_enabled || event.getAction() != Action.LEFT_CLICK_BLOCK)
      return;

    final Player player = event.getPlayer();
    final Arena arena = GameAPI.get().getArenaByPlayer(player);

    if (arena == null || arena.getStatus() != ArenaStatus.RUNNING)
      return;

    final Block block = event.getClickedBlock();
    final ChestType chestType = arena.getChestType(block);

    if (chestType == null)
      return;

    final PlayerInventory pInv = player.getInventory();
    final ItemStack mainHand = pInv.getItem(pInv.getHeldItemSlot());

    if (mainHand == null || mainHand.getType() == Material.AIR)
      return;

    // anti chest
    if (SwordsToolsConfig.anti_chest_enabled) {
      if (SwordsToolsConfig.anti_chest_materials.contains(mainHand.getType()) &&
          ToolSwordHelper.isNotToIgnore(mainHand))
        return;
    }

    // identify amount we can deposit
    final Inventory cInv = arena.getChestInventory(chestType, player);

    if (cInv == null)
      return;

    final MutableInt available = new MutableInt();
    final List<Integer> relevantSlots = IntStream.range(0, cInv.getSize())
        .filter(slot -> {
          final ItemStack is = cInv.getItem(slot);

          if (is == null || is.getType() == Material.AIR) {
            available.add(mainHand.getMaxStackSize());
            return true;
          }

          if (!is.isSimilar(mainHand))
            return false;

          final int avail = Math.max(0, mainHand.getMaxStackSize() - is.getAmount());

          available.add(avail);
          return avail != 0;
        })
        .boxed()
        .sorted(Comparator.comparing(slot -> {
          final ItemStack is = cInv.getItem(slot);

          return is != null && is.getType() != Material.AIR;
        }))
        .collect(Collectors.toCollection(ArrayList::new));

    if (available.intValue() == 0)
      return;

    // deposit
    int depositAmount = 0;
    int minSlot, maxSlot;

    if (player.isSneaking()) {
      minSlot = 0;
      maxSlot = pInv.getSize()-1;
    } else
      minSlot = maxSlot = pInv.getHeldItemSlot();

    // go item-by-item in player inv
    for (int i=minSlot; i<=maxSlot; i++) {
      final ItemStack is = pInv.getItem(i);

      if (is == null || !is.isSimilar(mainHand))
        continue;

      final int reduceBy = Math.max(0, Math.min(is.getMaxStackSize()-is.getAmount(), available.intValue()));

      if (reduceBy == 0)
        continue;

      // deposit one slot
      for (int addLeft=reduceBy; addLeft>0; ) {
        if (relevantSlots.isEmpty())
          break;

        final Integer cSlot = relevantSlots.get(0);
        ItemStack cIs = cInv.getItem(cSlot);

        if (cIs == null || cIs.getType() == Material.AIR)
          (cIs = is.clone()).setAmount(addLeft -= is.getMaxStackSize());
        else {
          final int add = Math.min(reduceBy, cIs.getMaxStackSize()-cIs.getAmount());

          cIs.setAmount(cIs.getAmount() + add);
          addLeft -= add;

          if (cIs.getAmount() == cIs.getMaxStackSize())
            relevantSlots.remove(0);
        }

        cInv.setItem(cSlot, cIs);
      }

      depositAmount += reduceBy;
      available.subtract(reduceBy);

      if (available.intValue() == 0)
        break;
    }

    final Message msg = getMessage(chestType);
  }

  @Nullable
  private static Message getMessage(ChestType type) {
    switch (type) {
      case TEAM:
        return Message.buildByKey("Tweaks_PunchDepositChest_TeamChest");
      case PRIVATE:
        return Message.buildByKey("Tweaks_PunchDepositChest_PrivateChest");
      default:
        return null;
    }
  }
}
