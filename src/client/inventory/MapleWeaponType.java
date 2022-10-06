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
package client.inventory;

public enum MapleWeaponType {

    沒有武器(0.0f, 0),
    單手劍(1.2f, 20),
    單手斧(1.2f, 20),
    單手棍(1.2f, 20),
    短劍(1.3f, 20),
    短杖(1.0f, 25),//RED改版後冒險家法師係數為1.2
    長杖(1.0f, 25),//RED改版後冒險家法師係數為1.2
    雙手劍(1.34f, 20),
    雙手斧(1.34f, 20),
    雙手棍(1.34f, 20),
    槍(1.49f, 20),
    矛(1.49f, 20),
    弓(1.3f, 15),
    弩(1.35f, 15),
    拳套(1.75f, 15),
    指虎(1.7f, 20),
    火槍(1.5f, 15),
    未知(0.0f, 0),;
    private final float damageMultiplier; // 武器係數
    private final int baseMastery; // 初始武器熟練度

    private MapleWeaponType(final float maxDamageMultiplier, int baseMastery) {
        this.damageMultiplier = maxDamageMultiplier;
        this.baseMastery = baseMastery;
    }

    public final float getMaxDamageMultiplier() {
        return damageMultiplier;
    }

    public final int getBaseMastery() {
        return baseMastery;
    }
};
