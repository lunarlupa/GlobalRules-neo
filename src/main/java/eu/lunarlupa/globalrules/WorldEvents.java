package eu.lunarlupa.globalrules;

import com.mojang.logging.LogUtils;
import eu.lunarlupa.globalrules.config.DifConfig;
import eu.lunarlupa.globalrules.config.GamerulesConfig;
import eu.lunarlupa.globalrules.config.MiscConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.storage.PrimaryLevelData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import org.apache.logging.log4j.LogManager;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Handles events for when the world is loaded/unloaded and when a player joins the world
 */
public class WorldEvents {

    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Runs the default command on users when they join a world.
     * This should not be called, it's automatically called by the {@link SubscribeEvent} annotation
     *
     * @param event The data for the {@link EntityJoinLevelEvent} event
     * @see EntityJoinLevelEvent
     */
    @SubscribeEvent
    public static void onWorldJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof Player player) {
            Level world = event.getLevel();
            MinecraftServer server = world.getServer();

            if (server != null) {
                MiscConfig.defaultCommands.get().forEach((s) -> {
                    String command = s.replaceAll("@p", player.getGameProfile().getName());
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command);
                });
            }
        }
    }

    /**
     * Applies all the gamerules and any configured difficultly configs from the config.
     * This should not be called, it's automatically called by the {@link SubscribeEvent} annotation
     *
     * @param event The data for the {@link LevelEvent.Load} event
     * @see LevelEvent.Load
     */
    @SubscribeEvent
    public static void onWorldLoad(LevelEvent.Load event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getLevel() instanceof ServerLevel world)) return;

        // Only apply on the primary level data as all dimensions share settings
        if (!(world.getLevelData() instanceof PrimaryLevelData info)) return;

        // Check if the settings only should be used as a default when creating a new world
        if (MiscConfig.useAsDefaultOnly.get() && info.isInitialized()) return;

        GameRules rules = info.getGameRules();
        MinecraftServer server = world.getServer();

        LOGGER.info("Applying config gamerules to level {}", info.getLevelName());
        GamerulesConfig.gameRules.forEach((key, configValue) -> {
            var rule = rules.getRule(key);
            if (rule instanceof GameRules.IntegerValue intVal && configValue instanceof IntValue config)
                intVal.set(config.get(), server);
            else if (rule instanceof GameRules.BooleanValue boolVal && configValue instanceof BooleanValue config)
                boolVal.set(config.get(), server);
            else
                LOGGER.warn("Could not apply gamerule {} as it is not an integer or boolean, it's {}", key.getId(), configValue.getClass().getSimpleName());
        });

        var entries = GamerulesConfig.modGameRules.get().valueMap();
        if (!entries.isEmpty()) {
            LOGGER.info("Applying config modded gamerules to level {}", info.getLevelName());
            GameRules.visitGameRuleTypes(new GameRules.GameRuleTypeVisitor() {
                @Override
                public <T extends GameRules.Value<T>> void visit(GameRules.@NotNull Key<T> key, GameRules.@NotNull Type<T> type) {
                    String category = key.getCategory().toString().toLowerCase();
                    var cat = entries.get(category);
                    if (!(cat instanceof com.electronwill.nightconfig.core.Config cfg)) return;

                    var val = cfg.get(key.getId());
                    if (val == null) return;

                    var rule = rules.getRule(key);
                    if (rule instanceof GameRules.IntegerValue intRule && val instanceof Integer iVal)
                        intRule.set(iVal, server);
                    else if (rule instanceof GameRules.BooleanValue boolRule && val instanceof Boolean bVal)
                        boolRule.set(bVal, server);
                    else
                        LOGGER.warn("Could not apply mod gamerule {} as it is not an integer or boolean, it's {}", key.getId(), val.getClass().getSimpleName());
                }
            });
        } else
            LOGGER.info("No modded gamerules to apply to level {}", info.getLevelName());

        if (!info.isDifficultyLocked()) {

            var hardcore = DifConfig.hardcore.get();
            var enforceHardcore = DifConfig.enforceHardcore.get();

            // Only change the hardcore setting if we are enforcing it
            if (enforceHardcore && info.isHardcore() != hardcore) {
                LevelSettings settings = info.settings;
                info.settings = new LevelSettings(settings.levelName(), settings.gameType(), hardcore, settings.difficulty(), settings.allowCommands(), settings.gameRules(), settings.getDataConfiguration());

                if (hardcore && info.getDifficulty() != Difficulty.HARD)
                    server.setDifficulty(Difficulty.HARD, false);

                if (hardcore)
                    LOGGER.info("Enabling hardcore in level {}", info.getLevelName());
                else
                    LOGGER.info("Disabling hardcore in level {}", info.getLevelName());
            }

            if (DifConfig.setDifficulty.get()) {
                Difficulty diff = DifConfig.difficulty.get();
                server.setDifficulty(diff, false);
                LOGGER.info("Setting difficulty of level {} to {}", info.getLevelName(), diff.toString());
            }
        }

        if (DifConfig.lockDifficulty.get()) {
            server.setDifficultyLocked(true);
            LOGGER.info("Locking difficulty of level {}", info.getLevelName());
        }
    }

    /**
     * Saves all the gamerules and any changed difficultly settings to the config.
     * This should not be called, it's automatically called by the {@link SubscribeEvent} annotation
     *
     * @param event The data for the {@link LevelEvent.Unload} event
     * @see LevelEvent.Unload
     */
    @SubscribeEvent
    public static void onWorldUnLoad(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getLevel() instanceof ServerLevel world)) return;

        // Only apply on the primary level data as all dimensions share settings
        if (!(world.getLevelData() instanceof PrimaryLevelData info)) return;

        // Check if the settings only should be used as a default we never save changes
        if (MiscConfig.useAsDefaultOnly.get()) return;

        GameRules rules = info.getGameRules();

        AtomicBoolean dirty = new AtomicBoolean(false);
        if (MiscConfig.saveGamerules.get()) {
            GamerulesConfig.gameRules.forEach((ruleKey, configValue) -> {
                GameRules.Value<?> val = rules.getRule(ruleKey);
                if (val instanceof GameRules.BooleanValue booleanValue) {
                    var config = (BooleanValue) configValue;
                    if (config.get() != booleanValue.get()) {
                        config.set(booleanValue.get());
                        dirty.set(true);
                    }
                } else if (val instanceof GameRules.IntegerValue intValue) {
                    var config = (IntValue) configValue;
                    if (config.get() != intValue.get()) {
                        config.set(intValue.get());
                        dirty.set(true);
                    }
                }
            });

            if (GamerulesConfig.updateModdedGamerules(rules))
                dirty.set(true);
        }

        if (DifConfig.setDifficulty.get() && !info.isDifficultyLocked()) {
            if (DifConfig.difficulty.get() != info.getDifficulty()) {
                DifConfig.difficulty.set(info.getDifficulty());
                dirty.set(true);
            }
        }

        if (dirty.get())
            Config.spec.save();
    }
}