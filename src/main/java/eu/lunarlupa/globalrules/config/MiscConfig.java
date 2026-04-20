package eu.lunarlupa.globalrules.config;

import com.google.common.base.Strings;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.EnumValue;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;

import java.util.ArrayList;
import java.util.List;

public class MiscConfig {
    public static BooleanValue saveGamerules;
    public static BooleanValue useAsDefaultOnly;
    public static ConfigValue<List<? extends String>> defaultCommands;


    public static void init(ModConfigSpec.Builder builder) {
        builder.push("misc");

        builder.comment("Determines if gamerules should be saved to config on world leave");
        saveGamerules = builder.define("saveGamerules", false);

        builder.comment("Determines if rules and settings are only set on world creation");
        useAsDefaultOnly = builder.define("useAsDefaultOnly", false);

        builder.comment("A comma separated list of commands to run on world join, @p is replaced with joining player name, command is run by the server", "Example: default_commands = [\"/tellraw @p [\\\"\\\",{\\\"text\\\":\\\"Hi \\\"},{\\\"text\\\":\\\"@p\\\",\\\"color\\\":\\\"aqua\\\"}]\"]");
        defaultCommands = builder.defineList("defaultCommands", ArrayList::new, o -> !Strings.isNullOrEmpty(String.valueOf(o)) && String.valueOf(o).startsWith("/"));

        builder.pop();
    }
}
