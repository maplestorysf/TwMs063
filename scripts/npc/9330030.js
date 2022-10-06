var status = -1;

function start() {
	action(1, 0, 0);
}

function action(mode, type, selection) {
	if (mode == 1) {
		status++;
	} else {
		status--;
	}
	if (status == 0) {
		cm.sendSimple("您可以選擇付出#i4000254# 30個, #i4000255# 30個, #i4000256# 30個 以及#i4000201# 100個﹒或是選擇付出#r楓幣35000#k元﹒\r\n#b#L0#我要給道具#l\r\n#L1#我要給楓幣#l#k");
	} else if (status == 1) {
		gg = selection;
		cm.sendNext("當您進入楓狂抓魚地圖時﹐你的身上會自動穿上泳裝﹐接著就可以抓魚了！" );
	} else if (status == 2) {
		cm.sendNextPrev("冒險者利用手上的道具普功打魚，每隻魚的分數各有不同，限時五分鐘結算最後成績。\r\n#b泡泡魚 x 20分\r\n獨角尼莫 x -5分\r\n河豚 x 10分\r\n黃金海馬 x 15分\r\n粉紅小海豹 x -50分");
	} else if (status == 3) {
		if (gg == 0) {
			if (cm.haveItem(4000254, 30) && cm.haveItem(4000255, 30) && cm.haveItem(4000256, 30) && cm.havItem(4000201, 100)) {
				var em = cm.getEventManager("FishKing1");
				if (em == null) {
					cm.sendOk("找不到腳本，請聯繫GM！");
					cm.dispose();
					return;
				} else {
					var prop = em.getProperty("state");
					if (prop == null || prop.equals("0")) {
						cm.gainItem(4000254, -30);
						cm.gainItem(4000255, -30);
						cm.gainItem(4000256, -30);
						cm.gainItem(4000201, -100);
						em.startInstance(cm.getPlayer(), cm.getMap());
					} else {
						cm.sendOk("已經有人在裡面挑戰了。");
						cm.dispose();
						return;
					}
				}
			} else {
				cm.sendNext("我需要這些#i4000254# 30個, #i4000255# 30個, #i4000256# 30個 以及#i4000201# 100個！");
				cm.dispose();
			}
		}  else if (gg == 1) {
			if (cm.getPlayer().getMeso() >= 35000) {
				var em = cm.getEventManager("FishKing2");
				if (em == null) {
					cm.sendOk("找不到腳本，請聯繫GM！");
					cm.dispose();
					return;
				} else {
					var prop = em.getProperty("state");
					if (prop == null || prop.equals("0")) {
						cm.gainMeso(-35000);
						em.startInstance(cm.getPlayer(), cm.getMap());
					} else {
						cm.sendOk("已經有人在裡面挑戰了。");
						cm.dispose();
						return;
					}
				}
			} else {
				cm.sendNext("您身上的楓幣貌似不足！");
				cm.dispose();
			}
		}
		cm.dispose();
	}
}
