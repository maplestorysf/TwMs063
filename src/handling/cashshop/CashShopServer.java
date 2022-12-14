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
package handling.cashshop;

import constants.ServerConfig;
import handling.MapleServerHandler;
import java.net.InetSocketAddress;

import handling.channel.PlayerStorage;
import handling.netty.ServerConnection;

import server.ServerProperties;

public class CashShopServer {

    private static String ip;
    private static InetSocketAddress InetSocketadd;
    private static int port = 8600;
    private static ServerConnection acceptor;
    private static PlayerStorage players, playersMTS;
    private static boolean finishedShutdown = false;

    public static final void setup() {
        System.out.println("【啟動中】 購物商城:::");
        port = Integer.parseInt(ServerProperties.getProperty("server.settings.cashshop.port", "8700"));
        players = new PlayerStorage(-10);
        playersMTS = new PlayerStorage(-20);
        acceptor = new ServerConnection(ServerConfig.IP, port, 0, MapleServerHandler.CASH_SHOP_SERVER);
        acceptor.run();
        System.out.println("購物商城    : 綁定端口 " + port);
    }

    public static final String getIP() {
        return ServerConfig.IP + ":" + port;
    }

    public static final PlayerStorage getPlayerStorage() {
        return players;
    }

    public static final PlayerStorage getPlayerStorageMTS() {
        return playersMTS;
    }

    public static final void shutdown() {
        if (finishedShutdown) {
            return;
        }
        System.out.println("[購物商城] 準備關閉...");
        System.out.println("[購物商城] 儲存資料中...");
        players.disconnectAll();
        playersMTS.disconnectAll();
        //MTSStorage.getInstance().saveBuyNow(true);
        System.out.println("[購物商城] 解除綁定端口...");
        acceptor.close();

        System.out.println("[購物商城] 關閉完成...");
    }

    public static boolean isShutdown() {
        return finishedShutdown;
    }
}
