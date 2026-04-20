package eu.lunarlupa.globalrules;

import com.google.common.base.CaseFormat;
import com.google.common.base.Converter;
import net.minecraft.world.level.GameRules;
import net.neoforged.neoforge.common.ModConfigSpec;
import eu.lunarlupa.globalrules.config.DifConfig;
import eu.lunarlupa.globalrules.config.MiscConfig;
import eu.lunarlupa.globalrules.config.GamerulesConfig;

import java.util.HashMap;
//import com.electronwill.nightconfig.core.Config;

public class Config {
      public static ModConfigSpec spec;
      private static final HashMap<GameRules.Key<?>, String> COMMENTS = new HashMap<>();
      private static final Converter<String, String> RULE_CONVERTER = CaseFormat.LOWER_CAMEL.converterTo(CaseFormat.LOWER_UNDERSCORE);

      static {


          ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
          GamerulesConfig.init(builder);
          DifConfig.init(builder);
          MiscConfig.init(builder);
          spec = builder.build();
      }
}
