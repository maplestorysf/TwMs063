var status = 0;

function start() {
	status = -1;
	action(1, 0, 0);
}

function action(mode, type, selection) {
	if (mode == 1)
		status++;
	else
		cm.dispose();
	if (status == 0 && mode == 1) {
		var selStr = "請選擇一種擂台賽場地!\r\n#L100#兌換黃金楓葉標誌#l";
		var found = false;
		for (var i = 0; i < 3; i++) {
			if (getCPQField(i + 1) != "") {
				selStr += "\r\n#b#L" + i + "# " + getCPQField(i + 1) + "#l#k";
				found = true;
			}
		}
		if (cm.getParty() == null) {
			cm.sendSimple("請組隊再來找我。\r\n#L100#楓葉黃金標誌兌換#l");
		} else {
			if (cm.isLeader()) {
				var pt = cm.getPlayer().getParty();
				if (pt.getMembers().size() < 2) {
					if (cm.getParty() != null && 1 < cm.getParty().getMembers().size() && cm.getParty().getMembers().size() < (selection == 4 || selection == 5 || selection == 8 ? 4 : 3)) {
						if (checkLevelsAndMap(51, 120) == 1) {
							cm.sendOk("隊伍裡有人等級不符合。");
							cm.dispose();
						} else if (checkLevelsAndMap(51, 120) == 2) {
							cm.sendOk("在地圖上找不到您的隊友。");
							cm.dispose();
						} else if (checkLevelsAndMap(51, 120) == 3) {
							cm.sendOk("在頻道的找不到您的隊友。");
							cm.dispose();
						}
					}
				}
				if (found) {
					cm.sendSimple(selStr);
				} else {
					cm.sendSimple("目前沒有房間.\r\n#L100#兌換黃金楓葉標誌#l");
				}
			} else {
				cm.sendSimple("請叫你的隊長來找我\r\n#L100#楓葉黃金標誌兌換#l");
			}
		}
	} else if (status == 1) {
		if (selection == 100) {
			cm.sendSimple("#b#L0#50個閃亮的楓葉黃金標誌 = 休菲凱曼的混亂項鍊#l#k");
		} else if (selection >= 0 && selection < 3) {
			var mapid = 980030000 + ((selection + 1) * 1000);
			if (cm.getEventManager("cpq2").getInstance("cpq" + mapid) == null) {
				var party = cm.getParty().getMembers();
				if (cm.getParty() != null && party.size() == 3) {
					if (checkLevelsAndMap(51, 120) == 3) {
						cm.sendOk("隊伍裡有人等級不符合。");
						cm.dispose();
					} else if (checkLevelsAndMap(51, 120) == 2) {
						cm.sendOk("在地圖上找不到您的隊友。");
						cm.dispose();
					} else if (checkLevelsAndMap(51, 120) == 3) {
						cm.sendOk("在頻道的找不到您的隊友。");
						cm.dispose();
					} else {
						cm.getEventManager("cpq2").startInstance("" + mapid, cm.getPlayer());
						cm.dispose();
					}
				} else {
					cm.sendOk("隊伍裡人數不符合。");
					cm.dispose();
				}
			} else if (cm.getParty() != null && cm.getEventManager("cpq2").getInstance("cpq" + mapid).getPlayerCount() == cm.getParty().getMembers().size()) {
				if (checkLevelsAndMap(51, 120) == 1) {
					cm.sendOk("隊伍裡有人等級不符合。");
					cm.dispose();
				} else if (checkLevelsAndMap(51, 120) == 2) {
					cm.sendOk("在地圖上找不到您的隊友。");
					cm.dispose();
				} else if (checkLevelsAndMap(51, 120) == 3) {
					cm.sendOk("在頻道的找不到您的隊友。");
					cm.dispose();
				} else {
					var pt = cm.getPlayer().getParty();
					if (pt.getMembers().size() < 2) {
						cm.sendOk("需要 2 人以上才可以擂台！！");
						cm.dispose();
					} else {
						//Send challenge packet here
						var owner = cm.getChannelServer().getPlayerStorage().getCharacterByName(cm.getEventManager("cpq2").getInstance("cpq" + mapid).getPlayers().get(0).getParty().getLeader().getName());
						owner.addCarnivalRequest(cm.getCarnivalChallenge(cm.getChar()));
						//if (owner.getConversation() != 1) {
						cm.openNpc(owner.getClient(), 2042006);
						//}
						cm.sendOk("您的挑戰已經發送。");
						cm.dispose();
					}
				}
			} else {
				cm.sendOk("隊伍人數不相符。");
				cm.dispose();
			}
		} else {
			cm.dispose();
		}
	} else if (status == 2) {
		if (selection == 0) {
			if (!cm.haveItem(4001254, 50)) {
				cm.sendOk("很抱歉您並沒有#t4001129# #b50#k個");
			} else if (!cm.canHold(1122058, 1)) {
				cm.sendOk("請清出空間.");
			} else {
				cm.gainItem(1122058, 1);
				cm.gainItem(4001254, -50);
			}
			cm.dispose();
		}
	}
}

function checkLevelsAndMap(lowestlevel, highestlevel) {
	var party = cm.getParty().getMembers();
	var mapId = cm.getMapId();
	var Ch = cm.getClient().getChannel();
	var valid = 0;
	var inMap = 0;

	var it = party.iterator();
	while (it.hasNext()) {
		var cPlayer = it.next();
		if (!(cPlayer.getLevel() >= lowestlevel && cPlayer.getLevel() <= highestlevel) && cPlayer.getJobId() != 900) {
			valid = 1;
		}
		if (cPlayer.getMapid() != mapId) {
			valid = 2;
		}
		if (cPlayer.getChannel() != Ch) {
			valid = 3;
		}
	}
	return valid;
}

function getCPQField(fieldnumber) {
	var status = "";
	var event1 = cm.getEventManager("cpq2");
	if (event1 != null) {
		var event = event1.getInstance("cpq" + (980030000 + (fieldnumber * 1000)));
		if (event == null) {
			status = "擂台賽場地 " + fieldnumber + "(3v3)";
		} else if (event != null && (event.getProperty("started").equals("false"))) {
			var averagelevel = 0;
			for (i = 0; i < event.getPlayerCount(); i++) {
				averagelevel += event.getPlayers().get(i).getLevel();
			}
			averagelevel /= event.getPlayerCount();
			status = event.getPlayers().get(0).getParty().getLeader().getName() + "/" + event.getPlayerCount() + "人/平均等級  " + averagelevel;
		}
	}
	return status;
}
