var status = -1;

function start(mode, type, selection) {
	if (mode == 1) {
		status++;
	} else {
		if (status == 0) {
			qm.dispose();
			return;
		}
		status--;
	}
	if (status == 0) {
		if (qm.getPlayer().getLevel() >= 120) {
			qm.forceStartQuest();
			qm.sendNext("任務已經開始...");
		} else {
			qm.sendNext("貌似等級不足，無法挑戰.....");
		}
		qm.dispose();
	}
}

function end(mode, type, selection) {
	if (mode == 1) {
		status++;
	} else {
		if (status == 0) {
			qm.dispose();
			return;
		}
		status--;
	}
	if (qm.getPlayer().getMobCount() >= 100000) {
		if (qm.canHold(1142004) && !qm.haveItem(1142004, 1)) {
			qm.forceCompleteQuest(29400);
			qm.getPlayer().addMobCount(0);
			qm.sendNext("恭喜您完成精明的獵人任務。");
			qm.worldMessage("『稱號挑戰』：恭喜 " + qm.getChar().getName() + "  成功挑戰精明的獵人任務！");
			qm.gainItem(1142004, 1);
		} else {
			qm.sendNext("請確認是否背包空間是否足夠和是否已經得到相同的道具....");
			qm.dispose();
			return;
		}
	} else {
		qm.sendNext("看來您還沒完成阿... 目前已經擊殺了: " + qm.getPlayer().getMobCount() + " / 100000");
	}
	qm.dispose();
}
