package com.wildex999.schematicbuilder;

import net.minecraftforge.fml.common.Optional;

public class ModCheckCoFHAPI extends ModCheck {

	@Override
	@Optional.Method(modid = "CoFHAPI|energy")
	public boolean hasMod() {
		return true;
	}
}
