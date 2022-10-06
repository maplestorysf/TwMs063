var status = -1;
var s = 0;
var t = 0;

function start() {
	action(1, 0, 0);
}

function action(mode, type, selection) {
	var c = cm.getPlayer();
	var VipMedal = c.getVipMedal() ? "#r顯示" : "#b未顯示";
	if (!c.isVip()) {
		cm.sendNext("歡迎來到辛巴谷v113");
		cm.dispose();
		return
	}
	if (mode == 1) {
		status++;
	} else if (mode == 0) {
		status--;
	} else {
		cm.dispose();
		return;
	}
	if (status == 0) {
		cm.sendSimple("#b本NPC提供選擇您腳下顯示的東西\r\n#rPS: 別人看您也如此顯示\r\n" +
			"#g#L1#VIP勳章	  : " + VipMedal + "\r\n" +
			"#d#L4#設定完成");
	} else if (status == 1) {
		s = selection;
		if (s == 1) {
			t = c.getVipMedal();
			t = !t;
			c.setVipMedal(t);
			cm.dispose();
			cm.openNpc(cm.getNpc());
		} else {
			c.fakeRelog();
			cm.dispose();
		}
	}
}