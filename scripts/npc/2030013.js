var status = 0;
function start() {
	if (cm.getPlayer().getClient().getChannel() != 1 && cm.getPlayer().getClient().getChannel() != 5) {
		cm.playerMessage(5, "只能在4頻或5頻打");
		cm.dispose();
		return false;
	}
	action(1, 0, 0);
}
function action(mode, type, selection) {
	switch (status) {
	case 0:
		if (cm.getPlayer().getLevel() < 50 || !cm.haveItem(4001017)) {
			cm.playerMessage("請確定你的等級是否有到達50等並且確認身上是否有火焰之眼");
			cm.dispose();
			return;
		}
		var em = cm.getEventManager("ZakumBattle");

		if (em == null) {
			cm.sendOk("找不到副本請聯絡GM。");
			cm.safeDispose();
			return;
		}
		var prop = em.getProperty("state");
		var marr = cm.getQuestRecord(160101);
		var data = marr.getCustomData();
		if (data == null) {
			marr.setCustomData("0");
			data = "0";
		}
		var time = parseInt(data);
		if (prop == null || prop.equals("0")) {
			var squadAvailability = cm.getSquadAvailability("ZAK");
			if (squadAvailability == -1) {
				status = 1;
				cm.sendYesNo("你想要成為殘暴炎魔遠征隊隊長?");

			} else if (squadAvailability == 1) {
				// -1 = Cancelled, 0 = not, 1 = true
				var type = cm.isSquadLeader("ZAK");
				if (type == -1) {
					cm.sendOk("已經結束了申請。");
					cm.safeDispose();
				} else if (type == 0) {
					var memberType = cm.isSquadMember("ZAK");
					if (memberType == 2) {
						cm.sendOk("在遠征隊的制裁名單。");
						cm.safeDispose();
					} else if (memberType == 1) {
						status = 5;
						cm.sendSimple("你要做什麼? \r\n#b#L0#查看遠征隊名單#l \r\n#b#L1#加入遠征隊#l \r\n#b#L2#退出遠征隊#l");
					} else if (memberType == -1) {
						cm.sendOk("遠征隊員已經達到30名，請稍後再試。");
						cm.safeDispose();
					} else {
						status = 5;
						cm.sendSimple("你要做什麼? \r\n#b#L0#查看遠征隊成員。#l \r\n#b#L1#加入遠征隊#l \r\n#b#L2#退出遠征隊#l");
					}
				} else { // Is leader
					status = 10;
					cm.sendSimple("你要做什麼? \r\n#b#L0#查看遠征隊成員。#l \r\n#b#L1#管理遠征隊成員。#l \r\n#b#L2#編輯限制列表。#l \r\n#r#L3#進入地圖。#l");
					// TODO viewing!
				}
			} else {
				var eim = cm.getDisconnected("ZakumBattle");
				if (eim == null) {
					var squd = cm.getSquad("ZAK");
					if (squd != null) {
						cm.sendYesNo("已經遠征隊正在進行挑戰了.\r\n" + squd.getNextPlayer());
						status = 3;
					} else {
						cm.sendOk("遠征隊的挑戰已經開始.");
						cm.safeDispose();
					}
				} else {
					cm.playerMessage("無法再次加入遠征隊。");
					cm.dispose();
					return;
				}
			}
		} else {
			var eim = cm.getDisconnected("ZakumBattle");
			if (eim == null) {
				var squd = cm.getSquad("ZAK");
				if (squd != null) {
					cm.sendYesNo("已經遠征隊正在進行挑戰了.\r\n" + squd.getNextPlayer());
					status = 3;
				} else {
					cm.sendOk("遠征隊的挑戰已經開始.");
					cm.safeDispose();
				}
			} else {
				cm.playerMessage("無法再次加入遠征隊。");
				cm.dispose();
				return;
			}
		}
		break;
	case 1:
		if (mode == 1) {
			if (cm.registerSquad("ZAK", 5, " 已經成為了遠征隊隊長。如果你想加入遠征隊，請重新打開對話申請加入遠征隊。")) {
				cm.sendOk("你已經成為了遠征隊隊長。接下來的5分鐘，請等待隊員們的申請。");
			} else {
				cm.sendOk("未知錯誤.");
			}
		} else {
			cm.sendOk("如果你想要成為遠征隊隊長請務必告訴我！");
		}
		cm.safeDispose();
		break;
	case 2:
		if (!cm.reAdd("ZakumBattle", "ZAK")) {
			cm.sendOk("錯誤請稍後在嘗試...");
		}
		cm.safeDispose();
		break;
	case 3:
		if (mode == 1) {
			var squd = cm.getSquad("ZAK");
			if (squd != null && !squd.getAllNextPlayer().contains(cm.getPlayer().getName())) {
				squd.setNextPlayer(cm.getPlayer().getName());
				cm.sendOk("你已經成功登記為下一組..");
			}
		}
		cm.dispose();
		break;
	case 5:
		if (selection == 0) {
			if (!cm.getSquadList("ZAK", 0)) {
				cm.sendOk("由於未知的錯誤，操作失敗。");
				cm.safeDispose();
			} else {
				cm.dispose();
			}
		} else if (selection == 1) { // join
			var ba = cm.addMember("ZAK", true);
			if (ba == 2) {
				cm.sendOk("遠征隊員已經達到30名，請稍後再試。");
				cm.safeDispose();
			} else if (ba == 1) {
				cm.sendOk("你已經參加了遠征隊，請等候隊長指示。");
				cm.safeDispose();
			} else {
				cm.sendOk("你已經參加了遠征隊，請等候隊長指示。");
				cm.safeDispose();
			}
		} else { // withdraw
			var baa = cm.addMember("ZAK", false);
			if (baa == 1) {
				cm.sendOk("成功退出遠征隊。");
				cm.safeDispose();
			} else {
				cm.sendOk("你沒有參加遠征隊。");
				cm.safeDispose();
			}
		}
		break;
	case 10:
		if (selection == 0) {
			if (!cm.getSquadList("ZAK", 0)) {
				cm.sendOk("由於未知的錯誤，操作失敗。");
			}
			cm.safeDispose();
		} else if (selection == 1) {
			status = 11;
			if (!cm.getSquadList("ZAK", 1)) {
				cm.sendOk("由於未知的錯誤，操作失敗。");
				cm.safeDispose();
			}

		} else if (selection == 2) {
			status = 12;
			if (!cm.getSquadList("ZAK", 2)) {
				cm.sendOk("由於未知的錯誤，操作失敗。");
				cm.safeDispose();
			}

		} else if (selection == 3) { // get insode
			if (cm.getSquad("ZAK") != null) {
				var dd = cm.getEventManager("ZakumBattle");
				dd.startInstance(cm.getSquad("ZAK"), cm.getMap(), 160101);
				cm.dispose();
			} else {
				cm.sendOk("由於未知的錯誤，操作失敗。");
				cm.safeDispose();
			}
		}
		break;
	case 11:
		cm.banMember("ZAK", selection);
		cm.dispose();
		break;
	case 12:
		if (selection != -1) {
			cm.acceptMember("ZAK", selection);
		}
		cm.dispose();
		break;
	}
}
