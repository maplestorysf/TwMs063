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
package handling.login;

import client.MapleClient;
import constants.ServerConfig;
import constants.WorldConstants;
import handling.channel.ChannelServer;
import handling.world.World;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import server.Timer.PingTimer;
import tools.FileoutputUtil;
import tools.MaplePacketCreator;
import tools.packet.LoginPacket;

public class LoginWorker {

    private static long lastUpdate = 0;

    public static void registerClient(final MapleClient c) {
        if (WorldConstants.ADMIN_ONLY && !c.isGm()) {
            c.sendPacket(MaplePacketCreator.getPopupMsg("伺服器目前正在維修中.\r\n目前管理員正在測試物品.\r\n請稍後等待維修。"));
            c.sendPacket(LoginPacket.getLoginFailed(7));
            return;
        }
        if (!c.isGm() && (c.hasBannedMac() || c.hasBannedIP())) {
            c.sendPacket(LoginPacket.getLoginFailed(3)); //
            return;
        }
        if (System.currentTimeMillis() - lastUpdate > 600000) { // Update once every 10 minutes
            lastUpdate = System.currentTimeMillis();
            Map<Integer, Map<Integer, Integer>> load = ChannelServer.getChannelLoad();
            int usersOn = 0;
            if (load == null || load.size() <= 0) { // In an unfortunate event that client logged in before load
                lastUpdate = 0;
                c.sendPacket(LoginPacket.getLoginFailed(7));
                return;
            }
            int channelsize = 0;
            for (Entry<Integer, Map<Integer, Integer>> entry : load.entrySet()) {
                for (Entry<Integer, Integer> entry_ : entry.getValue().entrySet()) {
                    channelsize++;
                }
            }

            final double loadFactor = 1200 / ((double) WorldConstants.UserLimit / channelsize);

            for (Entry<Integer, Map<Integer, Integer>> entry : load.entrySet()) {
                Map<Integer, Integer> load_ = new HashMap();
                for (Entry<Integer, Integer> entry_ : entry.getValue().entrySet()) {
                    usersOn += entry_.getValue();
                    load_.put(entry_.getKey(), Math.min(1200, (int) (entry_.getValue() * loadFactor)));
                }
                load.put(entry.getKey(), load_);

            }

            LoginServer.setLoad(load, usersOn);
            lastUpdate = System.currentTimeMillis();
        }

        if (c.finishLogin() == 0) {
            if (c.getSecondPassword() == null) {
                c.sendPacket(LoginPacket.getGenderNeeded(c));
            } else {
                World.clearChannelChangeDataByAccountId(c.getAccID());
                LoginServer.forceRemoveClient(c);
                ChannelServer.forceRemovePlayerByAccId(c, c.getAccID());
                LoginServer.getClientStorage().registerAccount(c);
                c.sendPacket(LoginPacket.getAuthSuccessRequest(c));
                for (World iWorld : LoginServer.getWorlds()) {
                    c.sendPacket(LoginPacket.getServerList(iWorld.getWorldId(), LoginPacket.Server.getById(iWorld.getWorldId()).toString(), iWorld.getFlag(), iWorld.getEventMessage(), iWorld.getChannels()));
                }
                c.sendPacket(LoginPacket.getEndOfServerList());
            }
            c.setIdleTask(PingTimer.getInstance().schedule(new Runnable() {

                @Override
                public void run() {
                    c.getSession().close();
                    if (ServerConfig.LOG_DC) {
                        FileoutputUtil.logToFile("logs/data/DC.txt", "\r\n伺服器主動斷開用戶端連接，調用位置: " + new java.lang.Throwable().getStackTrace()[0]);
                    }
                }
            }, 100 * 60 * 1000));// 60秒 * 100次 100分鐘(? 
        } else {
            c.sendPacket(LoginPacket.getLoginFailed(7));

        }
    }
}
