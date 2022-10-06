package client.messages.commands;

import client.messages.CommandExecute;
import client.MapleCharacter;
import client.MapleClient;
import constants.ServerConstants;
import handling.cashshop.CashShopServer;
import handling.channel.ChannelServer;
import handling.world.World;
import server.maps.MapleMap;

/**
 *
 * @author Windyboy
 */
public class PracticerCommand {

    public static ServerConstants.PlayerGMRank getPlayerLevelRequired() {
        return ServerConstants.PlayerGMRank.廢物實習生;
    }

    public static class Warp extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String[] splitted) {

            if (splitted.length < 2) {
                return false;
            }
            int ch = World.Find.findChannel(splitted[1]);
            MapleCharacter victim = World.Find.findChr(splitted[1]);
            if (victim != null && ch > 0) {
                if (splitted.length == 2) {
                    if (ch == c.getChannel()) {
                        c.getPlayer().changeMap(victim.getMap(), victim.getMap().findClosestSpawnpoint(victim.getPosition()));
                    } else {
                        if (victim.getMapId() != c.getPlayer().getMapId()) {
                            final MapleMap mapp = victim.getClient().getChannelServer().getMapFactory().getMap(c.getPlayer().getMapId());
                            c.getPlayer().changeMap(mapp, mapp.getPortal(0));
                        }
                        c.getPlayer().changeChannel(victim.getClient().getChannel());
                    }
                } else {
                    MapleMap target = ChannelServer.getInstance(c.getWorld(), c.getChannel()).getMapFactory().getMap(Integer.parseInt(splitted[2]));
                    c.getPlayer().changeMap(target, target.getPortal(0));
                }
            } else {
                try {
                    if (victim == null) {// 輸入的是地圖
                        MapleMap target = c.getChannelServer().getMapFactory().getMap(Integer.parseInt(splitted[1]));
                        if (target == null) {
                            c.getPlayer().dropMessage(6, "地圖[" + splitted[1] + "]不存在");
                        } else {
                            c.getPlayer().changeMap(target, target.getPortal(0));
                        }
                    } else if (ch == -10) {
                        c.getPlayer().dropMessage(6, "玩家目前在購物商城內...");
                    } else if (ch == -20) {
                        c.getPlayer().dropMessage(6, "玩家目前在拍賣商城內...");
                    } else {
                        c.getPlayer().dropMessage(6, "正在切換頻道中...");
                        if (victim.getMapId() != c.getPlayer().getMapId()) {
                            final MapleMap mapp = c.getChannelServer().getMapFactory().getMap(victim.getMapId());
                            c.getPlayer().changeMap(mapp, mapp.getPortal(0));
                        }
                        c.getPlayer().changeChannel(ch);
                    }
                } catch (NumberFormatException e) {
                    c.getPlayer().dropMessage(6, "出錯了... " + e.getMessage());
                    return true;
                }
            }
            return true;
        }

        @Override
        public String getHelp() {
            return "<玩家名稱> <地圖ID> - 移動到某個地圖或某個玩家所在的地方";
        }
    }

    public static class WarpID extends CommandExecute {

        @Override
        public boolean execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                return false;
            }
            int input = 0;
            try {
                input = Integer.parseInt(splitted[1]);
            } catch (Exception ex) {

            }
            int ch = World.Find.findChannel(input);
            int wl = World.Find.findWorld(input);
            if (ch < 1) {
                MapleCharacter victim = CashShopServer.getPlayerStorage().getCharacterById(input);
                if (victim == null) {
                    c.getPlayer().dropMessage(6, "玩家編號[" + input + "] 不在線上");
                } else {
                    c.getPlayer().dropMessage("玩家編號「" + input + "」目前位於商城");
                }
                return true;
            }
            MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterById(input);
            if (victim != null) {
                if (splitted.length == 2) {
                    c.getPlayer().changeMap(victim.getMap(), victim.getMap().findClosestSpawnpoint(victim.getPosition()));
                } else {
                    MapleMap target = ChannelServer.getInstance(c.getWorld(), c.getChannel()).getMapFactory().getMap(Integer.parseInt(splitted[2]));
                    if (target == null) {
                        c.getPlayer().dropMessage(6, "地圖不存在");
                    } else {
                        victim.changeMap(target, target.getPortal(0));
                    }
                }
            } else {
                try {
                    try {
                        victim = ChannelServer.getInstance(wl, ch).getPlayerStorage().getCharacterById(Integer.parseInt(splitted[1]));
                    } catch (Exception e) {
                        c.getPlayer().dropMessage(6, "出問題了 " + e.getMessage());
                    }
                    if (victim != null) {
                        if (victim.getMapId() != c.getPlayer().getMapId()) {
                            final MapleMap mapp = c.getChannelServer().getMapFactory().getMap(victim.getMapId());
                            c.getPlayer().changeMap(mapp, mapp.getPortal(0));
                        }
                        c.getPlayer().dropMessage(6, "正在改變頻道請等待");
                        c.getPlayer().changeChannel(ch);

                    } else {
                        c.getPlayer().dropMessage("角色不存在");
                    }

                } catch (Exception e) {
                    c.getPlayer().dropMessage(6, "出問題了 " + e.getMessage());
                }
            }
            return true;
        }

        @Override
        public String getHelp() {
            return "<玩家編號> - 移動到某個玩家所在的地方";
        }
    }

}
