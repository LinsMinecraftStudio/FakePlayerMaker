package org.lins.mmmjjkx.ownfakeplayers.utils;

import io.github.linsminecraftstudio.polymer.utils.ListUtil;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;

import java.util.Optional;

public class PermissionCheck {
    public static int getCreationLimit(Player p) {
        Optional<PermissionAttachmentInfo> info = ListUtil.getIf(p.getEffectivePermissions(), i -> i.getPermission().startsWith("ownfakeplayers.create."));
        if (info.isEmpty()) {
            return 0;
        }
        PermissionAttachmentInfo info1 = info.get();
        return Integer.parseInt(info1.getPermission().substring("ownfakeplayers.create.".length()));
    }
}
