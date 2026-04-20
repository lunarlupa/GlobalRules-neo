package eu.lunarlupa.globalrules;

import com.mojang.logging.LogUtils;

import eu.lunarlupa.globalrules.config.GamerulesConfig;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(Globalrules.MODID)
public class Globalrules {
    public static final String MODID = "globalrules";
    private static final Logger LOGGER = LogUtils.getLogger();

    public Globalrules(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.spec);

        NeoForge.EVENT_BUS.register(WorldEvents.class);

        modEventBus.addListener(this::loadComplete);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
    }

    public void loadComplete(FMLLoadCompleteEvent e) {
        e.enqueueWork(() -> {
            GamerulesConfig.updateModdedGamerules(null);
        });
    }

}
