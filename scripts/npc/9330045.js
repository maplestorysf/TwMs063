/* Kedrick
	Fishking King NPC
*/

var status = -1;
var sel;

function action(mode, type, selection) {
    if (mode == 1) {
	status++;
    } else {
	status--;
    }

    if (status == 0) {
	cm.sendSimple("請選擇服務：\n\r #b#L0#我要到釣魚場#l \n\r #L1#購買魚餌(1200楓幣)#l \n\r #L2#購買釣魚用椅子(50,000楓幣)#l \n\r #L3#使用高級魚餌罐頭#l");
    } else if (status == 1) {
	sel = selection;
	if (sel == 0) {
	    if (cm.haveItem(5340000) || cm.haveItem(5340001)) {
		if (cm.haveItem(3011000)) {
		    cm.saveLocation("FISHING");
		    cm.warp(741000200);
		    cm.dispose();
		} else {
		    cm.sendNext("請先購買#b釣魚用椅子#k。");
		    cm.safeDispose();
		}
	    } else {
		cm.sendNext("請先至購物商城購買#b釣竿#k和#b高級釣竿#k。");
		cm.safeDispose();
	    }
	} else if (sel == 1) {
	    cm.sendYesNo("真的要使用 1,200楓幣 購買 120個 魚餌嗎？");
	} else if (sel == 2) {
	    if (cm.haveItem(3011000)) {
		cm.sendNext("無法重複取得釣魚用椅子。");
	    } else {
		if (cm.canHold(3011000) && cm.getMeso() >= 50000) {
		    cm.gainMeso(-50000);
		    cm.gainItem(3011000, 1);
		    cm.sendNext("祝你釣魚快樂。");
		} else {
		    cm.sendOk("請檢查楓幣數量和道具欄位空間。");
		}
	    }
	    cm.safeDispose();
	} else if (sel == 3) {
	    if (cm.canHold(2300001,120) && cm.haveItem(5350000,1)) {
		if (!cm.haveItem(2300001)) {
		    cm.gainItem(2300001, 120);
		    cm.gainItem(5350000,-1);
		    cm.sendNext("釣魚快樂。");
		} else {
		    cm.sendNext("請先使用完魚餌再來兌換。");
		}
	    } else {
		cm.sendOk("請檢查道具欄位空間和是否擁有高級魚餌罐頭。");
	    }
	    cm.safeDispose();
	} else if (sel == 4) {
	    cm.sendOk("You need to be above level 10, with a fishing rod, fishing baits and a fishing chair in order to enter the Fishing Lagoon. You will reel in a catch every 1 minute. Talk to lagoon's NPC Madrick to check out your catch record!");
	    cm.safeDispose();
	} else if (sel == 5) {
	    if (cm.haveItem(4000518, 500)) {
		if (cm.canHold(1142146)) {
		    cm.gainItem(4000518, -500);
		    cm.gainItemPeriod(1142146, 1, 30);
		    cm.sendOk("Woah, I guess you must have spend quite a lot of effort in the Fishing Lagoon fishing for these eggs. Here, take it. The #bFishing King Medal#k!")
		} else {
		    cm.sendOk("Please check if you have sufficient inventory slot for it.");
		}
	    } else {
		cm.sendOk("Please get me 500 #i4000518:# Golden Fish Egg in exchange for a Fishing King medal!")
	    }
	    cm.safeDispose();
	}
    } else if (status == 2) {
	if (sel == 1) {
	    if (cm.canHold(2300000,120) && cm.getMeso() >= 3000) {
		if (!cm.haveItem(2300000)) {
		    cm.gainMeso(-3000);
		    cm.gainItem(2300000, 120);
		    cm.sendNext("祝你釣魚快樂。");
		} else {
		    cm.sendNext("請先使用完魚餌再來購買。");
		}
	    } else {
		cm.sendOk("請檢查楓幣數量和道具欄位空間。");
	    }
	    cm.safeDispose();
	}
    }
}