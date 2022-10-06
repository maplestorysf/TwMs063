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
package client.anticheat;

public enum CheatingOffense {

    召喚獸無延遲(5, 6000, 50, 3),
    MOVE_MONSTERS(1, 30000),
    FAST_HP_MP_REGEN(5, 20000, 10, 2),
    SAME_DAMAGE(2, 30000, 150),
    ATTACK_WITHOUT_GETTING_HIT(1, 30000, 1200, 0),
    HIGH_DAMAGE_MAGIC(5, 30000, -1, 0),
    HIGH_DAMAGE_MAGIC_2(10, 30000, -1, 0),
    HIGH_DAMAGE(5, 30000, -1, 0),
    HIGH_DAMAGE_2(10, 30000, -1, 0),
    EXCEED_DAMAGE_CAP(5, 60000, 800, 0),
    ATTACK_FARAWAY_MONSTER(5, 60000, 500, 1),
    ATTACK_FARAWAY_MONSTER_BAN(3, 60000, 60, 3),
    REGEN_HIGH_HP(100, 30000, 1000, 2),
    REGEN_HIGH_MP(100, 30000, 1000, 2),
    ITEMVAC_CLIENT(5, 5000, 10),
    ITEMVAC_SERVER(1, 10000, 50, 2),
    PET_ITEMVAC_CLIENT(5, 20000, 20, 2),
    PET_ITEMVAC_SERVER(3, 10000, 150, 2),
    USING_FARAWAY_PORTAL(1, 60000, 100, 0),
    AST_TAKE_DAMAG(1, 60000, 100),
    HIGH_AVOID(20, 180000, 100),
    MISMATCHING_BULLETCOUNT(1, 300000),
    ETC_EXPLOSION(1, 300000),
    ATTACKING_WHILE_DEAD(1, 300000, -1, 0),
    USING_UNAVAILABLE_ITEM(1, 300000),
    EXPLODING_NONEXISTANT(1, 300000),
    快速回HP(1, 15000, 150, 1),
    快速回MP(1, 15000, 150, 1),
    快速攻擊(10, 60000, 50, 2),// 60秒內 觸發50次 DC
    無延遲攻擊(5, 10000, 20, 3),// 10秒內 觸發20次 自訂封鎖
    MOB_VAC_X(1, 10000, 20, 3),// 10秒內 觸發20次 自訂封鎖
    吸怪(1, 7000, 5, 3),// 7秒內 觸發5次 自訂封鎖
    ARAN_COMBO_HACK(1, 60000, 20, 2),// 60秒內 觸發7次 自訂封鎖
    召喚獸全圖打(5, 180000, 20, 2),// 180秒內觸二十次即DC
    召喚獸攻擊怪物數量異常(1, 600000, 7, 3),// 600秒內 觸發7次 自訂封鎖
    攻擊怪物數量異常(1, 600000, 7, 3),// 600秒內 觸發7次 自訂封鎖
    技能攻擊次數異常(1, 600000, 7, 3),// 600秒內 觸發7次 自訂封鎖
    群體治癒攻擊不死系怪物(1, 600000, 7, 3),// 600秒內 觸發7次 自訂封鎖
    無MP使用技能(1, 60000, 100, 3),// 60秒內 觸發20次 自訂封鎖
    無箭矢發射弓箭(1, 60000, 20, 3),// 60秒內 觸發20次 自訂封鎖
    無鬥氣使用鬥氣技能(1, 60000, 20, 3),// 60秒內 觸發20次 自訂封鎖
    ATTACK_TYPE_ERROR(5, 60000, 30, 2),;

    private final int points;
    private final long validityDuration;
    private final int autobancount;
    private int bantype = 0; // 0 = Disabled, 1 = Enabled, 2 = DC, 3 = custom Ban

    public final int getPoints() {
        return points;
    }

    public final long getValidityDuration() {
        return validityDuration;
    }

    public final boolean shouldAutoban(final int count) {
        if (autobancount == -1) {
            return false;
        }
        return count >= autobancount;
    }

    public final int getBanType() {
        return bantype;
    }

    public final void setEnabled(final boolean enabled) {
        bantype = (enabled ? 1 : 0);
    }

    public final boolean isEnabled() {
        return bantype >= 1;
    }

    private CheatingOffense(final int points, final long validityDuration) {
        this(points, validityDuration, -1, 1);
    }

    private CheatingOffense(final int points, final long validityDuration, final int autobancount) {
        this(points, validityDuration, autobancount, 1);
    }

    private CheatingOffense(final int points, final long validityDuration, final int autobancount, final int bantype) {
        this.points = points;
        this.validityDuration = validityDuration;
        this.autobancount = autobancount;
        this.bantype = bantype;
    }
}
