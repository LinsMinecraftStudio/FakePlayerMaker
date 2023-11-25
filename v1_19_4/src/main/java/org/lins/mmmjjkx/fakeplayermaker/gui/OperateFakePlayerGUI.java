package org.lins.mmmjjkx.fakeplayermaker.gui;

import io.github.linsminecraftstudio.fakeplayermaker.api.utils.MinecraftUtils;
import io.github.linsminecraftstudio.polymer.gui.InventoryActionType;
import io.github.linsminecraftstudio.polymer.gui.SimpleInventoryHandler;
import io.github.linsminecraftstudio.polymer.itemstack.ItemStackBuilder;
import io.github.linsminecraftstudio.polymer.objects.array.ObjectArray;
import io.github.linsminecraftstudio.polymer.utils.UserInputGetter;
import net.kyori.adventure.text.Component;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.lins.mmmjjkx.fakeplayermaker.FakePlayerMaker;
import org.lins.mmmjjkx.fakeplayermaker.utils.NMSFakePlayerMaker;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

class OperateFakePlayerGUI extends SimpleInventoryHandler {
    private final ServerPlayer data;
    private final FakePlayerMaker fpm = FakePlayerMaker.INSTANCE;

    public OperateFakePlayerGUI(ServerPlayer data) {
        this.data = data;
    }

    @Override
    public void placeButtons(Player p, Inventory inv) {
        Player bukkit = data.getBukkitEntity();
        Date d = new Date(bukkit.getFirstPlayed());
        ItemStackBuilder infoBuilder = new ItemStackBuilder(Material.PLAYER_HEAD, 1)
                .name(Component.text(data.getName().getString()))
                .lore(fpm.getMessageHandler().getColoredMessages(p, "GUI.Info", new ObjectArray(bukkit.getUniqueId().toString())
                        , new ObjectArray(new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(d))));

        if (bukkit.getPlayerProfile().getTextures().getSkin() != null) {
            infoBuilder = infoBuilder.head(bukkit.getPlayerProfile().getTextures().getSkin().toString());
        }

        inv.setItem(13, infoBuilder.build());

        ItemStack tp = new ItemStackBuilder(Material.COMPASS, 1)
                .name(fpm.getMessageHandler().getColored(p, "GUI.Button.TP"))
                .build();
        inv.setItem(19, tp);

        ItemStack tph = new ItemStackBuilder(Material.COMPASS, 1)
                .name(fpm.getMessageHandler().getColored(p, "GUI.Button.TPHere"))
                .build();
        inv.setItem(21, tph);

        ItemStack remove = new ItemStackBuilder(Material.BARRIER, 1)
                .name(fpm.getMessageHandler().getColored(p, "GUI.Button.Remove"))
                .build();

        inv.setItem(23, remove);

        ItemStack skin = new ItemStackBuilder(Material.PLAYER_HEAD, 1)
                .name(fpm.getMessageHandler().getColored(p, "GUI.Button.Skin"))
                .build();
        inv.setItem(25, skin);

        ItemStack inventory = new ItemStackBuilder(Material.CHEST, 1)
                .name(fpm.getMessageHandler().getColored(p, "GUI.Button.Inventory"))
                .build();
        inv.setItem(29, inventory);

        ItemStack sneak = new ItemStackBuilder(Material.LEATHER_BOOTS, 1)
                .name(fpm.getMessageHandler().getColored(p, "GUI.Button.Sneak"))
                .build();
        inv.setItem(31, sneak);
    }

    @Override
    public Component title(Player p) {
        return fpm.getMessageHandler().getColored(p, "GUI.EachPlayerTitle", data.getName().getString());
    }

    @Override
    public void doListen(InventoryActionType type, Player p, int slot, Inventory inventory) {
        Player bukkit = data.getBukkitEntity();
        if (type == InventoryActionType.CLICK) {
            switch (slot) {
                case 19:
                    p.teleport(bukkit);
                    p.closeInventory();
                    sendMessage(p, "TeleportSuccess");
                    break;
                case 21:
                    bukkit.teleport(p);
                    break;
                case 23:
                    NMSFakePlayerMaker.removeFakePlayer(bukkit.getName(), null);
                    p.closeInventory();
                    break;
                case 25:
                    p.closeInventory();
                    String result = UserInputGetter.getUserInput(fpm.getMessageHandler().getColored(p, "GUI.SkinInput"), p);
                    if (result != null) {
                        try {
                            MinecraftUtils.skinChange(data, result);
                            fpm.getMessageHandler().sendMessage(p, "SkinChanged");
                        } catch (IOException e) {
                            fpm.getMessageHandler().sendMessage(p, "SkinChangeFailed");
                        }
                        FakePlayerMaker.fakePlayerSaver.syncPlayerInfo(data);
                        open(p);
                    }
                    break;
                case 29:
                    p.closeInventory();
                    p.openInventory(bukkit.getInventory());
                    break;
                case 31:
                    bukkit.setSneaking(!bukkit.isSneaking());
                    break;
            }
        }
    }

    private void sendMessage(CommandSender commandSender, String key, Object... args) {
        fpm.getMessageHandler().sendMessage(commandSender, key, args);
    }
}
