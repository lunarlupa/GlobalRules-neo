package eu.lunarlupa.globalrules.config;

import net.minecraft.world.Difficulty;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.EnumValue;

public class DifConfig {
    public static BooleanValue setDifficulty;
    public static EnumValue<Difficulty> difficulty;
    public static BooleanValue hardcore;
    public static BooleanValue enforceHardcore;
    public static BooleanValue lockDifficulty;

    public static void init(ModConfigSpec.Builder builder) {
        builder.push("difficulty");

        builder.comment("Determines if the difficulty is set on world load");
        setDifficulty = builder.define("setDifficulty", false);

        builder.comment("The difficulty that gets set is \"setDifficulty\" is 'true'");
        difficulty = builder.defineEnum("difficulty", Difficulty.NORMAL);

        builder.comment("Determines if a world is created in Hardcore mode");
        hardcore = builder.define("hardcore", false);

        builder.comment("Determines if the hardcore setting should be enforced on existing world (on join)");
        enforceHardcore = builder.define("enforceHardcore", true);

        builder.comment("Determines if the difficulty should be locked");
        lockDifficulty = builder.define("lockDifficulty", false);

        builder.pop();
    }
}
