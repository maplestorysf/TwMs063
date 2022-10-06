/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server.events;

import client.MapleCharacter;
import client.MapleStat;
import java.util.concurrent.ScheduledFuture;
import server.Timer;
import server.maps.MapleMap;
import tools.MaplePacketCreator;

/**
 * @EventName 你的生死
 * @author lrenex
 */
public class Mapledie extends MapleEvent {

    private int godtime = 0, answer = 0;
    private ScheduledFuture<?> GodSchedule, endSchedule;

    public Mapledie(final int world, final int channel, final int[] mapid) {
        super(world, channel, mapid);
    }

    private void resetSchedule() {
        if (endSchedule != null) {
            endSchedule.cancel(false);
            endSchedule = null;
        }
        if (GodSchedule != null) {
            GodSchedule.cancel(false);
            GodSchedule = null;
        }
    }

    @Override
    public void onMapLoad(MapleCharacter chr) { // 進地圖瞬間可以載入事件
        chr.setLifeRandom(); // 玩家進入時亂數自己的號碼
        getMap(0).broadcastMessage(MaplePacketCreator.musicChange("Bgm16/TimeTemple")); // 變換活動地圖音樂
    }

    @Override
    public void reset() {
        super.reset();
        getMap(0).getPortal("join00").setPortalState(false);
        resetSchedule();
        godtime = 0;
    }

    @Override
    public void unreset() {
        super.unreset();
        getMap(0).getPortal("join00").setPortalState(true);
        resetSchedule();
    }

    @Override
    public void startEvent() { //活動開始
        answer = (int) (Math.random() * 6) + 1; // 系統亂數
        startgod();
    }

    public void startgod() { //活動從第一個地圖開始
        startgod(getMap(0));
    }

    public void startgod(final MapleMap toSend) { //活動開始裏頭做的事件
        if (GodSchedule != null) {
            GodSchedule.cancel(false);
        }
                GodSchedule = Timer.autoEventTimer.getInstance().schedule(new Runnable() {

            @Override
            public void run() {
                if (godtime == 1) {
                    toSend.broadcastMessage(MaplePacketCreator.getItemNotice("本次活動已經結束！"));
                    unreset();
                    for (MapleCharacter chr : toSend.getCharactersThreadsafe()) {
                        if (chr != null && chr.isAlive()) {
                            givePrize(chr);
                            warpBack(chr);
                        }
                    }
                    return;
                }
                broadcast(MaplePacketCreator.serverNotice("活動已經開始，10秒後就知道結果!!"));
                toSend.broadcastMessage(MaplePacketCreator.getClock(10));
                if (endSchedule != null) {
                    endSchedule.cancel(false);
                }
                               endSchedule = Timer.autoEventTimer.getInstance().schedule(new Runnable() {

                    @Override
                    public void run() {
                        godtime++;
                        for (MapleCharacter chr : toSend.getCharactersThreadsafe()) {
                            if (chr != null && chr.isAlive()) {
                                if (!isGodChoosed(chr)) {
                                    chr.getStat().setHp((short) 0);
                                    chr.updateSingleStat(MapleStat.HP, 0);
                                }
                            }
                        }
                        startgod();
                    }
                }, 10000);
            }
        }, 10000);
    }

    private boolean isGodChoosed(MapleCharacter chr) { // 判斷是否被神選上
        if (chr.getLifeRandom() > answer) {
            chr.dropMessage(6, "恭喜您被神選上！");
            return true;
        }
        chr.dropMessage(6, "很抱歉您被神拋棄了。。");
        return false;
    }
}
