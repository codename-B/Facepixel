package de.bananaco;

import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.client.config.GuiConfig;

public class FacepixelConfig extends GuiConfig {
    public FacepixelConfig(GuiScreen parentScreen) {
        super(parentScreen,
                new ConfigElement(Facepixel.config.getCategory(Configuration.CATEGORY_CLIENT)).getChildElements(),
                Facepixel.MODID, false, false,
                "",
                Facepixel.configFile.getAbsolutePath());
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        Facepixel.config.save();
    }
}
