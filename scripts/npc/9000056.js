/* global cm, Packages, MapleItemInformationProvider, World, MaplePacketCreator */
load('nashorn:mozilla_compat.js');
importPackage(Packages.tools);
importPackage(Packages.handling.world);
importPackage(Packages.client.inventory);
importPackage(Packages.server);

var status = -1;
var selected = 0;
var bag = -1;
var Cash = 0;
var itemId = 0;
var maxtimesperday = 5;
var neededCash = 10;
var s = 0;
var h = 0;

var msg = "";

var edit = false;

var inv = null;
var statsSel = null;
var medal = null;

var slot = Array();
var bosslog = ["禮拜日", "禮拜一", "禮拜二", "禮拜三", "禮拜四", "禮拜五", "禮拜六"];

var d = new Date();
var day = d.getDay();

function start() {
    if (edit && !cm.getPlayer().isGM()) {
        msg = "本NPC#r維修中#k，請稍後再試。";
        cm.sendNext(msg);
        cm.dispose();
        return;
    }
    action(1, 0, 0);
}

function action(mode, type, selection) {
    if (mode == 1) {
        status++;
    } else if (mode == 0) {
        status--;
    } else {
        cm.dispose();
        return;
    }

    if (status == 0) {
        if (cm.getPlayer().getAcLog(bosslog[day] + "_廣播") >= maxtimesperday) {
            msg = "一天最多使用次數為#r" + maxtimesperday;
            cm.sendNext(msg);
            cm.dispose();
            return;
        }
        msg = "本NPC可以提供給您廣播功能，使用一次為" + neededCash + "點點數(可以選擇點數/楓葉點數)\r\n" +
                "請選擇所使用的點數：\r\n \r\n#r" +
                "#L1#一般點數\r\n" +
                "#L2#楓葉點數";
        cm.sendSimple(msg);
    } else if (status == 1) {
        if (mode == 1) {
            Cash = selection;
        }
        msg = "請選擇要販賣的道具種類#r\r\n" +
                "#L1#裝備欄\r\n" +
                "#L2#消耗欄\r\n" +
                "#L3#裝飾欄\r\n" +
                "#L4#其他欄\r\n" +
                "#L5#特殊欄";
        cm.sendSimple(msg);
    } else if (status == 2) {
        msg = "請選擇要廣播的道具\r\n";
        if (mode == 1) {
            bag = selection;
            inv = cm.getInventory(bag);
        }
        for (var i = 1; i <= inv.getSlotLimit(); i++) {
            slot.push(i);
            var it = inv.getItem(i);
            if (it != null) {
                var itemid = it.getItemId();
                msg += "#L" + i + "##v" + itemid + "##t" + itemid + "##l\r\n";
            }
        }
        cm.sendSimple(msg);
    } else if (status == 3) {
        selected = selection - 1;
        if (selected >= inv.getSlotLimit()) {
            msg = "錯誤，請稍後再試。";
            cm.sendNext(msg);
            cm.dispose();
            return;
        }
        try {
            statsSel = inv.getItem(slot[selected]);
        } catch (err) {

        }
        if (statsSel == null) {
            msg = "錯誤，請稍後再試。";
            cm.sendNext(msg);
            cm.dispose();
            return;
        }
        itemId = statsSel.getItemId();
        msg = "請確認以下事項是否正確：\r\n" +
                "\t一、使用點數類型為：" + (Cash == 1 ? "普通點數" : Cash == 2 ? "楓葉點數" : "發生錯誤") + "\r\n" +
                "\t二、選擇道具欄位為：" + (bag == 1 ? "裝備" : bag == 2 ? "消耗" : bag == 3 ? "裝飾" : bag == 4 ? "其他" : bag == 5 ? "特殊" : "發生錯誤") + "欄\r\n" +
                "\t三、所選擇的道具為：#v" + itemId + "##t" + itemId + "#\r\n";
        cm.sendYesNo(msg);
    } else if (status == 4) {
        // 舊版功能
        // msg = "請輸入要廣播的訊息";
        // cm.sendGetText(msg);
        msg = "請選擇要廣播出來的頻道數值( 頻道-洞數 的頻道)\r\n" +
                "#L1#1\r\n" +
                "#L2#2\r\n" +
                "#L3#3\r\n" +
                "#L4#4\r\n" +
                "#L5#5\r\n" +
                "#L6#6\r\n" +
                "#L7#7\r\n" +
                "#L8#8\r\n" +
                "#L9#9\r\n";
        cm.sendSimple(msg);
    } else if (status == 5) {
        s = selection;
        msg = "請選擇要廣播出來的洞數數值( 頻道-洞數 的洞數)\r\n" +
                "#L1#1\r\n" +
                "#L2#2\r\n" +
                "#L3#3\r\n" +
                "#L4#4\r\n" +
                "#L5#5\r\n" +
                "#L6#6\r\n" +
                "#L7#7\r\n" +
                "#L8#8\r\n" +
                "#L9#9\r\n";
        cm.sendSimple(msg);
    } else if (status == 6) {
        h = selection;
        if (cm.getPlayer().getCSPoints(Cash) < neededCash) {
            msg = "您的點數不足。";
            cm.sendNext(msg);
            cm.dispose();
            return;
        }
        cm.getPlayer().setAcLog(bosslog[day]);
        cm.getPlayer().modifyCSPoints(Cash, -neededCash, false);
        // var text = cm.getText();
        var text = "      " + s + " - " + h + " 快來這邊找我買吧!";
		var medal = cm.getInventory(-1).getItem(-21);
		var medaltext = "";
		if (medal != null) {
			medaltext = "<" + MapleItemInformationProvider.getInstance().getName(medal.getItemId()) + "> ";
		} else {
			medaltext = "<辛巴谷粉絲> "
		}
        World.Broadcast.broadcastMessage(MaplePacketCreator.getGachaponMega(medaltext + cm.getPlayer().getName(), " : " + text, statsSel, 0));
        cm.dispose();
    } else {
        cm.dispose();
    }
}