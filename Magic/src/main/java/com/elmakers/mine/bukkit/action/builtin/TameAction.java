package com.elmakers.mine.bukkit.action.builtin;

import com.elmakers.mine.bukkit.action.BaseSpellAction;
import com.elmakers.mine.bukkit.api.action.CastContext;
import com.elmakers.mine.bukkit.api.spell.SpellResult;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;

public class TameAction extends BaseSpellAction
{
	@Override
	public SpellResult perform(CastContext context)
	{
        Entity entity = context.getTargetEntity();
		if (!(entity instanceof Tameable))
		{
			return SpellResult.NO_TARGET;
		}

		Tameable tameable = (Tameable)entity;
		if (tameable.isTamed()) {
			return SpellResult.NO_TARGET;
		}
		tameable.setTamed(true);
		Player tamer = context.getMage().getPlayer();
		if (tamer != null) {
			tameable.setOwner(tamer);
		}
		return SpellResult.CAST;
	}

	@Override
	public boolean isUndoable()
	{
		return false;
	}

    @Override
    public boolean requiresTargetEntity()
    {
        return true;
    }
}
