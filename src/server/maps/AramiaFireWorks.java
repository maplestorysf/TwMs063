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
package server.maps;

import java.awt.Point;

import client.MapleCharacter;
import handling.world.World;
import server.Randomizer;
import server.Timer.EventTimer;
import server.life.MapleLifeFactory;
import tools.MaplePacketCreator;

public class AramiaFireWorks {

    public final static int KEG_ID = 4031875, SUN_ID = 4001246, DEC_ID = 4001473, XIANG_ID = 4000516;
    public final static int MAX_KEGS = 2000, MAX_SUN = 14000, MAX_DEC = 18000; //max kegs = 5000 不夜城新年
    private short kegs = 0;
    private short sunshines = MAX_SUN / 6; //start at 1/6 then go from that
    private short decorations = MAX_DEC / 6;
    private static final AramiaFireWorks instance = new AramiaFireWorks();
    private static final int[] arrayMob = {9500317}; //9410066
    private static final int[] arrayMob1 = {9400710};
    private static final int[] arrayMob2 = {9400708};
    private static final int[] arrayX = {1242};//-255
    private static final int[] arrayY = {154};//340
    private static final int[] arrayX1 = {1092};
    private static final int[] arrayY1 = {154};
    private static final int[] arrayX2 = {1401};
    private static final int[] arrayY2 = {154};
    private static final int[] array_X = {720, 180, 630, 270, 360, 540, 450, 142,
        142, 218, 772, 810, 848, 232, 308, 142};
    private static final int[] array_Y = {1234, 1234, 1174, 1234, 1174, 1174, 1174, 1260,
        1234, 1234, 1234, 1234, 1234, 1114, 1114, 1140};
    private static final int flake_Y = 149;

    public static final AramiaFireWorks getInstance() {
        return instance;
    }
/*  新年不夜城↓
    public final void giveKegs(final MapleCharacter c, final int kegs) {
        this.kegs += kegs;
        switch (this.kegs) {
            case 1000:
                World.Broadcast.broadcastMessage(MaplePacketCreator.getItemNotice("<頻道 " + c.getClient().getChannel() + "> " + "不夜城新年活動進度目前是5000/" + this.kegs + "！！"));
                break;
            case 2000:
                World.Broadcast.broadcastMessage(MaplePacketCreator.getItemNotice("<頻道 " + c.getClient().getChannel() + "> " + "不夜城新年活動進度目前是5000/" + this.kegs + "！！"));
                break;
            case 3000:
                World.Broadcast.broadcastMessage(MaplePacketCreator.getItemNotice("<頻道 " + c.getClient().getChannel() + "> " + "不夜城新年活動進度目前是5000/" + this.kegs + "！！"));
                break;
            case 4000:
                World.Broadcast.broadcastMessage(MaplePacketCreator.getItemNotice("<頻道 " + c.getClient().getChannel() + "> " + "不夜城新年活動進度目前是5000/" + this.kegs + "！！"));
                break;
            case 4999:
                World.Broadcast.broadcastMessage(MaplePacketCreator.getItemNotice("<頻道 " + c.getClient().getChannel() + "> " + "不夜城新年活動進度目前是5000/" + this.kegs + "！！"));
                break;
        }
        if (this.kegs >= MAX_KEGS) {
            this.kegs = 0;
            broadcastEvent(c);
        }
    }

    private void broadcastServer(final MapleCharacter c, final int itemid) {
        World.Broadcast.broadcastMessage(MaplePacketCreator.getItemNotice("<頻道 " + c.getClient().getChannel() + "> " + "不夜城新年活動即將開始舉辦怪物大遊行！！", itemid));
    }

    public final short getKegsPercentage() {
        double per = (((kegs) * 1.0) / (MAX_KEGS * 1.0));
        return (short) (per * 100);
    }

    private void broadcastEvent(final MapleCharacter c) {
        // 不夜城新年活動
        broadcastServer(c, XIANG_ID);

        EventTimer.getInstance().schedule(new Runnable() {

            @Override
            public final void run() {
                startEvent(c.getClient().getChannelServer().getMapFactory().getMap(741000000));
            }
        }, 10000);
    }

    private void startEvent(final MapleMap map) {
        map.startMapEffect("可以進行新年活動的表演了！！", 5121020);
        EventTimer.getInstance().schedule(new Runnable() {
            @Override
            public final void run() {
                spawnMonster(map);
            }
        }, 5000);
    }

    private void spawnMonster(final MapleMap map) {
        Point pos;

        for (int i = 0; i < arrayMob.length; i++) {
            pos = new Point(arrayX[i], arrayY[i]);
            map.spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(arrayMob[i]), pos);
        }
    }新年不夜城↑*/
    
     public final void giveKegs(final MapleCharacter c, final int kegs) { //幸福村召喚聖誕雪人
        this.kegs += kegs;
        switch (this.kegs) {
            case 2000:
                World.Broadcast.broadcastMessage(MaplePacketCreator.getItemNotice("<頻道 " + c.getClient().getChannel() + "> " + "幸福村召喚雪人活動進度目前是2000/" + this.kegs + "！！"));
                break;
            case 4000:
                World.Broadcast.broadcastMessage(MaplePacketCreator.getItemNotice("<頻道 " + c.getClient().getChannel() + "> " + "幸福村召喚雪人活動進度目前是2000/" + this.kegs + "！！"));
                break;
            case 6000:
                World.Broadcast.broadcastMessage(MaplePacketCreator.getItemNotice("<頻道 " + c.getClient().getChannel() + "> " + "幸福村召喚雪人活動進度目前是2000/" + this.kegs + "！！"));
                break;
            case 8000:
                World.Broadcast.broadcastMessage(MaplePacketCreator.getItemNotice("<頻道 " + c.getClient().getChannel() + "> " + "幸福村召喚雪人活動進度目前是2000/" + this.kegs + "！！"));
                break;
            case 10000:
                World.Broadcast.broadcastMessage(MaplePacketCreator.getItemNotice("<頻道 " + c.getClient().getChannel() + "> " + "幸福村召喚雪人活動進度目前是2000/" + this.kegs + "！！"));
                break;
             case 12000:
                World.Broadcast.broadcastMessage(MaplePacketCreator.getItemNotice("<頻道 " + c.getClient().getChannel() + "> " + "幸福村召喚雪人活動進度目前是2000/" + this.kegs + "！！"));
                break;    
            case 14000:
            World.Broadcast.broadcastMessage(MaplePacketCreator.getItemNotice("<頻道 " + c.getClient().getChannel() + "> " + "幸福村召喚雪人活動進度目前是2000/" + this.kegs + "！！"));
                break;
                            case 16000:
            World.Broadcast.broadcastMessage(MaplePacketCreator.getItemNotice("<頻道 " + c.getClient().getChannel() + "> " + "幸福村召喚雪人活動進度目前是2000/" + this.kegs + "！！"));
                break;
                            case 18000:
            World.Broadcast.broadcastMessage(MaplePacketCreator.getItemNotice("<頻道 " + c.getClient().getChannel() + "> " + "幸福村召喚雪人活動進度目前是2000/" + this.kegs + "！！"));
                break;
                            case 19999:
            World.Broadcast.broadcastMessage(MaplePacketCreator.getItemNotice("<頻道 " + c.getClient().getChannel() + "> " + "幸福村召喚雪人活動進度目前是2000/" + this.kegs + "！！"));
                break;
                
                
        }
        if (this.kegs >= MAX_KEGS) {
            this.kegs = 0;
            broadcastEvent(c);
        }
    }

    private void broadcastServer(final MapleCharacter c, final int itemid) {
        World.Broadcast.broadcastMessage(MaplePacketCreator.getItemNotice("<頻道 " + c.getClient().getChannel() + "> " + "幸福村的冰雪區域將於30秒內出現雪人！！", itemid));
    }

    public final short getKegsPercentage() {
        double per = (((kegs) * 1.0) / (MAX_KEGS * 1.0));
        return (short) (per * 100);
    }

    private void broadcastEvent(final MapleCharacter c) {
     
        broadcastServer(c, XIANG_ID);

        EventTimer.getInstance().schedule(new Runnable() {

            @Override
            public final void run() {
                startEvent(c.getClient().getChannelServer().getMapFactory().getMap(209080000));
            }
        }, 10000);
    }

    private void startEvent(final MapleMap map) {
        map.startMapEffect("同心協力擊倒7PuPu的雪人吧！", 5121015); //雪人飄花
        EventTimer.getInstance().schedule(new Runnable() {
            @Override
            public final void run() {
                spawnMonster(map);
            }
        }, 10000);
    }

    private void spawnMonster(final MapleMap map) {
        Point pos;

        for (int i = 0; i < arrayMob.length; i++) {
            pos = new Point(arrayX[i], arrayY[i]);
            map.spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(arrayMob[i]), pos);
        }
       /*  for (int i = 0; i < arrayMob1.length; i++) {
            pos = new Point(arrayX1[i], arrayY1[i]);
            map.spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(arrayMob1[i]), pos);
        }
        
         for (int i = 0; i < arrayMob2.length; i++) {
            pos = new Point(arrayX2[i], arrayY2[i]);
            map.spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(arrayMob2[i]), pos);
        }*/
        
    }

    public final void giveSuns(final MapleCharacter c, final int kegs) {
        this.sunshines += kegs;
        //have to broadcast a Reactor?
        final MapleMap map = c.getClient().getChannelServer().getMapFactory().getMap(555000000);
        final MapleReactor reactor = map.getReactorByName("XmasTree");
        for (int gogo = kegs + (MAX_SUN / 6); gogo > 0; gogo -= (MAX_SUN / 6)) {
            switch (reactor.getState()) {
                case 0: //first state
                case 1: //first state
                case 2: //first state
                case 3: //first state
                case 4: //first state
                    if (this.sunshines >= (MAX_SUN / 6) * (2 + reactor.getState())) {
                        reactor.setState((byte) (reactor.getState() + 1));
                        reactor.setTimerActive(false);
                        map.broadcastMessage(MaplePacketCreator.triggerReactor(reactor, reactor.getState()));
                    }
                    break;
                default:
                    if (this.sunshines >= (MAX_SUN / 6)) {
                        map.resetReactors(); //back to state 0
                    }
                    break;
            }
        }
        if (this.sunshines >= MAX_SUN) {
            this.sunshines = 0;
            broadcastSun(c);
        }
    }

    public final short getSunsPercentage() {
        return (short) ((sunshines / MAX_SUN) * 10000);
    }

    private void broadcastSun(final MapleCharacter c) {
        broadcastServer(c, SUN_ID);
        // Henesys Park
        EventTimer.getInstance().schedule(new Runnable() {

            @Override
            public final void run() {
                startSun(c.getClient().getChannelServer().getMapFactory().getMap(970010000));
            }
        }, 10000);
    }

    private void startSun(final MapleMap map) {
        map.startMapEffect("陽光正在綻放！", 5121010);
        for (int i = 0; i < 3; i++) {
            EventTimer.getInstance().schedule(new Runnable() {

                @Override
                public final void run() {
                    spawnItem(map);
                }
            }, 20000 + (i * 10000));
        }
    }

    private void spawnItem(final MapleMap map) {
        Point pos;

        for (int i = 0; i < Randomizer.nextInt(5) + 10; i++) {
            pos = new Point(array_X[i], array_Y[i]);
            map.spawnAutoDrop(Randomizer.nextInt(3) == 1 ? 3010025 : 4001246, pos);
        }
    }

    public final void giveDecs(final MapleCharacter c, final int kegs) {
        this.decorations += kegs;
        //have to broadcast a Reactor?
        final MapleMap map = c.getClient().getChannelServer().getMapFactory().getMap(555000000);
        final MapleReactor reactor = map.getReactorByName("XmasTree");
        for (int gogo = kegs + (MAX_DEC / 6); gogo > 0; gogo -= (MAX_DEC / 6)) {
            switch (reactor.getState()) {
                case 0: //first state
                case 1: //first state
                case 2: //first state
                case 3: //first state
                case 4: //first state
                    if (this.decorations >= (MAX_DEC / 6) * (2 + reactor.getState())) {
                        reactor.setState((byte) (reactor.getState() + 1));
                        reactor.setTimerActive(false);
                        map.broadcastMessage(MaplePacketCreator.triggerReactor(reactor, reactor.getState()));
                    }
                    break;
                default:
                    if (this.decorations >= MAX_DEC / 6) {
                        map.resetReactors(); //back to state 0
                    }
                    break;
            }
        }
        if (this.decorations >= MAX_DEC) {
            this.decorations = 0;
            broadcastDec(c);
        }
    }

    public final short getDecsPercentage() {
        return (short) ((decorations / MAX_DEC) * 10000);
    }

    private final void broadcastDec(final MapleCharacter c) {
        broadcastServer(c, DEC_ID);
        EventTimer.getInstance().schedule(new Runnable() {

            @Override
            public final void run() {
                startDec(c.getClient().getChannelServer().getMapFactory().getMap(555000000));
            }
        }, 10000); //no msg
    }

    private final void startDec(final MapleMap map) {
        map.startMapEffect("聖誕樹正在綻放！", 5120000);
        for (int i = 0; i < 3; i++) {
            EventTimer.getInstance().schedule(new Runnable() {

                @Override
                public final void run() {
                    spawnDec(map);
                }
            }, 20000 + (i * 10000));
        }
    }

    private final void spawnDec(final MapleMap map) {
        Point pos;

        for (int i = 0; i < Randomizer.nextInt(10) + 40; i++) {
            pos = new Point(Randomizer.nextInt(800) - 400, flake_Y);
            map.spawnAutoDrop(Randomizer.nextInt(15) == 1 ? 2060006 : 2060006, pos);
        }
    }
}
