package org.lins.mmmjjkx.fakeplayermaker.gui;

import io.github.linsminecraftstudio.polymer.gui.MultiPageInventoryHandler;
import io.github.linsminecraftstudio.polymer.itemstack.ItemStackBuilder;
import io.github.linsminecraftstudio.polymer.objects.array.ObjectArray;
import io.github.linsminecraftstudio.polymer.objects.plugin.PolymerPlugin;
import net.kyori.adventure.text.Component;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.lins.mmmjjkx.fakeplayermaker.FakePlayerMaker;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class ListFakePlayerGUIHandler extends MultiPageInventoryHandler<ServerPlayer> {
    private final PolymerPlugin fpm = FakePlayerMaker.INSTANCE;

    public ListFakePlayerGUIHandler(List<ServerPlayer> data) {
        super(data);
    }

    @Override
    public Component title(Player p) {
        return fpm.getMessageHandler().getColored(p, "GUI.Title");
    }

    @Override
    public Component search(Player p) {
        return fpm.getMessageHandler().getColored(p, "GUI.SearchInput");
    }

    @Override
    public void buttonHandle(Player p, int slot, ServerPlayer data) {
        OperateFakePlayerGUI gui = new OperateFakePlayerGUI(data);
        gui.open(p);
    }

    @Override
    public ItemStack getItemStackButton(Player p, int slot, ServerPlayer data) {
        ItemStackBuilder builder = new ItemStackBuilder(Material.PLAYER_HEAD, 1);
        Player bukkit = data.getBukkitEntity();
        Date d = new Date(bukkit.getFirstPlayed());

        builder = builder.name(Component.text(data.getName().getString()))
                .lore(fpm.getMessageHandler().getColoredMessages(p, "GUI.Info",
                        new ObjectArray(bukkit.getUniqueId().toString()),
                        new ObjectArray(new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(d))));

        if (bukkit.getPlayerProfile().getTextures().getSkin() != null) {
            builder = builder.head(bukkit.getPlayerProfile().getTextures().getSkin().toString());
        }

        return builder.build();
    }

    @Override
    public String toSearchableText(ServerPlayer data) {
        return data.getName().getString();
    }
}
