/*
 This file is part of the OdinMS Maple Story Server
 Copyright (C) 2008 ~ 2010 Patrick Huy <patrick.huy@frz.cc> 
 Matthias Butz <matze@odinms.de>
 Jan Christian Meyer <vimes@odinms.de>

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License version 3
 as published by the Free Software Foundation. You may not use, modify
 or distribute this program under any other version of the
 GNU Affero General Public License.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package handling.channel.handler;

import java.awt.Point;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

import client.ISkill;
import client.MapleBuffStat;
import client.MapleClient;
import client.MapleCharacter;
import client.SkillFactory;
import client.SummonSkillEntry;
import client.status.MonsterStatusEffect;
import client.anticheat.CheatingOffense;
import client.status.MonsterStatus;
import constants.skills.SkillType.黑騎士;
import java.util.Map;
import server.MapleStatEffect;
import server.AutobanManager;
import server.Randomizer;
import server.movement.LifeMovementFragment;
import server.life.MapleMonster;
import server.life.SummonAttackEntry;
import server.maps.MapleMap;
import server.maps.MapleSummon;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import server.maps.SummonMovementType;
import tools.MaplePacketCreator;
import tools.data.LittleEndianAccessor;
import tools.packet.MobPacket;

public class SummonHandler {

    public static final void MoveSummon(final LittleEndianAccessor slea, final MapleCharacter chr) {
        int skillId = slea.readInt();
        final int oid = chr.getSummons().get(skillId).getObjectId();
        Point startPos = new Point(slea.readShort(), slea.readShort());
        List<LifeMovementFragment> res = MovementParse.parseMovement(slea, 4);
//        slea.skip(4); //startPOS
//        final List<LifeMovementFragment> res = MovementParse.parseMovement(slea, 4);
        if (chr == null) {
            return;
        }
        for (MapleSummon sum : chr.getSummons().values()) {
            if (sum.getObjectId() == oid && sum.getMovementType() != SummonMovementType.STATIONARY) {
                final Point pos = sum.getPosition();
                MovementParse.updatePosition(res, sum, 0);
                if (!sum.isChangedMap()) {
                    chr.getMap().broadcastMessage(chr, MaplePacketCreator.moveSummon(chr.getId(), skillId, startPos, res), sum.getPosition());
                }
                break;
            }
        }
    }

    public static final void DamageSummon(final LittleEndianAccessor slea, final MapleCharacter chr) {
        final int unkByte = slea.readByte();
        final int damage = slea.readInt();
        final int monsterIdFrom = slea.readInt();
        //       slea.readByte(); // stance

        final Iterator<MapleSummon> iter = chr.getSummons().values().iterator();
        MapleSummon summon;

        while (iter.hasNext()) {
            summon = iter.next();
            if (summon.isPuppet() && summon.getOwnerId() == chr.getId()) { //We can only have one puppet(AFAIK O.O) so this check is safe.
                summon.addHP((short) -damage);
                if (summon.getHP() <= 0) {
                    chr.cancelEffectFromBuffStat(MapleBuffStat.PUPPET);
                }
                chr.getMap().broadcastMessage(chr, MaplePacketCreator.damageSummon(chr.getId(), summon.getSkill(), damage, unkByte, monsterIdFrom), summon.getPosition());
                break;
            }
        }
    }

    public static final void SubSummon(final LittleEndianAccessor slea, final MapleCharacter chr) {
        final MapleMapObject obj = chr.getMap().getMapObject(slea.readInt(), MapleMapObjectType.SUMMON);
        if (obj == null || !(obj instanceof MapleSummon)) {
            return;
        }
        final MapleSummon sum = (MapleSummon) obj;
        if (sum.getOwnerId() != chr.getId() || sum.getSkillLevel() <= 0 || !chr.isAlive()) {
            return;
        }
        switch (sum.getSkill()) {
            case 黑騎士.暗之靈魂: // 黑暗之魂
                ISkill bHealing = SkillFactory.getSkill(slea.readInt());
                final int bHealingLvl = chr.getSkillLevel(bHealing);
                if (bHealingLvl <= 0 || bHealing == null) {
                    return;
                }
                final MapleStatEffect healEffect = bHealing.getEffect(bHealingLvl);

                if (bHealing.getId() == 黑騎士.黑暗守護) {
                    healEffect.applyTo(chr);
                } else if (bHealing.getId() == 黑騎士.闇靈治癒) {
                    if (!chr.canSummon(healEffect.getX() * 1000)) {
                        return;
                    }
                    chr.addHP(healEffect.getHp());
                }
                chr.getClient().sendPacket(MaplePacketCreator.showOwnBuffEffect(sum.getSkill(), 2));
                chr.getMap().broadcastMessage(MaplePacketCreator.summonSkill(chr.getId(), sum.getSkill(), bHealing.getId() == 黑騎士.闇靈治癒 ? 5 : (Randomizer.nextInt(3) + 6)));
                chr.getMap().broadcastMessage(chr, MaplePacketCreator.showBuffeffect(chr.getId(), sum.getSkill(), 2), false);
                break;
            default:
                if (chr.isShowErr()) {
                    chr.showInfo("子召喚獸", true, "未處理的子召喚獸::" + sum.getSkill());
                }
        }
    }

    public static void SummonAttack(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        if (chr == null || !chr.isAlive()) {
            return;
        }
        final MapleMap map = chr.getMap();
        final MapleMapObject obj = map.getMapObject(chr.getSummons().get(slea.readInt()).getObjectId(), MapleMapObjectType.SUMMON);
        if (obj == null) {
            return;
        }
        final MapleSummon summon = (MapleSummon) obj;
        if (summon.getOwnerId() != chr.getId() || summon.getSkillLevel() <= 0) {
            return;
        }
        final SummonSkillEntry sse = SkillFactory.getSummonData(summon.getSkill());
        if (sse == null) {
            return;
        }
//        slea.skip(8);
//        int tick = slea.readInt();
//        chr.updateTick(tick);
//        summon.CheckSummonAttackFrequency(chr, tick);
//        slea.skip(8);
        final byte animation = slea.readByte();
//        slea.skip(4);
        final byte numAttacked = slea.readByte();
        if (numAttacked > sse.mobCount) {
            chr.getCheatTracker().registerOffense(CheatingOffense.召喚獸攻擊怪物數量異常, " 召喚獸: " + summon.getSkill() + "  地圖: " + chr.getMapId() + " 正常怪物數量: " + sse.mobCount + " 攻擊怪物數量: " + numAttacked);
            return;
        }
//        slea.skip(8); //some pos stuff
        final List<SummonAttackEntry> allDamage = new ArrayList<>();
        chr.getCheatTracker().checkSummonAttack();

        for (int i = 0; i < numAttacked; i++) {
            final MapleMonster mob = map.getMonsterByOid(slea.readInt());

            if (mob == null) {
                continue;
            }
            double range = 400000.0;
            switch (summon.getSkill()) {
                case 5220002:
                    range = 590000.0;
                    break;
            }
            if (summon.getPosition().distanceSq(mob.getPosition()) > range && !chr.inBossMap()) {
                chr.getCheatTracker().registerOffense(CheatingOffense.召喚獸全圖打, " 召喚獸: " + summon.getSkill() + "  地圖: " + chr.getMapId() + " 預計範圍: " + range + " 實際範圍: " + (long) (chr.getPosition().distanceSq(mob.getPosition())) + " ");
                return;
            }
            slea.skip(14); // who knows
            final int damage = slea.readInt();
            allDamage.add(new SummonAttackEntry(mob, damage));
        }
        if (!summon.isChangedMap()) {
            map.broadcastMessage(chr, MaplePacketCreator.summonAttack(summon.getOwnerId(), summon.getObjectId(), animation, allDamage, chr.getLevel()), summon.getPosition());
        }
        final ISkill summonSkill = SkillFactory.getSkill(summon.getSkill());
        final MapleStatEffect summonEffect = summonSkill.getEffect(summon.getSkillLevel());

        if (summonEffect == null) {
            return;
        }
        for (SummonAttackEntry attackEntry : allDamage) {
            final int toDamage = attackEntry.getDamage();
            final MapleMonster mob = attackEntry.getMonster();

            if (toDamage > 0 && summonEffect.getMonsterStati().size() > 0) {
                if (summonEffect.makeChanceResult()) {
                    for (Map.Entry<MonsterStatus, Integer> z : summonEffect.getMonsterStati().entrySet()) {
                        mob.applyStatus(chr, new MonsterStatusEffect(z.getKey(), z.getValue(), summonSkill.getId(), null, false), summonEffect.isPoison(), 4000, true, summonEffect);
                    }
                }
            }
            if (chr.isGM() || toDamage < 120000) {
                mob.damage(chr, toDamage, true);
                chr.checkMonsterAggro(mob);
                if (!mob.isAlive()) {
                    chr.getClient().sendPacket(MobPacket.killMonster(mob.getObjectId(), 1));
                }
            } else {
                AutobanManager.getInstance().autoban(c, "High Summon Damage (" + toDamage + " to " + attackEntry.getMonster().getId() + ")");
                // TODO : Check player's stat for damage checking.
            }
        }
        if (summon.isGaviota()) {
            chr.getMap().broadcastMessage(MaplePacketCreator.removeSummon(summon, true));
            chr.getMap().removeMapObject(summon);
            chr.removeVisibleMapObject(summon);
            chr.cancelEffectFromBuffStat(MapleBuffStat.SUMMON);
            //TODO: Multi Summoning, must do something about hack buffstat
        }
    }
}
