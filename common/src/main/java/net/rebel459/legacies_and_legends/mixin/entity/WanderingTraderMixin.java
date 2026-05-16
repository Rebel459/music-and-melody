package net.rebel459.legacies_and_legends.mixin.entity;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.item.trading.TradeSet;
import net.rebel459.legacies_and_legends.LaLConstants;
import net.rebel459.legacies_and_legends.config.LaLConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WanderingTrader.class)
public abstract class WanderingTraderMixin {

    @Unique
    private static final ResourceKey<TradeSet> WANDERING_TRADER_MAPS = ResourceKey.create(Registries.TRADE_SET, LaLConstants.id("wandering_trader/maps"));

    @Inject(at = @At("HEAD"), method = "updateTrades")
    private void mapTrades(ServerLevel level, CallbackInfo ci) {
        if (!LaLConfig.get().misc.wandering_trader_trades || !LaLConfig.get().structures.dungeon_overhaul) return;
        WanderingTrader trader = WanderingTrader.class.cast(this);
        trader.addOffersFromTradeSet(level, trader.getOffers(), WANDERING_TRADER_MAPS);
    }
}
