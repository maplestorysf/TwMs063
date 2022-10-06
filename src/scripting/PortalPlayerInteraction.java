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
package scripting;

import client.MapleClient;
import server.MaplePortal;
import server.life.MapleLifeFactory;
import tools.FileoutputUtil;

public class PortalPlayerInteraction extends AbstractPlayerInteraction {

    private final MaplePortal portal;

    public PortalPlayerInteraction(final MapleClient c, final MaplePortal portal) {
        super(c);
        this.portal = portal;
        if (c.getPlayer() != null) {
            c.getPlayer().setPortalNow(portal.getId());
        }
    }

    public final MaplePortal getPortal() {
        return portal;
    }

    public final void inFreeMarket() {
//        if (getMapId() != 910000000) {
        if (getPlayer().getLevel() >= 10) {
            saveLocation("FREE_MARKET");
            playPortalSE();
            warp(910000000, "st00");
        } else {
            playerMessage(5, "你需要10級才可以進入自由市場");
        }
//        }
    }

    // summon one monster on reactor location
    @Override
    public void spawnMonster(int id) {
        if (MapleLifeFactory.getMonster(id) != null) {
            spawnMonster(id, 1, portal.getPosition());
        } else {
            if (getPlayer() != null) {
                if (getPlayer().isStaff()) {
                    getPlayer().dropMessage("怪物代碼[" + id + "] 不存在");
                    FileoutputUtil.logToFile_PortalScript_Bug(getPlayer(), "含有 怪物代碼[" + id + "] 不存在 之漏洞\r\n");
                }
            }
        }
    }

    // summon monsters on reactor location
    @Override
    public void spawnMonster(int id, int qty) {
        if (MapleLifeFactory.getMonster(id) != null) {
            spawnMonster(id, qty, portal.getPosition());
        } else {
            if (getPlayer() != null) {
                if (getPlayer().isStaff()) {
                    getPlayer().dropMessage("怪物代碼[" + id + "] 不存在");
                    FileoutputUtil.logToFile_PortalScript_Bug(getPlayer(), "含有 怪物代碼[" + id + "] 不存在 之漏洞\r\n");
                }
            }
        }
    }
}
