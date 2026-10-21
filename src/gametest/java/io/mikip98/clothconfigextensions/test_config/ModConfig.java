package io.mikip98.clothconfigextensions.test_config;

import io.mikip98.clothconfigextensions.api.annotaions.ConfigExtension;
import io.mikip98.clothconfigextensions.test_config.enums.TestEnum;
import io.mikip98.clothconfigextensions.test_config.enums.TestSettings;
import io.mikip98.clothconfigextensions.test_config.enums.TestToggle;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

import java.util.EnumMap;
import java.util.Map;

@Config(name = "TestConfig")
@ConfigExtension.File.AutoComment
@ConfigExtension.Gui.GlobalTooltip
public class ModConfig implements ConfigData {
    @ConfigEntry.Gui.Excluded
    public static transient ModConfig INSTANCE;

    @ConfigExtension.Gui.IntLimit(min = 0)
    public int testIntLimited = 260;
    @ConfigExtension.Gui.LongLimit(max = 0)
    public long testLongLimited = -128;
    @ConfigExtension.Gui.FloatLimit(min = -63.4f, max = 63.4f)
    public float testFloatLimited = 0.4f;
    @ConfigExtension.Gui.DoubleLimit(min = -1.0, max = -0.9f)

    @ConfigExtension.File.AutoComment(langFile = "en_gb")
    public String testString = "testString";

    @ConfigExtension.File.AutoComment(translationsString = "this.is.custom.string")
    public TestEnum testEnumString = TestEnum.THIS;

    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    public TestEnum testEnumButton = TestEnum.THIS;

    @ConfigExtension.Gui.SkipGlobalTooltip
    public Map<TestSettings, Boolean> testEnumBooleanMap = getTestBooleanMap();
    protected Map<TestSettings, Boolean> getTestBooleanMap() {
        Map<TestSettings, Boolean> testMap = new EnumMap<>(TestSettings.class);
        testMap.put(TestSettings.OPTION_1, true);
        testMap.put(TestSettings.OPTION_2, false);
        testMap.put(TestSettings.OPTION_3, true);
        return testMap;
    }

    @ConfigExtension.File.SkipComment
    public Map<TestSettings, TestToggle> testEnumEnumMap = getTestEnumMap();
    protected Map<TestSettings, TestToggle> getTestEnumMap() {
        Map<TestSettings, TestToggle> testMap = new EnumMap<>(TestSettings.class);
        testMap.put(TestSettings.OPTION_1, TestToggle.AUTO);
        testMap.put(TestSettings.OPTION_2, TestToggle.OFF);
        testMap.put(TestSettings.OPTION_3, TestToggle.ON);
        return testMap;
    }

    @ConfigEntry.Gui.CollapsibleObject
    public NextConfigLevel nextConfigLevel = new NextConfigLevel();

    public static class NextConfigLevel {
        @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
        public TestEnum testEnumButton = TestEnum.THIS;

        public Map<TestSettings, Boolean> testEnumBooleanMap = getTestBooleanMap();
        protected Map<TestSettings, Boolean> getTestBooleanMap() {
            Map<TestSettings, Boolean> testMap = new EnumMap<>(TestSettings.class);
            testMap.put(TestSettings.OPTION_1, true);
            testMap.put(TestSettings.OPTION_2, false);
            testMap.put(TestSettings.OPTION_3, true);
            return testMap;
        }

        public Map<TestSettings, TestToggle> testEnumEnumMap = getTestEnumMap();
        protected Map<TestSettings, TestToggle> getTestEnumMap() {
            Map<TestSettings, TestToggle> testMap = new EnumMap<>(TestSettings.class);
            testMap.put(TestSettings.OPTION_1, TestToggle.AUTO);
            testMap.put(TestSettings.OPTION_2, TestToggle.OFF);
            testMap.put(TestSettings.OPTION_3, TestToggle.ON);
            return testMap;
        }

        @ConfigEntry.Gui.CollapsibleObject
        public ConfigLevelThree configLevelThree = new ConfigLevelThree();

        public static class ConfigLevelThree {
            @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
            public TestEnum testEnumButton = TestEnum.THIS;

            public Map<TestSettings, TestToggle> testEnumEnumMap = getTestEnumMap();
            protected Map<TestSettings, TestToggle> getTestEnumMap() {
                Map<TestSettings, TestToggle> testMap = new EnumMap<>(TestSettings.class);
                testMap.put(TestSettings.OPTION_1, TestToggle.AUTO);
                testMap.put(TestSettings.OPTION_2, TestToggle.OFF);
                testMap.put(TestSettings.OPTION_3, TestToggle.ON);
                return testMap;
            }
        }
    }
}
