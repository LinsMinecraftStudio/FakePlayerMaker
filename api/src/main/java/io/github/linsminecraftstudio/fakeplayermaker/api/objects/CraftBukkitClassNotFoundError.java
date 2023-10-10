package io.github.linsminecraftstudio.fakeplayermaker.api.objects;

public class CraftBukkitClassNotFoundError extends Error {
    private final String clazz;

    public CraftBukkitClassNotFoundError(String clazz, Throwable t) {
        super(t);
        this.clazz = clazz;
    }

    @Override
    public String getMessage() {
        return "Could not find CraftBukkit class for " + clazz + " .\n" +
                "Maybe your server minecraft version is not compatible with the plugin or an error occurred while executing the remap task, " +
                "you should open a issue in our github repo!(Please confirm that you are using an official plugin)";
    }
}
