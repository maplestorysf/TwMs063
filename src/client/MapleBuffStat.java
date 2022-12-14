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
package client;

import java.io.Serializable;

public enum MapleBuffStat implements Serializable {

    //物理攻擊力
    WATK(0),
    //物理防禦力
    WDEF(1),
    //魔法攻擊力
    MATK(2),
    //魔法防禦力
    MDEF(3),
    //命中率
    ACC(4),
    //迴避率
    AVOID(5),
    //手技 
    HANDS(6),
    //移動速度 
    SPEED(7),
    //跳躍力
    JUMP(8),
    //魔心防禦 
    MAGIC_GUARD(9),
    //隱藏術  
    DARKSIGHT(10),
    //攻擊加速    
    BOOSTER(11),
    //反射之盾
    POWERGUARD(12),
    //最大HP
    MAXHP(13),
    //最大MP
    MAXMP(14),
    //神聖之光 
    INVINCIBLE(15),
    //無形之箭    
    SOULARROW(16),
    //昏迷
    STUN(17),
    //中毒
    POISON(18),
    //封印
    SEAL(19),
    //黑暗
    DARKNESS(20),
    //鬥氣集中
    COMBO(21),
    //召喚獸
    SUMMON(21), //hack buffstat for summons ^.- (does/should not increase damage... hopefully <3)
    //屬性攻擊
    WK_CHARGE(22),
    //龍之力量 ? 需要測試
    DRAGONBLOOD(23),
    //神聖祈禱
    HOLY_SYMBOL(24),
    //幸運術
    MESOUP(25),
    //影分身
    SHADOWPARTNER(26),
    //勇者掠奪術
    PICKPOCKET(27),
    //替身術
    PUPPET(28), // HACK - shares buffmask with pickpocket - odin special ^.-
    //楓幣護盾
    MESOGUARD(29),
    //虛弱 
    WEAKEN(30),
    //詛咒
    CURSE(31),
    //緩慢 
    SLOW(32),
    //變身
    MORPH(33),
    //恢復
    RECOVERY(34),
    //楓葉祝福  
    MAPLE_WARRIOR(35),
    //格擋(穩如泰山)   
    STANCE(36),
    //銳利之眼  
    SHARP_EYES(37),
    //魔法反擊
    MANA_REFLECTION(38),
    //誘惑  
    DRAGON_ROAR(39),
    //暗器傷人
    SPIRIT_CLAW(40),
    //魔力無限
    INFINITY(41),
    //進階祝福    
    HOLY_SHIELD(42),
    //敏捷提升    
    HAMSTRING(43),
    //命中率增加
    BLIND(44),
    //集中精力
    CONCENTRATE(45),
    //怪物騎乘
    MONSTER_RIDING(46),
    //不死化
    ZOMBIFY(47),
    //英雄的回響  
    ECHO_OF_HERO(48),
    UNKNOWN3(49),
    GHOST_MORPH(50),
    ARIANT_COSS_IMU(51), // The white ball around you
    DROP_RATE(53),
    MESO_RATE(54),
    EXPRATE(55),
    ACASH_RATE(56),
    GM_HIDE(57),
    
    BERSERK_FURY(999),
    ILLUSION(999),
    SPARK(999),
    DIVINE_BODY(999),
    FINAL_MELEE_ATTACK(999),
    FINAL_SHOOT_ATTACK(999),
    ELEMENT_RESET(999),
    WIND_WALK(999),
    UNKNOWN7(999),
    ARAN_COMBO(999),//68
    COMBO_DRAIN(999),//69
    COMBO_BARRIER(999),//70
    BODY_PRESSURE(999),//71
    SMART_KNOCKBACK(999),//72
    SOUL_STONE(999), //same as pyramid_pq
    ENERGY_CHARGE(999),//75
    DASH_SPEED(999),
    DASH_JUMP(999),
    SPEED_INFUSION(999),
    HOMING_BEACON(999),
    SOARING(999),
    FREEZE(999),
    LIGHTNING_CHARGE(999),
    MIRROR_IMAGE(999),
    OWL_SPIRIT(999), //POST BB
    ;

    private static final long serialVersionUID = 0L;
    private final int buffstat;
    private final int first;
    private final long oldvalue;

    private MapleBuffStat(int buffstat) {
        this.buffstat = 1 << (buffstat % 32);
        this.first = (int) Math.floor(buffstat / 32);
        this.oldvalue = new Long(this.buffstat) << (32 * (first % 2 + 1));
    }

    private MapleBuffStat(int buffstat, boolean stacked) {
        this.buffstat = 1 << ((buffstat % 32));
        this.first = (int) Math.floor(buffstat / 32);
        this.oldvalue = new Long(this.buffstat) << (32 * (first % 2 + 1));
    }

    public final long getOldValue() {
        return this.oldvalue;
    }

    public final int getPosition() {
        return first;
    }

    public final int getValue() {
        return buffstat;
    }
}
