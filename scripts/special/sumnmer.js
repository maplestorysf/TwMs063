var log = "暑期活動";
var item = 2450000;
var quantity = 5;
var MaplePoint = 666;
var player_old = false;
var text = "null";
var status = -1;

function start() {
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
	if (cm.getPlayer().getClient().getAccID() <= 25300) {
		player_old = true;
	} else {
		quantity = 0;
		MaplePoint = 1688;
	}
	if (status == 0) {
		if (cm.getPlayer().getPrizeLog(log) >= 1) {
			text = "您的帳號已經領取過了喔！";
		} else if (cm.getInventory(2).getNumFreeSlot() <= quantity && quantity > 0) {
			text = "您的消耗欄位空間不足5格唷";
		} else if (cm.getPlayer().getLevel() < 30) {
			text = "您的等級不足30";
		}
		if (text != "null") {
			cm.sendNext(text);
			cm.dispose();
			return;
		}
		
		text = "請問您是否要領取";
		if (player_old) {
			text += "楓葉點數666以及#i2450000##z2450000#x5嗎?";
		} else {
			text += "楓葉點數1688嗎?";
		}
		cm.sendYesNo(text);
		text = "null";
	} else if (status == 1) {
		if (cm.getPlayer().getPrizeLog(log) >= 1) {
			text = "您的帳號已經領取過了喔！";
		} else if (cm.getInventory(2).getNumFreeSlot() <= quantity && quantity > 0) {
			text = "您的消耗欄位空間不足5格唷";
		} else if (cm.getPlayer().getLevel() < 30) {
			text = "您的等級不足30";
		}

		if (text != "null") {
			cm.sendNext(text);
			cm.dispose();
			return;
		}
		text = "請問確定是否要領取獎勵??";
		cm.sendYesNo(text);
	} else if (status == 2) {
		if (quantity != 0) {
			cm.gainItem(item, quantity);
		}
		cm.getPlayer().modifyCSPoints(2, MaplePoint, true);
		cm.getPlayer().setPrizeLog(log);
		cm.sendNext("獎勵皆已發放，請前往背包查收");
		cm.dispose();
	} else {
		cm.dispose();
	}
}
