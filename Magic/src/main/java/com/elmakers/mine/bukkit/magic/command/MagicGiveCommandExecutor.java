package com.elmakers.mine.bukkit.magic.command;

import com.elmakers.mine.bukkit.api.magic.Mage;
import com.elmakers.mine.bukkit.api.magic.MagicAPI;
import com.elmakers.mine.bukkit.api.spell.SpellTemplate;
import com.elmakers.mine.bukkit.utility.DeprecatedUtils;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class MagicGiveCommandExecutor extends MagicTabExecutor {
	public MagicGiveCommandExecutor(MagicAPI api) {
		super(api);
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!api.hasPermission(sender, "Magic.commands.mgive"))
        {
            sendNoPermission(sender);
            return true;
        }

        if (args.length == 0 || args.length > 3)
		{
            sender.sendMessage("Usage: mgive [player] <item> [count]");
			return true;
		}

        String playerName = null;
		String itemName = null;
        String countString = null;

        if (args.length == 1) {
            itemName = args[0];
        } else if (args.length == 3) {
            playerName = args[0];
            itemName = args[1];
            countString = args[2];
        } else {
            playerName = args[0];
            Player testPlayer = DeprecatedUtils.getPlayer(playerName);
            if (testPlayer == null) {
                itemName = args[0];
                countString = args[1];
            } else {
                itemName = args[1];
            }
        }

        int count = 1;
        if (countString != null) {
            try {
                count = Integer.parseInt(countString);
            } catch (Exception ex) {
                sender.sendMessage("Error parsing count: " + countString + ", should be an integer.");
                return true;
            }
        }

        Player player = null;
        if (playerName != null) {
            player = DeprecatedUtils.getPlayer(playerName);
        }

        if (player == null) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Console usage: mgive <player> <item> [count]");
                return true;
            }
            player = (Player)sender;
        }

        if (itemName.equalsIgnoreCase("xp")) {
            api.giveExperienceToPlayer(player, count);
            sender.sendMessage("Gave " + count + " experience to " + player.getName());
            return true;
        } else if (itemName.equalsIgnoreCase("sp")) {
            Mage mage = api.getMage(player);
            mage.addSkillPoints(count);
            sender.sendMessage("Gave " + count + " skill points to " + player.getName());
            return true;
        } else {
            Mage mage = api.getMage(player);
            ItemStack item = api.createItem(itemName, mage);
            if (item == null) {
                sender.sendMessage(ChatColor.RED + "Unknown item type " + itemName);
                return true;
            }
            item.setAmount(count);
            String displayName = api.describeItem(item);
            sender.sendMessage("Gave " + count + " " + displayName + " to " + player.getName());
            api.giveItemToPlayer(player, item);
        }

        return true;
	}

	@Override
	public Collection<String> onTabComplete(CommandSender sender, String commandName, String[] args) {
		Set<String> options = new HashSet<String>();
        if (!sender.hasPermission("Magic.commands.mgive")) return options;

		if (args.length == 1) {
            options.addAll(api.getPlayerNames());
		}

        if (args.length == 1 || args.length == 2) {
            Collection<SpellTemplate> spellList = api.getSpellTemplates(sender.hasPermission("Magic.bypass_hidden"));
            for (SpellTemplate spell : spellList) {
                options.add(spell.getKey());
            }
            Collection<String> allWands = api.getWandKeys();
            for (String wandKey : allWands) {
                options.add(wandKey);
            }
            for (Material material : Material.values()) {
                options.add(material.name().toLowerCase());
            }
            Collection<String> allItems = api.getController().getItemKeys();
            for (String itemKey : allItems) {
                options.add(itemKey);
            }
            options.add("xp");
            options.add("sp");
		}
		return options;
	}
}
