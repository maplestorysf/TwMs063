/* global cm */
load('nashorn:mozilla_compat.js');
importPackage(Packages.tools);

var status = -1;
var banMap = Array(109080000, 109080010, 109040000, 109030001, 109060000, 109010000);

function start() {
	action(1, 0, 0);
}

function action(mode, type, selection) {
	if (mode === 1) {
		status++;
	} else if (mode === 0) {
		status--;
	} else {
		cm.dispose();
		return;
	}

	if (status === 0) {
		if (!cm.isQuestFinished(29934)) {
			NewPlayer1();
		} else {
			if (cm.getPlayer().getOneTimeLog("新手獎勵") == 0) {
				cm.getPlayer().setOneTimeLog("新手獎勵");
			}
		}
		for (var i = 0; i < banMap.length; i++) {
			if (cm.getPlayer().getMapId() == banMap[i]) {
				cm.sendOk("幹！想要偷跑吃屎去吧！");
				cm.dispose();
				break;
			}
		}
		cm.sendSimple("#b歡迎來到辛巴谷!!\r\n" +
			"#L2#我要打開藍色小箱子#l\r\n" +
			//"#L3#當鋪裡的大蟾蜍錢包(100等以上才能領)解未來東京任務用#l\r\n" +
			//"#L4#我要把坐騎升級成銀色鬃毛!!#l\r\n" +
			//"#L5#我要進行忍影瞬殺的任務(四轉盜賊限定)#l\r\n" +
			//"#L6#我要刪除銀或金寶箱空白道具(並且補償一次道具)#l\r\n" +
			//"#L7#我要完成燈泡不能接的任務#l\r\n" +
			/* "#L8#我領取廣播道具#ll\r\n" +
			"#L9#我領取愛心廣播道具#\l\r\n" +
			"#L10#我領取骷簍廣播道具#l\r\n" +*/
			//"#L11#我領取精靈商人#l\r\n" +
			"#L12#我要打恰吉#l\r\n" +
			"#L13#我要廣播道具#l\r\n" +
			"#L17#我要買狼的生命水，1瓶1億，解狂狼坐騎需要的");
	} else if (status === 1) {
		var level = cm.getPlayer().getLevel();
		if (selection === 2) {
			if (!cm.haveItem(4031307, 1)) {
				cm.sendOk("#b檢查一下背包有沒有藍色禮物盒哦");
				cm.dispose();
				return;
			}
			cm.gainItem(4031307, -1);
			cm.gainItem(2020020, 100);
			cm.sendOk("#b蛋糕不要吃太多~旅遊愉快~");
		} else if (selection === 3) {
			if (level < 100) {
				cm.sendOk("你的等級還不夠。");
				cm.dispose();
				return;
			}
			cm.gainItem(5252002, 1);
		} else if (selection === 4) {
			if (!cm.haveItem(4000264, 400) || !cm.haveItem(4000266, 400) || !cm.haveItem(4000267, 400) || level < 120 || !cm.canHold(1902001, 1)) {
				cm.sendOk("請檢查一下背包有沒有金色皮革４００個、木頭肩護帶４００個、骷髏肩護帶４００個,或者是你等級不夠");
				cm.dispose();
				return;
			}
			cm.gainItem(4000264, -400);
			cm.gainItem(4000266, -400);
			cm.gainItem(4000267, -400);
			cm.gainItem(1902001, 1);
			cm.sendOk("#b好好珍惜銀色鬃毛~~");
		} else if (selection === 8 || selection === 9 || selection === 10) { //廣播
			var Item = 0;
			var amount = 0;
			var reqLevel = 0;
			var BossLog = '';
			switch (selection) {
			case 8:
				Item = 5072000;
				amount = 5;
				BossLog = '1';
				reqLevel = 1;
				break;
			case 9:
				Item = 5073000;
				amount = 10;
				BossLog = '30';
				reqLevel = 30;
				break;
			case 10:
				Item = 5074000;
				amount = 5;
				BossLog = '70';
				reqLevel = 70;
				break;
			}
			if (level < reqLevel || cm.getPlayer().getBossLog(BossLog) > 0) {
				cm.sendNext("一天只能領一次或你的等級還不夠。");
				cm.dispose();
				return;
			}

			cm.setBossLog(BossLog);
			cm.gainItem(Item, amount);
			cm.sendNext("已經獲得#i" + Item + "#x" + amount + "。");
		} else if (selection === 11) { //商人
			if (level < 10 || cm.getPlayer().getBossLog('sell') > 1) {
				cm.sendOk("1天只能領一次或你的等級還不夠10等才能領唷。");
				cm.dispose();
				return;
			}
			cm.setBossLog('sell');
			cm.gainItem(5030000, 1);
		} else if (selection === 12) {
			if (cm.getPlayer().getMapId() == 749050400) {
				cm.playerMessage("轉蛋屋不能去打恰吉T.T");
			} else {
				cm.warp(229010000);
			}
			cm.dispose();
		} else if (selection === 13) {
			cm.dispose();
			cm.openNpc(9000056);
			return;
		} else if (selection === 17) {
			if (cm.getPlayer().getMeso() >= 100000000 && cm.canHold(4032334, 1)) {
				cm.gainMeso(-100000000);
				cm.gainItem(4032334, 1);
				cm.sendOk("感謝購買!");
				cm.dispose();
			} else {
				cm.sendOk("#d請確認楓幣是否足夠，或者背包欄位滿了！");
				cm.dispose();
			}
			cm.dispose();
		}
	} else {
		cm.dispose();
	}

}

function NewPlayer1() {
	if (cm.getPlayer().getOneTimeLog("新手獎勵") == 0) {
		var item = [5000007, 2450000, 1002419, 5370000];
		var amount = [1, 10, 1, 1];
		var next = true;
		for (var i = 0; i < item.length; i++) {
			if (!cm.canHold(item[i], amount[i])) {
				next = false;
			}
		}
		if (!next) {
			cm.sendNext("背包空間不足以領新手獎勵唷。");
			cm.dispose();
			return;
		}
		cm.gainPet(5000007, "黑色小豬", 1, 0, 100, 0, 45);
		cm.gainItem(2450000, 10); //獵人的幸運
		cm.gainItemPeriod(1002419, 1, 30); //紅葉黑頭巾
		cm.gainItemPeriod(5370000, 1, 7); //黑板 7天
		cm.forceCompleteQuest(29934); //完成新手獎勵
		cm.sendNext("歡迎來到 辛巴谷 請使用 @help/@幫助 了解各式指令\r\n\r\n\r\n遊戲愉快^^");
		FileoutputUtil.logToFile("logs/Data/新手獎勵.txt", "領取時間:" + FileoutputUtil.NowTime() + "角色名稱:" + cm.getPlayer().getName() + "角色等級: " + cm.getPlayer().getLevel() + "\r\n");
		cm.getPlayer().setOneTimeLog("新手獎勵");
		cm.dispose();
	}
}
